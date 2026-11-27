# QA — Widget host mechanism (ENG-102 slice 1, v0.4.110)

**Scope:** the widget-host mechanism only (`WidgetHostManager`, the additive
`widgets` Room table, the startup reconcile). No user-facing placement UI
ships in this release; the fixture provider lives in the debug source set.

## Evidence (emulator-5560, API 35, 2026-11-27)

| Check | Result |
| --- | --- |
| `WidgetHostLifecycleTest` — allocate gives a fresh host ID, delete removes it | pass |
| Reconcile removes an orphaned host ID (no committed row) without touching committed rows | pass |
| A committed row (and its host ID) survives the reconcile | pass |
| Deletion order: the Room record first, then the binding release | pass |
| `:app:testDebugUnitTest` | pass (no failures) |
| Release hygiene: `assembleRelease` badging contains no fixture provider | pass — `aapt2 dump badging` grep "fixture" = 0 hits |
| Debug Home launches with the reconcile wired; no crash at start | pass |

## Notes

- The release APK must never contain `WidgetFixtureProvider`; the provider,
  its layout and `appwidget-provider` metadata are in `app/src/debug/` only.
- The reconcile deletes unused host IDs individually (never
  `deleteAllHosts`) so other hosts' IDs on the device are untouched.
- Schema 6 → 7 is additive (`CREATE TABLE IF NOT EXISTS widgets` + two
  indexes); no destructive fallback, per ADR-0004.
