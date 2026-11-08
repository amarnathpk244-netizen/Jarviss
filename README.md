# Jarvis (Android starter, UNTESTED)

Written without an Android SDK, so nothing here has been compiled or run. Do not treat any feature as working
until it is built and tested. See BUILD_BRIEF.md for next steps (best done with Claude Code on a computer).

## Quick start
1. Open this folder in Android Studio (it will create the Gradle wrapper and sync).
2. Train a "Hey Jarvis" keyword at console.picovoice.ai (Android), save it as `app/src/main/assets/Hey-Jarvis_en_android.ppn`.
3. Deploy `proxy/server.js` (Node 18+) with `GEMINI_API_KEY` and `APP_TOKEN`; put its URL in the app.
4. Run on a real phone, tap "Enable Jarvis", paste your Picovoice key, allow the microphone.
5. Say "Hey Jarvis", then e.g. "open WhatsApp", "call home", "navigate to college", "set alarm 6:30 am", "flashlight on".

Place your original SPEC.md in this folder for Claude Code.
