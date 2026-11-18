# Observations — Round 5 Log (2026-11-18 night, chronological index)

Chronological log of the fifth research round. Canonical OBS entries live in
their per-area files; this is a session index only.

| Sequence | OBS ID (canonical location) | Behavior |
|---|---|---|
| 1 | OBS-GESTURE-112 (gestures.md) | Prime-unlocked gesture assignment catalog: NOVA/APPS/SHORTCUTS tabs with the full action list; Prime-era binding rows observed (Swipe-up value ambiguity flagged for re-check) |
| 2 | OBS-SYS-114 (system-integration.md) | Third-party badge matrix attempts INCONCLUSIVE (Calendar reminder and Clock alarm routes failed to post); popup-menu notification content remains Not Tested (needs a badged app); BadgeTest calendar event deleted |
| 3 | OBS-CARDS-112/113 (cards.md) | Nova Now live probe with active condition: no card on either surface — render requires condition AND Location permission; the sheet's manual-location fallback does not exist in settings (OQ #12 resolved) |
| 4 | OBS-CARDS-114 (cards.md) | Location GRANTED via adb (pm grant FINE+COARSE, verified granted=true); render probe BLOCKED — the device self-locked with the pattern after a launcher force-stop (user away); probe is the first action next session |

## Pending for next session (device locked — pattern required, user away)

1. Unlock the device (user), then check the AccuWeather card render in the
   drawer / Nova Now. Location is already granted (persistent); if the
   Wednesday 03:20–04:20 condition was auto-removed on expiry, add a fresh
   Day/time condition covering the current time first.
2. Re-verify the "Swipe up" binding (Prime-era reading said "Expand quick
   settings"; free-era read "App drawer").
3. Lessons recorded: do NOT force-stop the launcher while the screen may
   lock — the keyguard engages and only the user can unlock.

Session notes:
- Consolidation: round2/3/4 confirmed as thin indexes; OBS-SYS-111 stale
  pending line updated; TEST_ENVIRONMENTS device-config note updated to the
  resolved state (listener bound, badges working for third-party apps).
