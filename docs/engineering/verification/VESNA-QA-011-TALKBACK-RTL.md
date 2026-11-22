# VESNA-QA-011: TalkBack, Russian, and RTL verification of the workspace lock (PR #105)

**Verdict:** PASS. The Settings → Home screen → Lock workspace layout switch is a single
merged, checkable TalkBack node that reads its title, detail, role, and state, and toggles
under TalkBack. Locked Home hides placement actions in the workspace, dock, launcher, and
drawer menus (App info retained), and answers folder/empty-area long presses with a
"Workspace layout is locked" snackbar instead of customization. Tapping still launches apps.
All new strings are present in Russian with no layout breakage, and the whole app mirrors
under an RTL locale (`supportsRtl="true"`). No accessibility regressions were found.

**Item:** [PR #105](https://github.com/vladon/vesnalauncher/pull/105) implementing
[issue #103](https://github.com/vladon/vesnalauncher/issues/103) (NT-HOME-02).
Sweep covers the checks the implementation session left unverified: TalkBack, Russian, RTL.

## Code under test

- Branch `vladon/vesna-workspace-lock` @ `c13a992` ("feat(home): lock workspace layout from Settings")
- APK `app/build/outputs/apk/debug/app-debug.apk`, sha256
  `5d1c04d66b38af98be7fb6110447ae7850e2653ee332c0565df0b3edbb0a0413`,
  versionCode 50, versionName 0.4.45 (Gradle confirmed the APK is an up-to-date HEAD build)
- `WorkspacePreferences.LAYOUT_LOCKED = "workspace_layout_locked"` in `shared_prefs/vesna_launcher.xml`

## Environment

| Item | Value |
| --- | --- |
| Device | `VesnaIssue103_QA` AVD on emulator-5554 (dedicated to this workstream), API 35 / Android 15, 1080×2400 |
| TalkBack | Google Accessibility Suite `com.google.android.marvin.talkback` (installed on image) |
| HOME role | `com.google.android.apps.nexuslauncher` throughout (never changed) |
| Physical devices | None touched (Pixel 9a untouched; `Vesna_Signed_Upgrade_QA`/emulator-5576 rejected a debug install on signature and was left unmodified) |

## Baseline state (actual)

- Locale en-US, per-app locale override empty; `debug.force_rtl` null.
- Accessibility off: `enabled_accessibility_services` null, `accessibility_enabled` 0.
- Prefs: `workspace_seeded=true`, `workspace_active_page=1`, `workspace_layout_locked=false`.
- Seeded QA layout preserved from the implementation session via `install -r` (same debug keystore):
  grid Contacts/Drive/Folder/Gmail, Google/Maps/Messages/Phone, Photos/Safety/·/TMoble,
  Voice Search/YouTube/YT Music; dock Calendar/Camera/Chrome/Clock.

## Procedure and per-assertion verdicts

Evidence in `qa-evidence/qa011-talkback-rtl-pr105/` (dumps `*.xml`, screenshots `shots/*.png`).

### TalkBack (en-US)

| # | Assertion | Evidence | Verdict |
| --- | --- | --- | --- |
| T1 | Switch row is one merged node: `clickable+checkable`, state `checked=false`, title + detail children inside | `settings_homescreen_en.xml` | PASS |
| T2 | Toggling works under TalkBack: row flipped to `workspace_layout_locked=true` and back to `false`; thumb and prefs confirm both states | prefs reads; `shots/tb4_focus_on_switch.png`, `shots/tb5_switch_state_now.png` | PASS |
| T3 | TalkBack draws its focus rectangle on native dialog buttons and on Vesna popup menu items | `shots/tb1_after_dialog.png`, `shots/tb2_focus_dontallow.png`, `shots/lock1_contacts_longpress.png`, `shots/lock8_dock_menu.png` | PASS |
| T4 | Locked workspace item long-press opens a menu with **App info only** (Add to dock / Move to page / Remove omitted) | `shots/lock1_contacts_longpress.png` | PASS |
| T5 | Locked dock item long-press opens **App info only** (Remove from dock omitted) | `shots/lock8_dock_menu.png` | PASS |
| T6 | Locked folder long-press shows "Workspace layout is locked" snackbar, no folder menu | `shots/lock5_folder_snackbar.png` | PASS |
| T7 | Locked empty-area long-press shows the same snackbar, no customization menu | `shots/lock6_empty_snackbar.png` | PASS |
| T8 | Tap-to-launch still works under lock via TalkBack double-tap: Clock dock icon → `DeskClock` focused | `shots/lock7_clock_launched.png`, mCurrentFocus log | PASS |
| T9 | Unlocked menus fully labeled (text rows, TalkBack-readable): workspace Add to dock / Remove from workspace / App info; folder Rename folder / Remove folder; launcher Vesna settings / Set wallpaper / Default launcher / Add home page; drawer Add to workspace / Add to dock / App info | `menu_workspace_item_unlocked.xml`, `menu_folder_unlocked.xml`, `menu_launcher_unlocked.xml` | PASS |
| T10 | Icons expose `content-desc` (grid, dock, drawer); drawer has "Search apps" field and "Close app drawer" labeled button; Home options button labeled | `home_unlocked_initial.xml`, `drawer_unlocked.xml` | PASS |

### Russian (per-app locale ru-RU)

| # | Assertion | Evidence | Verdict |
| --- | --- | --- | --- |
| R1 | New strings localized: «Главный экран», «Заблокировать расположение», «Запретить изменение ярлыков, папок, панели избранного и страниц.»; row stays one merged checkable node with identical bounds | `settings_homescreen_ru.xml`, `shots/ru1_settings_switch.png` | PASS |
| R2 | Long Russian detail wraps to two lines without clipping; row height unchanged vs English | `shots/ru1_settings_switch.png` | PASS |
| R3 | Back-button description localized («Назад к настройкам Vesna») | `settings_homescreen_ru.xml` | PASS |
| R4 | Locked snackbar «Расположение заблокировано» | `shots/ru2_locked_snackbar.png` | PASS |
| R5 | Locked launcher menu = «Настройки Vesna» + «Основной лаунчер» only (wallpaper and add-page omitted) | `menu_launcher_locked_ru.xml` | PASS |
| R6 | Locked item menu = «О приложении» only | `menu_workspace_item_locked_ru.xml` | PASS |
| R7 | Unlocked drawer item menu = «Добавить на рабочий стол» / «Добавить в док» / «О приложении» | `menu_drawer_item_ru.xml` | PASS |
| R8 | Drawer chrome localized («Закрыть список приложений», «Поиск приложений»); long ru app labels ellipsize cleanly (Сообщени…, Безопас…, Голосов…) | `drawer_ru.xml`, `shots/ru2_locked_snackbar.png` | PASS |

### RTL (per-app locale ar-EG, strings fall back to English)

| # | Assertion | Evidence | Verdict |
| --- | --- | --- | --- |
| L1 | Settings page mirrors: back arrow at the right edge (x=975 vs 43 LTR) pointing right, title/detail right-aligned, switch moves to the row's leading (left) side | `settings_homescreen_rtl.xml`, `shots/rtl1_settings_switch.png` | PASS |
| L2 | Workspace mirrors fully: grid columns reversed (جهات الاتصال/Contacts top-right), dock order reversed, options/drawer buttons swapped, empty cell mirrored | `shots/rtl2_home_workspace.png` | PASS |
| L3 | Drawer mirrors; search icon on the right; grid order reversed | `shots/rtl3_home_drawer.png` | PASS |
| L4 | Manifest declares `android:supportsRtl="true"` | AndroidManifest.xml | PASS |

## Operational findings

- `Vesna_Signed_Upgrade_QA` (emulator-5576) holds a release-signed 0.4.45 **without** the lock
  strings (pulled and grep-verified). The debug build cannot be installed over it
  (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`); the fixture was left untouched and the sweep ran on
  `VesnaIssue103_QA` instead.
- TalkBack's first-enable notification prompt and a GMS sign-in WebView each stole the
  foreground once; both were dismissed (TalkBack notifications answered **Don't allow**).
  Neither entered any state.
- `cmd locale` on API 35 has no system-locales command; per-app
  `cmd locale set-app-locales dev.vladon.vesna.launcher --locales <tag>` was used (instant,
  scoped to Vesna, and cleanly revertible with an empty locale list).
- BACK on HomeActivity exits to NexusLauncher (no HOME role holder) — known gotcha, hit once,
  recovered with `am start`.

## Limitations

- TalkBack's spoken output cannot be captured over adb; announcements are inferred from the
  accessibility tree (text/desc/`checked`) plus Material 3 snackbar semantics. A one-minute
  human ear-check remains open for final sign-off.
- Injected taps on the Compose surfaces act as direct activations, so TalkBack focus-rectangle
  captures were only possible on native windows (dialog, popup menus); grid-icon focus rects
  were not captured.
- `uiautomator dump` is unreliable while TalkBack is enabled (stale repeats observed); ground
  truth for those moments came from prefs reads and screenshots.
- The empty-page locked hint (`emptyHint` text swap) is code-verified only; exercising it would
  have required adding a workspace page to the seeded fixture.
- Foldable postures remain unverified. See the compact-phone follow-up below
  for coverage in both Android navigation modes.

## Device state after the sweep (desired = baseline: reached)

- App locale override cleared (system en-US), `workspace_layout_locked=false`,
  `workspace_active_page=1`, a11y services null / disabled, `debug.force_rtl` null,
  HOME role NexusLauncher. Final screenshot matches the pre-sweep layout:
  `shots/final_restored_home.png`.
- Recorded delta: TalkBack's POST_NOTIFICATIONS is now denied with `USER_SET|USER_FIXED`
  (the sweep answered its prompt); previously the prompt was unanswered.

## Compact-phone follow-up

Verified the rebased PR at `291527e` (debug APK SHA-256
`8b8d9b0cc4d90fb92abdbd6e6b24436124010ab3ee87a9ed840f5132e5393aae`)
on the same Android 15 AVD at 320 × 640 dp and 200% text. Light used gesture
navigation; Dark used three-button navigation. In both, the switch exposed
one checkable row with a touch target larger than 48 dp. The title and
supporting text wrapped without clipping, and the switch toggled both ways.
A drag from Contacts into an empty cell while locked changed no shortcut,
folder, or page rows. The drawer remained usable and launched Clock.
Restored the original display size, font scale, night mode, navigation mode,
and unlocked state. Pixel Launcher remained the default Home app.

## Evidence inventory

- `baseline.txt` — device settings and APK sha256 at sweep start
- `a11y_dump.py` — dump parser (class/text/desc/clickable/checkable/checked/bounds)
- Dumps: `home_unlocked_initial.xml`, `settings_homescreen_en.xml`, `settings_homescreen_ru.xml`,
  `settings_homescreen_rtl.xml`, `menu_workspace_item_unlocked.xml`, `menu_folder_unlocked.xml`,
  `menu_launcher_unlocked.xml`, `menu_workspace_item_locked_ru.xml`, `menu_launcher_locked_ru.xml`,
  `menu_drawer_item_ru.xml`, `drawer_unlocked.xml`, `drawer_ru.xml`
- Screenshots: `shots/tb0…tb5*` (TalkBack enable/focus/switch states),
  `shots/lock1…lock8*` (locked menus, snackbars, launch), `shots/ru1…ru5*` (Russian),
  `shots/rtl1…rtl3*` (RTL), `shots/final_restored_home.png`
