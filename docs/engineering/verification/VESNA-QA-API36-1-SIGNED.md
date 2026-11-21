# Android 16 / API 36.1 — signed Vesna 0.4.10

The public signed 0.4.10 APK upgraded 0.4.9 in place on an Android 16 emulator. Both visible Home pages, the selected page, and the dock were retained. This follow-up complements the earlier [API 36.1 debug runs](VESNA-QA-006-API36-1.md); it uses the released APK and real input, not a new instrumentation run against launcher code.

| Check | Observed result |
| --- | --- |
| Signed upgrade and cold launch | Version code 14 → 15; page 2 remained selected, with a test shortcut intact. Page 1 retained all 20 visible shortcuts; the four shared dock shortcuts stayed in order. |
| HOME eligibility | Android resolved Vesna's `HomeActivity` for `MAIN`/`HOME`. Pixel Launcher remained the default HOME holder throughout; no role was granted. |
| Page navigation | A one-finger swipe moved between pages in both directions. Replacing a finger before the swipe threshold or adding a second finger after it left the original page and layout unchanged. |
| Drawer gestures | A downward swipe starting below the top returned to the top without dismissing. A second swipe from the top dismissed. Cancelling a stationary hold did not affect the next ordinary scroll. |
| Search and launch-return | Searched for `Frame App 001`, opened the unique result, and used Android Back to return to the drawer. Closing it restored the first page and dock. |
| Theme, text, and keyboard | Light Home at 100% and Dark Home and drawer at 200% text scale. Tab reached the named Next page control with a visible focus ring; D-pad Center advanced to page 2. The drawer close and page indicator measured 48 dp high. |
| Platform stability | SurfaceFlinger and system_server PIDs did not change. No Vesna or RegionSampling crash was present in the crash buffer. |

## Configuration and scope

- `Vesna_QA_API_36_1_CLI22` (`emulator-5564`), `ro.build.version.sdk_full=36.1`, build `BE4B.251210.005`, 1080 × 2400 at 420 dpi. Ran read-only, headless, software SwiftShader, with GLDMA and GLDMA2 disabled as in the earlier working configuration.
- [0.4.9 APK](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.9) SHA-256: `743b529f243a4996db2e9a444ee209f422565e551f263a9586f85a670aeb164f`. [0.4.10 APK](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.10) SHA-256: `c791803a687152aecaa864d3bae0b5d61a528ab1629a38f192c11ecae3d9d900`.
- Used a test APK with 120 launchable aliases and a separate test-only injector for the two-pointer replays. Created and filled page 2 via Vesna's controls before the in-place update. The layout comparison describes visible shortcuts and bounds, not a direct database byte comparison.
- Stopped the disposable AVD. Raw XML, screenshots, package and launch evidence are in `D:/orca/scratch/vesna-api361-signed/`. Default HOME selection, TalkBack, and physical devices were not exercised.
