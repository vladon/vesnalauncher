# Vesna 0.4.7 — app drawer scroll frame baseline

The signed [Vesna 0.4.7 release](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.7) recorded **10 janky frames out of 386 (2.59%)** across five repeats of the same scroll. The Android-reported p90 was 17 ms in every repeat. This captures warmed scrolling on one emulator, not first-open performance or physical-device latency.

| Trial | Rendered frames | Janky frames | p50 (ms) | p90 (ms) | p95 (ms) | p99 (ms) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 77 | 2 | 16 | 17 | 18 | 18 |
| 2 | 77 | 2 | 16 | 17 | 17 | 18 |
| 3 | 78 | 2 | 16 | 17 | 17 | 18 |
| 4 | 77 | 2 | 15 | 17 | 17 | 18 |
| 5 | 77 | 2 | 16 | 17 | 18 | 18 |

## Workload and procedure

- Android 15 (API 35), `Vesna_Signed_Upgrade_QA`, serial `emulator-5576`, on the Windows host. The emulator ran headless and read-only with software graphics (`swiftshader_indirect`), 1080 × 1920 at 420 dpi, 100% font scale, and a 16.67 ms refresh period reported by SurfaceFlinger.
- Used the exact public APK: `versionCode` 12, SHA-256 `772824e8b29f0a964e6e5af1be2e71c2dde38dc3df6b08c4be653eb509aa7874`. An additional APK declared 120 launchable aliases (`Frame App 001`–`120`); Android enumerated all 120. They share one package and reuse 12 framework icons, so this does not represent discovery or rendering of 120 distinct installed packages and their artwork.
- Each trial opened the drawer from Home and verified the same viewport before resetting graphics statistics (`adb -s emulator-5576 shell dumpsys gfxinfo dev.vladon.vesna.launcher reset`). Then it sent `input swipe 540 1632 540 657 700`, waited 1.2 seconds for the scroll to settle, and read `dumpsys gfxinfo dev.vladon.vesna.launcher framestats`. The first visible tile moved from Calendar to Frame App 014 in all six swipes. Drawer open/close animations were outside the sampled window.
- Used the process summary’s `Janky frames` counter, not `Janky frames (legacy)` or the duplicate window totals. Percentiles are Android’s per-trial frame duration summaries, not pooled percentiles or input-to-display latency.

One preliminary series was interrupted and excluded in full. The complete series excluded its first swipe as warmup (77 frames, 3 janky, p90 28 ms). Raw `gfxinfo` output, before/after accessibility trees, and JSONL results are in `D:/orca/scratch/vesna-drawer-frames/results/complete-series/`; the fixture manifest generator is next to that `results` directory. The read-only emulator was stopped after collection; the physical phone was not touched.

The stable repeat measurements do not identify a drawer bottleneck. Keep this as a comparison point; investigate a distinct cold-open or physical-device workload before changing the drawer rendering path.
