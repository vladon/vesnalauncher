# VESNA-QA-BADGE-DOTS — notification badge dots (#200, v0.4.89)

The notification badge dot pipeline approved in the #200 scope decision
(2026-11-25 loop session): a NotificationListenerService tracks which
packages hold active notifications; tiles draw a red dot (the design
system's error token #BA1A1A) at the artwork's top-end corner. Presence
only — one dot per package, no counts; package names only — notification
titles, texts, and extras are never read. Build: debug 0.4.89
(versionCode 94) on `Vesna_QA_Menu_API_35` (emulator-5560, Android
15/API 35, Russian locale).

## Instrumentation

`NotificationListenerBadgeTest` (androidTest, real preference file):
**5/5** — a posted package enters the set and `clear` removes it; Vesna's
own package is skipped (both through `addPackage` and a rebuild's
`setPackages`); the Settings toggle gates dot rendering; a rebuild replaces
the whole set.

## End-to-end (UI, evidence in EVI-VESNA-003)

| Scenario | Result |
| --- | --- |
| Settings | App drawer › «Показывать точки уведомлений» (off by default) toggles the dot rendering; «Доступ к уведомлениям» deep-links to the system notification-access page and its detail reflects the grant state (refreshed on resume). |
| Grant | `cmd notification allow_listener` granted the service; the system bound it into the launcher process (`dumpsys activity services` shows the ConnectionRecord). |
| Dot appears | A running Clock timer posts a real `com.google.android.deskclock` notification: the drawer's «Часы» tiles render the red dot (`badge-dot-main-grid.png`). |
| Dot clears | Stopping the timer removes the notification: the same tiles re-render without the dot (pixel scan: zero matching pixels, `badge-dot-cleared.png`). |
| Presence-only | One dot per package by name; both the personal and the work-profile copy of an app light up when the personal side has a notification (name-based matching; per-profile dots need a per-profile grant — see Limitations). |

## Process notes

- After `adb install -r` of an update the already-connected listener kept
  serving from the pre-update process; a force-stop re-bound it into the
  fresh process. Production updates trigger the same system rebind.
- Debug logging (`VesnaBadges`) in the listener's rebuild stays in the
  debug builds for the next QA pass.

## Limitations

- Per-profile dots: the listener runs per Android user; a work-profile dot
  requires granting notification access to Vesna inside the work profile.
  Without it, work-profile tiles light up only through name-based matching
  of a personal-side notification of the same package.
- TalkBack announcement of the dot state is not added (the dot is
  decorative; the tile's content description is unchanged).
- The D-pad focus pass for the new settings rows remains open (recorded in
  [VESNA-QA-BACKUP-EXPORT](VESNA-QA-BACKUP-EXPORT.md)).
