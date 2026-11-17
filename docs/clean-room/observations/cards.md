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
