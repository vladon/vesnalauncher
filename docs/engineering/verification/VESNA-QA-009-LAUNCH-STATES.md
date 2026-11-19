# VESNA-QA-009 — Debug HomeActivity launch timings by Android launch state

Six process-cold and six process-resident launches completed on the same
Android 15 emulator and debug APK. Android ActivityManager classified every
measured process-resident launch as `WARM`.

| Reported state | Samples | Min (ms) | Median (ms) | Max (ms) |
| --- | ---: | ---: | ---: | ---: |
| `COLD` | 6 | 2,696 | 2,741 | 2,906 |
| `WARM` | 6 | 998 | 1,151.5 | 1,644 |
| `HOT` | 0 | — | — | — |

## Environment

The test used `Vesna_QA_API_35` (`emulator-5556`, Android 15/API 35, x86_64,
user 0) on a shared Windows host. The installed APK was debug 0.4.1 (version
code 6), built at candidate `051febb09043039a870ac4f12199ca5aae5e08a0` for
[PR #31](https://github.com/vladon/vesnalauncher/pull/31). On-device
SHA-256 was `d2c61c344dea8107bf7af1924a94a663ddb78b98c1022ba789a7e2923359f417`.

## Procedure

All device commands were serial-pinned to `emulator-5556`. Each pair ran
one cold leg followed by one process-resident leg. Two preliminary pairs
were excluded as warmups. Start-call timestamps in the valid series were
15.111–17.264 seconds apart.

1. For a cold leg, force-stop Vesna and verify that `pidof` finds no Vesna
   process. Run `am start -W -n dev.vladon.vesna.launcher/.ui.HomeActivity`
   and record the Android launch state, **TotalTime**, and new PID.
2. For a process-resident leg, press BACK with Vesna in focus. Verify that
   NexusLauncher takes focus and that Vesna retains the previous PID.
   Run the same explicit start command and check the reported launch state
   and post-start PID. Every measured leg returned `Status: ok`.
3. Tally `TotalTime` from the six measured pairs. Each warm leg retained
   the same PID across BACK and restart. The median of each six-value
   series is the mean of its two middle sorted values.

| Pair | Cold TotalTime (ms) | Warm TotalTime (ms) |
| --- | ---: | ---: |
| 1 | 2,749 | 1,644 |
| 2 | 2,733 | 998 |
| 3 | 2,899 | 1,077 |
| 4 | 2,705 | 1,596 |
| 5 | 2,696 | 1,170 |
| 6 | 2,906 | 1,133 |

The first attempt was excluded. Its cadence code interpreted microseconds
as milliseconds, and its summary parser missed PID fields written with
`=`. All process-resident legs were skipped in that attempt. The repaired
harness collected the complete series above in a single rerun.

The measurements describe ActivityManager launch duration under the stated
debug-emulator conditions. Cold and warm were paired in a fixed order.
Release builds, real devices, time to interactivity, and longer background
intervals need separate measurements. The [QA-007](VESNA-QA-007-COLD-START.md)
run used a different build, AVD, and procedure.

## Teardown and evidence

The installed APK hash, all 18 persisted Vesna placement tuples, and the
empty folder table matched preflight. NexusLauncher remained the HOME
holder and was restored to the foreground.

Evidence is retained outside Git in `qa-evidence/qa009-launch-states/`.
`run1/` contains the excluded attempt. `run2/` contains the valid sample
summary and per-leg ActivityManager, focus, and PID records.
`teardown.log` records restoration checks.
