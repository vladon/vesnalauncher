# Vesna 0.4.8 — drag input delivery to compositor presentation

On an Android 15 emulator, all **15 of 15** measured drag moves matched a presented Home frame without speculative matching. Median delivery-to-presentation was **36.6 ms** (range **33.5–37.9 ms**). This interval starts when the event is delivered to the app, not when a finger touches the screen.

| Trial | Move 3 (ms) | Move 4 (ms) | Move 5 (ms) |
| --- | ---: | ---: | ---: |
| 1 | 36.7 | 35.1 | 36.8 |
| 2 | 36.8 | 36.3 | 37.1 |
| 3 | 37.9 | 33.5 | 36.1 |
| 4 | 36.0 | 37.0 | 36.6 |
| 5 | 37.0 | 35.5 | 35.3 |

## Workload

- Signed [Vesna 0.4.8](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.8), `versionCode` 13, SHA-256 `428eaaf22e910dc60d8a7fed95bd0fa7b55b213efa13cf0707008c025eb09695`.
- Read-only `Vesna_Signed_Upgrade_QA` (Android 15/API 35, userdebug), `emulator-5576`, 1080 × 1920 at 420 dpi, on Windows with `swiftshader_indirect`. SurfaceFlinger reported a 16.67 ms refresh period. Used the [drawer benchmark's fixture](VESNA-QA-DRAWER-FRAMES.md) with 120 launchable aliases.
- Each trial opened the drawer and began a touch hold on Frame App 001. After the first two moves transferred the drag to Home, the script waited 500 ms before moving across three different workspace cells, with a 200 ms pause between input commands. It waited another 200 ms before cancelling. The drawer had closed, and all visible Home and dock shortcuts remained in place. The first trial was excluded as warmup, and the two handoff moves were excluded from every trial.

## Measurement

Captured `gfx`, `view`, and `input` atrace categories and [SurfaceFlinger FrameTimeline](https://perfetto.dev/docs/data-sources/frametimeline) in a seven-second trace per trial. Trace Processor 59.1 located the launcher PID's `deliverInputEvent` slices for the five touchscreen moves. It followed slice ancestry to the numbered `Choreographer#doFrame`, matched that VSYNC ID to the same process's non-dropped Home surface frame, then joined its display token to the display frame. Latency is `display.ts + display.dur − deliverInputEvent.ts`. Results omit unmatched or speculative frames rather than substituting the next frame.

The pilot's package-filtered input-event source produced no raw motion packets, so this run uses the app's atrace delivery timestamp, not a hardware or kernel sample. Perfetto reported unsupported or unavailable atrace/ftrace setup probes on the emulator, but no trace parsing errors or data-loss counters; all expected move slices and all eligible exact matches were present. Raw traces, the config, SQL query, and results are in `D:/orca/scratch/vesna-drag-latency/`, outside Git. No input or traces were collected from the physical phone.

This baseline covers synthetic steady drag moves on one software-rendered emulator. It excludes physical touch-to-photon, the long-press and drawer-to-Home transition, continuous touch input, and drop-commit latency. It does not by itself identify an optimization target.
