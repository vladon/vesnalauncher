# Observations — Widgets (ENV-1, Nova 8.9.9 free)

Evidence under `EVI-WIDGET-001`. Widget placed during test: system Clock
"Analog" (declared 2×2), resized to ~2×3, then removed.

## OBS-WIDGET-001 — Widget placement flow

- Date / Env: 2026-11-17 / ENV-1
- Action: long-press empty workspace ▸ drag widgets sheet up to full-screen
  picker ▸ expand Clock group ▸ drag the "Analog 2 × 2" preview onto an empty
  workspace cell (`input draganddrop`, 1.5 s).
- Observed, in order:
  1. While dragging from the picker, a floating widget preview follows the
     pointer over the workspace.
  2. On drop, the **system widget-creation consent dialog** appears
     ("Create widget and allow access?" with an application/widget
     attribution and an optional "Always allow …" checkbox; Cancel/Create).
     This is the platform's widget-binding consent (Android 11+), not
     launcher UI.
  3. After Create, the widget provider's **configuration activity** opened
     ("Select a Clock Face": Scallop/Numeral/Ring…). Choosing a face
     completed placement.
  4. Immediately after placement the widget enters **resize mode
     automatically**: dashed selection frame with corner/edge handles and an
     edit (pencil) affordance, plus a toast "Tap to change widget settings".
  5. Tapping elsewhere exits resize mode; the widget remains placed.
- Evidence: EVI-WIDGET-001 (picker-full, clock-group, widget-drop,
  widget-placed, widget-final, after-dismiss)
- Confidence: Observed

## OBS-WIDGET-002 — Widget resize

- Action: long-press the placed widget ▸ popup ▸ Resize; drag the bottom
  handle down one row.
- Observed: resize frame expands (~2×2 → ~2×3); the widget content
  re-centers within the new bounds; resizing happens live during the drag.
- Widget popup menu (long-press): Widget settings, Custom color, App info,
  Remove, Padding, Resize.
- Evidence: EVI-WIDGET-001 (widget-popup, resize-mode, resized)
- Confidence: Observed

## OBS-WIDGET-003 — Widget removal with undo

- Action: widget popup ▸ Remove.
- Observed: the widget is removed immediately (no confirmation dialog) and a
  snackbar appears at the bottom of the workspace: "Item removed" with an
  "Undo" button (consistent with the "Show undo bar" setting, on by default).
  Undo was NOT exercised in this pass (to verify: restores widget, position,
  and size).
- Evidence: EVI-WIDGET-001 (popup-again, removed)
- Confidence: Observed (removal); undo restore verified separately in
  OBS-WIDGET-005

## OBS-WIDGET-005 — Undo restores the removed widget (verified)

- Date / Env: 2026-11-17 / ENV-1
- Action: placed a Numeral-clock widget at workspace rows 1-2 / cols 1-2,
  opened its popup, tapped Remove, then tapped Undo on the "Item removed"
  snackbar within its timeout (the timed sequence had to run in a single
  scripted step; a first manual attempt missed the snackbar window — the
  snackbar lifetime is a few seconds).
- Observed: the widget reappeared at its exact previous position and grid
  size with its configuration intact (Numeral face — a different face than
  the earlier placement, proving configuration restore rather than a default
  re-placement).
- Additional observation: after checking "Always allow Nova Launcher to
  create widgets and access their data" in the consent dialog once, a later
  widget placement skipped the consent dialog entirely.
- Evidence: EVI-WIDGET-004 (popup-numeral, removed-snackbar, after-undo
  [failed attempt], re-placed, undo-timed [success])
- Confidence: Observed
