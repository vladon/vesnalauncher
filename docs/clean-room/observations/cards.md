# Observations — Cards (ENV-1, Nova 8.9.9: free era inventory, Prime era condition testing)

## OBS-CARDS-110 — Cards internals: condition-triggered drawer cards

- Date / Env: 2026-11-17 / ENV-1
- Action: Settings ▸ Cards ▸ tapped AccuWeather's gear.
- Observed: "Card details" page (Save + overflow menu) with:
  - a live card preview;
  - an "Activate in Nova Now" section: "Configure a set of conditions
    that, when all met, will cause this card to show in the app drawer";
  - condition adders: Day/time (+); Wi-Fi network (+) "Requires Location
    permission"; When weather changes (+); Bluetooth connection (+)
    "Requires Bluetooth permission".
- Card inventory ("Your cards" page): Media controls, AccuWeather, Games —
  each with a configure gear; a (+) add-card FAB. The Media Player card
  preview shows album art, Title/Artist, play control, seek bar, prev/next,
  star, share, collapse chevron.
- Interpretation: Cards are condition-triggered content cards whose target
  surface is the APP DRAWER (the "Nova Now" branding on Style/Search
  surfaces refers to this drawer card layer). Conditions are composable
  ("all met") and permission-gated.
- Evidence: EVI-CARDS-001 (cards-page2, card-config)
- Confidence: Observed (config page); condition behavior Not Tested

## OBS-CARDS-111 — Day/time condition: UI, persistence, and unresolved render surface

- Date / Env: 2026-11-18 / ENV-1 (Prime active)
- Action: Card details (AccuWeather) ▸ tapped "Day/time" adder.
- Observed:
  - condition editor: "Show this card on selected days between the
    specified times" — day toggles S M T W T F S (current day preselected),
    Start-time hour/minute steppers PREFILLED with the current time, and a
    "For duration" stepper (default 01h 00m); Confirm button.
  - after Confirm + Save, the Card details page lists the condition:
    "Day/time — 12:40 AM +1 hours on Wednesday" — persistence verified.
- Render test: with the condition window ACTIVE (00:40–01:40 Wednesday,
  observed at 00:42–00:44), the AccuWeather card did NOT appear:
  - not at the drawer top (search/predictions area);
  - not between the predictions divider and the app grid;
  - not at the drawer bottom;
  - not in the drawer SEARCH results ("acc" query showed providers +
    web suggestions only).
- Conclusion: the condition UI/persistence is verified, but the render
  surface for cards ("show in the app drawer") was not observed during the
  active window. Candidates: the card layer requires the Nova Now SEARCH
  surface (immersive search rather than the plain drawer), additional
  gating, or a render delay/bug. OPEN — needs a targeted follow-up (e.g.
  open the Nova Now overlay via the left-edge swipe with the condition
  active).
- Test artifact note: the Wednesday 00:40+1h condition was left on the
  user's AccuWeather card (harmless 1 h weekly window); removable via
  Cards ▸ AccuWeather ▸ Day/time if unwanted.
- Evidence: EVI-CARDS-001 (carddetails-r2, daytime-ui, condition-added,
  drawer-with-card, drawer-scrolled, drawer-live-top, search-with-card)
- Confidence: Observed (UI/persistence); render surface Not Observed

## OBS-CARDS-112 — Nova Now live probe: permission gating confirmed

- Date / Env: 2026-11-19 03:17–03:23 / ENV-1 (Nova Prime)
- Action: added a fresh Day/time condition covering the current time
  (Wednesday, 03:20 + 1 h — editor prefills the start with the current
  time; W preselected), saved, then probed both surfaces while the window
  was active (03:21–03:23):
  1. **Nova Now** (left-edge swipe): surface opened (search bar, Nova AI
     section with Enable/Disable, frequently-used icons, prediction chips)
     — **no AccuWeather card**. The "Welcome to Nova Now!" bottom sheet
     states the gating explicitly: "We can create these Cards for you if
     you grant their required permissions", listing a Calendar Card
     (Calendar permission) and a Weather Card ("See current weather by
     granting precise Location permission. Alternatively, set a manual
     location in Nova Settings") with enable toggles.
  2. Toggled the Weather Card ON in the sheet — after dismissing, the
     toggle REVERTED to OFF (Nova will not keep card creation enabled
     without its Location permission being granted; granting precise
     location was deliberately NOT done on the user's device — the card's
     documented alternative is a manual location in Nova Settings).
  3. **Drawer**: still no card between predictions and the grid (divider
     slot empty; light theme this time).
- ALSO: the previously-persisted Wednesday 00:40+1 h condition was found
  GONE from the Card details page at 03:20 (row showed "+" again) — the
  window had passed 100 minutes earlier. Either one-shot semantics (the
  condition is removed after its window elapses) or an expiry mechanism;
  the semantics are undetermined — new conditions persist while active
  (the fresh 03:20 condition is currently listed).
- CONCLUSION: the render chain is **condition AND data-permission**: a
  met Day/time condition alone does not render the card; the card's data
  permission (Location for weather) must also be granted. This closes the
  render-surface open question with an observed gating model; the exact
  permission-grant flow was not exercised (privacy-sensitive precise
  location on the user's device).
- Test artifact: the fresh Wednesday 03:20–04:20 condition remains on the
  AccuWeather card (1 h weekly window); removable via Cards ▸ AccuWeather
  ▸ Day/time if unwanted.
- Evidence: EVI-CARDS-001 (now-probe-start, condition-editor-r5,
  cond-editor2, cond-confirmed, novanow-live, novanow-sheet,
  weather-toggle, novanow-after-toggle, drawer-live-r5)
- Confidence: Observed (absence across both surfaces + sheet gating text +
  toggle revert); permission-granted render Not Tested

## OBS-CARDS-113 — The advertised manual-location fallback does not exist (FINAL)

- Date / Env: 2026-11-19 00:00–00:07 / ENV-1 (Nova Prime) — searches re-run
  and the remaining surfaces exhausted; this entry supersedes the initial
  partial finding.
- Context: the Nova Now welcome sheet offers "Alternatively, set a manual
  location in Nova Settings" for the Weather Card.
- EXHAUSTIVE CHECKS (all negative):
  1. Settings search: "location", "weather", "city", "AccuWeather" — ALL
     zero results (search indexes titles AND subtitles; the "dock" control
     query returns two matches, proving the index works).
  2. Card details page (AccuWeather): no location field; overflow (⋮) has
     only "Reset to default"; "See forecast details >" is inert.
  3. Cards list page: card entries only (Media controls, AccuWeather,
     Games, Photos, Screenshots) — no location row.
  4. Settings ▸ Integrations (complete inventory): Calendar ("Your
     upcoming events"), Spotify, Deezer, Twitch, Slack, Discord, Dropbox,
     GitHub, Microsoft OneDrive, Sesame Shortcuts — NO weather/AccuWeather/
     location entry.
  5. Overlay surfaces (2026-11-19 00:04–0:08, see OBS-CARDS-116): card
     long-press = external provider link; card gear = the same Card
     details page; search-bar ⋮ = Add card / API Integrations / Settings;
     "Add card" = custom card builder — none offers a location setting.
  6. Final probes (2026-11-19 0:12–0:15): the Weather result-row gear
     opens a DISTINCT config page (built-in Weather card — generic
     template, no location field, page bottom = Bluetooth row); the
     Settings ▸ Cards list re-scan shows NO distinct "Weather" entry
     (the sheet toggle feeds the overlay row instead) — see OBS-CARDS-116.
- CONCLUSION: the manual-location affordance advertised by the welcome
  sheet does not exist anywhere in Nova 8.9.9's UI. The Weather/AccuWeather
  card's ONLY location path is the precise-Location permission grant
  (verified working via adb grant: the card then rendered live district
  data — see OBS-CARDS-115). The unfulfilled sheet promise is a Nova
  quality gap and a Vesna design lesson: every advertised fallback must be
  implemented and discoverable.
- The Day/time condition's WINDOW ended during the checks (00:00
  Thursday); the condition itself persists and did not gate the renders —
  see OBS-CARDS-117. The earlier render verification stands (OBS-CARDS-115).
- Evidence: EVI-SETT-003 (search-location, search-weather2, search-city2,
  main-top-r6), EVI-CARDS-001 (details-r6, details-scroll, details-overflow,
  forecast-details, cards-scrolled-r6, cards-r6b, integrations-find through
  integrations-final)
- Confidence: Observed (exhaustive)

## OBS-CARDS-114 — Location granted via adb; render probe blocked by device lock

- Date / Env: 2026-11-18 16:43 / ENV-1 (Nova Prime)
- Action: granted Nova's declared runtime permissions over adb:
  `pm grant com.teslacoilsw.launcher android.permission.ACCESS_FINE_LOCATION`
  and `ACCESS_COARSE_LOCATION` (both verified `granted=true` in dumpsys —
  persistent, applies without reboot). Asked for by the user (they could not
  reach the phone).
- Probe attempt: restarting Nova to pick up the grant force-stopped the
  launcher, which let the keyguard engage — the device now sits on the
  pattern lock ("Draw pattern — Pattern required for additional security").
  All input stopped (only the user can draw the pattern).
- Status: the permission-GRANTED card-render probe is the FIRST action next
  session (after the user unlocks). If the Wednesday 03:20–04:20 condition
  was auto-removed on expiry, add a fresh Day/time window first.
- Evidence: EVI-CARDS-001 (settings-post-grant, unlock-r5), dumpsys grant
  lines
- Confidence: Observed (grant verified); render Not Tested (locked)

## OBS-CARDS-115 — RENDER VERIFIED: cards render in the Nova Now search surface

- Date / Env: 2026-11-18 22:00–22:02 / ENV-1 (Nova Prime)
- Setup: Location (FINE+COARSE) granted via adb; Day/time condition active
  (Wednesday 21:57–22:57, replaced the expired 03:20 window); "Activate in
  Nova Now" ON; Card details saved; the Nova Now welcome-sheet toggles
  (Calendar Card + Weather Card) now STAY ON (with Location granted the
  earlier toggle-revert no longer occurs).
- Action: opened Nova Now (left-edge swipe) and typed a query.
- Observed: **the AccuWeather card rendered in the SEARCH RESULTS**, above
  the "Search with" provider row:
  - card header "AccuWeather" with its configure gear;
  - **real location-based data**: 32°, "Snow", "Khamovniki, Moscow"
    (a Moscow district — the Location grant in use), "See forecast details
    >", weather artwork, timestamp 21:56;
  - alongside it, the system **Weather card** (created via the welcome
    sheet's Weather toggle) also rendered with its own gear.
- Cross-check: with the query CLEARED, the plain drawer browse view still
  shows NO card (divider slot empty) — **cards render in the Nova Now
  search surface only**, not in the plain all-apps drawer browse.
- COMPLETE RENDER MODEL (all cells now Observed):
  card configured + "Activate in Nova Now" ON + conditions met + data
  permission granted + a search query typed → card renders in Nova Now
  search results with live data. Removing any element (condition expired,
  permission ungranted, no query) → no card (each variant observed).
- Evidence: EVI-CARDS-001 (drawer-render-check, novanow-r7,
  sheet-expanded-r7, drawer-final-render, search-results-r7,
  drawer-cleared-check)
- Confidence: Observed
- Test artifact: Wednesday 21:57–22:57 condition left on the AccuWeather
  card (did NOT expire — persists past window, OBS-CARDS-117; removable
  via Cards ▸ AccuWeather ▸ Day/time).
  Welcome-sheet Calendar/Weather toggles left ON (user-facing feature,
  enabled state).

## OBS-CARDS-116 — Nova Now overlay affordances: card gear, ⋮ menu, custom card builder, provider deep link

- Date / Env: 2026-11-19 00:04–00:15 / ENV-1 (Nova Prime)
- Discovered while exhaustively hunting a manual weather-location setting
  (completes OBS-CARDS-113): the overlay surfaces hide four affordances
  the settings-side inventory never showed.
- **Card long-press = provider deep link**: long-pressing the rendered
  AccuWeather card does NOT open a Nova popup — it opens accuweather.com
  in a browser custom tab, pre-navigated to the card's location
  (Khamovniki, Moscow page: 0°C light snow, RealFeel -6°, wind, humidity).
  This is the working "forecast details" route the preview link lacks.
- **Card gear (top-right of the rendered card) = Card details shortcut**:
  opens the same Card details page as Settings ▸ Cards ▸ AccuWeather, but
  with an enabled **Save** button (changes apply on Save when entered from
  the overlay). No location field there either.
- **Search-bar ⋮ overflow** (top-right of the overlay): exactly three
  items — "Add card", "API Integrations", "Settings". No weather/location
  entry.
- **"Add card" = custom card builder** (NEW Prime capability surface):
  Card name field, "Featured content" (feature an app/shortcut/widget),
  "Actions" (pin up to 6 app or shortcut actions), Activate-in-Nova-Now +
  the full condition set (Day/time, Wi-Fi, weather-change, Bluetooth).
  Users can author their OWN cards; nothing was created (backed out
  without Save). Reached BOTH from the overlay ⋮ and from a "+" FAB on
  the Settings ▸ Cards page (added this session).
- **Weather result-row gear = a DISTINCT config page** (the built-in
  "Weather" search-result card, separate from the AccuWeather card):
  generic template only — Actions ("App shortcuts: this app does not
  provide any shortcuts" toggle), Add action, Activate-in-Nova-Now +
  conditions. NO location field (page bottom = Bluetooth row). The
  sheet's Weather toggle does NOT create a Cards-page entry: the Cards
  list re-scan (0:15) shows Media controls, AccuWeather, Games, Photos,
  Screenshots — unchanged, plus the new "+" FAB.
- **Card renders Fahrenheit regardless of locale**: the card shows 32°/Snow
  while the accuweather.com page it deep-links to shows 0°C/Light snow —
  32°F = 0°C exactly, and 32°C with actively falling snow is physically
  impossible, so the unit (not the value) differs. Settings search
  "fahrenheit" returns nothing: no user-facing unit preference exists
  (device locale ru-RU, provider page °C).
- **Stamp behavior**: the card's timestamp (0:05) did not advance across
  re-renders within ~8 minutes (re-checked 0:13); whether the underlying
  data refetches per render is undetermined (the value was constant and
  consistent with current conditions).
- **Condition persistence (see OBS-CARDS-117)**: the Day/time condition
  attached to the AccuWeather card remained attached 2h20m past its
  window while the card kept rendering outside the window.
- Manual-location verdict (with OBS-CARDS-113): NO weather-location
  setting exists on any surface — settings search ×4 (location/weather/
  city/accuweather), Cards page, Card details (both entries), card
  long-press (external link), overlay ⋮ (3 items), Integrations (full
  inventory). The precise-Location grant remains the only render path.
- Evidence: EVI-CARDS-001 (overlay-topright, card-longpress, card-gear,
  overlay-overflow2, add-card-flow, hunt-exit), EVI-SETT-003
  (search-accuweather)
- Confidence: Observed

## OBS-CARDS-117 — Attached conditions are decorative: they gate neither the search render nor the plain drawer (expired Day/time, never-satisfied Wi-Fi, later-satisfied transition all verified non-blocking)

- Date / Env: 2026-11-19 0:04–0:58 / ENV-1 (Nova Prime)
- The AccuWeather card's condition reads "Day/time: 09:57 PM +1 hours on
  Wednesday" (window ended 22:57 Wednesday). At 0:04–0:13 THURSDAY —
  66–76 minutes past the window end, wrong day — the card still RENDERED
  in the Nova Now search surface (32°/Snow, Khamovniki).
- Condition-row check (0:16, Settings ▸ Cards ▸ AccuWeather): the
  Day/time row is STILL ATTACHED with the original window — no auto-
  removal 2h20m past the end. This matches the earlier 17h-past-window
  persistence anomaly; the single observed auto-disappearance remains
  unexplained.
- CONSEQUENCES: (a) attached Day/time conditions do NOT auto-expire;
  (b) an expired/out-of-window condition does NOT block card rendering —
  OBS-CARDS-112's "render requires condition met" is REVISED: the
  condition gate did not prevent renders at 0:04–0:13. Either the gate
  evaluates only at attach/enable time, or expiry enforcement is broken
  for built-in cards.
- **NEVER-SATISFIED TEST (0:35–0:40, closes the open question)**: attached
  a Wi-Fi condition "When connected to MGTS_GPON_9FqM network" while the
  device was on RG-5WiFi-68a4 (condition permanently false) — the editor
  pre-fills the first scan-result SSID, which was NOT the current
  network; BLUETOOTH conditions were not attachable on this phone ("No
  paired devices", Add disabled). Saved via the card's Save button
  (exit dialog verified), then re-rendered the Nova Now search: **the
  card STILL RENDERED (32°/Snow)** with the never-satisfied Wi-Fi
  condition attached. Re-opened Card details from the render itself:
  BOTH rows attached (Day/time Wednesday + Wi-Fi MGTS) — the save and
  attach verified end-to-end. CONCLUSION: attached conditions are
  entirely NON-GATING for the Nova Now search render — neither expired
  Day/time nor a never-satisfied Wi-Fi condition blocks. The render
  requirements remain: Activate toggle ON + Location granted + typed
  query. (A condition satisfied LATER would presumably add the card to
  the plain drawer per the editor copy "will cause this card to show in
  the app drawer" — untested; the search surface ignores conditions.)
- Side observations: tapping the attached Wi-Fi condition row itself
  REMOVES it (no separate X control on that row); the condition editor
  "Add" button stays disabled until a device/network is chosen; the
  BLUETOOTH_CONNECT runtime permission grants cleanly via adb but the
  condition is unusable without a paired device (revoked after test).
- **PLAIN-DRAWER / LATER-SATISFIED TEST (0:51–0:57, closes the last
  open branch)**: attached a Wi-Fi condition for the CURRENT network
  (RG-5WiFi-68a4 — attachable via the SSID dropdown after scrolling) and
  exercised the full state transition while the drawer was opened fresh
  at every step: (a) condition SATISFIED (Wi-Fi connected) → no card in
  the drawer's divider slot; (b) Wi-Fi disabled via `svc wifi disable`
  (condition false) → still no card; (c) Wi-Fi re-enabled and the
  reconnection verified at system level (SSID RG-5WiFi-68a4, supplicant
  COMPLETED, IP up) → the condition flipped false→true WHILE attached,
  two fresh drawer opens → STILL no card. The card never appears in the
  plain drawer under any condition state; the divider slot stays empty.
- **OVERALL CONDITION SEMANTICS (this build)**: the condition system is
  decorative — conditions neither gate the Nova Now search render
  (expired AND never-satisfied both render; OBS-CARDS-117 above) nor
  surface the card in the plain drawer (satisfied, false, or false→true
  transition — none show it). The editor copy "when all met, will cause
  this card to show in the app drawer" is UNFULFILLED in 8.9.9, matching
  the welcome sheet's unfulfilled manual-location promise (OBS-CARDS-113)
  — Vesna design lesson: shipping copy must match behavior.
- Test anomalies: mid-screen swipe-up on the workspace pulled the ANDROID
  quick settings instead of the drawer — CAUSE FOUND: the restored
  gestures-section binding ("Expand quick settings") FIRES on the
  workspace and preempts the drawer-open config (see OBS-GESTURE-112
  refinement); the search pill's grid icon was the deterministic drawer
  route. `svc wifi` toggling worked cleanly with reconnection in ~10 s.
- Test cleanup: BOTH test conditions were removed after their tests
  (MGTS at 0:45, RG-5 at 0:58 — row tap + Save); removal SEALED by a
  post-save re-open of the AccuWeather Card details (1:06): only the
  original Wednesday Day/time condition remains, the Wi-Fi row is
  unattached. Wi-Fi left ON and reconnected; no other settings changed.
- Corrections issued: OBS-CARDS-113 "expired during the checks" → the
  window ended but the condition persists; OBS-CARDS-115's artifact note
  "expires tonight" → did not expire.
- Evidence: EVI-CARDS-001 (overlay-card-render, rerender-stamp,
  cond-check, bt-test-start, bt-plus-tap, wifi-picker, wifi-cond-attached,
  never-satisfied-render, cond-verify-saved, wifi-row-tapped,
  cleanup-done, drawer-checkA3, drawer-wifi-off, drawer-wifi-reconnected,
  drawer-final-check, rg5-verify, cleanup-removed), EVI-SETT-003
  (search-fahrenheit2)
- Confidence: Observed
