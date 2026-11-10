# Jarvis voice fix v2

## What changed
- Wake recognition accepts English and Malayalam spellings with a word boundary.
- Wake listening uses English (India); the next command uses the selected Malayalam or English setting.
- Partial recognition updates the screen without cancelling an unfinished utterance.
- Final recognition alternatives are checked for a supported command.
- Cancelled recognizer callbacks cannot run commands or restart obsolete sessions.
- One pending restart, session timeout, bounded error retries, and a command timeout prevent overlapping sessions and a permanently armed command state.
- Listening resumes after actual TTS completion/error. Missing TTS no longer prevents microphone startup; unsupported Malayalam TTS falls back to English text and voice.
- Added Malayalam time/date and app aliases, camera/settings, navigation, numeric dialler, cancellation, and shutdown.
- Added TALK NOW, selectable command language, status/errors, and permission feedback. Notification denial does not block microphone use.
- Unified GitHub build: Gradle wrapper, Android SDK 36, tests, and one named APK artifact. The duplicate workflow is archived outside the workflow directory.

## Apply in Termux
Save the updated source ZIP as Download/Jarvis-Current-Source.zip. Install unzip and Python if needed (`pkg install unzip python`).

```bash
mkdir -p ~/jarvis-voice-fix-v2
unzip -o ~/storage/downloads/Jarvis-Current-Source.zip -d ~/jarvis-voice-fix-v2
bash ~/jarvis-voice-fix-v2/APPLY_VOICE_FIX.sh ~/Jarviss
```

The installer backs up touched files, copies only the fix files, and prints exact git commands. Run those commands to push a new branch; the APK workflow runs on that branch. It does not require merging into main first.

GitHub → Jarviss → Actions → **Build Jarvis APK** → latest successful branch run → **Jarvis-Voice-Fix-APK**. Download/extract the artifact ZIP and install `app-debug.apk`. This source archive is not an APK. Use the same existing debug signing key if Android requires an update; uninstalling an older differently signed build clears its local data.

## Phone check (parked)
1. Enable your Android speech recognition service and internet. Allow microphone access. Enable notification permission for the app-opening fallback.
2. Open Jarvis; select **Malayalam commands**; tap START. Wait for the ready reply/status.
3. Say **Hey Jarvis** in English. Wait for the reply. Say **YouTube തുറക്കൂ** or **സമയം എത്രയായി**. Keep Jarvis visible when testing direct app opening.
4. Press TALK NOW and try the same command. This separates wake recognition from command recognition.
5. If Malayalam is unavailable, select **English commands**, stop/start, and try **Open YouTube** / **What is the time?** Check the status for microphone/network/language errors.
6. Repeat commands, cancel, stop/start, deny microphone/notification permission separately, and test a missing app.
7. Test background behavior separately. Android restricts app launches from a background service. The app posts a notification to open the requested activity when Jarvis is not visible. This is not fully hands-free background app control.

## Verification and remaining limits
28 host-side regression checks compile and pass against the actual Java parser (English/Malayalam wake variants, wake boundary, inline commands, app aliases, dates, time, navigation, dialling, cancellation, unknown commands). Run:

```bash
javac -encoding UTF-8 -d /tmp/jarvis-tests app/src/main/java/com/jarvis/assistant/VoiceCommands.java tools/VoiceRegression.java
java -cp /tmp/jarvis-tests VoiceRegression
```

The Android build was attempted here but Gradle distribution download failed with `Network is unreachable`. No APK was produced, and Android/Kotlin compilation and physical-device speech behavior remain unverified. GitHub workflow runs Android build and existing unit tests.

This fixes the existing Android SpeechRecognizer approach. It still relies on the phone's speech service and may use a network connection. Sherpa assets/native libraries are retained but are not wired into an offline keyword detector. Android SpeechRecognizer is not designed for reliable continuous low-power wake listening; truly always-on offline wake detection remains separate unfinished work. Contact-name lookup, arbitrary AI tasks, and fully hands-free background/lock-screen launches are not implemented. The existing Gemini proxy placeholder is unchanged and not used by this command service.

References:
- https://developer.android.com/reference/android/speech/SpeechRecognizer
- https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener
- https://developer.android.com/guide/components/activities/background-starts
