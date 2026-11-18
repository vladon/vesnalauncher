# Observations — Round 5 Log (2026-11-18/19, chronological index)

Chronological log of the fifth research round. Canonical OBS entries live in
their per-area files; this is a session index only.

| Sequence | OBS ID (canonical location) | Behavior |
|---|---|---|
| 1 | OBS-SYS-114 (system-integration.md) | Third-party badge matrix attempts INCONCLUSIVE (Calendar reminder and Clock alarm routes failed to post); BadgeTest calendar event deleted |
| 2 | OBS-GESTURE-112 (gestures.md) | Prime gesture assignment catalog captured (NOVA/APPS/SHORTCUTS tabs, full action list); binding ambiguity RESOLVED (drawer-open = separate App-drawer config; HOME two-press semantics documented); LIVE BINDING EXPERIMENT: gestures-section "Swipe up" binding is NON-AUTHORITATIVE (assigned Expand notifications → swipe-up still opened the drawer); restored to Expand quick settings |
| 3 | OBS-CARDS-112/113 (cards.md) | Nova Now live probe with active condition: no card on either surface — render requires condition AND Location permission; the sheet's manual-location fallback does not exist (EXHAUSTIVE final check: location/weather/city/AccuWeather searches, card page, overflow, Integrations, overlay surfaces all negative — OQ #12 resolved) |
| 4 | OBS-CARDS-114 (cards.md) | Location GRANTED via adb (pm grant FINE+COARSE, verified granted=true); render probe initially BLOCKED by the pattern lock, completed after the user unlocked |
| 5 | OBS-SYS-115 (system-integration.md) | Chrome badge route design-suppressed (DOWNLOAD_WITHOUT_NOTIFICATION); third-party badge matrix final state: all adb routes design-suppressed |
| 6 | OBS-CARDS-115 (cards.md) | RENDER VERIFIED: AccuWeather card renders in Nova Now SEARCH results with live data ("Khamovniki, Moscow") once Location granted; plain drawer never renders cards; welcome-sheet toggles persist once permissions granted |
| 7 | OBS-CARDS-116 (cards.md) | Overlay affordances discovered: card long-press = accuweather.com deep link; card gear = Card details w/ Save; Weather-row gear = DISTINCT built-in-Weather config (no location); ⋮ = Add card/API Integrations/Settings; Add card = custom card builder (also via Cards-page + FAB); card renders FAHRENHEIT regardless of locale (32°F = 0°C site value; no unit preference); stamp static over 8 min |
| 8 | OBS-CARDS-117 (cards.md) | Attached conditions do NOT gate the Nova Now search render: expired Day/time persisted 2h20m+ AND a never-satisfied Wi-Fi condition (MGTS vs RG-5, saved + verified end-to-end) both rendered anyway; BT conditions unusable without paired devices; render requirements = Activate + Location + query; test artifact cleaned up |

## Pending for next session

1. Popup-menu notification content: needs a real user-triggered
   notification (message/mail) — all adb routes are design-suppressed.
2. Whether the gestures-section "Expand quick settings" swipe-up binding
   ever fires on the workspace (both observed swipe-ups opened the drawer).
3. Nova Now with the Calendar Card toggle variant; drawer groups/tabs with
   Prime custom groups.

Session notes:
- Consolidation: round2/3/4 confirmed as thin indexes; OBS-SYS-111 stale
  line updated; TEST_ENVIRONMENTS device-config note at resolved state.
- Lessons codified in the skill: keyguard caution (force-stop while idle
  locks the device), runtime-permission granting, group-summary pitfall,
  cards render model (query-gated Nova Now).
