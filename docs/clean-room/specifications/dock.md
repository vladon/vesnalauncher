# Specifications — Dock

Derived from `observations/dock.md` (inventory) and workspace captures.
Behavioral testing of dock interactions is pending; this file covers the
approved inventory-derived structure plus verified rendering.

## DOCK-100 — Dock structure and configuration

- Description: a persistent favorites bar at the bottom of the workspace,
  configurable in count, pages, appearance, and presentation mode.
- Preconditions: dock enabled (default).
- Configuration parameters (observed):
  1. Enable/disable the dock entirely.
  2. Icon count via a discrete selector (observed 5).
  3. Dock pages: number of dock pages (observed 1).
  4. Infinite scroll across dock pages (default off).
  5. Icon size, with a "match desktop size" mode (checked by default; the
     independent size control is disabled while matching).
  6. Labels (off by default for the dock).
  7. Padding (horizontal × vertical, "Auto × Medium" default).
  8. Dock background: style selector + on/off plate toggle (off default).
  9. Presentation: "display as overlay" mode (on default) — the dock renders
     above the workspace to support a dock-toggle gesture; companion options
     "automatically close on app launch" (disabled unless a dock-toggle
     gesture is used) and "remember position" (off default).
- Expected rendering (verified): a single row of icons without labels or
  background, positioned above the workspace search bar.
- Boundary conditions: icon-count selector bounds the dock width behavior
  (exact min/max to verify behaviorally).
- Interactions: search bar placement interacts with dock position
  (HOME-010: "Dock (Below icons)" places search under the dock).
- Persistence: dock contents/config persist across launcher restart
  (workspace-level persistence verified under OBS-HOME-005; dock-specific
  re-test pending).
- Evidence: OBS-DOCK-001, OBS-DOCK-002 (EVI-DOCK-001)
- Confidence: Observed (inventory/rendering); interactions Not Tested

### Vesna requirements

- R-DOCK-100.1: Provide a dock that can be enabled/disabled.
- R-DOCK-100.2: Dock icon count is user-configurable via a discrete
  selector.
- R-DOCK-100.3: Dock supports multiple pages with optional infinite scroll.
- R-DOCK-100.4: Dock icon size may follow the desktop size or be set
  independently (matching disables the independent control).
- R-DOCK-100.5: Dock labels are independently togglable (default off).
- R-DOCK-100.6: Dock padding is configurable (auto/medium default).
- R-DOCK-100.7: Optional dock background plate with style options.
- R-DOCK-100.8: Overlay presentation mode with auto-close and
  position-remembering options for dock-toggle workflows.
