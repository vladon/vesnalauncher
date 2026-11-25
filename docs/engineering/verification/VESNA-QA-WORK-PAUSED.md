# VESNA-QA-WORK-PAUSED — paused-work banner and resume (#198, v0.4.85)

Devices: Android 15 QA AVD (emulator-5560, managed profile user 10 created for the #199 verification, unprovisioned) for view-level tests, and the research AVD `Nova_Reboot_API35` (emulator-5588, provisioned managed profile user 11, Nova 9.0.1 Prime as HOME) for the end-to-end quiet-mode cycle. Builds: v0.4.85 (90) debug candidate re-signed with the release certificate for the research AVD (certificate 21a9fb9c…e1c58 matches the installed release).

## The feature (#198)

When a managed profile is in quiet mode (paused), Vesna's drawer shows a banner above the apps — **"Work apps are paused. Resume to launch them and receive their notifications."** with a **Turn on work apps** action — while the work tiles keep the already-shipped dimmed presentation. The resume action calls `UserManager.requestQuietModeEnabled(false, profile)`; when the launcher is not the foreground default launcher the platform throws `SecurityException` and Vesna routes to the system Settings that resumes the profile instead (the acceptance's allowed route). Tapping a paused work tile surfaces the same paused notice as a snackbar instead of the generic launch-failed message. The quiet-mode APIs are API 28+; API 26/27 devices keep the plain dimmed tiles.

## Checklist results

| Check | Evidence | Result |
|---|---|---|
| Banner hidden without a pause; no resume callback armed | `WorkPausedBannerTest.bannerHiddenWithoutAPause` | PASS |
| Pause shows the banner with the acceptance wording and a working resume action | `WorkPausedBannerTest.pauseShowsBannerWithTheResumeAction` (live managed-profile device) | PASS |
| Real quiet-mode cycle (research AVD, provisioned profile) | Nova "Turn off work apps" → `dumpsys user` user 11 Flags gain QUIET_MODE (4272); Vesna's drawer shows the banner with dimmed badge-carrying pairs (`vesna-banner-live.png`); the banner's Turn on action clears quiet mode (flags back to 4144), removes the banner and restores the bright badge-carrying pairs exactly (`qa-resumed.png`, `qa-resumed2.png`/tiles crop) | PASS |
| Resume works only from the foreground default launcher | With Nova as HOME the platform rejects the request (`SecurityException: caller is neither foreground default launcher nor has MANAGE_USERS/MODIFY_QUIET_MODE permission`); Vesna catches it and opens the system Settings that resumes the profile | PASS (route) |
| A paused work tile tap is not silent | `launchApp` surfaces the paused notice as a snackbar when the target profile is in quiet mode | PASS |
| Suspended tiles stay listed (nothing lost across pause/resume) | `pm suspend --user 10 <youtube> <music>` + full restart on device B kept Work YouTube and Work YT Music in the drawer (dimmed); resume restores brightness and launch behavior | PASS |
| Quiet-mode APIs are API 28+ | `Build.VERSION.SDK_INT < P` guard; API 26/27 keep the pre-existing dimmed-only behavior | PASS |

## Gates

64 JVM tests, lintDebug. Instrumentation on the candidate: workspace 90/90, platform 1/1 (`PackageChangeMonitorSuspendTest`), `WorkPausedBannerTest` 2/2, `FolderRenameValidationTest` 1/1; the ui/settings suites add 5 + 9 failures on this **freshly cold-booted AVD session** that reproduce identically against the unmodified v0.4.84 baseline code (A/B documented above in this cycle) — keyboard/a11y session flakiness, environmental; the same suites were green on the warmed system in the earlier releases.

## Limitations

- The banner requires a provisioned managed profile (quiet mode on an ownerless profile cannot be toggled by the launcher); verified on the provisioned research profile.
- Nova-side popup-menu notification content remains unconfirmed (OBS-SYS-116) — the deterministic notification probe now exists for the next at-the-phone session.
- Multiple paused profiles: the banner resumes the first paused profile found; additional paused profiles remain (an edge case deferred with the #197 work-tab presentation ticket).
