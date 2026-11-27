# VESNA-QA-GESTURE-BINDINGS — Home gesture bindings phase 1 (#183, v0.4.104)

Phase 1 of the adjudicated #183 split: the gesture framework, the
assignment surface and the internal-action bindings for the double-tap,
the two-finger swipe up, the pinch in and the pinch out — each
unassigned by default and assignable through the new Settings › Gestures
& input page (the rows: None / the drawer / search / Settings). The
notification-expansion binding is NOT offered in this phase: it is
decision-gated (see the #183 adjudication — the accessibility-service
versus hidden-API verdict), the phase-2 remainder. Build: debug 0.4.104
(versionCode 109) on `Vesna_QA_Menu_API_35` (emulator-5560, API 35, RU
locale, dark theme).

## What is verified

| Check | Result |
| --- | --- |
| The Gestures & input page | The directory row navigates to the new page; the four gesture sections render with the four action radios each, all «Не назначено» on a fresh install. |
| The assignment persists | The double tap bound to «Открыть список приложений» through the page: the pref `gesture_double_tap = drawer` written and the binding survives restarts. |
| The double-tap gesture | Two taps on an empty Home cell (a fresh process, the assignment from above): the drawer opens (`gesture-doubletap-drawer.png`). The taps on a tile still launch the app (the empty-grid gate). |
| The two-finger swipe up | Bound to the drawer via the preferences: the injected two-pointer stream (both pointers up past 80 dp) opens the drawer (the instrumented dispatch through the real touch path). |
| The pinch in / out | The pinch in bound to the drawer: the converging two-pointer stream opens the drawer; an unassigned pinch out fires nothing. The pinch and the swipe share one session flag — the first recognised gesture wins. |
| The suppression rules | The second finger arriving during a started one-finger page swipe suppresses the two-finger gesture entirely (the drawer never opens); the gestures never fire over an open drag session, dialog, popup or the back-gated state (the dispatcher guards). |
| The unassigned defaults | The fresh install: every gesture is None and every gesture input produces no visible change. |
| Regression suites | GestureDispatchTest 4/4; FolderDisplacement 3 + PageAutoRemoval 5 + Badges 6 = 14/14; WorkTabDrawerTest + SettingsSearchLayoutTest 18/18 on the same build. |

## Implementation notes

- The double-tap gate reads the previous DOWN's empty-grid state: the
  detector's callback fires before the current DOWN's own flag is set
  (the fixed ordering bug found during this verification).
- The pinch and the two-finger swipe share the session: the first
  recognised gesture blocks the other, so a converging upward pinch is
  the pinch, a straight upward two-finger travel is the swipe.
- The notification-expansion action exists in the enum but is not
  offered in this phase (the owner verdict pending); adding it later is
  a data change plus the expansion implementation.

## Known limits

- The assigned actions in this phase: the drawer, search, Settings and
  none. The shortcuts and other Nova-style actions are future scope.
- The phase-2 remainder: the swipe-down binding (decision-gated, above).
- The release build and physical devices need separate runs.
