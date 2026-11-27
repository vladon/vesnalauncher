# QA — Widget removal with undo (ENG-102 slice 2b, v0.4.113)

**Scope:** the placed-widget options (a long press on the widget cell opens
the Vesna popup with **Remove widget**), the removal with undo per the
ADR-0004 ordering: the Room record first, the host binding released only
after the undo window closes.

## Evidence (emulator-5560, API 35, ru locale, 2026-11-27)

| Check | Result |
| --- | --- |
| The long press on the live widget opens the cell options popup (the gesture detector observes the gesture; taps still reach the widget) | pass |
| Remove → the widget disappears immediately, the snackbar «Виджет удалён / Отменить» is shown, **the binding stays alive** (`widgets.size=1`) | pass |
| Undo within the window → the record re-inserted, the widget reappears with the same binding, no rebind dialog (`widgets.size=1`, the host view rendered) | pass |
| The window expires → the binding is released (`widgets.size=0`), the ID no longer bound | pass |
| `WidgetHostLifecycleTest` 4/4 (the host contract incl. the record-before-binding deletion order) | pass |
| `WorkspaceResolverWidgetTest`, `:app:lintDebug`, `:app:testDebugUnitTest` | pass |

## Notes

- A rebind crash was caught on-device and fixed in the same commit:
  `findChildForCell` cast children to `AppIconView`, which threw a
  `ClassCastException` for a `WidgetCellView` when the state re-emitted with
  a placed widget (the restart crash loop). It is now type-agnostic.
- The undo re-insert keeps the exact record (the cell, the spans, the
  provider), so the undo never re-opens the bind dialog.
- Resize and drag-move of a placed widget remain future slices; the removal
  with undo (`WIDGET-102`) is complete.
