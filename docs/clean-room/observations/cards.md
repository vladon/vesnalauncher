# Observations — Cards (ENV-1, Nova 8.9.9 free)

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
