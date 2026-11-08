# Jarvis Android — cloud APK build

This is an **untested starter**. The GitHub workflow attempts to compile and test it; no APK is included or verified.

1. Create a **private** GitHub repository and upload the contents of this `jarvis-android` directory to its root (including the hidden `.github/workflows/android.yml` file). Do not upload the parent directory alone.
2. Open **Actions → Build Jarvis APK → Run workflow**.
3. If successful, download the **jarvis-debug-apk** artifact, extract the ZIP, and install the APK on your Android phone. If it fails, share the first error in the build log.

**Before voice activation can work:** Obtain a Picovoice AccessKey and an Android-specific custom `Hey-Jarvis_en_android.ppn` keyword file; put the latter in `app/src/main/assets/` and rebuild. Configure a privately hosted Gemini proxy. Never commit keys or tokens. Android 16 foreground microphone restrictions and device battery policies still apply. Some actions (calling, opening apps) are currently coded without sufficient user confirmation and may be blocked from background execution. Do not rely on the app while driving until tested.

**Known omissions:** `SPEC.md` referenced in the build brief is not in the supplied ZIP. Reminders, persistent history, notification reading, OAuth and other features listed in `BUILD_BRIEF.md` are not implemented.
