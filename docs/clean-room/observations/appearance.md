# Observations — Appearance and Personalization (ENV-1, Nova 8.9.9 free)

Evidence under `EVI-APPPEAR-001`.

## OBS-APPPEAR-001 — Theme Mode switch (instant, all surfaces)

- Date / Env: 2026-11-17 / ENV-1
- Navigation: Settings ▸ Style tab ▸ Mode
- Action: changed Mode from "Follow system" to "Dark", then back.
- Observed:
  - Mode dropdown offers: Follow system, Light, Dark, Auto, Schedule.
  - Selecting Dark re-themed the settings app INSTANTLY (no restart, no
    reload flash): background, cards, bottom bar, status/nav areas.
  - The drawer re-themed as well (dark background, light labels) on next
    open; workspace chrome follows the theme.
  - Reverting to "Follow system" restored the light theme instantly.
- Additional inventory in the same view: Palette [System] with a color-wheel
  control (dynamic-color extraction; deeper behavior untested); per-surface
  text-color/background-color rows (9 surfaces).
- Evidence: EVI-APPPEAR-001 (style-tab, mode-dropdown, mode-dark,
  drawer-dark, mode-reopen, mode-reverted)
- Confidence: Observed

## OBS-APPPEAR-002 — Settings bottom rows (bonus capture)

- Observed while leaving the Style tab: main settings list ends with
  "Say thanks" (Play Store comment link), "About" (launcher information,
  open-source licenses, translation information), and "Contact support"
  (feedback/contact entry).
- Evidence: EVI-APPPEAR-001 (drawer-dark capture showing the section)
- Confidence: Observed
