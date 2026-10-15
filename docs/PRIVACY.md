# Focus Flow — Privacy Policy

_Effective: October 15, 2026_

Focus Flow ("the app") is a screen-time and digital-wellbeing app developed by Vidan Stojić.
The app is designed to work entirely on your device. This policy explains what the app
accesses, why, and what happens to that data.

## Summary

- The app does **not** collect, transmit, sell or share any personal data.
- The app has no account, no analytics, no advertising and does not request internet access.
- All data stays on your device and is deleted when you uninstall the app.

## Data the app accesses and stores on your device

| Data | Source | Purpose |
|------|--------|---------|
| App usage times (which app was in the foreground and for how long) | Android Usage Access (`PACKAGE_USAGE_STATS`) | Screen-time statistics, daily limits, session limits, schedules |
| List of installed apps with a launcher icon (name, icon) | Android package manager | Choosing which apps your rules apply to |
| Your rules, limits, schedules and alarms | Entered by you | Enforcing the limits you set |
| Shorts/Reels watch-time totals | Calculated by the app | Shorts/Reels limits |
| Step count during an alarm "steps" mission | Step sensor (`ACTIVITY_RECOGNITION`) | Dismissing the alarm only after real steps |
| Screen on/off times | Android system events | Detecting when you wake up for the morning lock |

This data is stored only in the app's private storage on your device. It is excluded from
cloud backup and device-to-device transfer.

## Accessibility service

Focus Flow includes an optional Accessibility service. It is used to enforce limits you set
yourself, not as an assistive tool. Before you turn it on, the app shows what it does and
asks for your consent.

- **YouTube and Instagram:** the service reads the on-screen layout (element IDs and labels)
  to recognize when the Shorts or Reels player is open, and leaves it or shows a block
  screen when your limit is reached.
- **Web browsers:** only while a wake-up detox with blocked sites is active, the service
  reads the browser's address bar to check whether a blocked site is open.

Screen content is checked in memory as it appears and is never stored, logged or sent
anywhere. You can turn the service off at any time in Android Settings › Accessibility.

## Other permissions

- **Display over other apps** — to show the block screen on top of a limited app.
- **Alarms & reminders / exact alarms** — for alarms, schedules and cool-down timers.
- **Notifications** — for alarms and the monitoring status notification.
- **Run at startup** — to restore alarms and monitoring after a restart.

## Data sharing

Focus Flow does not share any data with third parties and does not send data off the device.

## Children

The app is not directed at children under 13 and does not knowingly collect any data from them.

## Changes

If this policy changes, the updated version will be published at this address with a new
effective date.

## Contact

Questions about this policy: **TODO-kontakt-email**
