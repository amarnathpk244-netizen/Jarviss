# Claude Code build brief: JARVIS (Android)

Read `SPEC.md` (the owner's full specification; copy the original file into the repo root) and this
project. The code here is an UNTESTED starter written without an Android SDK. Treat it as a draft.

## Your job
Make this a working, installable Android app that satisfies SPEC.md, then report verified results honestly.
Never claim a feature works unless you built it and tested it (emulator or device).

## Do first
1. Install/confirm JDK 17, Android SDK (API 36), and run `gradle wrapper` (no wrapper is included).
2. Fix versions (AGP / Kotlin / Compose BOM / Porcupine) to whatever resolves; get `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` green.
3. Report exact compile errors you fixed.

## Already drafted (unverified)
Foreground microphone service, Porcupine wake-word wrapper, SpeechRecognizer (en-IN / ml-IN), TTS (Malayalam when
reply is in Malayalam), offline commands (open app, call contact, navigate, alarm, torch, battery, volume, camera,
settings), Gemini via a proxy in `/proxy`, encrypted prefs, one Compose screen, unit tests for text parsing.

## Still to build (from SPEC.md)
- Command history, tasks/reminders (Room + WorkManager), personal memory, permissions screen, AI settings, dedicated driving-mode screen.
- Gemini function calling so reminders, calendar, notes and shopping lists run as tools; contextual memory.
- Reminders in Malayalam ("നാളെ രാവിലെ 6 മണിക്ക് ഓർമ്മിപ്പിക്കണം") and Manglish.
- SMS (with confirmation), WhatsApp chat open, media control, notification reading (NotificationListenerService, explicit permission), incoming-call announcements, Bluetooth headset routing.
- Confirmation step for sensitive actions; "emergency stop"; delete-history control.
- Project workspaces: Dental Buddys, MOFU, freelancing (via authorized integrations only).
- Gmail / Google Calendar via OAuth.

## Known Android constraints to handle (and document honestly)
- Background activity starts are blocked on Android 10+. Launching apps/Maps/calls from the service needs the
  "Display over other apps" permission or a full-screen-intent notification. Implement and test it.
- Android 14+ forbids starting a microphone foreground service from the background. Start it from a visible Activity
  (or boot-time exemptions where allowed); document the restart behaviour.
- Android 16 and OEM battery savers (Xiaomi, Samsung, Realme) may kill the service; add the battery-optimisation prompt.
- Wake word and SpeechRecognizer cannot share the mic: the service stops one before starting the other (already done); verify it.
- "Hey Jarvis" needs a custom .ppn from console.picovoice.ai in `app/src/main/assets/` plus a free AccessKey. Verify how
  Porcupine resolves asset paths. The wake word stays English; Malayalam is for commands.
- Gemini key lives only on the proxy (`proxy/server.js`). Deploy it (Cloud Run, Fly, Supabase edge function) and set
  `GEMINI_API_KEY` and `APP_TOKEN`. A shared token in the app is weak: add Firebase App Check or user auth.
- Signing: produce a debug APK; explain how to make a release keystore. Do not commit secrets.

## Deliverables
Source, debug APK, test results, a README with install steps (`adb install` and sideloading), and a
"verified / not verified / needs configuration" table for every SPEC.md item.
