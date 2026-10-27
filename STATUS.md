# Verification and feature status

Last scaffolded: 2026-10-27. These statuses describe source implementation, not successful compilation.

| Feature | Status |
|---|---|
| New project / package `com.nezuko.ai` | Implemented, build not verified |
| Compose assistant UI | Implemented, build not verified |
| Gemini HTTP generation | Implemented, not integration-tested |
| OpenRouter HTTP generation | Implemented, not integration-tested |
| Provider test and error mapping | Implemented, not integration-tested |
| API key encryption at rest | Implemented, not security-audited |
| Room messages, memories, tasks | Implemented, migrations/persistence not tested |
| Text conversation | Implemented, build/provider unverified |
| Speech recognition | Partially implemented: launches system recognizer chooser; callback integration is not wired |
| Text-to-speech | Implemented, device not tested |
| User-approved memory UI | Implemented, retrieval included as prompt context; build unverified |
| Tasks | Partially implemented: CRUD only; no timed task UI |
| Alarm scheduling helper | Partially implemented: helper exists but not wired to task UI or reboot recovery |
| Notification permission flow | Not implemented |
| App launch / dialer / HTTPS / settings helpers | Implemented as helpers, not wired into agent tool execution |
| Confirmation policy / structured planner / bounded tool loop | Not implemented |
| Calls/messages confirmation UI | Not implemented |
| Calendar integration | Not implemented |
| Notification-listener access | Not implemented |
| Accessibility automation | Not implemented |
| Foreground listening service / wake phrase | Not implemented |
| Kannada/Hindi/Hinglish recognition validation | Not tested; depends on device speech engine |
| Unit tests | Basic config tests written; not run |
| Instrumentation tests | Not implemented |
| Gradle wrapper JAR | Blocked: network DNS unavailable in scaffold environment |
| APK generation | Blocked: no Android SDK, Gradle installation, or wrapper JAR |
| Installation / real-device tests | Not performed: no ADB/device available |

## Environment inspection

- OS: Linux x86_64
- Java: OpenJDK 21
- Gradle executable: not found
- Android SDK / `sdkmanager`: not found
- ADB: not found
- Network retrieval of Gradle wrapper artifacts: failed due DNS resolution failure

## Known issues to fix before acceptance

- Connect Android speech recognition results back into app state via `ActivityResultContracts` or `SpeechRecognizer` listener.
- Replace `MainScope()` use with lifecycle-owned coroutine scope/ViewModel.
- Wire reminder scheduling and notification permission request to task UI; persist alarm identity and reschedule after reboot if needed.
- Add model/provider-specific parsing tests and database instrumentation tests.
- Implement robust agent planner, structured tool schemas, permission/confirmation gates, action history, cancellation, and bounded execution.
- Add clear-all confirmation dialogs and search/edit/pin memory UI.
- Review provider API versions and model availability at implementation time.
- Build and run all tests on an SDK-equipped host, then install and verify on a physical Android device.
