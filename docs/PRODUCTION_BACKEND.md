# Production backend deployment

The backend can run on Cloud Run with a provider-assigned HTTPS URL. A custom domain is optional. **Production chat is disabled by default** (`APP_ENV=production` with no `CHAT_ENABLED=true`) while the [release safety review](SAFETY.md) is outstanding. Deploying the container is not approval to serve real emotional-support conversations.

## What the deployment contains

- Root [Dockerfile](../Dockerfile): Java 21, multi-stage build, non-root runtime user. The source upload and Docker build exclude local secrets through `.gcloudignore` and `.dockerignore`.
- Firebase ID-token verification, including revoked/deleted account checks in production, using Cloud Run's service identity; do **not** upload `backend/firebase-admin.json` or set `GOOGLE_APPLICATION_CREDENTIALS` on Cloud Run. [Firebase Admin setup](https://firebase.google.com/docs/admin/setup), [Cloud Run service identity](https://docs.cloud.google.com/run/docs/securing/service-identity), [revocation checks](https://firebase.google.com/docs/auth/admin/manage-sessions).
- Groq key from Secret Manager, pinned to a specific version. [Cloud Run secrets](https://docs.cloud.google.com/run/docs/configuring/services/secrets).
- Firestore transaction for shared hourly limits across instances: 20 requests per Firebase UID and 300 total by default. Only a SHA-256 digest of the UID, counters, and an expiry timestamp are stored; no chat text is stored. Requests fail closed with 503 if rate-limit storage fails.

## One-time cloud setup

1. Attach billing to Firebase project `let-it-out-d661a`. Choose a region (the examples below use `asia-south1`). Enable Cloud Run, Cloud Build, Artifact Registry, Secret Manager, and Firestore APIs.
2. Create a **Firestore Native mode** database named `(default)` in the same project. Create a TTL policy on collection group `chat_rate_limits`, timestamp field `expiresAt`. The backend sets this to 48 hours after each hourly bucket; Firestore deletes expired documents asynchronously. [Firestore TTL setup](https://docs.cloud.google.com/firestore/native/docs/ttl).
3. In Secret Manager, create `let-it-out-groq` and add the existing Groq API key as version `1`. Do not put the key in a shell command, source archive, APK, or Git.
4. Create a dedicated service account such as `let-it-out-chat@let-it-out-d661a.iam.gserviceaccount.com`. Grant it `roles/datastore.user` and `roles/firebaseauth.viewer` on the project, and `roles/secretmanager.secretAccessor` on **only** the Groq secret. The viewer role permits the production revocation lookup without account-edit permissions. Cloud Run uses this identity for Firebase Admin and Firestore. [Firebase Auth IAM roles](https://docs.cloud.google.com/iam/docs/roles-permissions/firebaseauth).
5. Set a Google Cloud billing budget and provider-side Groq spending or rate alerts. The in-app limits are a second layer, not a billing cap. Cloud Run's maximum instance setting helps control cost but is not a hard spend ceiling. [Cloud Run scaling limits](https://docs.cloud.google.com/run/docs/configuring/max-instances-limits).

## Deploy a disabled service for infrastructure checks

From the repository root, with the Google Cloud CLI signed into the intended account:

```bash
gcloud run deploy let-it-out-chat \
  --source . \
  --project let-it-out-d661a \
  --region asia-south1 \
  --service-account let-it-out-chat@let-it-out-d661a.iam.gserviceaccount.com \
  --allow-unauthenticated \
  --set-env-vars APP_ENV=production,CHAT_ENABLED=false,FIREBASE_PROJECT_ID=let-it-out-d661a \
  --set-secrets GROQ_API_KEY=let-it-out-groq:1 \
  --max-instances 3 \
  --concurrency 20 \
  --memory 1Gi \
  --timeout 45s
```

`--source .` uses the root Dockerfile. The URL is public at the network layer because an Android Firebase ID token is not a Cloud Run IAM token; the backend checks the Firebase token before any enabled chat request. While chat is disabled, `/health` returns 200 and `/ready` and `/v1/chat` return 503. [Cloud Run end-user authentication](https://docs.cloud.google.com/run/docs/tutorials/identity-platform).

After independent safety/privacy review and a closed-beta authorization, set `CHAT_ENABLED=true` on the service. Confirm `/ready` returns 200, then test guest and Google sign-in plus real chat through a **release-signed** Android build. The Android release URL must be the Cloud Run HTTPS URL with a trailing slash, for example:

```bash
./gradlew :app:assembleRelease -PapiBaseUrl=https://YOUR_CLOUD_RUN_URL/
```

This Gradle command overrides the current emulator-only fallback. The Android release manifest refuses cleartext HTTP. Add the release and, if using Play App Signing, Play signing SHA fingerprints to Firebase before testing Google sign-in.

## Rollout checks

- Verify the source upload contains no `.env`, Admin JSON, keystore, or `local.properties`; both ignore files are explicit because a Docker-only ignore does not protect Cloud Build uploads.
- Confirm Firestore counters rise on successful requests, reject at the limits, and expire under the TTL policy. Anonymous users can create new identities; the global bucket limits total requests but is not a substitute for abuse monitoring or App Check.
- Confirm invalid/missing Firebase tokens return 401; oversized requests return 413; provider and Firestore outages produce safe errors without logging messages or keys.
- Monitor 5xx rates, 429 rates, Cloud Run cost, Firestore cost, and Groq usage without logging chat contents. Rotate the Groq secret version and redeploy if exposed.
- Complete the items in [SAFETY.md](SAFETY.md), including expert review, multilingual crisis evaluation, localized support resources, consent, and data protection, before any public launch.
