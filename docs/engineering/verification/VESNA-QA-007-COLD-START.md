# VESNA-QA-007 — Debug Activity cold launch on Android 15

Ten process-cold launches completed on a dedicated API 35 emulator. Android
ActivityManager reported a minimum of **2,677 ms**, a median of **3,072.5 ms**,
and a maximum of **3,830 ms**. The metric is `am start -W` **TotalTime** for
`dev.vladon.vesna.launcher/.ui.HomeActivity` under the conditions below.

## Environment

| Item | Observed value |
| --- | --- |
| Build | Debug 0.4.1 (version code 6), commit `81cea186d58e13ba8141c95b661dd8ccf5aadb12` |
| APK SHA-256 | `d7290a053b77ed383e8c5c926bb5cdaa71b386b19ef4ec5a72268d420a38aa70` |
| Device | `Vesna_QA_Persistence_API_35`, serial `emulator-5558`, user 0 |
| System image | Android 15/API 35, Google APIs x86_64, build `AE3A.240806.043/12960925` |
| Display and rendering | 1080×2400, density 420, Google SwiftShader CPU rendering |
| Host | Windows, shared with other running emulators |
| Test window | 2026-11-19, measured series 02:56–02:58 local (UTC+03:00) |

Vesna was absent at preflight after the
[shared-host instrumentation incident](VESNA-QA-003E.md#operational-findings).
The measured APK initialized a fresh layout on the first untallied launch.
The measured series used that layout.

## Procedure and results

1. Install the measured APK on the named AVD. Launch the activity twice to
   initialize its workspace and warm the emulator. Exclude both launches from
   the sample tally.
2. Before each sample, force-stop Vesna, wait two seconds, and confirm
   `pidof dev.vladon.vesna.launcher` returns no process. Every adb command is
   pinned to `emulator-5558`.
3. Start the explicit activity with `am start -W -n`. Bound each call with a
   timeout and record the timestamp, exit code, ActivityManager output, and
   post-launch process ID. Start calls are spaced 15 seconds apart.
4. Confirm `Status: ok`, `LaunchState: COLD`, and a new process ID for each
   successful sample. Compute statistics from `TotalTime` in milliseconds.

| Sample | TotalTime (ms) |
| --- | ---: |
| 1 | 3,815 |
| 2 | 3,830 |
| 3 | 3,080 |
| 4 | 2,709 |
| 5 | 3,065 |
| 6 | 3,149 |
| 7 | 2,856 |
| 8 | 2,846 |
| 9 | 2,677 |
| 10 | 3,111 |

The median is the mean of the fifth and sixth sorted observations (3,065
and 3,080 ms). The arithmetic mean is 3,113.8 ms. The first attempt was
excluded as a series after its timestamp command failed to enforce the cadence
and sample 9 hung without creating a Vesna process. The complete series above
was collected in one rerun with second-based pacing and per-call timeouts.

These timings characterize process-cold Activity launches on the stated debug
build and shared emulator. Release builds, real devices, warm launches, and
time to interactivity require separate measurements.

## Recovery and evidence

The coordinator returned the AVD to the prior `0c7269c` debug APK
(SHA-256 `94277eb4682ffc1b91ab816f59be32752a7f5104cdb26e80afbf07b0e15e8688`).
The first comparison found an additional SIM Toolkit entry in the fresh seed
and three icons shifted by one cell. The coordinator removed the extra pin and
moved the three icons via Vesna UI. All 18 placement tuples matched the
[QA-004B](VESNA-QA-004B.md) snapshot, the folder table was empty, the HOME role
had no holder, and Android's **Select a Home app** chooser was back in focus.

Evidence is retained outside Git in `qa-evidence/qa007-startup/`:

- `01-getprop-props.txt` and `04-dumpsys-package-q007-current.txt` record
  emulator and measured build metadata.
- `06-launch-samples.log` records the warmups and interrupted first attempt.
  `07-launch-samples-rerun.log` contains the complete measured series.
- `09-diff-vs-final3norm.txt` captures the initial difference from QA-004B.
  `incident-5558-after-recovery.txt` records the matching placement tuples.
