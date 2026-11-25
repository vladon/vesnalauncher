# ADR-NOTIF-1 — Notification dot badges and privacy

**Status:** Accepted and implemented (2026-11-25, v0.4.89 with the
recycling/dark-theme/a11y follow-ups in v0.4.90). Approved by the recorded
[#200](https://github.com/vladon/vesnalauncher/issues/200) scope decision:
"listener, dots, no texts".

**Gates:** [ENG-106](../engineering/BACKLOG.md#eng-106-backuprestore-policy)
family precedent — a listener-class privacy surface is recorded as an ADR
(the [ADR-BACKUP-1](0002-backup-and-privacy.md) pattern).

## Decision

- Vesna runs a `NotificationListenerService`
  ([VesnaNotificationListener](../../app/src/main/java/dev/vladon/vesna/launcher/notifications/VesnaNotificationListener.kt))
  that tracks **only which packages hold at least one active notification**
  — a presence set of package names. Notification titles, texts, extras,
  and per-notification counts are never read, stored, or transmitted.
- Tiles in the drawer, the dock, and the workspace draw a **presence dot**
  (the design system's error token: #BA1A1A light / #FFB4AB dark) at the
  artwork's top-end corner when their package is in the set. One dot per
  package; no counts.
- Rendering is **off by default**. Settings › App drawer › Show
  notification dots flips the switch and deep-links to the system
  notification-access grant; the row's detail reflects the grant state.
  Revoking the grant disconnects the listener and clears all dots.
- No network, no storage of notification content, no analytics: the tracked
  state is one in-memory package-name set plus a single boolean preference.

## Known limits

- The listener runs per Android user: work-profile tiles light up only
  through name-based matching of a personal-side notification of the same
  package, or after the user grants notification access to Vesna inside
  the work profile.
- The dot is decorative; the tile's content description gains a localized
  "has notifications" suffix so TalkBack announces the state.
