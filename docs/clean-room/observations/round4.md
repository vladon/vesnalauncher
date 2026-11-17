# Observations — Round 4 Log (2026-11-18, chronological index)

Chronological log of the fourth research round. Canonical OBS entries live in
their per-area files; this is a session index only.

| Sequence | OBS ID (canonical location) | Behavior |
|---|---|---|
| 1 | OBS-SYS-112 (system-integration.md) | Badge enablement chain verified (listener bound post-reboot, notifications allowed, test notification live); self-badge absent on all icon instances (zoom-verified); `.NovaSettings` activity does not exist (am start red herring) |
| 2 | OBS-SYS-113 (system-integration.md) | Numeric-style re-test + ROOT CAUSE: the Test notification is a GROUP_SUMMARY — Android never badges group summaries; style restored to Dynamic |

## Interruption record correction (from round 3)

- The "Tilt your head down" face prompt encountered at ~23:25 on 2026-11-17
  was **Google Play's biometric verification for the user's Nova Prime
  lifetime purchase** (the purchase completed minutes later) — not a device
  unlock attempt as initially assumed. The research's Cancel tap aborted the
  user's first verification attempt; the user re-verified and the install
  proceeded. Recorded for an accurate session chronology.

## Pending for next session

- Badge matrix re-test with a THIRD-PARTY app notification (Nova's own test
  button cannot badge by construction — see OBS-SYS-113): post a real
  notification from an installed app and verify the badge on its icon
  (dynamic + numeric styles).
- Badge "Notifications in popup menu" behavior (long-press an app with a
  badge).
