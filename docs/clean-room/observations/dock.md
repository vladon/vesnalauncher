# Observations — Dock (ENV-1, Nova 8.9.9 free)

Evidence under `EVI-DOCK-001`. Dock subpage path: Settings ▸ Home screen ▸
Dock ("Icon layout, background, pages").

## OBS-DOCK-001 — Dock settings inventory

- Date / Env: 2026-11-17 / ENV-1
- Observed (Dock subpage, top to bottom; live preview strip shows dock icons
  over the wallpaper):
  - Enable: toggle ON.
  - Layout: Icon size slider (125%, disabled/greyed while "Match desktop
    size" is checked); Match desktop size checkbox (checked); Label dropdown
    + toggle (OFF — dock icons show no labels by default); Padding value
    "Auto × Medium"; Dock background dropdown + toggle (OFF — no background
    plate behind dock icons by default).
  - Dock icons: count selector rendered as a segmented control (5 segments,
    all filled — current count 5).
  - Dock pages: count (1).
  - Infinite scroll: toggle OFF.
  - Advanced (collapsible): Display as overlay toggle (ON; subtitle "Places
    the dock above the home screen for a smoother experience when using a
    dock toggle gesture"); Automatically close checkbox (GREYED/disabled;
    subtitle "Hide dock once an app is launched (best with a gesture to
    toggle the dock)"); Remember position checkbox (unchecked).
- Evidence: EVI-DOCK-001 (dock-page1, dock-page2, dock-advanced)
- Confidence: Observed (inventory); behaviors Not Tested

## OBS-DOCK-002 — Dock rendering on the workspace

- Observed: with defaults, the dock shows 5 app icons without labels and
  without a background plate, above the search bar ("Dock (Below icons)"
  search placement). Dock remains fixed while swiping workspace pages (to be
  verified across multiple pages — single page at observation time).
- Evidence: EVI-HOME-002/EVI-FOLDER-001 workspace captures
- Confidence: Observed (presence/position); paging behavior Not Tested

## OBS-DOCK-003 — Drawer grid dimension reading

- Note (inventory accuracy): the drawer renders 5 app columns while the
  "Drawer App Grid" setting displays "6 ✕ 5". Nova's X-by-Y label is read
  here as rows × columns for the drawer (6 rows × 5 columns) — i.e. the
  label order for the drawer grid differs from the desktop grid reading
  ("Desktop grid 6 ✕ 5" renders 6 columns). To verify precisely by changing
  the value during behavioral testing (planned; not yet performed).
- Evidence: EVI-DRAWER-001 (drawer-open), EVI-SETT-002/003
- Confidence: Partially Observed
