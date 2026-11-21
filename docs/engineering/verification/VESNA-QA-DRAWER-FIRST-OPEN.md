# Vesna 0.4.8 — first drawer open after a cold launch

On an Android 15 software-rendered emulator, the first open was slower than the repeat open in all three comparable recordings. The median was **544 ms** versus **127 ms** from app dispatch of the button-release event to capture of the complete first app row. These are recorded-emulator timings, not a device performance target.

| Measured pair | First row on first open (ms) | First row on repeat (ms) | Row at rest on first open (ms) | Row at rest on repeat (ms) |
| --- | ---: | ---: | ---: | ---: |
| 1 | 522 | 111 | 554 | 328 |
| 3 | 544 | 127 | 559 | 308 |
| 4 | 600 | 146 | 646 | 326 |

## Workload and measurement

- Exact [signed 0.4.8 APK](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.8), version code 13, SHA-256 `428eaaf22e910dc60d8a7fed95bd0fa7b55b213efa13cf0707008c025eb09695`. Android 15 `Vesna_Signed_Upgrade_QA` (`emulator-5576`), 1080 × 1920 at 420 dpi, 100% text, `swiftshader_indirect`, 120 aliases from the [drawer fixture](VESNA-QA-DRAWER-FRAMES.md). No HOME role changes or physical-device traffic.
- After one excluded warmup, force-stopped the process and verified the PID was absent before each of five launches at least 15 seconds apart. `am start -W` reported COLD and `Status: ok`. The shell immediately tapped the real **Open app drawer** button, waited one second, tapped **Close app drawer**, waited 600 ms, then tapped **Open app drawer** again. Verified the final drawer viewport including the fixture entries.
- Used `input` and `view` atrace slices for the app's `ACTION_UP` dispatch and `screenrecord` at 540 × 960 for the pixels. Matched the complete drawer row against a settled reference while allowing vertical movement, then identified when it reached its final position. Validated [AOSP screenrecord's Winscope timestamps](https://android.googlesource.com/platform/frameworks/av/+/refs/heads/android15-release/cmds/screenrecord/screenrecord.cpp) against decoded frame timestamps and the trace's BOOTTIME clock. This is the first **captured** matching frame; the gap from the prior frame was 19–169 ms on first open and 16–152 ms on repeat.

## Missed inputs and recording overhead

Two recorded pairs (2 and 5) did not have a first-open input dispatch to Vesna. The input traces show motion sent to Android's Taskbar channel but no matching dispatch to HomeActivity during those attempts, and the recorded first app row did not appear until the last tap. The timings table excludes those pairs, rather than treating the delay to the last tap as app latency.

Repeated the same sequence without recording (five attempts plus an excluded warmup). One measured attempt likewise had no first-open dispatch to Vesna. In the four delivered trace-only pairs, the longest main-thread `postAndWait` after first open was 136–147 ms versus 8–14 ms after repeat open. With video recording, the corresponding ranges were 163–203 ms and 27–37 ms. The comparison was sequential and cannot isolate the recording effect, but the first-open wait was present without video. It does not establish an app-side cause for the undelivered taps or a specific optimization for the draw wait.

Physical-device first-open timing and touch-to-display latency remain unmeasured. The read-only AVD was stopped. Raw video, traces, view trees, and timing records are saved outside Git under `D:/orca/scratch/vesna-drawer-first-open/`.
