# Observations — Round 5 Log (2026-11-18/19, chronological index)

Chronological log of the fifth research round. Canonical OBS entries live in
their per-area files; this is a session index only.

| Sequence | OBS ID (canonical location) | Behavior |
|---|---|---|
| 1 | OBS-SYS-114 (system-integration.md) | Third-party badge matrix attempts INCONCLUSIVE (Calendar reminder and Clock alarm routes failed to post); BadgeTest calendar event deleted |
| 2 | OBS-GESTURE-112 (gestures.md) | Prime gesture assignment catalog captured (NOVA/APPS/SHORTCUTS tabs, full action list) |
| 3 | OBS-CARDS-112/113 (cards.md) | Nova Now live probe with active condition: no card on either surface; the sheet's manual-location fallback does not exist (settings search empty) |
| 4 | OBS-CARDS-114 (cards.md) | Location GRANTED via adb (pm grant FINE+COARSE, verified granted=true); render probe BLOCKED — device self-locked with the pattern (launcher force-stop while idle; user away) |
| 5 | OBS-SYS-115 (system-integration.md) | Chrome badge route design-suppressed (DOWNLOAD_WITHOUT_NOTIFICATION); third-party badge matrix final state: all adb routes design-suppressed |
| 6 | OBS-GESTURE-112 (gestures.md) | Binding ambiguity RESOLVED: drawer-open is a separate App-drawer gesture config, independent of the gestures-section bindings (Swipe up → Expand quick settings confirmed); HOME-button two-press semantics documented |
| 7 | OBS-CARDS-115 (cards.md) | RENDER VERIFIED: AccuWeather card renders in Nova Now SEARCH results with live data ("Khamovniki, Moscow") once Location granted; plain drawer never renders cards; welcome-sheet toggles persist once permissions granted |

## Pending for next session (device locked — pattern required)

1. Unlock the device (user), then check the AccuWeather card render in
   Nova Now search with a fresh condition window (Location already granted;
   the Wednesday 21:57–22:57 condition likely auto-expired).
2. Popup-menu notification content: needs a real user-triggered
   notification (message/mail) — all adb routes are design-suppressed.
3. Re-verify "Swipe up" is truly bound to "Expand quick settings" by
   triggering it deliberately (quick-settings panel should open).

Session notes:
- Consolidation: round2/3/4 confirmed as thin indexes; OBS-SYS-111 stale
  line updated; TEST_ENVIRONMENTS device-config note at resolved state.
- Lessons codified in the skill: keyguard caution (force-stop while idle
  locks the device), runtime-permission granting, group-summary pitfall,
  cards render model (query-gated Nova Now).
