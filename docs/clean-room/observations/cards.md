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

## OBS-CARDS-113 — The advertised manual-location fallback does not exist

- Date / Env: 2026-11-19 ~03:51 / ENV-1 (Nova Prime)
- Context: the Nova Now welcome sheet offers "Alternatively, set a manual
  location in Nova Settings" for the Weather Card.
- Action: searched Nova Settings for "location" and then "weather" via the
  settings search field (search indexes titles AND subtitles — verified
  earlier with the "dock" query returning two matches).
- Result: **both queries returned zero results** — no manual-location (or
  weather) setting exists anywhere in Nova Settings 8.9.9. Additional checks
  found no location field on the Card details page, the Cards list page
  (which also holds Games/Photos/Screenshots cards), or the Card details
  overflow menu (whose only item is "Reset to default"). "See forecast
  details >" on the card preview is inert (does not navigate).
- Conclusion: the Weather Card's only render path in this build is the
  precise-Location permission grant; the advertised manual-location
  fallback is not implemented/reachable. This makes the weather card
  effectively render-less without a privacy-sensitive grant — recorded as
  a Nova quirk and a Vesna design lesson (fallbacks advertised in UI copy
  must exist).
- The Day/time condition (Wednesday 03:20+1 h) remained ACTIVE throughout
  (03:44–03:52 observations inside the window).
- Evidence: EVI-SETT-003 (search-location, search-weather2),
  EVI-CARDS-001 (details-r6, details-scroll, details-overflow,
  forecast-details, cards-scrolled-r6)
- Confidence: Observed

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
