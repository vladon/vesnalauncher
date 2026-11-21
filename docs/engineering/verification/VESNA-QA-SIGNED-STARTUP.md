# Vesna 0.4.6 — startup baseline for the signed APK

Measured the [published Vesna 0.4.6 APK](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.6), version code 11, from release commit `44f3d76144ca90d147a1c4e7c7ff0c1b778d903a`. The installed source APK SHA-256 was `3244129ea5b9729390800f34ab858fca463fee9c553282774e5007f2c9c99a88`.

| ActivityManager state | Samples | Min (ms) | Median (ms) | Max (ms) |
| --- | ---: | ---: | ---: | ---: |
| COLD | 5 | 599 | 607 | 854 |
| WARM | 5 | 636 | 692 | 768 |
| HOT | 5 | 68 | 76 | 131 |

## Procedure

- Dedicated `Vesna_Signed_Upgrade_QA` read-only AVD, Android 15/API 35, 1080 × 1920 at 420 dpi. All commands targeted `emulator-5576`; the phone and HOME role were not modified.
- Ran one excluded warmup and five measured triples. Vesna starts were separated by at least 15 seconds. Measured `am start -W` `TotalTime` using the state reported by ActivityManager, not inferred from the trigger.
- For COLD, force-stopped Vesna and verified its prior PID was absent. For WARM, pressed Back with Home in focus and verified the process stayed resident. For HOT, put Android Settings in the foreground without killing the Home process, then started Vesna explicitly. All measured legs returned `Status: ok`, reached Home, and satisfied the expected PID condition.

| Trial | COLD (ms) | WARM (ms) | HOT (ms) |
| --- | ---: | ---: | ---: |
| 1 | 852 | 768 | 131 |
| 2 | 854 | 700 | 85 |
| 3 | 605 | 636 | 68 |
| 4 | 607 | 692 | 70 |
| 5 | 599 | 690 | 76 |

This is a single-AVD baseline, not a release-to-release comparison or a measurement of time to interactivity. Cold and warm ranges overlap; the medians do not establish that cold is faster than warm. The older [QA-009 debug run](VESNA-QA-009-LAUNCH-STATES.md) used a different build and emulator. Raw per-leg outputs and the summary are stored outside Git in `app/build/reports/vesna-0.4.6-startup/`.
