# Firebase authentication setup

## Current development project

- Firebase project: `let-it-out-d661a` ([console](https://console.firebase.google.com/project/let-it-out-d661a/overview)), Spark plan.
- Android app: `com.letitout.app` (`Let It Out Android`).
- Google and Anonymous sign-in providers are enabled.
- This machine's debug SHA-1 and SHA-256 fingerprints are registered. Add the release and Google Play signing fingerprints before distributing a release build.
- The local release keystore uses alias `puneeth`. Add its SHA-1 `19:32:8C:19:BC:6B:1D:BA:97:BA:A9:75:E5:EB:43:C9:63:5A:EB:BF` and SHA-256 `87:23:A2:C8:40:C6:09:D8:89:F7:C2:82:F1:60:45:3B:A3:B8:12:2F:A2:CF:7E:FF:42:7E:68:1A:CD:29:8F:92` in Firebase Project settings for the Android app. These are from the signed release APK.
- The updated `app/google-services.json` includes both Android and Web OAuth clients and is ignored by Git. Download it again from Firebase Project settings if this local copy is lost.

The Android debug build, anonymous guest sign-in, and guest-to-Google account linking were verified on an emulator on 19 October 2026, on the `ui-changes` branch. After account selection, Settings showed the linked Google email. The first account-picker attempt stalled in the emulator's Credential Manager; the retry completed. A fresh Google sign-in and the full flow on a physical device remain to be tested. `claude/lavender-companion` changes `auth/AuthManager.kt` (switching to a Google account that already exists, closing the picker without an error, removing the guest account on sign-out) and has not been built yet, so repeat these checks on it.

1. Create a Firebase project and register Android application `com.letitout.app`.
2. In Firebase Authentication, enable **Google** and **Anonymous** providers (only if you want guest access).
3. Add debug SHA-1 and SHA-256 fingerprints for local signing and register release signatures before publishing.
4. Download your Android app's `google-services.json` into `app/`. It is deliberately Git-ignored in this starter. Do not use real service account private keys in Android.
5. After enabling Google sign-in and adding the app's SHA-1, download the **updated** `google-services.json`. It must contain a Web OAuth client (`client_type: 3`) for `com.letitout.app`. The Google Services Gradle plugin generates `default_web_client_id`, which the app reads automatically. `webClientId` in `local.properties` remains an optional local override if the generated value is missing.
6. Ensure Gradle sync sees `app/google-services.json`, so the Google Services Gradle plugin is applied automatically. The app ID in that file must match `com.letitout.app`.
7. Test **Continue with Google**, **Continue as guest**, **Sign out**, **Delete account and data**, and guest→Google account linking (Settings → **Link** on the Guest card) on a real Android device. Also test linking a guest to a Google account that already exists in your project (the app should switch to that account), closing the account picker (no error should show), and signing out as a guest (the anonymous user should disappear from the Firebase console).
8. On the backend, create a restricted Firebase Admin service account and set `GOOGLE_APPLICATION_CREDENTIALS` and `FIREBASE_PROJECT_ID` as **server environment variables**. Never copy the service account JSON to Android.
9. Account collisions, identity provider cancellation and the recent-sign-in requirement for deletion are handled in `auth/AuthManager.kt`. That file has not been compiled or run on this branch, so verify all three on a device before a production release.

Official guide: https://firebase.google.com/docs/auth/android/google-signin
