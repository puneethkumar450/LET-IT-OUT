# Git and GitHub

Target: `https://github.com/puneethkumar450/LET-IT-OUT.git`

## Current state (checked 22 October 2026)

The GitHub repository exists and has history. `git ls-remote origin` shows two branches:

- `main` at `2261c65`, "Redesign Android UI and fix Back navigation".
- `ui-changes` at `2d84981`: three commits on top of that `main`. They are the lavender redesign (`729410a`), mood stickers and the Firebase connection (`3acaf10`), and local release signing with Firebase fingerprints (`2d84981`), plus README and `docs/FIREBASE_SETUP.md` updates.

The human-companion work (persona, gentle support card, feeling check, daily check-in, backend split and tests) is on the local branch `claude/human-companion-fixes`. It starts from `main` (`2261c65`). **It has not been pushed**, and it does not include `ui-changes`; the two branches change many of the same Android files, so one has to be ported onto the other.

**Expect conflicts when you bring the two lines of work together.**

- Both split `MainActivity.kt` into screen files, but differently: `ui/ChatScreen.kt`, `ui/HomeScreens.kt`, `ui/IntroScreens.kt`, `ui/SettingsScreens.kt` and `ui/Components.kt` on `ui-changes`, versus `ui/screens/*.kt` and `ui/components/Common.kt` here.
- Both also change `AuthManager.kt`, `Models.kt`, `AppViewModel.kt`, `ChatApi.kt`, `Preferences.kt`, `AppVisuals.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, the backend `Main.kt` and the README.
- `ui-changes` has a support card with a 112 dial button for flagged replies, plus an optional intensity rating sent to the backend. The owner's later decision replaces that card with the gentle doctor / Tele-MANAS card: no crisis mode, and 112 only on the help page and, at the model's judgement, in the extra-care prompt (see [SAFETY.md](SAFETY.md)).

Merge by hand and review each conflict; do not resolve by taking one side wholesale. After merging, rebuild and rerun both test suites (see the README).

## Existing local project

Open Terminal **in your original project** and inspect before changing anything:

```bash
pwd
git status
git remote -v
git branch --show-current
git fetch origin
git log --oneline --graph --all
```

If a Git origin already exists, **do not overwrite it blindly**. If `origin` does not exist and the folder already has `.git`:

```bash
git remote add origin https://github.com/puneethkumar450/LET-IT-OUT.git
git ls-remote origin HEAD
```

If GitHub requests authorization, use your configured SSH key, Git Credential Manager or GitHub CLI (`gh auth login`) as appropriate. Confirm private repository access via your browser and GitHub account.

## Fresh checkout on another machine

```bash
git clone https://github.com/puneethkumar450/LET-IT-OUT.git
cd LET-IT-OUT
git branch -a
```

Then create `local.properties`, add your own `app/google-services.json`, and set the backend environment variables as described in the README and [FIREBASE_SETUP.md](FIREBASE_SETUP.md). These files are Git-ignored on purpose.

## Sharing work

Push a feature branch and open a pull request into `main` rather than committing to `main` directly:

```bash
git push -u origin YOUR_BRANCH
```

Do **not** force-push. Fetch and review the remote branch before committing or pushing. Never commit API keys. `.gitignore` already excludes `local.properties`, `app/google-services.json`, keystores and `.env` files.
