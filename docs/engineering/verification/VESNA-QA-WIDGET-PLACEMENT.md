# QA — Widget placement flow (ENG-102 slice 2a, v0.4.112)

**Scope:** the minimal add-widget flow: the Home-menu entry → the provider
picker → the platform bind grant → the commit at the first free 2x2 cell →
the live `AppWidgetHostView` rendered in the workspace.

## Evidence (emulator-5560, API 35, ru locale, 2026-11-27)

| Check | Result |
| --- | --- |
| `WorkspaceResolverWidgetTest` (JVM): the anchor cell projects, the covered cells are reserved, the other page is ignored, the occupied anchor never displaces the item but still reserves | pass |
| `WidgetHostLifecycleTest` (instrumented): the ADR-0004 host contract 4/4 | pass |
| `WidgetPlacementRenderTest` (instrumented): the render check is gated on the platform bind privilege and is verified in the manual walkthrough below | pass (skipped when ungranted) |
| Manual walkthrough: the Home menu → **Add widget** → the picker lists the installed providers → the fixture → the platform dialog «Создать виджет и разрешить доступ?» → **Создать** → the widget is bound (`widgets.size=1`, provider = fixture) | pass |
| The workspace renders `AppWidgetHostView` at the first free 2x2 anchor, `content-desc="Vesna widget fixture"` | pass |
| Release hygiene: the fixture provider is absent from the release badging | pass (`grep fixture` = 0 hits) |

## Notes

- The bind grant follows the platform model used by third-party launchers:
  `bindAppWidgetIdIfAllowed` first, then the `ACTION_APPWIDGET_BIND` dialog;
  cancellation releases the host ID (ADR-0004 ordering).
- The bind dialog button is locale-dependent ("OK"/"Allow"/"Создать"); the
  automated tap of it is not part of the CI set.
- The placement span defaults to the provider's `targetCellWidth/Height`
  (API 31+) or 2x2; resize and drag of placed widgets are later slices.
