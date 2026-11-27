# VESNA-QA-SWIPE-DOWN-NOTIFICATIONS — the swipe-down gesture (phase 2, v0.4.105)

Phase 2 of the adjudicated #183 split: the swipe-down gesture binds to the
notification expansion BY DEFAULT (Nova's default), implemented through
the status bar service reflection with the graceful degradation; the
binding is reassignable on Settings › Gestures & input (the row: None /
expand notifications / the drawer / search / Settings). The permission
surface: `android.permission.EXPAND_STATUS_BAR` — a NORMAL install-time
permission with no user-data access (declared in the manifest; no
accessibility service is shipped). Build: debug 0.4.105 (versionCode
110) on `Vesna_QA_Menu_API_35` (emulator-5560, API 35, RU locale, dark
theme).

## What is verified

| Check | Result |
| --- | --- |
| The default expands the shade | The downward drag on an empty Home cell (400→1400, 250 ms): the notification shade expands — the window focus moves to NotificationShade and the screen shows the shade surface over the home (`swipedown-shade-04105.png`). |
| The reflection path | `StatusBarManager.expandNotificationsPanel()` via `getSystemService("statusbar")`: the hidden-API access is ALLOWED for the launcher (the logcat record) and the call succeeds with `EXPAND_STATUS_BAR` granted (the install permission). |
| The graceful degradation | The invocation is wrapped: an unavailable service or a future platform restriction degrades the gesture to a no-op (no crash; the launcher untouched). |
| The reassignment | The Gestures & input row for the swipe down offers None / expand notifications / the drawer / search / Settings; binding the swipe down to the drawer opens the drawer on the same gesture. |
| The suppression rules | The gestures never fire during a drag, a started one-finger page swipe, over an open dialog, popup, folder or the back-gated state (the dispatcher guards + the view gate). |
| Regression suites | GestureDispatchTest 4/4; FolderDisplacement 3 + PageAutoRemoval 5 + Badges 6 = 14/14; WorkTabDrawerTest + SettingsSearchLayoutTest 18/18 on the same build. |

## Implementation notes

- The swipe-down detection mirrors the shipped #170 swipe-up: a slow
  downward drag on an empty grid (the drift and the distance guards are
  the same), dispatched on the stream end.
- The decision trail: the accessibility-service alternative was
  REJECTED as a privacy-significant permission surface (the #183
  adjudication comment); the status-bar-service reflection ships instead
  with `EXPAND_STATUS_BAR` (the normal permission) and the try/catch
  degradation. The owner may still prefer the a11y route later — that
  would be a separate change.

## Known limits

- The reflection is a hidden test-api marked «allowed» on API 35; a
  future platform tightening would degrade the DEFAULT swipe-down action
  to a no-op (the binding stays reassignable). The a11y route remains the
  documented fallback if that ever happens.
- The release build and physical devices need separate runs.
