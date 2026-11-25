# VESNA-QA-WORK-TAB — Work apps in their own tab (#197, v0.4.86)

Devices: the research AVD `Nova_Reboot_API35` (emulator-5588, managed work profile user 11, 17 work apps + the enumdiag/IconProbe fixtures, Nova 9.0.1 Prime and Vesna v0.4.86 both exercised) and the QA AVD (emulator-5560, managed profile user 10) for instrumentation.

## The feature (#197, the acceptance's option A)

- Settings → App drawer → **Work apps in their own tab**: a switch (default off — no behavior change until opted in; hidden entirely when the device has no managed profile).
- When enabled, the drawer shows an **Apps | Work** tab bar under the search field. The **Apps** tab lists every non-work app; the **Work** tab lists only the managed profile's apps, alphabetically with plain labels and the blue work badge on each icon.
- The Work tab carries the pause affordance: **Turn off work apps** while the profile runs; when paused, the pause banner ("Work apps are paused…" + Turn on work apps, from #198) shows on the Work tab. Quiet-mode toggling works only from the foreground default launcher — from a non-default launcher both actions route to the system Settings (same fallback as #198).
- Switching tabs resets the scroll and re-applies the search query within the active tab; hidden apps stay hidden on both tabs; the fast scroller, section indicator and grid/list/pager layouts all work per tab.

## Checklist results (research AVD, managed profile user 11)

| Check | Evidence | Result |
|---|---|---|
| Settings switch appears only with a managed profile | `SettingsActivity.hasWorkProfile` from `userProfiles`; the switch rendered on the research AVD (`st4/st5` dumps), hidden on profile-less AVDs by construction | PASS |
| Tab bar appears with the switch on: full-width **Apps \| Work** under the search field | `worktab-active.png`: the Work tab lists exactly the work set — Calendar, Camera, Chrome, Clock, Contacts, Drive, EnumDiag, Files, Gmail, IconProbe, Maps, Messages, Phone, Photos, Safety, YouTube, YT Music (the OBS-VESNA-157 set) — alphabetically, plain labels, blue badges | PASS |
| Apps tab excludes work apps while the Work tab holds them | Dump-driven: no `Work …` entries under Apps; no personal-only apps (Nova Launcher, TMoble, Vesna) under Work | PASS |
| Tab switching resets scroll and re-filters | `DrawerView` tab click handlers + `WorkTabDrawerTest` view assertions | PASS |
| The pause affordance on the Work tab | Turn off work apps → `dumpsys user` user 11 QUIET_MODE (flags 4272); the Work tab swaps to the paused banner + **TURN ON WORK APPS** (`worktab-paused.png`, `worktab-paused-vesna-home.png` with Vesna as HOME); Turn on → flags 4144, the bright list returns (`worktab-resumed.png`) | PASS |
| Hidden work apps stay hidden on the Work tab | Hidden filtering runs before the tab slice (`AppSearch` hiddenApps + the userSerial slice); hidden-fixture coverage inherited from the drawer suites | PASS |
| No work profile → drawer unchanged | The tab bar is visible only when `workProfileUserSerial != null && workTabEnabled`; on profile-less AVDs the drawer is byte-identical to v0.4.85 (`WorkTabDrawerTest` no-profile case) | PASS |
| Switch survives restart; the preference persists | `DrawerPreferences.workTabEnabled` (`drawer_work_tab`) — the drawer's tab bar state was verified across a force-stop/restart during the QA cycle | PASS |

## Gates

68 JVM tests (the new `DrawerWorkTabSliceTest` slice cases), lintDebug. Instrumentation on the final merged tree: workspace 90/90, platform 1/1, ui 78 (incl. `WorkTabDrawerTest` 4/4 and `WorkPausedBannerTest` 2/2), settings 15. The ui/settings suites add 4 + 9 keyboard/a11y failures on this **freshly cold-booted AVD session**; the same tests pass individually in isolation and reproduce identically against the unmodified v0.4.84 baseline code (A/B: the same three classes, 15/24 failures on the baseline build — documented in [VESNA-QA-WORK-PAUSED](VESNA-QA-WORK-PAUSED.md) and [VESNA-QA-COLD-START](VESNA-QA-COLD-START.md)); the same suites were green on the warmed system in earlier releases. This session’s instrumentation also includes the #203 resume-refresh self-heal test (`LauncherControllerResumeRefreshTest`), added after this feature’s commit on the follow-up branch.

## Limitations

- The search field filters within the active tab (documented behavior); a cross-tab search would need a dedicated flow.
- The paused banner and the Turn on/Turn off affordances live inside the drawer; Nova additionally shows them per Work-tab row — presentation detail not replicated.
- Tab state resets to Apps on drawer close (session state, like the #195 drawer scroll decisions); the switch itself persists in `DrawerPreferences`.
