# Firebase authentication setup

## Current development project

- Firebase project: `let-it-out-d661a` ([console](https://console.firebase.google.com/project/let-it-out-d661a/overview)), Spark plan.
- Android app: `com.letitout.app` (`Let It Out Android`).
- Google and Anonymous sign-in providers are enabled.
- This machine's debug SHA-1 and SHA-256 fingerprints are registered. Add the release and Google Play signing fingerprints before distributing a release build.
- The updated `app/google-services.json` includes both Android and Web OAuth clients and is ignored by Git. Download it again from Firebase Project settings if this local copy is lost.

The Android debug build, anonymous guest sign-in, and guest-to-Google account linking were verified on an emulator on 19 October 2026. After account selection, Settings showed the linked Google email. The first account-picker attempt stalled in the emulator's Credential Manager; the retry completed. A fresh Google sign-in and the full flow on a physical device remain to be tested.

1. Create a Firebase project and register Android application `com.letitout.app`.
2. In Firebase Authentication, enable **Google** and **Anonymous** providers (only if you want guest access).
3. Add debug SHA-1 and SHA-256 fingerprints for local signing and register release signatures before publishing.
4. Download your Android app's `google-services.json` into `app/`. It is deliberately Git-ignored in this starter. Do not use real service account private keys in Android.
5. After enabling Google sign-in and adding the app's SHA-1, download the **updated** `google-services.json`. It must contain a Web OAuth client (`client_type: 3`) for `com.letitout.app`. The Google Services Gradle plugin generates `default_web_client_id`, which the app reads automatically. `webClientId` in `local.properties` remains an optional local override if the generated value is missing.
6. Ensure Gradle sync sees `app/google-services.json`, so the Google Services Gradle plugin is applied automatically. The app ID in that file must match `com.letitout.app`.
7. Test **Continue with Google**, **Continue as guest**, **Sign out**, **Delete account**, and guest→Google account linking on a real Android device.
8. On the backend, create a restricted Firebase Admin service account and set `GOOGLE_APPLICATION_CREDENTIALS` and `FIREBASE_PROJECT_ID` as **server environment variables**. Never copy the service account JSON to Android.
9. Handle account collisions, identity provider cancellation, and Firebase reauthentication before a production release.

Official guide: https://firebase.google.com/docs/auth/android/google-signin
