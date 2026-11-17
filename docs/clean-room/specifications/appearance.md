# Specifications — Appearance and Personalization

Derived from `observations/appearance.md` and Style-tab inventory
(OBS-SETT-010).

## APPPEAR-100 — Theme mode selection with instant application

- Description: the launcher offers a theme mode control with system-follow,
  fixed light/dark, automatic, and scheduled options; changes apply
  immediately across all launcher surfaces without restart.
- Trigger: changing Mode in the Style settings.
- Expected behavior:
  1. Mode options: Follow system, Light, Dark, Auto, Schedule.
  2. Switching modes re-themes open UI instantly (settings, drawer,
     workspace chrome, popups) with no restart.
  3. Follow system tracks the OS dark-mode setting.
- Configuration parameters: Mode; Palette (color source, default System);
  per-surface text/background color overrides for 9 surfaces.
- Persistence: mode change persists across restart (part of theme config;
  reboot-level persistence to verify).
- Evidence: OBS-APPPEAR-001 (EVI-APPPEAR-001)
- Confidence: Observed (mode switch); Palette and per-surface colors Not
  Tested.

### Vesna requirements

- R-APPPEAR-100.1: Provide theme modes: follow-system, light, dark, plus
  time-based options (auto/schedule).
- R-APPPEAR-100.2: Theme changes apply live to every launcher surface
  without restart.
- R-APPPEAR-100.3: Offer a palette/color-source control and per-surface
  text/background color overrides.
- R-APPPEAR-100.4: Theme configuration persists across restarts.
