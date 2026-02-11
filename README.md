# Voltflow

Voltflow is a Jetpack Compose Android application for viewing and paying electricity bills.

Quick notes:
- Minimum SDK: 24 (Android 7.0+)
- Compose + Material3 UI

Local build (on your machine):

```bash
./gradlew assembleDebug
```

To push this workspace to GitHub:

```bash
git init
git add .
git commit -m "checkpoint: initial voltflow workspace"
# use gh CLI (recommended) after authenticating:
# gh repo create YOUR_USER/voltflow --public --source=. --push
# or add remote and push manually
# git remote add origin https://github.com/YOUR_USER/voltflow.git
# git branch -M main
# git push -u origin main
```

Next steps implemented by assistant:
- Mock payment flow and UI wiring in `MainActivity.kt`
- Updated strings and created this README and `.gitignore`
- `PROGRESS.md` added with next tasks

Release & signing notes:

1. Create a signing key (keep it safe):

```bash
keytool -genkey -v -keystore release-keystore.jks -alias voltflow_key -keyalg RSA -keysize 2048 -validity 10000
```

2. Add signing config to `~/.gradle/gradle.properties` (DO NOT COMMIT):

```
RELEASE_STORE_FILE=/path/to/release-keystore.jks
RELEASE_KEY_ALIAS=voltflow_key
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_PASSWORD=your_key_password
```

3. Configure `app/build.gradle.kts` signing config (example) and build release AAB:

```bash
./gradlew bundleRelease
```

CI note:
- A GitHub Actions workflow was added at `.github/workflows/android-ci.yml` to build and run unit tests on push/PR. Instrumented tests require a connected device and are not executed by the workflow.

If you'd like, I can add a sample `signingConfigs` block to `app/build.gradle.kts` that reads the properties above.
I also added a helper script `scripts/run_checks.ps1` to run basic Gradle checks on Windows and capture logs.
