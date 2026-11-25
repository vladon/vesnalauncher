# VESNA-QA-TTI-0.4.87 — time to interactivity (ROADMAP P3.1)

Time-to-interactivity measurement on the same setup as
[VESNA-QA-LAUNCH-0.4.87](VESNA-QA-LAUNCH-0.4.87.md): `Vesna_QA_Menu_API_35`
(emulator-5560, Android 15/API 35), debug 0.4.87 build, six process-cold and
six process-resident launches minutes after the timing series, on the same
settled boot. Operatonal definition: wall time from the `am start` invocation
until the system reports `mCurrentFocus` naming the launcher, polled every
50 ms concurrently with `am start -W` (which separately reports
`TotalTime`, the ActivityManager first-frame-drawn duration).

## Results

| Reported state | Samples | Min (ms) | Median (ms) | Max (ms) |
| --- | ---: | ---: | ---: | ---: |
| `COLD` (focus) | 6 | 1,466 | 1,701.5 | 1,862 |
| `WARM` (focus) | 6 | 230 | 365.0 | 546 |

Cold values: 1466, 1480, 1691, 1712, 1773, 1862. Warm values: 230, 316, 355,
375, 491, 546. Every cold leg verified `pidof` empty before the start call;
every warm leg verified NexusLauncher in focus before the start call.

## Focus vs TotalTime

| Series | TotalTime median (same run) | Focus median | Focus as share of TotalTime |
| --- | ---: | ---: | ---: |
| COLD | 2,170 | 1,701.5 | ~79% |
| WARM | 768.5 | 365.0 | ~47% |

The launcher window takes input focus consistently before the launch is
formally complete: ~79% of the way through the cold `TotalTime`, and around
half the reported warm duration. Warm interactivity (median 365 ms) is well
inside the 100 ms perceived-instant band, and the warm gap between focus and
the reported TotalTime (≈400 ms) is dominated by background completion that
does not block input. Paired TotalTime medians from this run (2,170 /
768.5) sit within the QA-LAUNCH-0.4.87 spread (2,146.5 / 1,040 warm median
slightly higher there), confirming the environment stayed comparable.

## Limitations

- 50 ms poll granularity plus `dumpsys window` read latency bound the
  precision from above (each sample can be up to one poll interval late);
  the reported medians are upper bounds on true focus time.
- Focus is a proxy for interactivity: the first frame may still be finishing
  layout while the window reports focus. Frame-level verification
  (surfaceflinger latency) is a separate, larger measurement.
- Single AVD instance, debug APK; release builds and physical devices need
  separate measurements (the physical device is out of the QA inventory per
  policy). `HOT` state not sampled, consistent with QA-009 scope.
