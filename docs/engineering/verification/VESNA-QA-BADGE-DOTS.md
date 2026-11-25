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
- The debug logging added for this pass (`VesnaBadges` in the listener's
  rebuild, `FolderMove169` in the folder move) was removed from main after
  the diagnosis; it shipped briefly in v0.4.91 as `Log.d` noise.

## v0.4.90 follow-ups (recycling, dark variant, a11y)

The v0.4.89 review surfaced three gaps, fixed and verified in v0.4.90:

| Fix | Verification |
| --- | --- |
| Stale dot on recycled tiles | `bindContent` (folders and any non-app content) resets the dot: a tile recycled from a dotted app into a folder loses it. Covered by `folderRecycleClearsTheDot` (the suite grew to 6/6). |
| Dark-theme dot color | `values-night/colors.xml` adds the dark error token (#FFB4AB); the dark drawer verified live — both BadgeProbe tiles render the dot (`badge-dot-dark-0490.png`), and the uninstall-driven clear leaves zero dot pixels (`badge-dot-dark-cleared.png`). |
| TalkBack announcement | The tile's content description gains a localized «есть уведомления» suffix while the dot shows (`accessibility_notification_dot`), so the badge state is announced. |

The record also notes [ADR-NOTIF-1](../../adr/0004-notification-dots.md)
(written for this surface per the ADR-BACKUP-1 pattern) and the E2E detail
that the drawer's RU collation places Latin names (BadgeProbe) next to their
Cyrillic neighbours, which made the live dot visible without scrolling.

## Limitations

- Per-profile dots: the listener runs per Android user; a work-profile dot
  requires granting notification access to Vesna inside the work profile.
  Without it, work-profile tiles light up only through name-based matching
  of a personal-side notification of the same package.
- The D-pad focus pass for the new settings rows remains open (recorded in
  [VESNA-QA-BACKUP-EXPORT](VESNA-QA-BACKUP-EXPORT.md)).
