# Firebase authentication setup

1. Create a Firebase project and register Android application `com.letitout.app`.
2. In Firebase Authentication, enable **Google** and **Anonymous** providers (only if you want guest access).
3. Add debug SHA-1 and SHA-256 fingerprints for local signing and register release signatures before publishing.
4. Download your Android app's `google-services.json` into `app/`. It is deliberately Git-ignored in this starter. Do not use real service account private keys in Android.
5. Retrieve the **Web client ID** from the Google provider/OAuth configuration, not the Android client ID. Supply it in `local.properties` with `webClientId=...`.
6. Ensure Gradle sync sees `google-services.json`, so the Google Services Gradle plugin is applied automatically.
7. Test **Continue with Google**, **Continue as guest**, **Sign out**, **Delete account**, and guest→Google account linking (Settings → **Keep your space with Google** while signed in as a guest) on a real Android device. Also test upgrading a guest to a Google account that already exists in your project (the app should switch to that account), closing the account picker (no error should show), and signing out as a guest (the anonymous user should disappear from the Firebase console).
8. On the backend, create a restricted Firebase Admin service account and set `GOOGLE_APPLICATION_CREDENTIALS` and `FIREBASE_PROJECT_ID` as **server environment variables**. Never copy the service account JSON to Android.
9. Account collisions, identity provider cancellation and the recent-sign-in requirement for deletion are handled in `auth/AuthManager.kt`. That file has not been compiled or run since these changes, so verify all three on a device before a production release.

Official guide: https://firebase.google.com/docs/auth/android/google-signin
