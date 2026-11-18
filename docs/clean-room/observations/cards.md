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
  1. Settings search: "location", "weather", "city" — ALL zero results
     (search indexes titles AND subtitles; the "dock" control query returns
     two matches, proving the index works).
  2. Card details page (AccuWeather): no location field; overflow (⋮) has
     only "Reset to default"; "See forecast details >" is inert.
  3. Cards list page: card entries only (Media controls, AccuWeather,
     Games, Photos, Screenshots) — no location row.
  4. Settings ▸ Integrations (complete inventory): Calendar ("Your
     upcoming events"), Spotify, Deezer, Twitch, Slack, Discord, Dropbox,
     GitHub, Sesame Shortcuts — NO weather/AccuWeather/location entry.
- CONCLUSION: the manual-location affordance advertised by the welcome
  sheet does not exist anywhere in Nova 8.9.9's UI. The Weather/AccuWeather
  card's ONLY location path is the precise-Location permission grant
  (verified working via adb grant: the card then rendered live district
  data — see OBS-CARDS-115). The unfulfilled sheet promise is a Nova
  quality gap and a Vesna design lesson: every advertised fallback must be
  implemented and discoverable.
- The Day/time condition (Wednesday 21:57–22:57) expired during the checks
  (00:00 Thursday); its earlier render verification stands (OBS-CARDS-115).
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
  card (expires tonight; removable via Cards ▸ AccuWeather ▸ Day/time).
  Welcome-sheet Calendar/Weather toggles left ON (user-facing feature,
  enabled state).
