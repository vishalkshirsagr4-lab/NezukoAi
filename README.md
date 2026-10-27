# Nezuko AI

A clean native Android Kotlin/Jetpack Compose starter for a personal AI assistant. Package: `com.nezuko.ai`.

## Current implementation status

This is a **starter implementation**, not yet a release-ready agent. See `STATUS.md` for a candid feature-by-feature report.

Included in source:
- Compose dark/pink assistant UI with Voice, Chat, Memory, Tasks, and Settings tabs.
- Gemini and OpenRouter HTTPS text-generation adapters with timeouts and user-readable HTTP/network errors.
- Provider credentials encrypted at rest using AES-GCM with a key held by Android Keystore.
- Room persistence for messages, approved memories, and tasks.
- Android TTS speech output and Android's system speech-recognition chooser for push-to-talk.
- Validated HTTPS URL, dialer, app-launch, and Settings intent helpers.
- Reminder receiver and alarm scheduling helper.
- Basic unit tests.

## Prerequisites

- JDK 17 or 21 (the Android Gradle Plugin uses a compatible JDK).
- Android Studio with Android SDK Platform 35 and Build Tools installed.
- Network access for Gradle dependency resolution.
- Gradle 8.11.1 is configured by the wrapper properties.

The provided environment used to scaffold this project had Java 21 but **no Android SDK, no installed Gradle, no ADB, and no network/DNS access**. As a result, the Gradle wrapper JAR could not be fetched and the APK could not be built or tested here. Restore `gradle/wrapper/gradle-wrapper.jar` from a trusted Gradle 8.11.1 wrapper generation, or open in Android Studio and let it regenerate the wrapper. Then sync and build.

## Build

From this directory in an Android SDK-equipped environment:

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Expected APK after a successful build: `app/build/outputs/apk/debug/app-debug.apk`.

## Provider setup

1. Create an API key through the provider's official console.
2. Install and open the app; go to **Settings**.
3. Choose Gemini or OpenRouter, enter the model ID and key, and tap **Save**.
4. Use **Test connection** before chatting.
5. The app sends conversation context and relevant approved memory text to the chosen provider for a response. Do not save secrets or highly sensitive information as memory.

Default model IDs are editable and may change in provider availability. Verify current model names and quotas with the provider.

## Voice

Push-to-talk launches Android's speech-recognition UI. Recognition availability, language, and online/offline behavior depend on the installed speech service. TTS uses the device's configured engine. The assistant does not implement always-on listening or a wake phrase.

## Privacy and security notes

- API keys are encrypted using AES-GCM with an Android Keystore key before persistence in DataStore.
- TLS/HTTPS is required for AI requests and URL-opening helper.
- No analytics are included. Avoid sharing private text with a provider unless you accept its data handling.
- Local Room database content is **not separately encrypted** in this version.
- `allowBackup=false` disables Android app backup for this app.
- No notification listener or accessibility service is declared.
- Calls/messages are not sent automatically. The dialer helper opens the dialer; the app does not silently place calls or send messages.

## Troubleshooting

- **Build fails:** use Android Studio with SDK Platform 35 and a compatible JDK; sync Gradle dependencies.
- **Invalid key/model:** re-check provider key access and exact model identifier.
- **Quota/rate limit:** wait or check provider billing/quota.
- **No speech recognition:** install/enable a speech recognition service in Android settings and grant microphone permission.
- **No TTS audio:** configure a text-to-speech engine in Android settings.
- **Reminders:** Android 13+ requires notification permission; exact alarms may be restricted. The current Tasks UI only creates untimed tasks.

## Distribution

Do not distribute as production-ready until the build and automated/device test suite passes, accessibility and privacy review is completed, reminder UI is wired to scheduling, and the incomplete features in `STATUS.md` are addressed.
