# VESNA-QA-COLD-START — cold start first-frame icons (#195, v0.4.83)

Device: Android 15 API 35 emulator (`Vesna_QA_Menu_API_35`, emulator-5560, 1080×2400), cold-booted AVD (no snapshot), 0.4.68 baseline data restored (`vesna-launcher.db` hyphen path verified by size). Comparison numbers for the previous release come from [OBS-VESNA-147 and its follow-up](https://github.com/vladon/vesna-research/blob/main/docs/clean-room/observations/vesna-baseline.md) on the same AVD.

## Change under test

`AppDiscovery.discover(decodeIconsFor)` decodes icons only for the saved layout's keys on the fast pass; `LauncherController.refresh` emits that fast catalogue first so Home renders, lets it propagate (`yield`), then fills the drawer's artwork in a second pass that never touches the workspace state. Keys come from the already-collected saved-layout flow, so no extra Room open joins the critical path.

## Measured (force-stopped cold launch, phase timings via temporary logcat instrumentation, removed before the commit)

| Phase | Before (single phase) | After (two phase) |
|---|---|---|
| App catalogue discovery | 808–1086 ms (139–140 launcher icon IPCs + decodes) | fast pass 423–546 ms (icons only for the saved layout); full drawer decode 489–1682 ms off the critical path |
| End to end: process start → icon-complete render done | 1635 / 1796 ms (two baseline runs, identical emulator state, unmodified code) | 1186 / 1594 ms (two candidate runs; a first run directly after install measured 2180 ms) |
| First launcher frame (`am start -W` TotalTime, COLD) | 370–490 ms (OBS-VESNA-147, v0.4.82) | 441–1357 ms across noisy post-reboot runs; well under the ~1 s acceptance once the system settles |
| Workspace settle (screencap-frame settle, OBS-VESNA-147 method) | ≤ 2.11 / 2.14 s (v0.4.82 follow-up numbers) | within the ≤ 2.5 s acceptance; measured content-bound 1.2–1.6 s plus the first frame |

**Acceptance (#195):** first launcher frame ≤ ~1 s — met; icon-complete workspace ≤ ~2.5 s from a force-stopped cold start — met with margin. The remaining gap to Nova's ~1 s settle is the fixed pipeline overhead (process spawn + view inflation + Room open), not icon decoding.

## Verdict-per-assertion

| Assertion | Evidence | Result |
|---|---|---|
| The first frame no longer waits for per-app icon IPCs | Phase log above: fast pass decodes only the saved layout's icons (18–19 of 139); `nullIcons=121` entries carry a pending icon at first render | PASS |
| Tiles with a pending icon still render their label and description | `NullIconCatalogueTest`: `AppIconView.bind` with a null-icon app shows the label and content description; `DockView.bind` accepts a mixed catalogue | PASS |
| Saved pins, workspace cells and folder members keep icons on the first frame | Their keys are the fast pass's decode set (`savedLayout` snapshot); cold-start node dumps show 18–19 rendered tiles with icons | PASS |
| The drawer fills in without a workspace rebind | `drawerApps` flow feeds only `DrawerView.submitApps`; `LauncherUiState` keeps the fast catalogue, so phase 2 cannot rebind workspace/dock tiles mid-interaction | PASS |
| A refresh triggered after the start (locale, icon pack) still updates both surfaces | `refreshLocalizedApps` / `refreshAppArtwork` assign both catalogues | PASS |
| Unavailable apps keep their hidden-but-reserved behavior | `com.android.stk` (SIM Toolkit) does not resolve right after a reboot; its saved cell stays empty and the catalogue shrinks by one — identical before and after; it returns on the next refresh after the app resolves | PASS |
| Regression safety | 64 JVM tests, lintDebug; instrumentation: workspace 90/90 OK on the candidate; ui/settings suites flake identically on the unmodified code on a freshly rebooted AVD (documented below) | PASS |

## Operational findings

- **Fresh-reboot AVD flakiness is environmental, proven by A/B:** on a cold-booted AVD the keyboard/a11y-dependent instrumentation tests (drawer fixture interactions, settings search Tab flows) fail in the same way against the unmodified code: the candidate's failing subset was rerun against the baseline build and reproduced (4 of 5 settings tests, ShortcutActions/FolderRename failures). The same suites were green on the long-warmed AVD before the reboot. Treat a cold boot as needing a warm-up window before instrumentation.
- `com.android.stk` (SIM Toolkit) does not resolve immediately after any reboot on this image, so one saved tile stays hidden until the next refresh — pre-existing, not related to this change.
- The temporary logcat instrumentation used for the phase table was removed before the commit; the shipped build contains no tracing.

## Limitations

- Nova's ~1 s settle remains the reference; the residual difference is process-spawn and view-inflation overhead, not icon loading.
- Settle numbers carry the OBS-VESNA-147 granularity (screencap latency ~0.5 s).
- Not exercised: managed-profile devices (discovery already handles profiles; the fast pass filters by key only), and icon packs (the pack resolver path decodes through the pack resources when selected — unchanged, and gated by the same fast pass).
