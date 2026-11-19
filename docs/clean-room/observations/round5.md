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
| 8 | OBS-CARDS-117 (cards.md) | Conditions are DECORATIVE in 8.9.9: expired Day/time AND never-satisfied Wi-Fi both render in Nova Now search; the later-satisfied false→true Wi-Fi transition (svc wifi off→on, reconnection system-verified) NEVER adds the card to the plain drawer — divider slot empty in every state; editor's "will cause this card to show in the app drawer" copy is unfulfilled; both test conditions cleaned up and removal sealed via post-save re-open |
| 9 | OBS-GESTURE-112 (gestures.md, refinement + standing change) | Swipe-up routing is BINDING-DEPENDENT: "Expand quick settings" binding FIRES on the workspace (both swipe zones) overriding the drawer-open config; only "Expand notifications" was non-authoritative in the r6 experiment; "Open App Drawer: Swipe" unchanged throughout; search-pill grid icon = deterministic drawer route. USER-DIRECTED: Swipe up binding reassigned to "App drawer" — gesture verified opening the drawer |
| 10 | OBS-CARDS-118 (cards.md) | Calendar card variant: card renders with permission empty-state + New event chip; adb CALENDAR grant does NOT flip it (prompt persists across restart); in-app "Connect to Calendar" on the card's config page is the working route — banner clears instantly, preview + search render real event chips (titles withheld); READ/WRITE_CALENDAR left granted (in use) |
| 11 | OBS-DRAWER-112 (app-drawer.md) | Drawer groups SPLIT VERDICT: management UI works; TABS work end-to-end (TestTab created → Tab bar Top → bar rendered both tabs → switch verified → full revert: placement None + tab deleted); SMART FOLDERS render-inert in every state (restart, Folders first checked) — member apps render raw; Folders first restored unchecked; the render failure is specific to auto/decorative structures |
| 12 | OBS-CARDS-119 (cards.md) | Builder save flow: Save GATED on ≥1 action (grey/inert for name-only, even in the discard dialog); name+action saves into a CUSTOM SECTION below built-ins; builder defaults Activate ON + pre-attaches a never-satisfied Wi-Fi condition; delete via details ⋮ ▸ Delete (no confirm). DISCOVERED: sixth built-in card "Map locations" (missed by scans stopping at Screenshots) with a two-stage geocoded location editor (unresolvable text leaves OK grey); both test cards + test location cleaned up; AccuWeather stamp advanced across Nova restart |

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
