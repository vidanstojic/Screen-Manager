# Focus Flow — Privacy Policy

_This policy is also published at https://vidanstojic.me/projects/focus-flow/privacy/_

Focus Flow ("the app", package `rs.vidanstojic.focusflow`) is a screen-time and digital-wellbeing app for Android, developed by Vidan Stojić. The app works entirely on your device. This policy explains what the app accesses, why, and what happens to that data.

_Effective: November 15, 2026_

## Summary

- The app does **not** collect, transmit, sell or share any personal data.
- The app has no account, no analytics and no advertising, and it does not request internet access.
- All data stays on your device and is deleted when you clear the app's data or uninstall it.

## Data the app accesses and stores on your device

| Data | Source | Purpose |
| --- | --- | --- |
| App usage times (which app was in the foreground and for how long) | Android Usage Access (`PACKAGE_USAGE_STATS`) | Screen-time statistics, daily limits, session limits, schedules |
| List of installed apps with a launcher icon (name, icon) | Android package manager | Choosing which apps your rules apply to |
| Your rules, limits, schedules and alarms | Entered by you | Enforcing the limits you set |
| Shorts/Reels watch-time totals | Calculated by the app | Shorts/Reels limits |
| Step count during an alarm "steps" mission | Step sensor (`ACTIVITY_RECOGNITION`) | Dismissing the alarm only after real steps |
| Screen on/off times | Android system events | Detecting when you wake up for the morning lock |

This data is stored only in the app's private storage on your device. It is excluded from cloud backup and device-to-device transfer.

## Accessibility service

Focus Flow includes an optional Accessibility service. It is used to enforce limits you set yourself, not as an assistive tool. Before you turn it on, the app shows what it does and asks for your consent.

- **YouTube and Instagram:** the service reads the on-screen layout (element IDs and labels) to recognize when the Shorts or Reels player is open, and leaves it or shows a block screen when your limit is reached.
- **Detecting when a limited app is open:** the service notes when a YouTube or Instagram window is on screen, so a limit you set applies immediately, also when the app is opened from a link or reopened from recent apps.
- **Web browsers:** only while a wake-up detox with blocked sites is active, the service reads the browser's address bar to check whether a blocked site is open.
- **Actions it performs:** going back, going to the home screen ("Go to home screen" on the block screen) and restarting Focus Flow's own monitoring if the system stopped it.

Screen content is checked in memory as it appears and is never stored, logged or sent anywhere. You can turn the service off at any time in Android Settings › Accessibility.

## Other permissions

- **Display over other apps**: to show the block screen on top of a limited app.
- **Alarms & reminders / exact alarms**: for alarms, schedules and cool-down timers, and to restart monitoring if the system stops it.
- **Notifications**: for alarms and the monitoring status notification.
- **Run at startup**: to restore alarms and monitoring after a restart.
- **Close other apps** (`KILL_BACKGROUND_PROCESSES`): when you tap "Go to home screen" on the block screen, the blocked app is closed so it cannot be reopened past the limit. It only affects the app you were blocked from, and only once it is in the background.

## Data security

All data is kept in the app's private storage, which other apps cannot read. The app has no internet permission, so no data can leave the device through it.

## Data retention and deletion

- Detailed usage sessions are kept for 12 days. Hourly usage totals are kept for about 400 days, so you can see weekly and monthly statistics. Older records are removed automatically.
- You can delete all data at any time in Android Settings › Apps › Focus Flow › Storage › Clear data, or by uninstalling the app.
- The developer has no copy of your data and cannot access or delete it remotely.

## Data sharing

Focus Flow does not share any data with third parties and does not send data off the device.

## Children

The app is not directed at children under 13 and does not knowingly collect any data from them.

## Changes

If this policy changes, the updated version will be published at this address with a new effective date.

## Contact

Questions about this policy: **vidan.stojic+focusflow@gmail.com**
