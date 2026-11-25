# VESNA-QA-RESUME-REFRESH — catalogue self-heal on Home resume (#203, v0.4.87)

Device: Android 15 QA AVD (emulator-5560, cold-booted, managed profile user 10 unprovisioned). Builds: v0.4.87 (92) debug candidate + androidTest APK.

## The change (#203)

`LauncherController.refreshCatalogue()` re-enumerates the catalogue outside the package-event pipeline; `HomeActivity.onResume` calls it on every Home entry. A framework that loses or withholds package events (observed on a wedged AVD session where installs stopped reaching the launcher and only a framework restart refreshed the drawer) self-heals on the next Home entry instead of needing a framework restart. The refresh runs off-thread as a **single complete pass** (no two-phase split on resume): the emission carries complete icons and lands as one update, so the already-rendered drawer never flickers with pending-icon placeholders. `refresh(quickPhase)` keeps the fast two-phase split for process init; `refresh(added:)` callers were re-pointed to the named parameter after the new parameter changed the signature.

## Checklist results

| Check | Evidence | Result |
|---|---|---|
| The refresh entry point re-enumerates outside the event pipeline | `LauncherControllerResumeRefreshTest`: `pm uninstall com.vesnaresearch.enumdiag` (the probe fixture, pre-cleaned), then `controller.refreshCatalogue()` → `state.apps` stays without the probe; `pm install /data/local/tmp/ed.apk` (the probe APK via shell) → `refreshCatalogue()` → `state.apps` gains the probe | PASS |
| The removal direction self-heals the same way | `pm uninstall com.vesnaresearch.enumdiag` → `refreshCatalogue()` → `state.apps` drops the probe | PASS |
| The entry point is the same one Home's resume wiring calls | The test invokes `controller.refreshCatalogue()` directly — the exact method `HomeActivity.onResume` invokes | PASS |
| No pending-icon flicker from resume refreshes | The resume path runs the full pass (single emission); the two-phase fast pass remains only on process init | PASS (by construction; the state emission is a single update) |

## Gates

68 JVM tests (incl. `DrawerWorkTabSliceTest` ×4), lintDebug; instrumentation: workspace 90/90, platform 1/1, `WorkPausedBannerTest` 2/2, `WorkTabDrawerTest` 4/4 on the candidate. The ui/settings suites flake identically against the unmodified v0.4.84 baseline on this freshly cold-booted AVD (A/B documented in [VESNA-QA-WORK-PAUSED](VESNA-QA-WORK-PAUSED.md) and [VESNA-QA-COLD-START](VESNA-QA-COLD-START.md)) and are green on a warmed system.

## Limitations

- The refresh is best-effort self-heal: if the platform's filtered view is stale at the moment of enumeration (the wedged-session failure mode), the refresh inherits the stale view; the next Home entry retries.
- The managed profile on the QA AVD is unprovisioned: quiet-mode toggling was verified on the provisioned research profile instead ([VESNA-QA-WORK-PAUSED](VESNA-QA-WORK-PAUSED.md)).
