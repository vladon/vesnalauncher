# VESNA-QA-LAUNCH-0.4.87 — launch timings with the two-phase catalogue (ROADMAP P3.1)

Re-run of the [VESNA-QA-009](VESNA-QA-009-LAUNCH-STATES.md) methodology on the current release line: six process-cold and six process-resident launches on `Vesna_QA_Menu_API_35` (emulator-5560, Android 15/API 35), installed APK **debug 0.4.87-candidate (version code 91 tree, built from the v0.4.86 + resume-refresh main line)**. The build includes the #195 two-phase catalogue and the #197/#198/#203 work-profile features. No same-build predecessor exists on this AVD (the QA-009 numbers are 0.4.1 on a different emulator-5556 instance), so the QA-009 values are the directional reference.

## Results

| Reported state | Samples | Min (ms) | Median (ms) | Max (ms) |
| --- | ---: | ---: | ---: | ---: |
| `COLD` | 6 | 2,011 | 2,146.5 | 2,428 |
| `WARM` | 6 | 830 | 1,040 | 1,081 |

Cold values: 2011, 2105, 2121, 2172, 2193, 2428. Warm values: 830, 957, 1024, 1056, 1069, 1081. Every cold leg verified `pidof` empty before the start call; every warm leg verified NexusLauncher in focus (the HOME role on this AVD is NexusLauncher) and the same PID across the leg.

## Directional comparison

| Series | QA-009 (0.4.1, emulator-5556) | This run (v0.4.87, emulator-5560) |
| --- | ---: | ---: |
| COLD median | 2,741.5 | 2,146.5 |
| WARM median | 1,151.5 | 1,040 |

The cold median improved ~22% against the 0.4.1 reference despite the work-profile features added since; the two-phase catalogue (#195/#203) keeps the first-frame work on the fast saved-layout pass. The comparison is directional (different builds, different emulator instances of the same AVD type on the same host); no same-build baseline exists on this AVD.

## Relation to #195/#203

- The two-phase catalogue (#195) moves the full icon decode off the first frame: the fast phase decodes only the saved layout's icons; the drawer fills in behind the first frame.
- The resume-refresh (#203) re-enumerates on Home entry outside the package-event pipeline; the full pass emits once with complete icons (no pending-icon flicker).

## Limitations

- Single AVD instance, debug APK, cold-boot AVD state (the boot settled before the series; the first pair after an install measured 2,105–2,428 with the install itself excluded).
- Release builds and physical devices need separate measurements (the physical device is out of the QA inventory per policy).
- `HOT` state not sampled (requires a third-party app in front; QA-009 scoped it out the same way).
