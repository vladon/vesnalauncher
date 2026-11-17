# Observations — Folders (ENV-1, Nova 8.9.9 free)

Evidence under `EVI-FOLDER-001`. Environment: Folders settings: Mode Window,
Shadow on, Corner radius 24dp, Show folder name on, Grid AUTO, Scroll
direction Horizontal.

## OBS-FOLDER-001 — Folder creation by dropping an icon onto another

- Initial state: workspace with Gmail (cell row4/col1) and Photos
  (row4/col2), single page.
- Action: drag Photos onto Gmail (`input draganddrop`, ~1.2 s).
- Observed:
  - the two icons merged into a folder icon at Gmail's cell;
  - folder icon renders as a rounded container with mini previews of both
    app icons side by side (matches "Folder icon appearance" setting);
  - Photos' previous cell became empty;
  - the new folder has NO label initially (placeholder "Edit Name" shown in
    the open folder window).
- Evidence: EVI-FOLDER-001 (dnd-attempt)
- Confidence: Observed

## OBS-FOLDER-002 — Folder window contents and renaming

- Action: tap the folder icon.
- Observed:
  - a rounded folder window opens anchored near the folder (not full-screen);
    background visible around it;
  - contents shown as an auto-sized grid with labels (2 icons → one row of
    two);
  - bottom row: "Edit Name" placeholder text + overflow (⋮) button;
  - typing a name commits it live (field shows "Media" with cursor while
    editing; placeholder replaced by the typed name);
  - closing (BACK) returns to the workspace; the folder icon now shows the
    label "Media" beneath it.
- Dismissal: BACK closes the folder window. (An outside tap did NOT close it
  in one attempt; to re-verify.)
- Evidence: EVI-FOLDER-001 (folder-open, folder-renamed, folder-closed,
  folder-label)
- Confidence: Observed
- Open questions: outside-tap dismissal; overflow (⋮) menu contents; drag
  app into/out of open folder; empty-folder dissolution.
