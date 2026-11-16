# Focus Flow

An Android app that tracks screen time and enforces the limits you set: daily limits per app, schedules, session limits, a blocker for Shorts and Reels, and a smart alarm that keeps distracting apps locked until you are up.

Built with Kotlin and Jetpack Compose. Preparing for release on Google Play.

Case study: [vidanstojic.me/projects/focus-flow](https://vidanstojic.me/projects/focus-flow/)

## Features

- **Usage you can trust.** Screen time is rebuilt from the system's raw usage events, split by hour and stored locally, so daily and hourly numbers match reality to the minute.
- **Limits and schedules.** Daily limits for single apps or groups, time windows when apps are blocked, and session limits (use an app for M minutes, then a K-minute pause, at most N sessions a day).
- **Shorts and Reels blocker.** Blocks short-video feeds in YouTube and Instagram without blocking the rest of the app. Both apps share one budget.
- **Smart alarm.** Missions to dismiss it (math, typing, walking), gradual volume, snooze rules, a "wake-up detox" that keeps chosen apps and sites locked after the alarm, and an "are you awake?" check.
- **Block screen.** A full-screen overlay appears as soon as a rule is hit and stays until the rule allows the app again.

## Architecture

```
UsageStatsManager ─► UsageEventSource ─► SessionReconstructor ─► app_usage_log
                                                                     │
                                         UsageSplitter (per hour) ─► usage_hourly
                                                                     │
                     UI (dashboard, details) ◄───────────────────────┤
                                                                     ▼
 SettingsRepository ─► RulesSnapshot ──────────────────► RulesEngine.evaluate()
                                                                     │
                                                                     ▼
                                                     BlockOverlayController
```

- **One source of truth for usage.** Only the sync job writes usage; every screen and the enforcement service read the same Room tables.
- **One source of truth for blocking.** `RulesEngine` is plain Kotlin with no Android dependencies. The rules you edit in the UI are exactly the rules the service evaluates.
- **Logic separated from the platform.** Session reconstruction, hourly splitting, rules, alarm policies and missions live in pure Kotlin packages (`domain/`, `alarm/domain/`) covered by JUnit tests that run without an emulator. Android classes are thin adapters around them.
- **Enforcement.** `FocusMonitorService` (foreground service) evaluates the rules and shows the block screen. `ShortsAccessibilityService` detects the Shorts and Reels players by their view IDs and on-screen size, scanning on a background dispatcher and only when a rule needs it.

| Package | Role |
|---|---|
| `domain/usage`, `domain/rules`, `domain/wakeup` | Session reconstruction, hourly splitting, rules engine (pure Kotlin) |
| `alarm/domain` | Next-alarm calculation (DST-aware), ring policy, missions, wake-up detox (pure Kotlin) |
| `data/` | Room database, repositories, adapter over `UsageStatsManager` |
| `service/`, `alarm/service/` | Foreground services, overlay, accessibility service, alarm ringing |
| `worker/` | Periodic sync and data retention (WorkManager) |
| `ui/` | Compose UI: theme, components, navigation, one folder per screen |

The full design document (in Serbian) is in [`docs/SISTEM.md`](docs/SISTEM.md).

## Tech stack

Kotlin 2.0 · Jetpack Compose (Material 3) · Room · WorkManager · UsageStatsManager · AccessibilityService · AlarmManager · JUnit 4

Min SDK 29 (Android 10), target SDK 36.

## Build and test

```bash
./gradlew assembleDebug      # debug APK
./gradlew testDebugUnitTest  # JUnit tests for the domain logic
```

Release builds for Google Play are described in [`RELEASE.md`](RELEASE.md).

## Permissions

The app needs usage access, "display over other apps" and an accessibility service to enforce limits, plus exact alarms, notifications and activity recognition for the smart alarm. It does **not** request internet access.

## Privacy

Everything stays on the device: no account, no analytics, no ads and no network access. See the [privacy policy](docs/PRIVACY.md).

## Author

[Vidan Stojić](https://vidanstojic.me) · [GitHub](https://github.com/vidanstojic) · [LinkedIn](https://www.linkedin.com/in/vidan-stoji%C4%87-57166032b/)
