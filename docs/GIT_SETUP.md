# Git and GitHub

Target: `https://github.com/puneethkumar450/LET-IT-OUT.git`

## Branches (checked 23 October 2026)

`git ls-remote origin` shows three branches:

- `main` at `2261c65`, "Redesign Android UI and fix Back navigation".
- `ui-changes` at `2d84981`: the lavender redesign, three commits on top of that `main`. They are the lavender design (`729410a`), mood stickers and the Firebase connection (`3acaf10`), and local release signing with Firebase fingerprints (`2d84981`), plus README and `docs/FIREBASE_SETUP.md` updates.
- `claude/human-companion-fixes` at `64efa3f`: the human-companion work (persona, gentle support, feeling check, daily check-in, reply reporting, backend split and tests) in three commits on top of `main`, so it uses the older design. The environment that wrote it did not push it, but it is on GitHub now.

One more branch existed only in a local clone when this was written:

- `claude/lavender-companion`: this merge. It starts from `ui-changes` (`2d84981`) and ports the companion work onto the lavender design by hand. It was not pushed from the environment that wrote it.

## What the merge kept

`claude/lavender-companion` keeps everything from `ui-changes`: the design, screens and navigation (splash, onboarding, sign-in, home with stickers, mood check-in with the intensity rating, chat, settings, Privacy & Help), its file layout (`ui/IntroScreens.kt`, `ui/HomeScreens.kt`, `ui/ChatScreen.kt`, `ui/SettingsScreens.kt`, `ui/Components.kt`, `ui/AppVisuals.kt`), its icons and palettes, release signing, and reading the Web client ID from `google-services.json`.

From `claude/human-companion-fixes` it takes:

- the backend, with two changes: it keeps the optional `intensity` field that `ui-changes` sends (a hint in the prompt, ignored outside 1–10, never a reason for extra care), and its extra-care prompt no longer mentions 112;
- `reminder/**`, `data/ChatErrors.kt`, `data/AppModule.kt`, the gateway interfaces, the ViewModel behaviour (dropped late replies, retry without duplicates, feeling check, reporting, sign-out clean-up) and the unit tests, ported to the lavender surface;
- the notification icon, the backup and device-transfer exclusions, the text-to-speech query in the manifest, and hiding the Recents preview.

Changed on purpose while merging:

- The `ui-changes` support card with a 112 row, and the backend's canned hard-stop reply, are gone. A heavy reply now carries a gentle doctor note and one Tele-MANAS 14416 row, at most once every 4 replies, and 112 appears only on Privacy & Help (see [SAFETY.md](SAFETY.md)).
- The companion branch's separate support card with **Not now** became that note inside the reply bubble, so there is nothing to dismiss.
- The companion branch's `ui/screens/*.kt` and `ui/components/Common.kt` layout was not used.

`claude/human-companion-fixes` is superseded by `claude/lavender-companion`. Do not merge both: they change the same Android files in different layouts. After `./gradlew :app:assembleDebug :app:testDebugUnitTest` pass on `claude/lavender-companion`, open a pull request from it into `ui-changes` or `main` and review each conflict by hand.

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

Do **not** force-push. Fetch and review the remote branch before committing or pushing. Never commit API keys. `.gitignore` already excludes `local.properties`, `app/google-services.json`, keystores, `.env` files and `reports*.jsonl`.
