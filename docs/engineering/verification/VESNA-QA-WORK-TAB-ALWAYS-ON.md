# VESNA-QA-WORK-TAB-ALWAYS-ON — drawer Work tab always on (v0.4.100)

Integration of the owner-prepared parity branch `work-tab-always-on`
(42287ae) per the full-copy directive recorded in the audit: the drawer
renders the Apps | Work tab bar whenever a managed profile exists — no
opt-in switch (Nova presentation supersedes the #197 default-off gate).
Build: debug 0.4.100 (versionCode 105) on `Vesna_QA_Menu_API_35`
(emulator-5560, Android 15/API 35, RU locale, dark theme; the device has
the WorkDiag managed profile, user 10, serial 11).

## What is verified

| Check | Result |
| --- | --- |
| Tab bar with a managed profile | The drawer opens straight to the tabbed bar: «Приложения» [42,441][540,567] + «Работа» [540,441][1038,567]; no settings interaction needed (`wtab-drawer-04100-debug.png`). |
| Work tab contents | Tapping «Работа» switches the list to work-profile apps only (Chrome/EnumDiag/Gmail/… «(рабочий профиль)») with the «Отключить рабочие приложения» row present. |
| Back to Apps | The bar stays; the Apps tab shows the merged list (main + work apps). |
| The switch is gone | Settings › Список приложений has no «Рабочие приложения на отдельной вкладке» row; the `drawer_work_tab` preference, its search entry and the `WorkTabSlice.apply` `tabEnabled` parameter are removed (the background detail string that merely mentions work apps remains). |
| Regression suites | WorkTabDrawerTest + SettingsSearchLayoutTest 18/18 (the reworked always-on semantics); unit DrawerWorkTabSliceTest pass; FolderDisplacement 3 + PageAutoRemoval 5 + Badges 6 = 14/14 on the same build. |

## Device-state note

The tab bar initially appeared MISSING on this AVD: the `pm clear` run
during the #231 reproduction had silently revoked Vesna's HOME role (the
system fell back to NexusLauncher), and without the role the launcher no
longer sees the managed profile, so `workProfileUserSerial` stays null
and the drawer correctly renders untabbed. Restoring the HOME role
(`cmd package set-home-activity`) restored the bar. This is expected
#197 behaviour, not an always-on defect — but the same role loss means
the earlier #231 live release check on this AVD ran against the debug
build (see the correction in the #231 record).

## Integration notes

- The second owner branch, `parity/pills-removal` (1893658), carries the
  same #231 label fix that already shipped in v0.4.99 ([PR #235](https://github.com/vladon/vesnalauncher/pull/235));
  it is therefore NOT integrated again — no duplicate change.
- The release build and the physical devices need separate runs; the
  published APK is verified by digest/cert/badging in the release chain
  plus a fresh-install live check (uninstall before install to rule out
  the silent debug-over-release failure mode).
