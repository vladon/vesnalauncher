# Specifications — Folders

Derived from `observations/folders.md` and folder settings inventory
(observations/settings.md OBS-SETT-004).

## FOLDER-100 — Folder creation by icon-on-icon drop

- Description: dragging one workspace app icon onto another creates a folder
  containing both.
- Preconditions: two app icons on the workspace; drag engaged per HOME-103.
- Trigger: drop an item onto an occupied app-icon cell.
- Expected behavior:
  1. A folder is created at the target cell containing both apps.
  2. The source cell becomes empty.
  3. The new folder icon renders a mini preview of contained app icons
     (per folder icon appearance setting).
  4. The new folder is unnamed; the folder window shows an "Edit Name"
     placeholder until named.
- State transitions: two icon cells → one folder cell + one empty cell.
- Evidence: OBS-FOLDER-001 (EVI-FOLDER-001)
- Confidence: Observed

### Vesna requirements

- R-FOLDER-100.1: Dropping an app icon onto another creates a folder at the
  target position containing both apps.
- R-FOLDER-100.2: The folder icon previews its contents.
- R-FOLDER-100.3: New folders start unnamed.

## FOLDER-101 — Folder window

- Description: tapping a folder opens an anchored window showing contents
  and folder controls.
- Expected behavior:
  1. A rounded window anchored near the folder icon shows contained apps
     with labels in an auto-sized grid.
  2. A name row allows renaming; changes commit live (icon label updates).
  3. An overflow control exposes folder actions (contents Not Tested).
  4. BACK dismisses the window.
- Configuration: window mode, shadow, corner radius, show folder name, grid
  AUTO, scroll direction (settings inventory OBS-SETT-004).
- Boundary conditions: contents beyond one window page paginate (pager dots
  seen in the settings preview mock).
- Evidence: OBS-FOLDER-002 (EVI-FOLDER-001)
- Confidence: Observed (open/rename/close); pagination and drag-into Not
  Tested.

### Vesna requirements

- R-FOLDER-101.1: Tapping a folder opens an anchored folder window; the
  workspace remains visible around it.
- R-FOLDER-101.2: Contents are shown as a labeled auto-grid; multiple pages
  paginate.
- R-FOLDER-101.3: The window offers rename; the change applies live to the
  desktop label.
- R-FOLDER-101.4: BACK dismisses the window.
- R-FOLDER-101.5: Folder window styling follows folder appearance settings
  (shadow, radius, name visibility).
