# Observations — Vesna Launcher release comparison

Installed on the research Pixel (55021JEBF03248) from the GitHub release
`v0.4.3` (2026-11-20). Purpose: the feature-state baseline against the Nova
findings and the Vesna specifications. Vesna runs as a foreground activity;
Nova remains the system HOME (comparison-only install).

## OBS-VESNA-001 — v0.4.3 feature baseline

- Date / Env: 2026-11-20 17:33 / ENV-1 (the Pixel, Vesna 0.4.3 over Nova)
- UI surface (single-container design): a workspace/app-list page with the
  search pill (grid icon + more button) and a 4-icon dock; a popup menu
  (Vesna settings / Set wallpaper / Default launcher / Add home page); a
  ONE-PAGE settings screen: Default launcher button, Theme
  (System/Light/Dark), Show app names toggle, MIT license + GitHub link.
- Feature state vs the researched areas:
  - Card system (conditions, schedules, provider cards, builder): NOT YET
    PRESENT.
  - Drawer groups / smart folders: NOT YET PRESENT.
  - Settings search field: absent; search-state reset was not applicable.
  - Widget placement: not yet observed in this pass.
- Settings opened on the single root page, without a search field or
  keyboard. This did not verify the full R-SETT-112 contract.
- The spec requirements (CARDS-100..105, DRAWER-106) are the design target
  for future releases; 0.4.3 predates them.
- Evidence: EVI-CARDS-001 (vesna-first-launch, vesna-explore-1,
  vesna-settings, vesna-settings-btm)
- Confidence: Observed

## OBS-VESNA-002 — v0.4.8 follow-up

- Upgraded the research Pixel in place from 0.4.3 to the
  [GitHub release v0.4.8](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.8).
  The APK matched the published SHA-256; Vesna launched and Nova remained
  the system HOME.
- Observed on the Pixel: opening settings from Vesna's menu showed the
  Default launcher, Appearance, and About directory, with an empty,
  unfocused search field. Searching for `theme` returned the Theme control
  under Appearance. The first Back preserved the query and results; the
  second cleared the query and restored the directory.
- Force-stop check on the Pixel: with `theme` entered, the search field
  focused, and the keyboard visible, force-stopped Vesna. The process
  exited. After a cold launch and reopening settings from the menu, the
  full directory returned with an empty, unfocused field and no keyboard.
- Evidence: adb UI dumps before and after, input-method state, and
  `am start -W` reporting `LaunchState: COLD`. Confidence: observed.
- Direct links on the Pixel: Android resolved unscoped browsable `VIEW`
  intents for `vesna://settings/`, `/default-launcher`, `/appearance`,
  and `/about` to Vesna. UI dumps confirmed the expected page for each
  link, with no keyboard. Appearance was tested from a cold start; the
  other three links were opened while Settings was already running.
- Drawer cancellation on the Pixel: at stock display density the app grid
  fit on screen. At a temporary density of 560 dpi, held the AICore tile
  for one second, injected `ACTION_CANCEL`, then swiped upward. UI dumps
  showed the drawer stayed open and the top visible row moved from
  AICore/Calculator to Files/Find Hub. Restored the stock 420 dpi.
- The [release diff](https://github.com/vladon/vesnalauncher/compare/v0.4.3...v0.4.8)
  showed no implementation of cards, smart folders, or the card builder.
  Those acceptance scenarios remain untested.

## Comparison: Nova 8.9.9 vs Vesna releases

| Researched area | Nova 8.9.9 | Vesna 0.4.3 | Vesna 0.4.8 | Spec requirement |
|---|---|---|---|---|
| Card conditions render gate | Decorative; expiry is silent | No cards | No implementation found | Enforce conditions and surface expiry (NT-CARDS-01/02) |
| Met-later surface promise | Never surfaced | Not applicable | Not applicable | Enqueue within one refresh (NT-CARDS-03) |
| Manual-location fallback | Promised but absent | Not applicable | Not applicable | Only promise reachable surfaces (NT-CARDS-04) |
| Scheduled location go-live | Did not render at activation | Not applicable | Not applicable | Surface at go-live (NT-CARDS-03; CARDS-105) |
| Calendar connect gate | Runtime grant ignored | No Calendar card | No implementation found | One-step enable (NT-CARDS-06) |
| Smart folders | Off until enabled per folder | No smart folders | No implementation found | Master toggle and first-run choice (NT-DRAWER-01/02) |
| Empty generated folders | Unlabeled icons | Not applicable | Not applicable | Do not render empty groups (NT-DRAWER-03) |
| Builder save gating | Disabled Save; silent persistence | No builder | No implementation found | Explain disabled Save and define save behavior (NT-CARDS-10/11) |
| Hidden defaults | Activate on with a pre-attached ineffective condition | Not applicable | Not applicable | No hidden defaults (NT-CARDS-12) |
| Settings state hygiene | Focused search survives force-stop | Single page; no search | Clean directory after force-stop; two-step Back and all four direct links verified | R-SETT-112 partially verified |

## OBS-VESNA-003 — Workspace gesture comparison on the Pixel

| Gesture | Nova 8.9.9, current bindings | Vesna 0.4.8 |
|---|---|---|
| Swipe up from mid-screen (y=1500→700) | Opened at 120 and 250 ms; stayed collapsed at 270, 290, 310, 330, 500 and 800 ms | Opened at 120, 250, 270, 290, 310, 330 and 500 ms; stayed closed at 800 ms |
| Swipe up from lower screen (y=1950→850) | Opened at 120, 250, 330, 500 and 800 ms | Opened at all five durations |
| Swipe down from mid-screen on Home (y=700→1500) | Opened Android's notification shade | Stayed on Home |
| Double-tap between icons | No visible action | No visible action |
| Swipe left from the workspace interior | No visible change | No visible change |
| Swipe right from the workspace interior | Opened the Nova Now panel (Search apps, Nova AI) | No visible change |

- Existing [Nova gesture observations](gestures.md) document that a
  configured Quick Settings action can preempt drawer opening. No Nova
  bindings were changed in this pass. Vesna 0.4.8 wires workspace swipe-up
  directly to drawer opening; the settings directory has no gesture
  assignment page. These checks do not cover the full GESTURE-112 contract.
- The horizontal swipes started at x=850 and x=230 respectively, at
  y=1150. Vesna's release code routes horizontal swipes to page navigation;
  no page transition was observed on the original single-page layout.
  See the multi-page follow-up below.
- Evidence: adb UI dumps for the initial vertical checks and focused
  Android window for the notification shade; retained screenshots and
  UI dumps in `EVI-VESNA-003` for the lower Vesna swipe, double-tap, and
  horizontal checks. Confidence: observed for the listed test conditions.
- Mid-screen Nova retest: three swipes from (520,1500) to (520,700) at
  330 ms, without changing bindings. All three left `apps_view` at
  `[0,1992][1080,2424]`, the same bounds as before the swipes. See
  `EVI-VESNA-003/nova-mid-recheck-*`.
- Speed follow-up: Nova's live Gestures & inputs page showed **Swipe up →
  App drawer**; the binding was not changed. For the same 800 px path,
  starting with the drawer verifiably collapsed, 120 and 250 ms opened it;
  500 and 800 ms did not. Speed affected these trials, but the earlier
  positive result had no recorded duration, and no exact threshold was
  established. Evidence:
  `EVI-VESNA-003/nova-live-gesture-binding-*`, `nova-controlled-*`, and
  `nova-final-*` in the same evidence directory.
- Speed matrix follow-up: both trajectories used x=520 and started with
  the drawer verified closed; `EVI-VESNA-003/matrix-*-before.xml` and
  `matrix-*-after.xml` retain the UI observations. These are sampled
  durations, not measured thresholds. The lower path was also longer
  (1100 px vs 800 px), so this test does not isolate the starting zone.
- Equal-length follow-up: the lower path (x=520, y=1950→1150) was
  800 px, matching the mid-screen path. At the shared durations
  (120, 250, 330, 500, 800 ms), each launcher gave the same result
  from both starting positions: Nova opened at 120 and 250 ms;
  Vesna opened at 120, 250, 330, and 500 ms. Both stayed closed
  at 800 ms. This does not establish zone independence for other paths.
- Finer mid-screen sampling: Nova opened at 255, 260, and 265 ms
  (270 ms stayed closed in the previous pass). Vesna opened at
  550, 600, and 650 ms; it stayed closed at 700 and 750 ms.
  These are observed brackets, not exact thresholds.
- Evidence: `EVI-VESNA-003/equal-*-before.xml` and
  `equal-*-after.xml`; the drawer was verified closed before each swipe.
- Repeated 800 px mid-screen trials: Nova opened at 265 ms in 3/3
  attempts and stayed closed at 270 ms in 3/3, alternating durations.
  Vesna opened at 660 ms and stayed closed at 670, 680, and 690 ms
  (one attempt per duration).
- With a 1100 px swipe from mid-screen (x=520, y=1500→400),
  both launchers opened at 120, 250, 330, 500, and 800 ms, matching
  the 1100 px lower path at those durations.
- Evidence: `EVI-VESNA-003/refine-*.xml`, including the closed-drawer
  starting state and outcome for every trial.
- Vesna 800 px repeat: alternating durations from the same mid-screen
  start, 660 ms opened in 3/3 trials and 670 ms stayed closed in 3/3.
- At 1100 px from mid-screen, Nova still opened the drawer at 900, 1000,
  1200, 1500, 2000, 3000, and 5000 ms; no cutoff was observed. Vesna
  opened at 900 ms, opened the workspace menu instead at 925 ms, and
  did not open the drawer at 1000, 1200, 1500, or 2000 ms. These are
  sampled outcomes, not exact thresholds.
- Evidence: `EVI-VESNA-003/slow-*.xml` and `long-*.xml`.
- Alternating 1100 px trials: Vesna showed the workspace menu instead
  of the drawer in 3/3 trials at 900 ms and 3/3 at 925 ms. The
  earlier drawer opening at 900 ms was not reproduced. See
  `EVI-VESNA-003/endurance-vesna-*`.
- Nova opened the drawer at 6000, 10000, 15000, and 20000 ms on that
  same path; no cutoff was observed through 20 seconds.
  See `EVI-VESNA-003/endurance-nova-*`.
- Vesna multi-page check: after adding a temporary empty page, a
  rightward swipe moved from page 2 to 1, and a leftward swipe moved
  from page 1 back to 2. Removed the empty page and verified that
  the single-page controls returned. See `EVI-VESNA-003/pages-*`.

## OBS-VESNA-004 — Vesna 0.4.10 slow swipes and page boundaries

- Upgraded the Pixel to GitHub release v0.4.10 in place after checking
  the published APK SHA-256. `adb dumpsys package` confirmed
  `versionName=0.4.10` before testing. Nova remained the system HOME.
- Replayed the 1100 px swipe from (520,1500) to (520,400). Vesna opened
  the workspace menu instead of the drawer at every tested duration:
  800 ms (1/1), 825/850/875 ms (3/3 each), 900 ms (2/2), and
  925 ms (1/1). The slow-swipe acceptance scenario still fails.
- Nova 8.9.9 opened Widgets, not the app drawer, at 22, 24, 25, and
  40 seconds on the same path (one trial each). It previously opened
  the drawer at 20 seconds. These samples do not establish an exact cutoff.
- With two Vesna Home pages, swipes from x=100 to x=980 and vice versa
  at y=1150 switched pages toward the interior. Outward swipes kept
  page 1 or page 2 selected as appropriate; there was no wraparound.
  Removed the temporary empty page and verified the single-page layout.
- Evidence: `EVI-VESNA-003/latest-nova-*`, `latest2-*`, and
  `latest-pages-*` / `latest-first-*` / `latest-last-*`. Confidence:
  observed for these paths and durations.
- Narrow-range replay on the same path: Nova opened Widgets at 20.5,
  21, and 21.5 seconds (2/2 each); Vesna opened the workspace menu
  at 725, 750, and 775 ms (2/2 each). The drawer never opened in
  these repeats. Evidence: `EVI-VESNA-003/narrow-*.xml`.
- Alternating Nova replay: 20 s opened the drawer in 3/3 trials;
  20.5 s opened it in 1/3 and showed Widgets in 2/3. Combined with
  the prior replay, 20.5 s opened the drawer in 1/5 and Widgets in
  4/5; it is not a deterministic cutoff.
- Vesna 0.4.10 opened the workspace menu at 665, 685, and 705 ms
  (2/2 each), never the drawer. Both launchers used the same 1100 px
  trajectory. Evidence: `EVI-VESNA-003/repeat-*.xml`.
- Two-page replay: horizontal two-pointer streams in both directions,
  with the second pointer added before motion or during the swipe,
  left the selected page unchanged in all four timed trials. Single-
  finger controls switched pages in both directions. An earlier
  synchronous two-pointer replay opened the workspace menu; its
  timing was not controlled. Removed the temporary page and verified
  the single-page layout. Evidence: `EVI-VESNA-003/two-fast-*`,
  `two-right-022210.*`, and `two-restored-*`.

## OBS-VESNA-005 — Vesna 0.4.12 gesture follow-up

- On the same 1100 px mid-screen path, Vesna 0.4.10 opened the
  workspace menu at 525, 575, 625, and 650 ms (2/2 each).
- Upgraded from GitHub after checking the APK digest. The installed
  package reported `versionName=0.4.12`. The menu still opened
  instead of the drawer at 525, 575, 625, 650, 665, 750, 900, and
  925 ms (one trial each); NT-GESTURE-03 remains open.
- Nova opened Widgets at 20.1 s and the drawer at 20.25 and 20.4 s
  (one trial each). These outcomes do not establish a stable cutoff.
- On the 0.4.12 two-page workspace, eight timed two-finger swipes
  (both directions, second pointer before or during motion) neither
  changed the selected page nor opened the menu. Single-finger
  controls changed pages in both directions. Removed the temporary
  page, verified the single-page layout, and returned to Nova.
- Evidence: `EVI-VESNA-003/v412-*.xml` and `v412-two-*.png`.

## OBS-VESNA-006 — Nova 9.0.1 gesture retest

- Updated Nova in place through Google Play on the research Pixel.
  Android reported `versionName=90100 (9.0.1)`; the live binding
  remained Swipe up → App drawer.
- On the 800 px mid-screen path, the drawer opened at 120, 250, 265,
  270, 330, and 500 ms, but not at 800 ms. On the 800 px lower
  path it opened at 250, 330, and 800 ms.
- On the 1100 px mid-screen path, the drawer stayed closed at
  800 ms; at 20 and 20.5 s the Android notification shade appeared,
  not the drawer. One trial per duration; no threshold inferred.
- Double-tap and leftward swipe left Home unchanged; rightward
  swipe opened Nova Now.
- Evidence: `EVI-VESNA-003/nova901-*.xml`. Confidence: observed.

## OBS-VESNA-007 — Nova 9.0.1 repeats and Vesna 0.4.13

- Nova 9.0.1 opened the drawer on all three 1100 px mid-screen
  swipes at 800 ms, unlike the earlier single closed result.
  At 800 px from the same start, 550, 650, and 750 ms stayed
  closed in both attempts each. See `EVI-VESNA-003/v413-nova*`.
- Upgraded Vesna to the GitHub release v0.4.13; the APK SHA-256
  matched and `versionName=0.4.13` was confirmed. On the same
  1100 px path, 665, 800, and 925 ms opened the workspace menu
  instead of the drawer in both attempts each. NT-GESTURE-03
  remains reproducible. See `EVI-VESNA-003/v413-vesna*`.

## OBS-VESNA-008 — Nova 9.0.1 cards and smart folders

- Settings still showed the same six built-ins as 8.9.9: Media
  controls, AccuWeather, Games, Photos, Screenshots, and Map locations.
  The Add control remained available. Nova Now search for `weather`
  rendered an AccuWeather card, as on 8.9.9. Save, scheduling, and
  condition enforcement were not retested.
- All seven smart folders enabled during the 8.9.9 research still
  rendered after the update. Unlike the prior unlabeled empty icons,
  the 9.0.1 drawer exposed names for Food, Games, and Shopping, with
  zero items each. The per-folder enable default was not retested
  on a fresh installation; no folder settings were changed.
- Evidence: `EVI-VESNA-003/nova901-cards-full-*`, `nova901-weather-query-*`,
  `nova901-groups-*`, `nova901-drawer-visible-*`, and
  `nova901-folder-scroll-*`. Confidence: observed for the checked surfaces.

## OBS-VESNA-009 — Vesna 0.4.14 and Nova 9.0.1 card persistence

- The GitHub Vesna APK matched the published SHA-256 and the Pixel
  reported `versionName=0.4.14`. On the 1100 px mid-screen path,
  665, 800, and 925 ms each opened the workspace menu instead of the
  drawer in 2/2 trials. NT-GESTURE-03 still reproduces.
- In Nova 9.0.1, tapping Save with only the name `VesnaSaveQA` left
  the builder open. After adding a Calculator action and a Saturday-only
  time condition, Save returned to Cards. Reopening the saved card
  confirmed both the action and the condition persisted.
- On Sunday, outside the saved Saturday time window, searching
  `VesnaSaveQA` in Nova Now still displayed the card and Calculator
  action. The condition did not gate that search result. Other condition
  types, active-window transitions, and plain-drawer surfacing were not
  retested. The temporary card was deleted.
- Evidence: `EVI-VESNA-003/v414-*`, `nova901-save-*`,
  `nova901-card-inactive-render-*`, and `nova901-saved-saturday-condition-*`.

## OBS-VESNA-010 — Nova 9.0.1 fresh smart-folder defaults

- Installed the Play-delivered Nova 9.0.1 APK and Prime companion
  on an isolated Android 16/API 36 emulator (`emulator-5580`), without
  restoring Nova settings. Android reported `versionName=90100 (9.0.1)`.
- The master Smart Folders switch started ON. Each of the seven
  generated folders (Entertainment, Finance, Food, Games, Shopping,
  Social, Travel) offered **Enable** in its own menu, not Disable.
  Thus all seven were individually off on the fresh profile, even
  though the master switch was on. No folder was toggled.
- This agrees with the Nova 8.9.9 per-folder behavior and is distinct
  from the upgrade case where previously enabled folders remained
  visible (OBS-VESNA-008).
- Evidence: `EVI-VESNA-003/nova901-isolated-fresh-1-*`,
  `nova901-isolated-groups-*`, and `nova901-isolated-default-*`.
  Confidence: observed.

## OBS-VESNA-011 — Nova 9.0.1 condition and plain-drawer follow-up

- A disposable custom card with a Calculator action and a Wi-Fi rule
  for a network other than the connected network still appeared on a
  matching Nova Now search. Reopening confirmed the Wi-Fi rule persisted.
- With Wi-Fi removed and only a 15-minute weather-change rule attached,
  the card still appeared on matching search and the rule persisted.
  No weather transition was independently observed or induced, so this
  does not establish whether that rule gates rendering.
- Bluetooth was permission-gated (`BLUETOOTH_CONNECT` not granted).
  No permission was changed and connected/disconnected rendering was
  not tested.
- With only an active Sunday time window attached, the card was not
  visible in query-free drawer browsing (four captures while scrolling),
  but its exact-name search still showed the card and action. Removed
  the disposable card; existing cards were not edited.
- Evidence: `EVI-VESNA-003/nova901-wifi-inactive-render-*`,
  `nova901-wifi-persisted-*`, `nova901-weather-*`, `nova901-active-*`,
  and `nova901-otherconditions-deleted-*`. Confidence: observed.

## OBS-VESNA-012 — Nova 9.0.1 master switch resets per-folder state

- On the upgraded Pixel, all seven smart folders were visible before
  turning off the master switch. None appeared in the query-free drawer
  with the switch OFF. Turning it ON again did not restore them:
  each folder menu offered Enable, not Disable.
- Re-enabled all seven individually and verified all seven were again
  visible in the drawer. Left the master switch ON, as before.
- Evidence: `EVI-VESNA-003/nova901-active-plain-drawer-*`,
  `nova901-master-off-*`, `nova901-master-restored-*`,
  `nova901-master-reset-*`, and `nova901-master-final-drawer-*`.
  Confidence: observed on the upgraded profile.

## OBS-VESNA-013 — Vesna 0.4.15 and Nova drawer search

- Verified the GitHub APK digest and installed `versionName=0.4.15`
  on the Pixel. The 1100 px mid-screen upward swipe opened the Vesna
  workspace menu instead of the drawer at 665, 800, and 925 ms (2/2
  each). NT-GESTURE-03 still reproduces.
- In Nova 9.0.1, opened the full app drawer, tapped its own Search apps
  field, and entered the exact name of a disposable card. The result
  showed the card name, Edit button, and Calculator action within
  `apps_view`. This is distinct from query-free browsing, where the
  earlier active-window card did not surface (OBS-VESNA-011).
- Tapping Bluetooth connection in the builder opened Android's
  permission prompt. Dismissed it without granting access; the
  permission stayed denied and the Pixel reported zero bonded devices.
  Connection-triggered rendering was not tested. Deleted the test card.
- Evidence: `EVI-VESNA-003/v415-*`, `nova901-exact-*` and
  `nova901-bt-*`. Confidence: observed for the checked paths.

## OBS-VESNA-014 — Vesna 0.4.16 and Nova 9.0.1 follow-up

- Vesna 0.4.16: verified the GitHub APK digest and installed version.
  On the 1100 px mid-screen path, 665, 800, and 925 ms opened the
  workspace menu rather than the drawer in 2/2 trials each. See
  `EVI-VESNA-003/v416-*.xml`; NT-GESTURE-03 still reproduces.
- Nova drawer search: a disposable `VesnaPrefixQuartz` card with a
  Calculator action appeared for the prefix `VesnaPre`, suffix `Quartz`,
  and full name; it did not appear for `VesnaNoMatch`. Each positive
  result showed the card and its Edit control. Verified that the test
  card was removed. See `EVI-VESNA-003/nova901-partial-*`.
- Nova smart folders: all seven appeared before and after force-stop
  and relaunch, with identical member counts. The process was absent
  after force-stop and relaunched with a new PID. No folder setting
  changed. This differs from the master OFF→ON reset in OBS-VESNA-012.
  See `EVI-VESNA-003/nova901-restart-*`.
- Bluetooth feasibility: Android Emulator 37.2.13 with an isolated API
  36 AVD reported Bluetooth ON and active discovery. The scan found
  no available peer, and the other isolated instances did not provide
  a usable second ADB target. Bluetooth support is present; pairing,
  connection changes, and the Nova trigger remain unverified. See
  `EVI-VESNA-003/emulator-bt-discovery-*`.

## OBS-VESNA-015 — Smart folders survive an Android reboot on API 35

- On an isolated writable Android 15 AVD (`emulator-5588`), installed
  Nova 9.0.1 (90100) and Prime, then enabled all seven generated
  folders. All seven rendered before the reboot.
- Rebooted the Android guest; the kernel boot ID changed and
  `sys.boot_completed` returned `1`. Opened Nova and used the drawer's
  grid button to verify the full drawer. All seven folders were visible
  without re-enabling any of them; folder names and member counts
  matched the pre-reboot UI dump exactly.
- Evidence: `EVI-VESNA-003/reboot-api35-before-130526.*` and
  `reboot-api35-after-grid-130635.*`. Confidence: observed on the
  Android 15 emulator; physical-device reboot persistence unverified.

## OBS-VESNA-016 — App icon versus system Home

- User-reported: opening Nova from its app icon opens a usable Nova
  workspace even when another launcher is the system Home. Opening
  Vesna from its icon instead leads to settings.
- The [Vesna v0.4.44 release manifest](https://github.com/vladon/vesnalauncher/blob/v0.4.44/app/src/main/AndroidManifest.xml)
  confirms that Android's ordinary app entry resolves to Settings,
  whereas the system Home entry resolves to the workspace. Directly
  opening the Vesna workspace alongside Nova as system Home already
  worked in OBS-VESNA-001; that direct launch did not exercise the
  app-icon path.
- Confidence: Nova app-icon behavior is user-reported; Vesna routing
  is verified from the release manifest. No new device test was run.

## OBS-VESNA-017 — Home and drawer comparison coverage gaps

The prior comparison emphasized cards, smart folders, and gestures. It
did not carry these already-seen Nova controls through to a comparison
of the released Vesna configuration surfaces:

| Nova control | Existing Nova evidence | Vesna v0.4.44 release evidence | Remaining behavioral check |
|---|---|---|---|
| Workspace grid size | Grid-size picker observed (OBS-SETT-002; HOME-002) | Fixed 4×5 workspace documented in engineering backlog CAND-206; no grid preference in Settings | Resize a populated grid and trace displaced items, then restart |
| Lock workspace editing | Lock control inventoried (OBS-SETT-002; HOME-013) | No corresponding control in Settings or the Home options menu | Check drag, removal, widget placement and ordinary app launch with lock on |
| Auto-place newly installed apps | Separate New apps control inventoried (OBS-SETT-002) | No corresponding control in Settings or the Home options menu | Install a disposable app with the control on and off; check drawer and workspace separately |
| Drawer layout mode | Style selector and horizontal-scroll section inventoried (OBS-SETT-003; DRAWER-003) | Drawer uses a responsive vertical grid; no mode selector in Settings | Switch modes and check app ordering, paging and search |
| Alphabetical fast scroll | Fast-scroll control inventoried (OBS-SETT-003; DRAWER-006) | No corresponding control in released Settings | Drag the thumb across app sections; verify target section and search interaction |

- Release references: [SettingsPage and SettingsScreen](https://github.com/vladon/vesnalauncher/tree/v0.4.44/app/src/main/java/dev/vladon/vesna/launcher/settings),
  [HomeActivity](https://github.com/vladon/vesnalauncher/blob/v0.4.44/app/src/main/java/dev/vladon/vesna/launcher/ui/HomeActivity.kt),
  [DrawerView](https://github.com/vladon/vesnalauncher/blob/v0.4.44/app/src/main/java/dev/vladon/vesna/launcher/ui/DrawerView.kt),
  and [engineering backlog](https://github.com/vladon/vesnalauncher/blob/v0.4.44/docs/engineering/BACKLOG.md).
- Confidence: Nova inventory plus released Vesna source and documentation;
  the listed end-to-end behavior is not newly verified. This is not a
  claim of exhaustive parity coverage.

## OBS-VESNA-018 — Workspace editing lock in Nova 9.0.1

- Android 15, isolated `emulator-5588`; Nova 9.0.1 Prime. The switch is
  in the advanced workspace settings. With the lock off, dragging a
  mail shortcut moved it to an empty cell. Its menu offered removal,
  shortcut editing, widget access and application details. Moved the
  shortcut back before testing the lock.
- With the lock on, the same drag left the shortcut in its original
  cell. The menu still offered application details but no editing or
  widget actions. Holding empty space reported that editing was
  locked instead of opening customization. A tap still launched the app.
- Restored the original OFF setting. Dock, folder, widget-mutation
  and restart behavior were not tested in this pass.
- Evidence: `EVI-VESNA-003/nova901-lock-*`; app launch confirmed by
  the foreground activity. Confidence: observed. Implementation handoff:
  [Vesna issue #103](https://github.com/vladon/vesnalauncher/issues/103).

## OBS-VESNA-019 — Workspace and drawer parity checks on Android 15

- Nova 9.0.1 Prime: reduced a populated workspace from a 6-row,
  5-column grid to 4×4. Three existing shortcuts moved into valid cells
  on the first row and retained those positions after stopping and
  restarting Nova. Widgets, a fully occupied grid, and overflow were
  not tested. Evidence: `EVI-VESNA-003/parity-grid-*`.
- Nova's new-app shortcut preference was tested off and on. Installing
  a disposable APK through ADB did not place a shortcut on the visible
  workspace in either case; the enabled-case app was found by drawer
  search. This does not verify Nova's behavior for store-driven installs.
  Removed the fixture. Evidence:
  `EVI-VESNA-003/parity-autoplace-*`.
- Vesna v0.4.45: verified the APK digest and installed version. Tapped
  the ordinary Vesna icon while Pixel Launcher was system Home. The
  foreground activity was Vesna Settings, not the workspace, and the
  system Home did not change. Issue #101 still reproduces. Evidence:
  `EVI-VESNA-003/parity-vesna045-icon-result-*`.
- Nova's drawer style selector offered a vertical grid, single-column
  list, and horizontal grid. The single-column view displayed one
  labeled entry per row. With 52 synthetic app entries installed, a
  horizontal swipe moved from an early to a later alphabetic page;
  searching still returned the installed maps app. Evidence:
  `EVI-VESNA-003/parity-drawer-list-grid-*` and `parity-horizontal-*`.
- In the single-column drawer with an empty query and keyboard hidden,
  dragging the right-hand thumb moved from early entries to V–Z. The
  thumb showed the letter P partway through. A text query returned
  matching app results without the thumb. Evidence:
  `EVI-VESNA-003/parity-fast-populated-settled-*`, `parity-fast-during.png`,
  `parity-fast-after-extended-*`, and `parity-fast-filtered-results-*`.
- Environment: isolated `emulator-5588`, Android 15. Confidence:
  observed for the exercised paths only. Physical phone untouched.

## OBS-VESNA-020 — Additional gaps against Vesna 0.4.46

These comparisons combine existing black-box Nova evidence with Vesna's
released source. The icon launch, drawer entry, and search were also
exercised on an isolated Android 15 AVD with Nova 9.0.1 as system Home.

| Area | Nova reference | Vesna 0.4.46 | Verification boundary |
|---|---|---|---|
| Drawer entry and suggestions | With auto-keyboard enabled, the drawer focused search and showed the keyboard and frequently used app predictions (OBS-DRAWER-001). | On the AVD, opening Vesna's drawer left the keyboard hidden and showed the app grid, without a prediction row. No preference for auto-opening the keyboard was found. | Nova setting-OFF comparison and prediction ranking not verified. |
| Search beyond installed apps | A drawer query showed installed apps, external search providers and web suggestions (OBS-DRAWER-002); the AVD reproduced this with an app-name query. | The released drawer filters installed apps; a nonmatching query on the AVD showed only a no-results state. | Provider launch/consent and offline behavior are separate checks. |
| Shortcut actions in the workspace icon menu | Nova exposed app-provided actions with a drag affordance, plus access to widgets and shortcut editing (OBS-HOME-003). | Vesna's workspace menu offers docking, moving between pages, removal and app details, but not those app actions or entry points. | The Nova menu affordances were observed; edit, widget and dragged-action outcomes were not verified in that observation. |
| Dock configuration | Independent enablement, slot count, pages and appearance controls were inventoried (OBS-DOCK-001). | Released dock capacity is tied to the four workspace columns; no equivalent dock settings surface was found. | Multi-page dock navigation and state persistence are not inferred from Nova's controls. |
| User-managed layout transfer | Nova created a user-selected backup file and restored an earlier layout after confirmation, removing later additions (OBS-BACKUP-002/004). | Vesna offers no in-app export or restore. ENG-106 already tracks backup policy; its draft covers Android device transfer rather than in-app files. | Existing policy gap, not a new implementation ticket or a claim that platform backup cannot run. |

- Vesna references: [drawer implementation](https://github.com/vladon/vesnalauncher/blob/v0.4.46/app/src/main/java/dev/vladon/vesna/launcher/ui/DrawerView.kt),
  [workspace menus](https://github.com/vladon/vesnalauncher/blob/v0.4.46/app/src/main/java/dev/vladon/vesna/launcher/ui/HomeActivity.kt),
  [dock geometry](https://github.com/vladon/vesnalauncher/blob/v0.4.46/app/src/main/java/dev/vladon/vesna/launcher/workspace/GridCanvas.kt),
  [settings surfaces](https://github.com/vladon/vesnalauncher/tree/v0.4.46/app/src/main/java/dev/vladon/vesna/launcher/settings),
  and [backup policy draft](https://github.com/vladon/vesnalauncher/blob/v0.4.46/docs/adr/0002-backup-and-privacy.md).
- The app-icon gap from OBS-VESNA-016/019 did not reproduce in the
  signed Vesna 0.4.46 APK. Tapping its result in Nova's drawer opened
  the Vesna workspace; its drawer launched Clock; pressing system Home
  returned to Nova. In-app Settings and the deep links were not part of
  this replay. Evidence: `EVI-VESNA-003/parity046-*.xml`.

## OBS-VESNA-021 — Shortcut actions, dock pages, and drawer visibility

- Nova 9.0.1 Prime on the isolated Android 15 AVD: the mail app's
  workspace context menu offered its app action, an app-scoped widget
  picker, and a shortcut editor with label and per-icon swipe options.
  Tapping the app action opened the mail app's initial setup, so the
  compose screen was not verified. Dragging the action from the menu
  did place it as a standalone workspace shortcut. No mail was sent.
- With two dock pages configured, a horizontal swipe on the dock
  switched from the original five favorites to an empty second page,
  without changing the workspace. Moved the test shortcut to page two:
  both pages kept separate contents. A process restart landed on
  page one, but the pinned action was still on page two. Removed the
  test pin. Infinite scrolling and page-position preference variants
  were not exercised.
- Hiding Chrome in Nova kept the package enabled and its dock favorite
  intact. The app was absent between Camera and Clock in alphabetical
  drawer browsing, but exact-name search still returned it. Restored
  the visibility setting. This is a browsing filter, not access control.
  Vesna 0.4.46 has no matching settings control (NT-DRAWER-09, #119).
- Release triage: [v0.4.47](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.47)
  carries the #101 app-icon fix from PR #104, already replayed on
  v0.4.46 (OBS-VESNA-020). The release changed APK packaging for
  F-Droid compatibility, not launcher behavior. Workspace lock #103
  is closed and [PR #105](https://github.com/vladon/vesnalauncher/pull/105)
  is merged, but its merge commit `c13d2fb0d` is one commit beyond
  the v0.4.47 tag `e4f460839`, so the released APK does not include it.
  #106–109, #112–116 and #119 remain open. Vesna v0.4.47 was checked
  against GitHub tags and issue states, not replayed on the emulator.
- Evidence: `EVI-VESNA-003/nova901-shortcut-*`, `nova901-dock-*`,
  and `nova901-hidden-*`. Confidence: observed for these emulator paths.

## OBS-VESNA-022 — Vesna main lock, Nova dock wrap and drawer menus

- Vesna `main` at `33f6cbc69a8ddc6b88338d5839e909aea14c0c88`,
  debug 0.4.48 (version code 53), built and installed on the isolated
  Android 15 `VesnaIssue103_QA` AVD. Enabling the workspace lock left
  all 19 placement rows unchanged under a staged Contacts drag.
  With the lock off the same drag moved Contacts from cell 0 to 10;
  the reverse drag restored the original rows.
- When locked, the workspace, dock, and drawer app menus retained
  application details but omitted placement actions. The folder opened
  without rename/remove controls, Home options omitted page creation,
  and holding empty space showed lock feedback. A normal Gmail tap
  still launched the app. The enabled switch survived a process restart.
  Restored the off setting; system Home was unchanged. This is an
  emulator replay of #103, not a widget or full accessibility sweep.
- Nova 9.0.1 Prime, on a separate Android 15 AVD: configured three
  distinguishable dock pages. With wrap off, a right swipe on page one
  stayed on page one and a left swipe on page three stayed on page
  three. With wrap on, the same gestures crossed the boundary in both
  directions. The workspace did not change. Restored two pages and
  wrap off. The wrap preference was not tested across a restart.
- Long-pressing Gmail in Nova's alphabetical drawer exposed its
  compose action, widget and shortcut-editor entry points, app
  information, and drawer hiding. Tapping the compose action reached
  Gmail's initial setup; composing itself was not verified. Dragging
  from this drawer menu was not replayed.
- New comparison gap: long-pressing the same app in Nova's prediction
  row offered per-item dismissal instead of drawer hiding. Dismissal
  removed Gmail from predictions, including after a Nova restart,
  while it remained in the alphabetical drawer. The Vesna drawer
  build tested here has no prediction row or corresponding action.
  This refines [#112](https://github.com/vladon/vesnalauncher/issues/112);
  the regular drawer menu complements #114 and #119.
- Evidence: `EVI-VESNA-003/parity22-vesna-*`, `parity22-nova-wrap-*`,
  and `parity22-nova-drawer-*`. Confidence: observed for these paths.

## OBS-VESNA-023 — Signed v0.4.48 lock and hidden-app persistence

- [Vesna v0.4.48](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.48)
  (code 53, tag `33f6cbc69a8ddc6b88338d5839e909aea14c0c88`)
  includes #103 via PR #105 (`c13d2fb0`). Verified GitHub's asset digest,
  verified the APK signer against v0.4.46, and updated the isolated
  Android 15 `Nova_Reboot_API35` research AVD from signed v0.4.46.
  No debug APK was used for this replay.
- In the release APK, a staged drag left Contacts in place when locked;
  the resulting menu offered app information but no placement actions.
  After a process restart the switch remained on and tapping Gmail
  reached the app. Turning off the lock allowed the same drag to move
  Contacts. Restored the shortcut position and the original off setting.
  This confirms the tested release path, not every main-build assertion.
- Nova stayed at 9.0.1 Prime (90100). The publisher's direct stable and
  beta links returned older versions; the Play listing did not expose an
  exact version and the independently indexed 9.0.0 was older. No newer
  trusted APK was verified for this fixture.
- On the same AVD, both unlocked launchers started with Chrome in the
  drawer and pinned in the dock. Nova's alphabetical item menu could
  hide Chrome. After force-stop and reopening Nova, Chrome was still
  absent between Camera and Clock, its dock pin remained, and tapping
  its exact-name search result opened Chrome's initial setup. Unhiding
  via settings, backing out, and reopening restored the alphabetical
  entry, including after another restart. On Vesna v0.4.48 the same
  drawer item offered pinning and app information, but no hiding action
  (#119). A settings exit is part of the confirmed restore path; a
  force-stop directly from the checkbox screen did not retain the change.
- System Home remained Nova. Evidence: `EVI-VESNA-003/parity23-*`.
  Confidence: observed on one Android 15 AVD; no physical-device run.

## OBS-VESNA-024 — Pin an app action directly from the drawer

- On the same Android 15 research AVD, both launchers had an ordinary
  Gmail pin on the workspace and no separate compose shortcut. Nova
  9.0.1 Prime (90100): long-press the Gmail row in the alphabetical
  drawer, then drag its compose action from the menu to the workspace.
  The drawer closed and a distinct action shortcut appeared alongside
  the existing Gmail pin.
- The action shortcut survived a Nova process restart. Tapping it
  launched Gmail's initial setup; the actual compose screen was not
  verified. Removed the action shortcut and confirmed the ordinary
  Gmail pin remained after another restart.
- In signed Vesna 0.4.48 (53), the equivalent unlocked drawer menu
  offered ordinary app pinning and app information, but no app action
  to drag. Follow-up for #114; placement of ordinary app icons is a
  separate behavior.
- Evidence: `EVI-VESNA-003/parity24-*`. Confidence: observed for Gmail
  on this AVD; no other app or dock drop tested.

## OBS-VESNA-025 — Keyboard and predictions are independent on drawer entry

- Same Android 15 research AVD, Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.48 (53). Used the same upward workspace swipe
  on both launchers, starting with no query or keyboard visible.
- Nova's keyboard setting off left the search field unfocused and
  the IME hidden on swipe, with app and shortcut predictions still
  visible. Tapping the workspace search affordance instead opened
  focused search with the IME, even with that setting off.
- Turning off app predictions removed the app row without removing
  the shortcut row. With both prediction options off, enabling the
  keyboard setting still focused search and opened the IME on swipe.
  A typed Gmail query still produced an app result. Restored all
  three original on settings.
- The same swipe in Vesna opened an unfocused app grid with no IME
  or prediction rows. Tapping search and typing Gmail still found
  the app. This refines #112; ranking was not measured.
- Evidence: `EVI-VESNA-003/parity25-*`. Confidence: observed for the
  tested entry routes and switch combinations.

## OBS-VESNA-026 — Fast scroll verified in signed Vesna v0.4.49

- Started on the Android 15 research AVD with 52 disposable A–Z
  entries. The full Nova 9.0.1 Prime (90100) drawer exposed its
  fast scroller; signed Vesna v0.4.48 (53) had no fast-scroll
  control on the same fixture. Nova's jump and letter behavior
  were already observed in OBS-VESNA-019.
- During this cycle #123 merged into `main` at
  `4516dc6b3eeea58b28e65dfa379501821cb8fc60`. On a separate owned
  Android 15 AVD, the [CI debug artifact](https://github.com/vladon/vesnalauncher/actions/runs/45077360823)
  from that SHA jumped to a viewport containing Z, showed a live
  letter, and hid the control for a Gmail query. This was not yet
  evidence about the signed release.
- After publication, verified the v0.4.49 asset digest and signer
  continuity and upgraded the first AVD between runs. The release
  tag resolves to the same merge, and the installed APK reported
  v0.4.49 (54). Dragging down reached Z and dragging up returned to
  A, with each target letter visible while held. Typing Gmail
  hid the scroller without blocking the result; clearing the
  query restored the control without reopening the drawer.
- Removed the disposable package from the release AVD. On the
  resulting short unfiltered list, the rail was absent. System
  Home stayed Nova. Verified #109 in the signed release; RTL,
  enlarged text and accessibility actions were not independently
  replayed here.
- Evidence: `EVI-VESNA-003/parity25-{vesna-fast-baseline,nova-fast,release049}-*`
  and `parity26-main-*`. Confidence: observed on Android 15.

## OBS-VESNA-027 — Horizontal drawer position across search and reopen

- Nova 9.0.1 Prime (90100) and signed Vesna v0.4.49 (54) on the same
  Android 15 research AVD with 52 disposable A–Z entries. Nova's
  starting List style was changed to Horizontal for this run.
- In Nova, page two contained a distinct set of fixture entries.
  Searching there found a fixture that was visible on page one,
  not page two. Clearing the query and dismissing the IME, without
  leaving the drawer, restored exactly the tested page-two fixture
  set. Closing and reopening the drawer started on page one.
- Restarting Nova while page two was visible also started on
  page one, while the selected Horizontal style persisted.
  Searching for Maps from the first page found and launched the
  app even though its icon was not on that page.
- In Vesna, the same horizontal swipe left the visible fixture
  set unchanged; a vertical swipe changed it. Drawer search
  found and launched Maps. No alternate style was found on
  the visible Settings pages. Restored Nova's initial List
  style and confirmed it was retained. Follow-up for #108.
- Evidence: `EVI-VESNA-003/parity27-*`. Confidence: observed
  for this style, fixture and the tested close/reopen routes.

## OBS-VESNA-028 — Drawer provider hands the query to Maps

- On the same Android 15 research AVD, opened drawer search in
  Nova 9.0.1 Prime (90100) and typed the public place name
  Golden Gate Bridge. The results offered a Maps provider.
- Tapping that provider opened Google Maps. After skipping its
  sign-in prompt, Maps showed the same query in its search bar and
  the matching place result. No account or permission was added.
- Signed Vesna v0.4.49 (54) showed no matching local apps for the
  same query and offered no Maps provider. A plain Maps app-name
  search launched the installed app in OBS-VESNA-027.
- This confirms a provider handoff for #113, not offline behavior,
  network opt-out, or other provider destinations. No launcher
  setting was changed for this case.
- Evidence: `EVI-VESNA-003/parity27-{nova,vesna}-provider-*`.
  Confidence: observed for one provider and one synthetic query.

## OBS-VESNA-029 — Dock capacity reduction preserves an overflow pin

- On the Android 15 research AVD, Nova 9.0.1 Prime (90100) had two
  dock pages, five slots per page, five pins on page one, and an
  empty second page. Reduced the slot count to four without
  changing the page count.
- The first four pins widened to fill the dock. The fifth pin
  was not visible on either dock page. The workspace icon
  positions did not change. After restarting Nova, the dock
  still showed four pins. Restoring five slots brought the fifth
  pin back in its original position without repinning it.
- On the same AVD, signed Vesna v0.4.49 (54) showed its four
  existing dock pins. Settings search found descriptions that
  mentioned the dock, but no slot-count control to run the
  same capacity change. Follow-up for #115.
- Evidence: `EVI-VESNA-003/parity29-{nova,vesna}-dock-*`.
  Confidence: observed for a reduction by one and an empty
  second page; no claim about larger reductions or adding
  pins while the capacity is reduced.

## OBS-VESNA-030 — Signed v0.4.50 hides apps; prefix search differs from Nova

- On the Android 15 research AVD, the signed Vesna v0.4.49 (54)
  baseline had Chrome in its drawer and dock, with no hide action
  in the drawer menu. Nova 9.0.1 Prime (90100) was unchanged.
- [PR #125](https://github.com/vladon/vesnalauncher/pull/125) merged
  at `4d3c30621f8a2e283f0aaeb3e98b2c78d3a5b8f6`. The signed
  [v0.4.50](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.50)
  tag pointed to that commit. Verified the official asset digest
  and prior release signer, then upgraded the research AVD between
  runs. Installed version: 0.4.50 (55).
- Hiding Chrome via Vesna's drawer settings removed it from the
  alphabetical grid after a process restart. Its existing dock
  pin stayed in place and launched Chrome. Entering the prefix
  `Chro` in the drawer returned no app; entering `Chrome` returned
  and launched Chrome. Unhiding restored it in the grid after
  another restart.
- Nova still returned the hidden app itself for both the same
  prefix and the full name, not just a web suggestion. This is a
  difference from Vesna's documented full-name-only policy, not
  a failure of #119. Restored both hide settings; no other profile
  or entry with a duplicate label was tested.
- Evidence: `EVI-VESNA-003/parity29-{nova-hidden,vesna-hidden-baseline,vesna050}-*`.
  Confidence: observed in the installed, signed release.

## OBS-VESNA-031 — Offline drawer search and web-suggestion opt-out

- On one Android 15 research AVD: Nova 9.0.1 Prime (90100)
  and signed Vesna v0.4.50 (55), no launcher update during the
  run. With Nova's original Google suggestion source and a
  connected default network, the public query Lake Baikal
  displayed web suggestions and external provider buttons.
- Enabled airplane mode and disabled Wi-Fi and mobile data
  on the AVD; Android reported no active default network
  before and after the test. Nova showed no web suggestions
  for the same query or for a fresh non-app query, but still
  exposed its external provider buttons. Vesna's same
  non-app query showed only the local no-match result.
  Both launchers still found and opened the installed Clock.
- Restored connectivity. With Nova's suggestion source set
  to None, the same online query showed no web suggestions,
  while provider buttons and local Clock search still worked.
  Restored Google and the web suggestions returned. Original
  radio settings and system Home were preserved.
- Follow-up for #113: this verifies UI behavior, not whether
  Nova sends network requests with suggestions set to None.
  External provider launches while offline were not tested.
- Evidence: `EVI-VESNA-003/parity31-*`. Confidence: observed
  on this fixture with test queries and no active default
  network during the offline pass.

## OBS-VESNA-032 — Workspace overflow goes to the next page

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.50 (55). On Nova, started with three existing
  shortcuts and added two identifiable disposable shortcuts
  to the bottom-right of a 4×4 workspace.
- Reduced the workspace to 2×2. Four shortcuts filled the
  original page, and a left swipe revealed the fifth alone
  on the next page. All five appeared exactly once across
  the two inspected pages. The division and positions
  survived a Nova process restart.
- Restoring 4×4 returned all five to their original page and
  bounds, with no manual repinning. Removed the disposable
  package; verified the original three shortcut positions
  and dock pins. In Vesna, Settings search for grid returned
  no control to run the same resize. Follow-up for #106.
- Widget migration was not tested: the widget placement
  prompted for access and was cancelled without granting it.
- Evidence: `EVI-VESNA-003/parity32-*`. Confidence: observed
  for five shortcuts shrinking from 4×4 to 2×2 and back.

## OBS-VESNA-033 — Backup restore cancel and invalid-file controls

- On the Android 15 research AVD, Nova 9.0.1 Prime (90100)
  exported a backup through the system picker to Downloads.
  Moved an existing Gmail shortcut to another cell as a
  positive marker, leaving the dock unchanged.
- Selecting the valid backup opened the replacement confirmation.
  Cancelling kept the moved Gmail shortcut and all dock pins.
  Selecting a disposable plain-text file with the backup suffix
  returned to settings without replacement; the moved shortcut
  and dock pins still matched. No transient error wording was
  captured. The valid backup was not opened or parsed by the test.
- As a positive control, selected the same valid backup and
  confirmed replacement. The original shortcut and dock positions
  returned and survived a Nova process restart. Removed both
  local test files; nothing was shared.
- In signed Vesna v0.4.50 (55), Settings search for backup,
  restore, and export exposed no equivalent manual route.
  Android-managed backup or transfer is outside this test.
  Follow-up for #116, not a backup-policy decision.
- Evidence: `EVI-VESNA-003/parity33-*`. Confidence: observed for
  one valid file and one malformed file on the research AVD.

## OBS-VESNA-034 — Restore keeps a placeholder for an absent app

- On the Android 15 research AVD, Nova 9.0.1 Prime (90100)
  exported a layout containing a disposable app shortcut.
  Uninstalled that package and moved Gmail as a marker; the
  disposable shortcut was no longer visible before restore.
- Confirmed restore from the valid file while the package
  was still absent. Gmail returned to its saved position.
  The disposable shortcut reappeared at its saved position
  with a visually distinct ring and remained after restarting
  Nova, although Android still reported no installed package.
- Reinstalled the same fixture. The ring cleared without
  moving or repinning the shortcut; its menu opened Android's
  app-details screen for the reinstalled package.
- Removed the fixture and local backup and verified the
  original workspace and dock. No share or store-based
  reinstall was exercised. Signed Vesna v0.4.50 (55) has
  no equivalent manual restore route in the matched fixture
  (OBS-VESNA-033), so no Vesna import behavior is inferred.
- Evidence: `EVI-VESNA-003/parity34-*` and
  `parity33-cycle34-restore-missing-*`. Confidence: observed
  for a single absent, later sideloaded app. Follow-up for #116.

## OBS-VESNA-035 — Signed v0.4.51 drawer modes and list-position difference

- Compared on Android 15 with 52 disposable A–Z entries. Nova
  9.0.1 Prime (90100) in single-column list mode found an item
  outside the visible rows; clearing the search and dismissing
  the IME returned to the same visible rows. Signed Vesna
  v0.4.50 (55) had no layout selector and the matching lateral
  swipe did not change its vertical grid viewport.
- [PR #126](https://github.com/vladon/vesnalauncher/pull/126)
  merged at `1b1f93c6b8813929739436b6f11032a36f5e4c2f`. The
  [CI debug artifact](https://github.com/vladon/vesnalauncher/actions/runs/45083411982)
  passed vertical list scrolling, cross-page search, page return
  on clear, and first-page entry after process restart on a
  separate owned AVD. This was not the release APK.
- [Signed v0.4.51](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.51)
  (56) points to the same commit. Verified the asset digest and
  signer, then upgraded the matched AVD between runs. The list
  scrolled in one column, and a search result opened Clock.
  Horizontal mode found a page-one item from page two; clearing
  returned page two. Closing/reopening or restarting from page
  two returned page one without changing the mode.
- List-only comparison gap: clearing the query in Vesna's
  vertical list returned the viewport to the first row, unlike
  Nova. Confirmed with two queries for initially off-screen
  items; in the second run it happened before dismissing the IME.
  The horizontal clear behavior still met #108. Restored the
  original Vesna grid and removed the fixture from the release AVD.
- Evidence: `EVI-VESNA-003/parity34-nova-list-*`,
  `parity34-vesna050-layout-*`, `parity35-release051-*`, and
  `parity36-main-*`. Confidence: observed for the tested gestures
  and queries. RTL, scaling and accessibility were not replayed.

## OBS-VESNA-036 — Disabling the dock retains both pages of pins

- On an Android 15 research AVD, Nova 9.0.1 Prime (90100)
  started with five ordinary pins on dock page one. Placed
  a disposable app pin on the normally empty page two.
- Turned off the dock in settings. The dock and its attached
  search affordance disappeared; workspace rows used more
  vertical space. This presentation survived a process
  restart. An upward swipe from the lower workspace still
  opened the app drawer.
- Re-enabled the dock. Both pages and their exact pin bounds
  returned without repinning, along with the search affordance
  and original workspace positions. Removed the disposable
  app and verified the original five pins and empty second page.
- Signed Vesna v0.4.51 (56) displayed four dock pins on this
  AVD. Searching its settings for dock surfaced descriptions,
  not an independent enable switch. Follow-up for #115.
- Evidence: `EVI-VESNA-003/parity37-*`. Confidence: observed
  for a dock-linked search bar and the tested two-page layout.
  Gesture-toggle and position-memory settings were not exercised.

## OBS-VESNA-037 — Dock gesture, automatic close, and restart state

- On Android 15, Nova 9.0.1 Prime (90100), left the research
  dock enabled with two identifiable pages. Temporarily enabled
  overlay and assigned a downward workspace swipe to toggle it.
- The swipe hid and showed the dock and its attached search bar
  without moving the workspace icons. Starting on page two, the
  same-process hide/show returned page two both with dock position
  memory off and with it on.
- Cold process restart from a visible page two showed page one
  with either memory setting; restarting from a hidden dock left
  it hidden in both cases. The tested paths did not distinguish
  the memory setting; other triggers were not tested.
- With automatic close off, launching Gmail from the workspace
  and returning Home left the dock visible. With it on, the same
  action returned to a hidden dock. Restored the original bindings,
  switches, visible dock, and pins; removed the disposable app.
- Signed Vesna v0.4.51 (56) had no dock gesture in Settings search.
  Follow-up for #115. Evidence: `EVI-VESNA-003/parity38-*` on the
  owned AVD; no physical device or other gesture route exercised.

## OBS-VESNA-038 — Signed Vesna release adds opt-in Home placement

- On an owned Android 15 AVD, installed the same disposable,
  launchable APK while Vesna Home was visible and unlocked.
  Signed Vesna v0.4.51 (56) and the default-off v0.4.52 (57)
  both exposed it in the drawer without placing a Home shortcut.
- In v0.4.52, Settings search `new apps` found **Add new apps to
  Home**, off by default. With it on, a fresh install added exactly
  one Home shortcut, which stayed in the same cell after an
  `adb install -r` replacement and a cold launcher restart. It
  also appeared in the drawer.
- Fresh installs with the setting on but the workspace locked, or
  with the Vesna process stopped, were still discoverable in the
  drawer and did not add a Home shortcut. Reset both preferences,
  removed the fixture, and matched the original Home layout.
- [PR #127](https://github.com/vladon/vesnalauncher/pull/127)
  merged at `f1f785c3478280300c91dd9a6fbeb7e3881695bd`, the
  commit of the published v0.4.52 tag. Verified the GitHub release
  asset digest and installed it as an update over signed v0.4.51.
  Evidence: `EVI-VESNA-003/parity39-*`; independent verification
  for #107.
- Nova 9.0.1 Prime (90100) did not pin an ADB install in the
  earlier run (OBS-VESNA-019). A positive store/installer path in
  Nova, and full or secondary selected pages in this Vesna
  release, were not tested here.

## OBS-VESNA-039 — Auto-placement stays on the selected page

- Signed Vesna v0.4.52 (57), tag at
  `f1f785c3478280300c91dd9a6fbeb7e3881695bd`, on the Android 15
  research AVD; auto-placement on and workspace unlocked.
- Added a blank second page through **Home options → Add home
  page**. With page two selected, a fresh install created one
  shortcut in its upper-left cell. Page one had no test shortcut.
- Uninstalled the fixture and filled all 20 cells on page one
  while leaving page two empty. With the full page selected, a
  fresh install changed none of its cells. The app appeared in
  the drawer, page two stayed empty, and no third page was added.
- Restored the original emulator snapshot and checked the
  layout, preferences and removal of both disposable packages.
  Evidence: `EVI-VESNA-003/parity39-vesna052-*page*` and
  `parity39-vesna052-full-primary-*`. Follow-up for #107; no
  new Nova installer run.

## OBS-VESNA-040 — Landscape dock placement and rotation

- Owned Android 15 AVD, Nova 9.0.1 Prime (90100) and signed Vesna
  v0.4.52 (57). Compared the same two emulated sensor orientations
  with Android rotation unlocked. Neither launcher was reinstalled.
- With **Look & feel → Allow Home screen rotation** enabled, Nova
  rotated to 2400×1080. Its five dock pins and attached search
  pill formed vertical columns at the left edge in one orientation
  and at the right edge in the other. With the preference off,
  the same sensor rotation left Nova at 1080×2400 in portrait.
- With its original settings, Vesna rotated to 2400×1080 in both
  orientations. The four dock pins stayed in a horizontal bottom
  row and kept their left-to-right order. The Camera dock pin
  opened Camera from both launchers in the first landscape
  orientation. Returning upright restored the portrait layouts.
- Restored the original rotation preference and AVD state.
  Evidence: `EVI-VESNA-003/parity41-*`. Follow-up for #115;
  multipage dock navigation in landscape was not established.
- Nova store-install auto-placement remains untested: neither a
  Play Store launcher activity nor the market details link
  resolved on this AVD.

## OBS-VESNA-041 — Dropping one dock pin onto another

- Owned Android 15 AVD, Nova 9.0.1 Prime (90100) and signed Vesna
  v0.4.52 (57). With the dock visible in portrait, long-pressed
  Camera, dragged it onto the adjacent Chrome dock pin, paused
  over the target, and released. Both shortcuts were regular app
  pins, not folders.
- Nova used Chrome's slot for a two-item folder containing Chrome
  and Camera. A cold launcher restart kept the folder in that
  slot and both app entries were still present on reopen.
- In Vesna the same drop exchanged Camera and Chrome's dock slots;
  no folder was created, and the new pin order survived a cold
  restart. The same gesture on the workspace (Files onto Gmail)
  did create a folder. After moving Camera to the workspace and
  verifying the compacted dock's last slot was empty, dragging
  that workspace folder to the empty slot left it on the
  workspace with both entries still present.
- Restored both original layouts. Evidence: `EVI-VESNA-003/parity42-*`;
  follow-up for #115. Only the tested drag paths were exercised.

## OBS-VESNA-042 — Editing a workspace shortcut label

- Owned Android 15 AVD, Nova 9.0.1 Prime (90100) and signed Vesna
  v0.4.52 (57). Both started with an ordinary Gmail pin on Home.
- In Nova, long-press Gmail → **Edit** exposes the app label.
  Editing it and tapping **Done** renamed the pin in place. The
  label survived a cold launcher restart, the renamed pin still
  launched Gmail, and drawer search still listed the app as Gmail.
- In a separate edit, pressing Back first hid the keyboard. A
  second Back closed the editor and **kept** the edited label,
  including after a cold restart; Back was not a cancel path.
- The same Home long-press in Vesna showed Add to dock, Remove
  from workspace and App info, with no label editor. This check
  did not exercise custom icons or per-icon gestures.
- Restored the original Gmail label. Evidence:
  `EVI-VESNA-003/parity43-*`; follow-up for #114.

## OBS-VESNA-043 — An edge folder through grid shrink and expansion

- On the owned Android 15 AVD, Nova 9.0.1 Prime (90100) had four
  shortcuts across the top row and a folder containing Gmail and
  Nova Settings in the bottom-right cell of a 4×4 workspace.
- Shrinking to 2×2 left the folder on page one, moved one of the
  ordinary shortcuts to page two, and kept all five top-level
  items reachable. The 2×2 setting and the folder's position
  survived a cold restart, and the folder still opened to both
  members. Expanding to 4×4 restored all five original cells and
  the folder still contained both entries.
- Restored the original Nova workspace and removed the fixture.
  In signed Vesna v0.4.52 (57), Settings search for `grid` still
  returned no results. This is the signed pre-fix baseline, not
  a test of merged PR #128. Widget migration was not exercised.
- Evidence: `EVI-VESNA-003/parity44-*`. Follow-up for #106.

## OBS-VESNA-044 — Signed Vesna grid resize independently verified

- On the owned Android 15 AVD, updated signed v0.4.52 (57) in
  place to the official v0.4.53 APK (58). Verified the release
  asset SHA-256 and the same signing certificate as v0.4.52.
  The v0.4.53 tag resolves to PR #128's merge commit
  `90e8cb3e7aff6fa91fe69f467de1356b0f7387f6`. All 17 existing
  workspace shortcuts and the dock retained their positions.
- Settings search for `grid` opened **Workspace grid**, showing
  4 columns × 5 rows. Selecting 2×2 and pressing **Cancel** left
  both the dimensions and the layout unchanged.
- Merged Files and Gmail into a folder and moved it to the
  bottom-right cell on the otherwise populated page. Applying
  2×2 spread the 16 top-level items across four pages (four each),
  with the folder on page four. Checked every page for missing or
  duplicated items, the unchanged dock, and both folder members.
  The layout and folder membership persisted after a cold start.
- Returning to 4×5 left the same items on each of the four pages;
  it did not reconstruct the previous one-page arrangement. Nova
  restored original cells in its separate five-item 4×4 fixture
  (OBS-VESNA-043); the fixture density and starting size differ.
- Restored the original layout and kept v0.4.53 installed.
  `EVI-VESNA-003/parity45-*`; independent device verification for
  #106. The eight-page cap was not replayed. Widget migration was
  not tested: the stock Nova widget drop reached Android's
  widget-data consent, which was declined.

## OBS-VESNA-045 — Matched grid geometry still yields different overflow

- Android 15 research AVD, Nova 9.0.1 Prime (90100, OBS-VESNA-043)
  and signed Vesna v0.4.53 (58, PR #128). Matched the 4×4 starting
  geometry: four ordinary shortcuts across row one, a two-app
  folder at row four/column four, and no other workspace pages.
  The app names and original dock contents differed.
- On 2×2, Nova kept the folder on page one and put one ordinary
  shortcut on page two. Vesna kept all four ordinary shortcuts
  on page one and put the folder on page two. Vesna's five
  top-level items and page assignments survived a cold restart;
  the folder reopened to Files and Gmail and the dock was unchanged.
- Returning to 4×4 restored the original five cells in Nova.
  Vesna retained both pages, with the folder at the top left of
  page two and both members intact. No items disappeared.
- Restored the original layout. Vesna evidence:
  `EVI-VESNA-003/parity46-*` and `parity45-cycle46-*`; Nova
  reference: `parity44-*`. Follow-up for #106. This case does
  not cover widgets or the eight-page limit.

## OBS-VESNA-046 — Per-icon swipe and tap use separate actions

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.53 (58), each with an ordinary Gmail pin on Home.
- In Nova, long-press Gmail → **Edit → Swipe up → Apps → Clock**
  → **Done**. A tap still opened Gmail. After a cold launcher
  restart the editor still showed the Clock binding, and a short
  upward swipe from the rendered Gmail icon opened Clock.
  The same swipe from a blank cell did not open Clock.
- Cleared the binding to **None**, restarted Nova, and verified
  that the same upward swipe on Gmail no longer opened Clock.
- In Vesna, the same short swipe left Home unchanged and tapping
  Gmail opened Gmail. The matching long-press menu had no icon
  editor or per-icon swipe entry.
- Gesture used for the paired positive/control trials: about
  130 px in 110 ms at 1080×2400, starting on the glyph, not the
  label. Restored the original binding and layouts. Evidence:
  `EVI-VESNA-003/parity47-*`; follow-up for #114. Swipe down and
  custom icon selection were not exercised.

## OBS-VESNA-047 — Dock pages swipe vertically in Nova landscape

- On the owned Android 15 AVD, Nova 9.0.1 Prime (90100) had five
  pins on dock page one and one identifiable test pin on page two.
  In portrait, horizontal swipes reached page two and returned.
- Enabled **Allow Home screen rotation**. In both landscape
  orientations, a downward swipe along the side dock opened page
  two, and an upward swipe over its pin returned to page one.
  The visible workspace icons kept their bounds throughout.
  Horizontal strokes from the left-side dock on page two did not
  switch the dock page.
- Signed Vesna v0.4.53 (58) kept four pins in the bottom row in
  landscape. Swiping left or right along that row left it and the
  workspace unchanged. Settings search for `dock` found other
  settings that mention the dock, but no dock paging control.
- Restored the rotation setting, portrait orientation and both
  layouts; removed the disposable pin and verified the second
  Nova dock page was empty again. Evidence:
  `EVI-VESNA-003/parity48-*`; follow-up for #115.

## OBS-VESNA-048 — Selected dock page carries across workspace pages

- Android 15 research AVD in portrait, Nova 9.0.1 Prime (90100)
  and signed Vesna v0.4.53 (58). Each had two workspace pages
  distinguished by their pins. Nova had two dock pages, with
  five regular pins on page one and one test pin on page two.
- In Nova, with dock page two selected, workspace swipes
  2→1→2 changed the workspace while keeping the same dock pin
  and bounds. On workspace page two, a dock swipe selected
  dock page one without moving the workspace. Workspace swipes
  2→1→2 then kept all five first-page dock pins and bounds.
  The active dock page was shared, not stored per workspace page.
- On Vesna, the same workspace swipes changed the visible page
  (confirmed by page labels and the test pin), while the four
  dock pins kept their order and bounds. No dock paging was used.
- The original layouts and package state were restored. This
  tests in-session paging, not dock position memory on restart.
  Evidence: `EVI-VESNA-003/parity49-*`; follow-up for #115.

## OBS-VESNA-049 — A built-in icon override is local to the Home shortcut

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.53 (58). Both had an ordinary Gmail shortcut on Home.
- In Nova, long-press Gmail → **Edit** → tap the icon preview
  → **Built-in** → select the pink mail icon → **Done**.
  The new artwork survived a cold launcher restart. The label
  and cell were unchanged, and tapping the pin still opened Gmail.
  The Gmail icon in the drawer search result was unchanged.
- The standard Gmail thumbnail under **Common** brought the
  original artwork back. The pre-test checkpoint and both
  launcher layouts were restored and checked.
- Vesna's matching Gmail Home menu showed Add to dock, Remove
  from workspace and App info, with no per-shortcut icon editor.
  No gallery import or installable icon pack was tested.
- Evidence: `EVI-VESNA-003/parity50-*`; follow-up for #114.

## OBS-VESNA-050 — A fresh app appeared in predictions after restart

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.53 (58). Pinned the same disposable app, **Nova
  Install Fixture**, to each launcher's Home. Used a fresh package
  rather than Gmail, which had been dismissed in an earlier
  prediction-menu test (OBS-VESNA-022).
- Opened each drawer by swiping up from Home with no query.
  In Nova, the fresh app was absent before launch; the five-app
  row remained unchanged after one and six verified launches
  from Home. After a cold Nova restart, the next drawer opening
  showed the fixture first in that row, replacing Chrome in the
  five visible entries.
- In Vesna, the drawer still began with the same alphabetical
  app grid after one and six verified Home launches and after
  a cold restart. No suggestion row appeared, and exact search
  still found the fixture.
- This run shows a delayed change, not a ranking formula or a
  requirement to restart. Restored both layouts and the prior
  Nova row, and removed the fixture. Evidence:
  `EVI-VESNA-003/parity51-*`; follow-up for #112.

## OBS-VESNA-051 — Signed Vesna opens and pins a native Clock action

- On the owned Android 15 AVD, placed an ordinary Google Clock pin
  on each workspace. Nova 9.0.1 Prime (90100) exposed **Create new
  timer** on long-press. Tapping the action opened Timer's keypad.
  Dragging the action made a separate pin; after a cold Nova restart
  that pin opened the keypad even with Clock last left on Alarm.
- Signed Vesna v0.4.53 (58) had no timer action in the matched menu.
  After finishing that baseline, installed signed v0.4.54 (59)
  over v0.4.53. The release digest and signer continuity checked;
  the tag resolves to PR #129's merge
  `c6b34cad27ad3c41cf17d92eaa72b72d5d85ec58`. The workspace and dock
  positions survived the update.
- With Nova still Android Home, v0.4.54 offered **Edit name** but
  did not list Clock's actions. Selecting Vesna as Home on this AVD
  exposed **New timer → Open / Add to Home**. Open reached the real
  Timer keypad. Add to Home created a separate pin; re-adding it
  did not duplicate it. No alarm or timer was started.
- **Cancel** discarded a draft name. **Save** renamed the pin
  `Timer QA` in place, and it still opened the same Timer keypad
  after a cold Vesna restart. With layout lock on, the menu retained
  **Open**, but not **Add to Home** or **Edit name**.
- Removed the temporary pins, restored the unlocked layout and Nova
  as Android Home, and kept signed v0.4.54. Widgets, action
  discovery from the drawer, and launcher-role loss after pinning
  were not replayed. Evidence: `EVI-VESNA-003/parity52-*`;
  independent device verification for #114.

## OBS-VESNA-052 — Drawer action discovery and launch after Home-role loss

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59), each tested as Android Home for discovery.
  Long-pressing Clock in Nova's drawer search results offered
  **Create new timer** and opened the native Timer keypad.
- In Vesna, Clock's menu offered Add to workspace, Add to dock and
  App info, but no publisher actions. The same was true in the
  unfiltered drawer. Adding the ordinary Clock pin to Home and
  long-pressing it exposed **New timer**, so the publisher and
  Home-role prerequisite were both present.
- Pinned **New timer** from Vesna's Home menu, left Clock on Alarm,
  and returned the Android Home role to Nova. Cold-starting Vesna
  from its app entry retained the pin; the ordinary Clock menu
  stopped listing **New timer**. Tapping the existing action pin
  still switched Clock from Alarm to the Timer keypad.
- No timer was started. Restored the original layouts and Nova
  as Android Home. Evidence: `EVI-VESNA-003/parity53-*` and the
  Nova search setup `parity50-cycle53-*`; follow-up for #114.

## OBS-VESNA-053 — Vesna name edits distinguish Back, Save and Reset

- On the Android 15 research AVD, signed Vesna v0.4.54 (59) had
  an ordinary Gmail Home pin matching the Nova 9.0.1 Prime
  (90100) reference in OBS-VESNA-042. Nova remained Android Home
  during the Vesna replay; the editor was still available.
- In Vesna, long-press Gmail → **Edit name**, type a draft, then
  press Back twice. The first Back left the editor open; the
  second dismissed it and discarded the draft. The original
  name was still present after restart. In the Nova reference,
  the equivalent Back sequence kept the edit.
- **Save** renamed the existing pin in the same cell. The
  alias survived restart and still opened Gmail. Drawer search
  found `Gmail` and returned no matches for the Home alias.
- **Reset** closed the editor and immediately restored `Gmail`
  without an additional Save. Restart confirmed the original
  layout and label; no changes to Nova were needed.
- Evidence: `EVI-VESNA-003/parity54-*` and draft-entry captures
  under `parity52-cycle54-*`. Follow-up for #114.

## OBS-VESNA-054 — The 2×2 resize boundary is 32 workspace items

- On the owned Android 15 AVD, signed Vesna v0.4.54 (59) used a
  4×5 grid. From the original 17 workspace pins, added ordinary
  Clock pins through the drawer and created a second page.
  Before shrinking, verified 33 items: 20 on page one and 13
  on page two. The four dock pins were not part of the count.
- Selecting 2×2 and pressing **Apply** showed “This grid is too
  small for your layout. Choose more columns or rows.” The
  applied size stayed 4×5. After closing the dialog and cold
  restarting Vesna, both pages and all 33 original cells from
  the staged layout matched their pre-attempt state.
- Removed only one of the added Clock pins and retried 2×2.
  Vesna accepted it. Browsed all eight pages: four distinct
  cells per page, all 32 expected pins including the deliberate
  repeated Clock pins, and the same four dock pins. After
  restarting, the eight-page count and the visible page held.
- Restored the original 17-pin layout. This verifies Vesna's
  capacity and atomic rejection for #106 / PR #128 on the
  signed build, not a Nova capacity limit. Evidence:
  `EVI-VESNA-003/parity55-*` and `parity45-cycle55-*`.

## OBS-VESNA-055 — Manual restore retains a renamed native action

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59). In Nova, pinned Clock's **Create new
  timer** action as a separate Home shortcut and renamed it
  `Backup Timer QA`, keeping the ordinary Clock pin alongside.
- Exported a completed local `.novabackup` through the system
  picker. Removed just the action pin and verified it was
  absent. Selected that file and accepted **Overwrite current
  layout?**. The restored shortcut kept the saved cell, the
  custom visible label and the publisher action description.
- Opened Clock on Alarm, cold-started Nova, and tapped the
  restored shortcut. It opened the native Timer keypad rather
  than the last-used Alarm screen. No timer was started.
- Vesna Settings searches for `backup`, `restore` and `export`
  exposed no manual file route. No Vesna restore or Android-
  managed transfer behavior is inferred from this.
- Restored both original layouts and removed the test files.
  Observed only the backup UI and file metadata, not its
  contents. Evidence: `EVI-VESNA-003/parity56-*` and
  `parity33-cycle56-*`; follow-up for #116.

## OBS-VESNA-056 — Nova hands a Maps query off while offline

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59). Enabled airplane mode and disabled Wi-Fi
  and mobile data; Android showed no active default network.
- Used the same fresh place query, `Plaza Mayor Lima QA 5726`,
  in both launchers. Vesna showed no matching apps and no
  provider action. A separate search for `Maps` still opened
  the installed app but did not pass the place query.
- Nova's **Google Maps** provider opened the installed Maps
  app and populated its search field with the exact query.
  Maps showed “Looks like you're offline” and asked for a
  connection to search the area. Android still had no active
  default network after the handoff.
- This verifies local handoff, not remote results, network
  transmission or behavior of other providers. Restored the
  original radios and layouts. Evidence: `EVI-VESNA-003/parity57-*`
  and `parity31-*cycle57-*`; follow-up for #113.

## OBS-VESNA-057 — Workspace folder removal and the chosen cell

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59). In each, dropped Photos onto Gmail,
  then Files onto the closed folder. Opening it showed all
  three members and Files was no longer a top-level pin.
- In Nova, dragging Files out of the open folder placed it
  in the selected empty Home cell and left a two-member folder.
  Dragging Photos out next dissolved it, leaving Gmail in
  the former folder cell.
- In Vesna, the corresponding hold-and-drag opened the member
  menu rather than moving Files to the chosen cell. **Remove
  from folder** returned members to Home automatically. In a
  control, removing Photos first put it into the earlier
  vacancy left by Files, not Photos' original cell.
- Removing the penultimate member through the Vesna menu
  dissolved the folder. Gmail remained in its former cell and
  both extracted apps were present after a cold restart.
- Restored both layouts. Evidence: `EVI-VESNA-003/parity58-*`.
  Workspace drag-out gap tracked in #130; paging and reordering
  inside an open folder were not tested here.

## OBS-VESNA-058 — Reordering members inside a workspace folder

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59). Both started with the same visual reading
  order: Files, Gmail, Photos. Held Photos and dragged it over
  Files, releasing inside the same open folder.
- Nova inserted Photos first, shifting Files and Gmail in order.
  The new order survived closing the folder, reopening it, and
  cold-starting the launcher.
- Vesna opened the Photos member menu. Dismissing it, reopening
  the folder, and cold-starting all left the original order and
  all three members intact.
- Restored both initial layouts. Evidence:
  `EVI-VESNA-003/parity58-cycle59-*`. Internal ordering is
  tracked in #131, separate from drag-out (#130). Multi-page
  behavior was not exercised.

## OBS-VESNA-059 — Whole folder removal and member pins

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59). Both used a closed Home folder with
  Gmail, Photos and Files and available workspace cells.
- Nova's **Remove** hid the folder and all three Home pins.
  Each app remained searchable in the drawer. In a second
  run, **Undo** restored the exact folder cell and the three
  members in the original order.
- Vesna's **Remove folder** instead returned all three pins
  to Home and removed only the folder. The ungrouped layout
  persisted after cold start. This matches the existing
  ungroup behavior described in PRs #5 and #6.
- Both original layouts restored. Evidence:
  `EVI-VESNA-003/parity60-*`. One-action unpin tracked in
  #132; packed-grid removal was not tested.

## OBS-VESNA-060 — Folder overflow and scroll direction

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59). Matched folders contained the same twelve
  apps. Tested at 520×890 pixels, 420 dpi (about 198×339 dp)
  and 200% text.
- Nova's Horizontal direction showed three pages of four
  members. A vertical swipe did not change pages; a further
  horizontal swipe on the last page stayed there, and reversing
  direction returned to page two. Selecting Vertical in Nova's
  Folders settings changed the active swipe axis while retaining
  the same three groups.
- Vesna showed all twelve by vertical scrolling. Horizontal
  swipe did not advance the list. The last row was fully visible
  at the settled bottom, and Rename folder and Remove folder
  remained in place across scrolling. Settings search for
  `folder` exposed layout lock and app-name visibility, not
  a folder direction preference.
- Restored both layouts, display and text size, and Nova's
  original Horizontal setting. Evidence: `EVI-VESNA-003/parity61-*`.
  Optional direction parity: #133. Compact vertical overflow
  worked on this release, consistent with PRs #55 and #71.

## OBS-VESNA-061 — Batch addition to an existing folder

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59). Each started with Gmail and Photos in a
  Home folder and no top-level Home pins for Contacts and Drive.
- In Nova, long-pressed the closed folder and opened **Select
  apps**. Staged two selections. BACK kept both original members
  and added neither app. Repeated the selection and tapped
  **Save**: both apps joined the folder and survived cold start.
- Vesna's closed-folder menu and open-folder actions showed
  rename and removal, not a batch selector. Dragging Contacts
  from the drawer directly onto the closed folder committed it
  immediately. Reopening the drawer and dragging Drive added
  the second app. Both persisted after cold start.
- Restored both layouts. Evidence: `EVI-VESNA-003/parity62-*`.
  Batch addition and cancellation tracked in #134. This did not
  test duplicate members, removing apps with the selector, or
  sorting.

## OBS-VESNA-062 — One-action alphabetical folder sort

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59). Both Home folders started with the same
  nonalphabetical visual order: Gmail, Photos, Files.
- Long-pressed Nova's closed folder and tapped **Sort**. It
  applied without another prompt. Reopening the folder showed
  Files, Gmail, Photos in the same folder cell with the same
  membership; the new order survived a cold restart.
- Vesna's closed-folder menu and open-folder actions offered
  rename and removal, not sorting. Settings search for `sort`
  found no match. The original nonalphabetical order survived
  a cold restart.
- Restored both layouts. Evidence: `EVI-VESNA-003/parity63-*`.
  The one-action sort is tracked in #135, separate from manual
  in-folder drag reorder (#131).

## OBS-VESNA-063 — Swipe-down arbitration in the app drawer list

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59), single-column vertical list in both,
  blank query and keyboard hidden. Nova had its existing drawer
  groups/predictions; the comparison used each list's own top
  and a visibly scrolled position.
- In both, a mid-list downward swipe from (530,1120) to
  (530,1850) over 440 ms moved back toward earlier rows without
  dismissing the drawer. Starting at the top, the same gesture
  left the drawer open with the same rows.
- Starting at the top, a downward swipe from (530,810) to
  (530,1710) over 280 ms closed both drawers and restored the
  same Home and dock layouts. Two gesture samples do not
  establish a velocity or distance threshold.
- No difference in these cases. Grid, horizontal pages, and
  keyboard-visible dismissal were not compared in this run.
  Restored Vesna's original grid setting and both layouts.
  Evidence: `EVI-VESNA-003/parity64-*`.

## OBS-VESNA-064 — Drawer return after launching an app

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.54 (59). Each was the system Home during its own
  run. Used the single-column list with a blank query and
  hidden keyboard, scrolled it, opened Clock from its normal
  row, and pressed Android BACK from Clock's main screen.
- With Nova's **Automatically close** off, BACK returned to
  the same visible drawer rows. Enabling the option under
  App drawer → Advanced made BACK return directly to Home.
  **Remember position** stayed off in both Nova trials.
- Vesna returned to the drawer with the same visible rows
  and their original bounds. The tested App drawer settings
  and `automatically close` settings search did not offer the
  optional Home-return mode.
- Restored the original Home role, both layouts, Nova's
  disabled auto-close, and Vesna's grid. Evidence:
  `EVI-VESNA-003/parity65-*`. Optional drawer auto-close is
  tracked in #137, separate from dock auto-close (#115).
  Search-result launches and other drawer layouts were not tested.

## OBS-VESNA-065 — Signed v0.4.55 folder drag regression

- [PR #136](https://github.com/vladon/vesnalauncher/pull/136)
  merged at `c3d466f65fafe456737fe81f6bc7c565d92a1f73`. The
  [v0.4.55 release tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.55)
  points to that commit. Independently downloaded the public
  APK, matched SHA-256
  `9a0406dc85c25dcbe5565371922308f2ea71d1ea1ddac1e6cee4fc9f14aae60b`
  and the v0.4.54 signer, installed on the Android 15 research
  AVD, and checked installed version 0.4.55 (60).
- Recreated the unlocked Files/Gmail/Photos folder of
  OBS-VESNA-057. Dropping Files on the occupied Contacts
  cell or cancelling a visible preview over an empty cell
  left the folder open without changes to membership or
  Home. Stationary long-press still showed member removal
  and App info; layout lock blocked extraction, leaving only
  App info in the member menu.
- Unlocked, Files landed in the chosen cell centred at
  (927,1772), leaving Gmail/Photos in the folder. Photos
  then landed at (409,1772); Gmail occupied the former
  folder cell (927,336). Other pins did not move. The final
  layout survived a cold launcher start.
- This closes the tested signed-release gap against Nova
  9.0.1 Prime from OBS-VESNA-057. It does not verify
  in-folder reorder, cross-page drag, or other window sizes.
  Restored both layouts and the original lock state, leaving
  v0.4.55 installed. Evidence: `EVI-VESNA-003/parity66-*`.
  [Independent result in #130](https://github.com/vladon/vesnalauncher/issues/130#issuecomment-7090925697).

## OBS-VESNA-066 — Internal folder drop after the drag-out fix

- With the signed v0.4.55 (60) APK on the Android 15 research
  AVD, replayed the internal drag from OBS-VESNA-058 because
  PR #136 changed how a folder member enters drag mode.
- Started with Files, Gmail, Photos in that reading order.
  Held Photos, moved horizontally over Files, and released
  within the original open folder's bounds. The new release
  briefly revealed Home during the gesture, then reopened
  the folder. It did not open the old member menu on this
  gesture, and it did not reorder or extract Photos.
- Both the original Home layout and the three members in
  their original order were present after a cold restart.
  Nova's insertion in OBS-VESNA-058 remains unmatched on
  this path. Exact-cell drag-out remains a separate passing
  case in OBS-VESNA-065.
- Restored original layouts and kept v0.4.55 installed.
  Evidence: `EVI-VESNA-003/parity67-*`.
  [Follow-up in #131](https://github.com/vladon/vesnalauncher/issues/131#issuecomment-7090974852).

## OBS-VESNA-067 — Edge drag from a folder to an existing Home page

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.55 (60). Both had Files, Gmail and Photos in a
  page-one folder, and a Drive marker on page two. The
  lower-right cell was vacant on both pages.
- Held Files in the open folder, moved to the right edge,
  dwelled for about 1.5 seconds, and released over the
  lower-right cell. Nova showed Drive on page two behind
  the still-open folder during the drag. After dismissing the
  folder window, Files occupied the chosen page-two cell.
- Vesna stayed on page one and put Files in its lower-right
  cell. Page two still contained only Drive. The two other
  members stayed in the original folder in both launchers,
  and the page assignments survived cold starts.
- An ordinary-pin control created a second page via edge
  drag in Nova. The equivalent edge dwell in Vesna moved
  the pin on page one both with and without a pre-existing
  neighbor; the Vesna test page was added via Home options.
- Restored the original one-page layouts and docks. Evidence:
  `EVI-VESNA-003/parity68-*`. Existing-page drag routing is
  tracked in #139, separate from the passing same-page
  exact-cell extraction in #130.

## OBS-VESNA-068 — Back from an active app drawer query

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.55 (60). Each was system Home during its run.
  With the vertical drawer list selected, focused the search
  field, typed `Clock`, and verified the keyboard was visible.
- In both launchers the first Android BACK hid the keyboard
  while leaving `Clock` in the focused field and keeping its
  result visible. The second BACK closed the drawer to the
  same Home page and dock. Neither showed a blank browse
  list as an intermediate step.
- Nova behaved the same when opened by a swipe from Home
  rather than by tapping the Home search icon.
- Identical behavior in this case; no issue filed. Restored
  Vesna's original grid, Nova as system Home, and both
  layouts. Evidence: `EVI-VESNA-003/parity69-*`.

## OBS-VESNA-069 — Signed v0.4.56 drawer auto-close

- [PR #138](https://github.com/vladon/vesnalauncher/pull/138)
  merged at `922dbac5795c415bb96fc924f8da2fbfad14d31e`.
  The [v0.4.56 release tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.56)
  points to that commit. Verified public APK SHA-256
  `09f22a1d798298a1ad348e6970dbb9202eba04f06cbeedb073a36af3e136ba28`
  and the same APK signer as v0.4.55. Upgraded the
  Android 15 research AVD in place and checked 0.4.56 (61).
- As system Home, Vesna defaulted to auto-close off. On
  Home page two, opening Clock from the scrolled app list
  and pressing BACK restored the same rows and bounds.
- Settings search for `automatically close` found the new
  switch. After enabling it, opening Clock from the same
  list returned directly to Home page two on BACK. A cold
  launcher restart produced a new PID and retained both
  the switch and the selected page.
- With auto-close enabled, pressing Enter on the Clock
  search result opened Clock and BACK returned to Home
  page two in both signed Vesna v0.4.56 and Nova 9.0.1
  Prime (90100). Each was default Home for its own run.
- Restored both original layouts, default Nova, Vesna's
  grid, and both switches off; kept v0.4.56 installed.
  Evidence: `EVI-VESNA-003/parity70-*`,
  `parity65-cycle70-*`, and `parity69-cycle70-*`.
  Grid/pager launch cases and launch failure were outside
  this independent replay. [Result in #137](https://github.com/vladon/vesnalauncher/issues/137#issuecomment-7091328895).

## OBS-VESNA-070 — Signed v0.4.56 auto-close in the grid and pager

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.56 (61). Each was system Home for
  its runs. Expanded OBS-VESNA-069 to ordinary browse
  launches in the vertical grid and the first page of
  the horizontal drawer, with no query and IME hidden.
- In both layouts and launchers, tapped the normal Clock
  tile, not the dock or predictions. With auto-close off,
  BACK from Clock returned to the drawer and the same
  Clock tile. With it on, BACK returned directly to the
  original Home with the drawer closed.
- Restored Nova List, Vesna Vertical grid, both auto-close
  switches off, the original Home role, and both layouts.
  Evidence: `EVI-VESNA-003/parity71-*`, `parity65-cycle71-*`,
  and `parity70-cycle71-*`. No second drawer page or failed
  launch was exercised.
  [Additional result in #137](https://github.com/vladon/vesnalauncher/issues/137#issuecomment-7091492246).

## OBS-VESNA-071 — Drawer position after system Home and reopen

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.56 (61). Each was system Home during
  its trials. Both used the single-column app list, no query
  or IME, with auto-close off. Disabled Nova's Show Keyboard
  so opening the drawer did not enter search.
- Scrolled away from the top, launched Clock from its normal
  row, pressed system HOME, then explicitly reopened the
  drawer. Nova with **Remember position** off returned to
  its original top rows. Switching it on preserved the
  pre-launch visible rows and their bounds on reopening.
- Vesna reset to its original top rows. The tested drawer
  settings and `remember position` settings search did not
  expose the optional retention behavior.
- Distinct from BACK into a drawer that stayed open (#137),
  restoring position upon clearing a query (#108), and dock
  memory (#115). Other drawer layouts, screen-off and process
  persistence of the offset were not tested. Filed #141.
- Restored both layouts, system Home, Nova's Show Keyboard
  and Remember position, and Vesna's original grid. Evidence:
  `EVI-VESNA-003/parity72-*`, `parity65-cycle72-*`,
  `parity70-cycle72-*`, and `parity71-cycle72-*`.

## OBS-VESNA-072 — Signed v0.4.57 in-folder reorder

- [PR #140](https://github.com/vladon/vesnalauncher/pull/140)
  merged at `701c65db61396bb392f153a4ad7887cf7cd5522b`.
  The [v0.4.57 release tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.57)
  points to that commit. Matched the public APK SHA-256
  `f5d1928ca59feca37842fd4e81d9f745ef30dc6ecc552882e72de8cf7c3584a3`
  and the v0.4.56 signer. Installed signed v0.4.57 (62) on
  the independent Android 15 research AVD.
- Started with Files, Gmail, Photos in both Nova 9.0.1
  Prime (90100) and Vesna. Moving Photos over Files
  produced Photos, Files, Gmail; moving it to the last
  position restored the original sequence. In both
  directions, the intervening members shifted rather than
  swapping, with all three still in the folder.
- In Vesna, cancelling a live internal drag and repeating a
  drag with layout lock on both kept the prior order. A
  stationary long-press still opened the member menu. The
  changed order survived folder reopen and cold launch,
  without any Home pin changing.
- Moving Files outside the panel still placed it in the
  chosen empty cell (927,1772), as in #130. Photos and Gmail
  stayed in the folder in that order; both persisted across
  another cold launcher start.
- Restored both layouts and unlock state, leaving v0.4.57
  installed. Evidence: `EVI-VESNA-003/parity73-*` and
  `parity66-cycle73-*`. Overflow, RTL, and another Home
  page were outside this independent replay.
  [Result in #131](https://github.com/vladon/vesnalauncher/issues/131#issuecomment-7091728287).

## OBS-VESNA-073 — Offscreen reorder in a compact folder

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.57 (62). Matched all twelve items and
  their initial order. Set both to 520×890 px at 420 dpi
  (about 198×339 dp) with 200% text. Contacts started at
  rank one; YT Music was initially offscreen at the end.
- Dragged Contacts toward the right edge of Nova's folder
  and the bottom edge of Vesna's. Without lifting, reached
  the last page in Nova and the bottom of the list in Vesna.
  Released at the last rank. Both inserted Contacts last and
  preserved the other eleven members' relative order. Cold
  launches confirmed all twelve ranks; no Contacts pin was
  created on Home.
- In separate cancelled trials, both still showed Contacts
  first when reopened. Immediately after cancel, Vesna kept
  the folder open at the far end; Nova dismissed it to Home.
- Restored the display, font scale, original layouts,
  and default Nova. Kept v0.4.57 installed. Evidence:
  `EVI-VESNA-003/parity74-*`.
  [Follow-up in #131](https://github.com/vladon/vesnalauncher/issues/131#issuecomment-7091858726).
  This did not test RTL or crossing Home pages while dragging.

## OBS-VESNA-074 — Signed v0.4.58 sorts by the visible label

- [PR #142](https://github.com/vladon/vesnalauncher/pull/142)
  merged at `0c4f408383542b0c40511ce6e1579c137a4a020e`.
  The [v0.4.58 release tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.58)
  points to that commit. Verified public APK SHA-256
  `dd473b100143ee86b10e10458462d0350c8fb350247c5c6e55c7a9f7d710a12b`
  and the same signer as v0.4.57. Installed signed v0.4.58
  (63) on the independent Android 15 research AVD.
- Renamed the Gmail Home pin to `Zulu QA`. In Nova 9.0.1
  Prime (90100) and signed Vesna v0.4.57/58, prepared the
  same visible order: Zulu QA, Photos, Files. v0.4.57 kept
  the custom name but had no sort in the tested menu.
- Opening the folder or dismissing its menu on v0.4.58
  did not change order. Choosing **Sort alphabetically**
  produced Files, Photos, Zulu QA, matching Nova's **Sort**.
  Using the original Gmail label would have placed it
  before Photos. The custom label, folder cell, unrelated
  pins and sorted order were preserved after cold start.
- Moving the renamed pin first afterward persisted on
  reopening without implicit sort. With layout lock on,
  the closed-folder sort action was unavailable and the
  manually chosen order remained.
- Restored both original layouts, labels and lock state;
  kept v0.4.58 installed. Evidence: `EVI-VESNA-003/parity75-*`
  and `parity66-cycle75-*`. Ties, locale-specific collation,
  RTL, and keyboard operation were outside this replay.
  [Result in #135](https://github.com/vladon/vesnalauncher/issues/135#issuecomment-7092040787).

## OBS-VESNA-075 — Stable sort for equal folder labels

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.58 (63). Renamed separate Gmail and
  Photos pins to the identical visible label `Twin QA`,
  then put them in a Home folder with Files. Verified the
  identities from the distinct app icons, not the label.
- In both, sorting Photos → Gmail → Files yielded
  Files → Photos → Gmail. Sorting the opposite input
  Gmail → Photos → Files yielded Files → Gmail → Photos.
  Sort preserved the pre-existing relative order of the
  equal labels rather than using the native app names.
- Both custom labels, member identities, the folder cell,
  and the second sorted order survived cold starts.
  Restored both original layouts and labels. Evidence:
  `EVI-VESNA-003/parity76-*`.
  [Follow-up in #135](https://github.com/vladon/vesnalauncher/issues/135#issuecomment-7092156729).
  Case and accent collation and RTL were not exercised.

## OBS-VESNA-076 — Whole folder removal on a packed Home page

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.58 (63). Each had one 2×2 Home page
  with all four cells occupied: Contacts, Drive, Google,
  and a folder containing Files, Gmail and Photos.
- Nova **Remove** unpinned the folder and all three
  members even with no spare cells. The other pins stayed
  put and survived restart. In a separate timed trial,
  **Undo** restored the exact folder cell, membership and
  original order.
- Vesna **Remove folder** refused with “Not enough room
  on this page” and kept both Home and folder unchanged.
  After freeing exactly two cells, the same command
  ungrouped all three members onto the page, including
  the former folder cell. Google stayed in its own cell;
  the ungrouped result survived cold start.
- This supports a separate unpin action, not a change
  to the existing capacity safeguard. Restored both
  original grids, layouts and docks.
  Evidence: `EVI-VESNA-003/parity77-*` and `parity45-cycle77-*`.
  [Follow-up in #132](https://github.com/vladon/vesnalauncher/issues/132#issuecomment-7092378192).

## OBS-VESNA-077 — Signed v0.4.59 remembers the vertical drawer after Home

- [PR #143](https://github.com/vladon/vesnalauncher/pull/143)
  merged at `1323070deea48d2f32aa2bdcf77d60cf401c6f60`;
  the [v0.4.59](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.59)
  tag points to the same commit. Independently installed the
  public release (64), not a local `main` build. APK SHA-256:
  `b924689015bef518a404b7764b59669bda99bea4630f2dc596e4869a5c8b6e84`;
  the v2 signer matched v0.4.58.
- Android 15 research AVD, gesture navigation, Vesna as Home,
  single-column **Vertical list**, auto-close off, no query or IME.
  After scrolling, opening Clock from its normal row, pressing
  Android Home and reopening the drawer, **Remember position**
  off (untouched default) returned to the original top rows.
  On restored all 12 visible rows at their original bounds,
  matching the Nova 9.0.1 Prime comparison in OBS-VESNA-071.
  Home and dock placements were unchanged.
- Back closed the restored drawer; its next opening showed
  Calendar first, matching the original top viewport (the
  restored viewport had Chrome first). In a separate run,
  replacing the Vesna process after Clock → Home discarded
  the pending offset, but the preference remained enabled.
  Other navigation modes, search and grid/pager behavior
  were not independently rerun here.
- Restored the vertical grid, the original drawer preferences
  and Nova as system Home. Verified both Home layouts and docks;
  Vesna v0.4.59 remained installed.
- Evidence: `EVI-VESNA-003/parity78-*`, `parity70-cycle78-*`
  and `release-0.4.59`.
  [Independent result in #141](https://github.com/vladon/vesnalauncher/issues/141#issuecomment-7092515174).

## OBS-VESNA-078 — Folder sorting with mixed-case labels

- Android 15 research AVD, `en-US`, Nova 9.0.1 Prime (90100)
  and signed Vesna v0.4.59 (64; APK identity in OBS-VESNA-077).
  Renamed the Gmail and Photos pins to `alpha` and `Beta`,
  respectively, and used an unchanged `Files` pin.
- Both Home folders began in the order `Files`, `alpha`, `Beta`.
  Nova **Sort** and Vesna **Sort alphabetically** both produced
  `alpha`, `Beta`, `Files`. Lowercase `alpha` sorted before
  uppercase `Beta`; the two commands do not simply sort by
  character code. The folder cell and unrelated Home pins did
  not move.
- Restored the original labels, workspace and docks in both
  launchers. This trial did not establish accent ordering or RTL.
  Evidence: `EVI-VESNA-003/parity79-*`.

## OBS-VESNA-079 — Global adaptive icon shape

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and signed
  Vesna v0.4.59 (64). Compared standard Home pins and the
  unfiltered single-column app list with Calendar visible.
- Nova **Look & feel → Icon style → Rounded square** changed
  the actual Photos and Gmail masks on Home, Chrome in the
  dock, and Calendar in the list. Rounded-square Home and dock
  icons survived a launcher restart. Returning to **Round
  (Default)** restored the previous appearance without moving
  Home or dock items.
- Vesna **Appearance** exposed theme and app names. Settings
  searches for `icon`, `shape`, and `icon style` returned no
  results. No equivalent global shape control surfaced on the
  tested public settings paths. This is not a per-pin artwork
  override (OBS-VESNA-049) or an icon pack comparison.
- Restored the original Nova shape and Vesna drawer grid;
  both layouts and docks were unchanged. Evidence:
  `EVI-VESNA-003/parity80-*` and `parity70-cycle80-*`.
  [Issue #144](https://github.com/vladon/vesnalauncher/issues/144).

## OBS-VESNA-080 — Desktop and drawer icon sizes

- Android 15 research AVD, Nova 9.0.1 Prime (90100)
  and signed Vesna v0.4.59 (64). Compared an ordinary Gmail
  Home pin and Calendar in the unfiltered vertical app list.
- Nova started with **Match desktop size** on and both sizes
  at 125%. Changing only **Home screen → Icon size** to 100%
  changed both surfaces: the sampled Gmail white mask went
  from 144 to 114 px wide and Calendar's colored artwork from
  88 to 70 px in 1080×2400 captures.
- Disabling **Match desktop size** revealed the separately
  saved 125% drawer setting. Calendar returned to 88 px,
  while Gmail stayed at 114 px. Relinking and restoring 125%
  returned both measurements and Home/dock geometry to baseline.
- The Vesna **Home screen** and **App drawer** settings had
  no equivalent size control. Searches for `icon size` and
  `scale` returned no results. Dock sizing and adaptive icon
  shape are separate. No layout changes were made to Vesna.
- Evidence: `EVI-VESNA-003/parity81-*`, `parity70-cycle81-*`,
  and the matched Vesna list from `parity80-vesna-vertical-list-calendar`.
  [Issue #146](https://github.com/vladon/vesnalauncher/issues/146).

## OBS-VESNA-081 — Signed v0.4.60 batch addition to a Home folder

- [PR #145](https://github.com/vladon/vesnalauncher/pull/145)
  merged at `f7aeecfff8ee2557a0047d7c2188a5a184cc3ba3`.
  The [v0.4.60](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.60)
  tag points to that commit. Independently verified the public
  signed APK (65), not a local `main` build. SHA-256:
  `b558e40da154530d5cb4e11365a95d4bfc5332a3b6225ac8f3b5e01b4f8ec557`;
  v2 signer matched v0.4.59.
- Android 15 research AVD. Prepared the Gmail/Photos folder
  from OBS-VESNA-061 on signed v0.4.59, where **Add apps** was
  absent. Upgrading in place preserved its exact cell, both
  members, and unrelated Home and dock pins. In Nova 9.0.1
  Prime (90100), on-screen **Navigate up** in **Select apps**
  also discarded the Contacts/Drive draft, in addition to the
  Android Back result in OBS-VESNA-061.
- On signed v0.4.60, selecting Contacts and Drive enabled
  **Add**; explicit **Cancel** and Android **Back** each
  kept just the two original members and cleared the draft.
  One **Add** kept Gmail and Photos in order, appended both
  choices and preserved the cell and surrounding pins. The
  same four members survived a cold launcher restart.
- A normal drawer drag appended Files without moving its
  existing Home pin. With workspace lock on, the closed-folder
  hold reported “Workspace layout is locked” and exposed no
  **Add apps** action.
- Evidence: `EVI-VESNA-003/parity82-*`, `parity66-cycle82-*`
  and `release-0.4.60`.
  [Independent result in #134](https://github.com/vladon/vesnalauncher/issues/134#issuecomment-7093214586).

## OBS-VESNA-082 — Drawer memory with auto-close enabled

- Android 15 research AVD, gesture navigation, each launcher
  system Home for its trial. Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.59 (64) / v0.4.60 (65). Single-column
  app list, no query or keyboard. Both auto-close and
  **Remember position** were on.
- Scrolled off the top, opened Clock from its ordinary row,
  pressed Android Home while Clock was foreground, and opened
  the list again. With Nova smart folders and frequently used
  rows off, Nova kept the exact offset and visible rows. With
  those extra sections on, Nova reopened at an intermediate
  position, not the top and not the exact prior rows.
- Both signed Vesna versions returned to their original top
  rows. On signed v0.4.60, disabling only auto-close restored
  the exact offset after the same Clock → Android Home flow.
  This does not change the auto-close-off result in OBS-VESNA-077.
- Restored both original workspaces, docks and drawer settings,
  the Vesna workspace lock, and Nova as system Home. Verified
  Vesna v0.4.60 remained installed.
- Evidence: `EVI-VESNA-003/parity82-combo-*` and
  `parity78-combo-*`. [Issue #147](https://github.com/vladon/vesnalauncher/issues/147).

## OBS-VESNA-083 — Accent ordering in an English Home folder

- Android 15 research AVD, `en-US`, Nova 9.0.1 Prime (90100)
  and signed Vesna v0.4.60 (65; APK identity in OBS-VESNA-081).
  Used three labels: Gmail → `eclair`, Photos → `éclair`
  (precomposed U+00E9), and unchanged `Files`.
- Tested two matching inputs in each launcher: `Files`,
  `éclair`, `eclair`; then `Files`, `eclair`, `éclair`.
  Nova **Sort** and Vesna **Sort alphabetically** returned
  `eclair`, `éclair`, `Files` from both inputs. In this case
  the accent pair did not behave as an exact tie. The folder
  cell and unrelated Home pins were unchanged.
- Restored both workspaces, docks and original labels. Other
  locales, combining-mark normalization and RTL were not
  covered by this trial. Evidence: `EVI-VESNA-003/parity83-*`.
  [Follow-up in #135](https://github.com/vladon/vesnalauncher/issues/135#issuecomment-7093413844).

## OBS-VESNA-084 — Folder rank and sort under RTL

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.60 (65). In `en-US`, both had a
  two-member Home folder with custom labels `Zulu` on
  the left and `Alpha` on the right. Switched Android
  to Arabic (Egypt), verified `ar-EG` and RTL, and
  reopened the same folders.
- Both now showed `Zulu` to the right of `Alpha`, retaining
  the original rank in RTL reading order. Dragging `Alpha`
  onto `Zulu` put `Alpha` first (right); the opposing drag
  restored the original rank.
- From that unsorted order, Nova's localized sort and
  Vesna **Sort alphabetically** put `Alpha` first (right)
  without moving the folder cell. Both retained the sorted
  order after a cold launcher restart in `ar-EG`.
- Restored Android to `en-US` with no Arabic entry, and
  both original workspaces and docks. RTL overflow,
  cross-page dragging and TalkBack were not covered.
  Evidence: `EVI-VESNA-003/parity84-*`.
  [Follow-up in #131](https://github.com/vladon/vesnalauncher/issues/131#issuecomment-7093561635).

## OBS-VESNA-085 — Signed v0.4.61 reaches an existing Home page during drag

- [PR #148](https://github.com/vladon/vesnalauncher/pull/148)
  and the [v0.4.61 tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.61)
  resolve to `005106deb1a9332ed9f471b5e2d549102c3ed6f0`.
  Tested the public signed APK (66), not a local `main` build:
  SHA-256 `76f8be4206665ad9ac072dcb723810780b92fb78d81d884a8af528a99a06ec1f`,
  with the same v2 signer as v0.4.60.
- Android 15 research AVD, Nova 9.0.1 Prime (90100). An
  ordinary Contacts pin in Nova previewed the pre-existing
  next page with Drive while the drag was held. Cancel left
  the original pin in its source cell even though the
  destination stayed visible. Releasing over a free cell
  moved Contacts. The earlier folder comparator is OBS-VESNA-067.
- On signed v0.4.60, the ordinary Contacts edge-hold stayed
  on page one. Upgrading the same fixture in place preserved
  both pages and the Files/Gmail/Photos folder. On v0.4.61 the
  visible Drive marker confirmed page-two preview before
  release for both Contacts and Files. They landed in the
  third and fourth cells of page two's bottom row respectively.
  Drive and the original folder cell did not move; Gmail and
  Photos retained order. Both exact pages survived a cold start.
- Android CANCEL and dropping on the occupied Drive cell each
  preserved both pages for both sources. The failed folder
  drops returned to the open folder with unchanged members.
  Workspace lock blocked folder-member page preview and movement.
  An unlocked same-page drop still placed Gmail exactly and
  dissolved the two-member folder to Photos in its former cell.
- Restored both original one-page layouts, docks and unlocked
  fixture. Kept v0.4.61 installed. RTL and nonexistent-page
  edge behavior were not independently tested on this release.
  Evidence: `EVI-VESNA-003/parity85-*` and `release-0.4.61`.
  [Independent result in #139](https://github.com/vladon/vesnalauncher/issues/139#issuecomment-7093817077).

## OBS-VESNA-086 — Widget catalog search and empty state

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.60 (65) / v0.4.61 (66), unlocked Home.
- In Nova, long-press empty Home and expand **Widgets**.
  `Clock` filtered to the Clock provider; expanding it
  showed named previews and size labels (e.g. **Analog**,
  **3 × 2**). `zzOneNoWidgetQA` showed “No widgets or
  shortcuts found.” Clearing restored the unfiltered catalog.
- Neither tested Vesna release exposed a catalog via **Home
  options** or Settings search for `widget`. On v0.4.60 the
  empty-space menu and Clock dock menu offered no widget entry.
  This is a gap in browsing and discovery, not a placement
  or permission test.
- Browsing and dismissal left both layouts and docks unchanged.
  No widget was selected for binding. Evidence:
  `EVI-VESNA-003/parity85-widget-*`.
  [Issue #149](https://github.com/vladon/vesnalauncher/issues/149).

## OBS-VESNA-087 — RTL edge drag with and without a neighbor

- Android 15 research AVD, Android system language Arabic
  (Egypt), confirmed `ar-EG` and RTL. Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.61 (66) used two unlocked
  Home pages and a four-column, five-row grid.
- From logical page two, holding Drive at the **physical
  right** edge showed the Contacts marker on logical page
  one while still held. Cancel preserved the source pin in
  both. A drop placed Drive in the chosen bottom-row cell
  on page one; in Vesna this placement persisted after a
  cold start.
- For the outer-edge comparison, logical page two contained
  Drive and Contacts in both. Holding Drive at the **physical
  left** edge, returning to its original cell coordinates
  and releasing created a third page in Nova. Contacts
  stayed on page two and Drive on page three after restart.
  Vesna kept exactly two pages and both original page-two
  cells, including after restart.
- Existing-neighbor RTL behavior is
  [verified in #139](https://github.com/vladon/vesnalauncher/issues/139#issuecomment-7094088125).
  On-demand creation is separate ([#151](https://github.com/vladon/vesnalauncher/issues/151));
  page auto-removal and folder-member drag in RTL were not
  independently tested here.
- Restored `en-US`, both original one-page layouts and docks.
  Evidence: `EVI-VESNA-003/parity86-*`.

## OBS-VESNA-088 — Home label visibility with a readable drawer list

- Android 15 research AVD, Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.61 (66), both in single-column app-list
  mode and initially showing names.
- Disabling Nova's **Home screen → Label** removed the
  visual Gmail and Photos labels on Home while retaining
  visible names such as Calendar in the unfiltered list.
- Disabling Vesna's **Appearance → Show app names** removed
  visible Home, dock and unfiltered list labels. Icon tiles
  and their accessible app names remained; the single-column
  list was a column of icons without visible names. The
  tested settings did not allow labels on the list while
  hiding them on Home.
- Restored both label switches, checked the original Home
  and dock items, and completed this run before installing
  v0.4.62. Evidence: `EVI-VESNA-003/parity87-*labels-*`.
  [Issue #152](https://github.com/vladon/vesnalauncher/issues/152).

## OBS-VESNA-089 — Signed v0.4.62 remembers the list with auto-close enabled

- [PR #150](https://github.com/vladon/vesnalauncher/pull/150)
  and the [v0.4.62 tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.62)
  point to `e7681cdbdb751a99f5b53170d204d14cdf813ac0`.
  Independently checked the signed release (67), not a
  local build. APK SHA-256:
  `84335769d268932385b4fb42e01299ea50d4b9b94551620fec10a6b38c01e2ed`;
  same v2 signer as v0.4.61.
- Android 15 research AVD, gesture navigation, Vesna as
  Android Home. With both switches on, signed v0.4.61 (66)
  still went back to the top in the unfiltered vertical list.
  Completed that run first; upgrading in place kept the
  switches, layout and dock.
- On v0.4.62, scrolling, tapping Clock's normal row, pressing
  Android Home and explicitly reopening restored all 12
  visible rows at their original bounds (Clock:
  `[0,489][954,657]`). This matches the plain Nova 9.0.1 Prime
  list in OBS-VESNA-082.
- Back from Clock left the drawer closed; its next opening was
  at the top. Launch from the filtered `Clock` query also
  reopened at the top with no query. Turning Remember position
  off returned to the top; enabling only Remember position
  retained exact bounds as before.
- Force-stopping after Clock → Home with a pending offset
  discarded it; both switches persisted in the new process.
  Restored the original viewport settings, labels, Home role
  and both layouts; v0.4.62 remained installed.
- Evidence: `EVI-VESNA-003/parity87-vesna-061-*`,
  `parity87-vesna-062-*`, `parity70-cycle87-*` and `release-0.4.62`.
  [Independent result in #147](https://github.com/vladon/vesnalauncher/issues/147#issuecomment-7094304067).

## OBS-VESNA-090 — Folder-member edge drag and dissolution in RTL

- Android 15 research AVD, system `ar-EG` in RTL, Nova 9.0.1
  Prime (90100) and signed Vesna v0.4.62 (67). Both had the
  same Files → Gmail → Photos rank order in a Home folder on
  logical page two, and a Contacts marker on logical page one.
- Holding Files at the physical **right** edge showed Contacts
  on page one before release in both. CANCEL kept both pages
  and the original three ranks. An occupied drop on Contacts
  in Vesna also kept both layouts and reopened the unchanged
  folder; the Nova occupied-cell case was not compared here.
- Dropping on the chosen empty bottom-row cell put Files on
  page one while Gmail and Photos stayed in the source folder
  at its original cell. The assignments and surviving order
  persisted in both after cold start.
- Then dragging Gmail from the two-member folder across the
  same edge left Photos as a regular pin in the original
  folder cell in both. Restored `en-US` and both original
  one-page layouts and docks.
- Evidence: `EVI-VESNA-003/parity88-*`.
  [Follow-up in #139](https://github.com/vladon/vesnalauncher/issues/139#issuecomment-7094554724).

## OBS-VESNA-091 — Last shortcut removal and empty-page cleanup

- Android 15 research AVD, `en-US`, Nova 9.0.1 Prime (90100)
  and signed Vesna v0.4.62 (67). Both had two unlocked pages
  of four columns by five rows. Page one was nonempty; page
  two held only a regular Drive pin in its bottom-left cell.
- In Nova, **Remove** left the blank second page reachable
  during the session even after the Undo bar disappeared.
  After a cold launcher restart, the outward swipe stayed on
  page one. In a separate timed trial, **Undo** restored Drive
  to the exact second-page cell and it survived restart.
- Vesna **Remove from workspace** left the blank “Home page
  2 of 2” reachable after restart. **Home options → Remove
  empty page** removed it manually, without changing the
  other remaining pins or the dock.
- This checks cleanup after explicit last-pin removal, not
  a never-populated page or emptying it through a drag.
  Restored both original layouts, grid settings and docks.
  Evidence: `EVI-VESNA-003/parity89-*`.
  [Issue #153](https://github.com/vladon/vesnalauncher/issues/153).

## OBS-VESNA-092 — Compact folder overflow in RTL

- Android 15 research AVD, system `ar-EG` in RTL, Nova 9.0.1
  Prime (90100) and signed Vesna v0.4.62 (67). Both had the
  same 12 folder members in the same saved order. The viewport
  was 520×890 px at 420 dpi (about 198×339 dp) with 200% text.
- With Nova's **Horizontal** direction, a physical rightward
  swipe advanced through three groups of four. Leftward
  swiping returned to the previous group. A vertical swipe
  did not advance; another rightward swipe on the last group
  stayed there. All members were reachable.
- In Vesna, the equivalent rightward swipe did not advance.
  Vertical scrolling reached all 12 in the saved RTL order.
  The final YouTube/YT Music row was fully inside the viewport;
  reverse scrolling worked. **Rename folder** and **Remove
  folder** stayed at the same bounds. This is a paging-option
  difference, not a loss of access to members.
- Restored `en-US`, 1080×2400 px, 420 dpi, 100% text, both
  layouts and docks. Verified Nova's original 4×4 Desktop
  grid in its picker after rollback.
  Evidence: `EVI-VESNA-003/parity90-*`.
  [Follow-up in #133](https://github.com/vladon/vesnalauncher/issues/133#issuecomment-7094935713).

## OBS-VESNA-093 — Signed Home label control

- Android 15 research AVD, `en-US`, 1080×2400 px at 420 dpi,
  100% text. Finished the signed v0.4.62 (67) control before
  upgrading in place to published v0.4.63 (68).
  [Release/tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.63):
  `ccf3d926978de877b3799aa83c87b58edbfcfecb`; APK SHA-256
  `08593de60aff3ca944eeed4f9987a18efd7e422277bf4431fb05efddcc13aa32`,
  with the same verified v2 signer as v0.4.62.
- The old global **Show app names** switch hid the Home,
  dock, unfiltered vertical list and folder labels on v0.4.62.
  The in-place upgrade preserved visible names and the layout
  with the new **Hide app names on Home** switch off.
- Turning on the new switch hid Files on Home and Calendar
  in the dock. Calendar still had visible text in the
  vertical list, as did Gmail and Photos inside the folder.
  Checked identical accessible names and clickable/focusable
  bounds, unchanged Home and dock positions, pixel-identical
  Files and Calendar artwork, and successful launches of
  Files from Home and Clock from the dock.
- The switch survived process termination and reopening.
  Global off still hid all four types of labels. Global on
  restored drawer and folder names without overriding the
  scoped choice; scoped off restored Home and dock names.
  This matches Nova's Home/list independence in OBS-VESNA-088.
- Removed the temporary folder and restored the original
  layouts and settings by snapshot rollback, then installed
  the verified v0.4.63 APK over the original state. Checked
  the saved layouts, both label settings and the Nova Home
  role. Evidence: `EVI-VESNA-003/parity91-62-*`,
  `parity91-63-*`, `release-0.4.63/`.
  [Independent verification in #152](https://github.com/vladon/vesnalauncher/issues/152#issuecomment-7095227180).

## OBS-VESNA-094 — Palette source independent of theme mode

- Same Android 15 AVD in system light mode. Nova 9.0.1 Prime
  (90100) offered **System**, **Local Wallpaper**, and
  **Custom** in Settings → Style → Palette. Opening the
  custom picker and canceling left **System** selected.
- Confirming the upper-left magenta swatch changed solid
  Settings title pixels from `#152e60` to `#581932` and
  nearby background pixels from `#fefbff` to `#fffbfa`,
  without changing **Mode → Follow system**. Switching
  back to **System** returned the sampled region exactly.
- In signed Vesna v0.4.62 (67) and v0.4.63 (68), Appearance
  showed **System / Light / Dark** and the global names
  switch, but no separate palette control. Searching
  settings for `palette` returned **No settings found**.
- Verified the original Nova palette after rollback.
  Wallpaper-derived colors, per-surface overrides, and
  icon packs were not exercised. Evidence:
  `EVI-VESNA-003/parity91-*color*`, `parity91-*palette*`.
  [Issue #155](https://github.com/vladon/vesnalauncher/issues/155).

## OBS-VESNA-095 — Installed icon pack on Home, dock and drawer

- Android 15 research AVD, `en-US`, Nova 9.0.1 Prime (90100)
  and signed Vesna v0.4.63 (68). Captured both without a
  pack, then installed official Arcticons Black 15.4.7 (1580),
  `com.donnnno.arcticons.light`, without opening the pack app.
  [Fixture](https://github.com/Arcticons-Team/Arcticons/releases/tag/15.4.7)
  APK SHA-256 `3cf9c829848a531a5d69ab5449e16b6c4be6caa7cc4e11b1744cc12a79c8b512`.
- Nova **Look & feel → Icon style → Icon theme** listed just
  **System** before installation, then also **Arcticons Black**.
  Selecting the pack replaced Gmail on Home, Chrome in the dock
  and Calendar in the unfiltered alphabetical list with outline
  artwork; Home and dock placements did not move. The pack was
  still selected after process restart and the two Home/dock
  icon centers matched their themed crops exactly. Selecting
  **System** restored those centers exactly while the pack
  was still installed.
- Vesna listed Arcticons Black as an ordinary app. The tested
  Home, dock, and drawer icons kept their system artwork.
  Appearance had no pack control and Settings search for
  `icon pack` said **No settings found**. Missing-match
  fallback and other icon pack formats were not tested.
- Rolled back the temporary install, verified the original
  Nova theme, grid, both Home/dock layouts and absence of
  the pack. Evidence: `EVI-VESNA-003/parity92-*`,
  `arcticons-15.4.7/`.
  [Issue #157](https://github.com/vladon/vesnalauncher/issues/157).

## OBS-VESNA-096 — Signed last-pin page cleanup

- Android 15 research AVD, `en-US`, 1080×2400 px at 420 dpi
  and 100% text. Prepared and verified four ordered pages
  under signed Vesna v0.4.63 (68), then upgraded in place to
  the published signed v0.4.64 (69).
  [Release/tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.64):
  `a3a59464b65dc57eb2a686200caf7c7d10e97a21`; APK SHA-256
  `4aa1732f05b0f143432477685d20664da506934ba29a2065eaf16d88865717e1`;
  same verified v2 signer as v0.4.63.
- Setup: original Home, Drive + Files, a manually added
  never-populated blank page, then Camera. Removing Files
  left page **2 of 4** and Drive in the same bounds.
  Removing Drive immediately pruned the second page and
  returned to **1 of 3**, with identical first-page pins
  and dock bounds. No Undo was offered. Nova's delayed
  cleanup and offered Undo are recorded in OBS-VESNA-091.
- After process termination and relaunch, the manual
  blank page was still reachable as page 2. Camera
  retained its exact cell on page 3. Removing Camera
  also pruned that now-empty last page immediately.
  The never-populated page remained and could separately
  be removed via **Remove empty page**. The test did not
  empty the primary page.
- Restored the original single-page layout, both docks and
  Nova's 4×4 grid; retained the verified v0.4.64 upgrade.
  Evidence: `EVI-VESNA-003/parity93-*first*`,
  `parity93-63-page-*`, `parity93-64-*`, `release-0.4.64/`.
  [Independent verification in #153](https://github.com/vladon/vesnalauncher/issues/153#issuecomment-7095711817).

## OBS-VESNA-097 — Drawer-only background color

- Android 15 research AVD, `en-US`, system light, Nova
  9.0.1 Prime (90100) and signed Vesna v0.4.64 (69).
  Both used their unfiltered vertical app list for this
  comparison.
- Nova's in-app update screen reported **Latest: 90100
  (9.0.1)**, matching the installed version. The legacy
  direct-download links still served 8.0.18 (stable) and
  8.1.1 (beta), so those links were not used to decide whether
  to update. Evidence: `parity94-nova-version-*`.
- Nova Settings → **Style → App drawer → Colors → Surface**
  allowed choosing a pale pink swatch while the global
  **Palette** remained **System** and **Mode** remained
  **Follow system**. Three points in the real drawer
  changed from `#fefbff` to `#fdd7fa`; Home and dock
  sample points and item positions were unchanged.
- The selected swatch and drawer color survived process
  restart. Choosing the original swatch exactly restored
  the sampled pixels.
- In Vesna, the App drawer settings exposed no background
  choice and Settings search for `drawer background`
  returned **No settings found**. Restored Vesna's original
  vertical grid and kept both Home and dock layouts.
- Text/accent colors, other surfaces, and opacity were
  not tested. Evidence: `EVI-VESNA-003/parity94-*`.
  [Issue #158](https://github.com/vladon/vesnalauncher/issues/158).

## OBS-VESNA-098 — Last pin on the only Home page

- Android 15 research AVD, `en-US`, Nova 9.0.1 Prime
  (90100) and published signed Vesna v0.4.64 (69).
  Each had just one 4×5 Home page, Gmail alone in the
  top-right cell, and its original dock.
  Nova's grid was still its default 4×4; see OBS-VESNA-107.
- Nova offered **Undo** for removal and put Gmail back in
  exactly the same cell. Removing it again without Undo
  left the empty primary page after process restart.
- Vesna kept the empty primary page immediately and after
  process restart. Its menu offered **Add home page** but
  not **Remove empty page**. No Undo was offered.
- In both, all dock placements stayed fixed, an outward
  swipe after restart did not expose another workspace page,
  and opening and closing the drawer returned to that
  empty Home. This covers the primary-page guard not
  exercised in OBS-VESNA-096.
- Restored both original sets of pins, both docks, and
  Nova's original 4×4 grid. Evidence: `EVI-VESNA-003/parity95-*`.
  [Follow-up in #153](https://github.com/vladon/vesnalauncher/issues/153#issuecomment-7096093071).

## OBS-VESNA-099 — Signed Home page creation at the outer edge

- Android 15 research AVD, 1080×2400 px at 420 dpi, 100%
  text. Nova 9.0.1 Prime (90100) and signed Vesna used
  matched 4×5 two-page fixtures: Gmail at bottom-left
  and Photos at top-right of page two in `en-US`.
  Nova's grid stayed at its default 4×4; see OBS-VESNA-107.
- Nova previewed an empty successor at the physical right
  edge. Cancel kept page two and both pins; a valid drop
  put Gmail in the chosen cell on page three. Photos and
  the dock stayed put, including after process restart.
  Vesna v0.4.64 (69) retained the occupied page while held.
- In-place upgrade to signed Vesna v0.4.65 (70):
  [release/tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.65)
  `82e7981d8b71ba2438d39ca49d7cc330a4d35a7a`, APK SHA-256
  `ea39f6c771209c4b1805f72dc9941d73e7ca74f50bc2fed01300e23d23386f57`,
  with the same verified v2 signer as v0.4.64.
- On v0.4.65 in LTR a held Gmail showed the blank
  successor. Cancel and release over the dock kept both
  pages, pins and dock unchanged. Releasing in the chosen
  cell created page three; all three layouts remained
  exact after process restart. The previous existing
  page was still previewable and accepted an exact drop.
- In Android system `ar-EG`, physical right previewed the
  previous page; physical left previewed a blank successor.
  Cancel was exact; a valid release put Gmail in the mirrored
  bottom-right cell of page three. Photos and the dock
  stayed fixed after process restart. This matches Nova's
  RTL direction from OBS-VESNA-087. Page cap, layout lock
  and action/folder sources were not exercised.
- Restored `en-US`, the original Home/dock layouts and
  Nova's 4×4 grid; kept the signed v0.4.65 update.
  Evidence: `EVI-VESNA-003/parity96-*`, `release-0.4.65/`.
  [Independent verification in #151](https://github.com/vladon/vesnalauncher/issues/151#issuecomment-7096324896).

## OBS-VESNA-100 — Folder member at the last workspace edge

- Android 15 research AVD, `en-US`, Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.65 (70). Each had one
  4×5 Home page, a top-right folder ordered Gmail → Photos,
  and a free bottom-right cell.
  Nova's grid stayed at its default 4×4; see OBS-VESNA-107.
- Held Gmail, moved out of the folder and dwelt at the
  physical right edge. Cancel preserved the exact source
  folder cell and member order, sole page, and dock in
  both launchers.
- Nova showed its add-page preview while Gmail was held.
  Releasing in the bottom-right cell put Gmail on a new
  page two. Photos became a regular pin in the original
  folder cell on page one. Both placements survived a
  process restart.
- Vesna stayed on the source page while held, then
  extracted Gmail into the same bottom-right cell on
  **page one**. Photos occupied the original folder cell.
  There was still only one page after process restart;
  the dock and other pins were unchanged. This is distinct
  from a regular pin (#151) or an existing neighbor (#139).
- Restored both original layouts and Nova's 4×4 grid.
  Evidence: `EVI-VESNA-003/parity97-*`.
  [Issue #160](https://github.com/vladon/vesnalauncher/issues/160).

## OBS-VESNA-101 — Outer edge at the eight-page cap

- Android 15 research AVD, `en-US`, Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.65 (70). Each used
  a 4×5 tail page with Clock at top-left and Gmail
  at bottom-right.
  Nova's grid stayed at its default 4×4; see OBS-VESNA-107.
- In Vesna, the right edge at **7 of 7** previewed the
  eighth page; release put Gmail in its chosen bottom-right
  cell and left Clock in place on page seven. At **8 of 8**
  the same outward gesture did not show a successor. Clock,
  Gmail and the dock stayed in place on the same-cell drop.
  **Add home page** was absent at the cap. Cold restart
  preserved the page count and both final-page cells.
- Nova accepted the same drop from its eighth page onto a
  ninth. Eight distinct previous-page swipes led back to
  the first page, with a stationary Clock marker on each
  intermediate page. This proves at least nine pages, not
  Nova's maximum. Vesna's existing cap is preserved as intended.
- Restored both original single-page layouts, docks, and
  the Nova 4×4 grid. Evidence: `EVI-VESNA-003/parity98-*`.
  [Cap verification in #151](https://github.com/vladon/vesnalauncher/issues/151#issuecomment-7096855113).

## OBS-VESNA-102 — Signed whole-folder unpin and Undo

- Android 15 research AVD, `en-US`, 1080×2400 px at 420 dpi.
  Prepared a 2×2 page with `Release QA` (Files → Gmail →
  Photos) and three other pins; a separate Gmail pin was on
  another page. Signed v0.4.65 (70) only offered **Remove
  folder** and refused because there was no room to ungroup.
- Upgraded in place to published signed v0.4.66 (71).
  [Release/tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.66)
  `bbb2c7e2b8eaad49e0072e7d557c55981cb3b6e0`, APK SHA-256
  `4377b2acff17717f2e89245870dec8e18982b3658ec176edecbd75a77c3b4518`;
  same verified v2 signer as v0.4.65.
- **Ungroup folder** and **Remove folder and shortcuts**
  were separate. Ungroup retained its full-page capacity
  guard, and with space kept both pins in a separate
  control. Lock hid both editing actions; unlocking restored them.
- The new action unpinned the packed folder and offered
  **Undo**. Undo restored the exact folder title, cell and
  member order; after process restart all five pages and the
  dock had their exact original placements for this fixture.
- Removing without Undo survived another restart. Only
  the target folder was absent; the separate Gmail, other
  pins and dock stayed put. Files, Gmail and Photos remained
  installed and visible in the drawer. This matches Nova's
  packed-page unpin and Undo in OBS-VESNA-076.
- Restored original layouts, grids and unlocked state;
  kept the signed v0.4.66 update.
  Evidence: `EVI-VESNA-003/parity99-*`, `release-0.4.66/`.
  [Independent verification in #132](https://github.com/vladon/vesnalauncher/issues/132#issuecomment-7097087961).

## OBS-VESNA-103 — Folder Undo after the original cell is occupied

- Android 15 research AVD, `en-US`, Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.66 (71). Both 4×5 Home
  grids had Gmail → Photos in a top-right folder and
  Drive in the second column. Android **Time to take
  action** was temporarily set to 30 seconds for both.
  Nova's grid stayed at its default 4×4; see OBS-VESNA-107.
- In Vesna, removed the folder and moved Drive into the
  original cell while Undo was still visible. Tapping
  Undo showed **Folder not restored. Its original spot is
  no longer available.** Drive stayed in the new cell;
  the folder did not reappear elsewhere, and unrelated
  pins and the dock were unchanged. Restart kept this result.
- In Nova, the initial no-edit frame still showed Undo
  3.33 seconds after Remove. In a separate measured run,
  moving Drive took about 2.0 seconds and the 3.01-second
  post-move frame no longer showed Undo. Drive occupied
  the former folder cell and survived restart. An Undo
  tap with that cell occupied was not observed in Nova.
- Restored the original timeout, grids, layouts and docks.
  Evidence: `EVI-VESNA-003/parity100-*`.
  [Follow-up in #132](https://github.com/vladon/vesnalauncher/issues/132#issuecomment-7097341299).

## OBS-VESNA-104 — Whole folder drag to an existing page

- Android 15 research AVD, system Arabic (Egypt) in actual
  RTL, 4×5 workspace. In Nova 9.0.1 Prime (90100) and both
  signed Vesna v0.4.66 (71) and v0.4.67 (72), page one had
  a closed Gmail → Photos folder; page two had Files at
  physical bottom-left and a free cell beside it.
  Nova's grid stayed at its default 4×4; see OBS-VESNA-107.
- Holding the whole folder at the physical left edge in Nova
  previewed the existing page with Files. Drop put the folder
  beside the marker; its two members stayed in order.
- In Vesna, both releases kept page one visible while held.
  Drop moved the folder to the chosen screen cell on **page
  one**, retaining both members. Files still occupied its
  exact cell on page two and dock locations were unchanged.
  This is not the individual member route fixed in #160.
- LTR and whole-folder creation of a new page were not
  compared in this pass. Evidence: `EVI-VESNA-003/parity101-*whole-folder*`.
  [Issue #163](https://github.com/vladon/vesnalauncher/issues/163)
  and [signed-release confirmation](https://github.com/vladon/vesnalauncher/issues/163#issuecomment-7097779694).

## OBS-VESNA-105 — Signed folder-member edge append

- Android 15 research AVD, 1080×2400 at 420 dpi, 100% text,
  4×5 Home. Nova 9.0.1 Prime (90100) and signed Vesna
  v0.4.66 (71) started the RTL run with a single-page
  Files → Gmail → Photos folder. Nova used the physical left
  edge to put Files on page two. v0.4.66 stayed on page one
  and extracted it into the same screen cell there.
  Nova's grid was its default 4×4; see OBS-VESNA-107.
- Upgraded in place to signed v0.4.67 (72).
  [Release/tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.67)
  `4c3ee82a5b08ef2df1c5b1e2a0325c12e1bce67b`; APK SHA-256
  `a52594cc9cf21474f00fbf32735ff08e1b43f8667227d995bb901c8d37d7b9ea`,
  with the same verified v2 signer as v0.4.66.
- In actual `ar-EG`, the held left edge now showed a blank
  successor. Cancel and a release over the dock reopened
  the original ordered folder and kept one page. Valid drop
  placed Files in the selected bottom-left cell of page two.
  Gmail → Photos remained in the original folder cell.
- Both page layouts, member order and dock survived cold
  restart. Dropping Gmail onto Files on the now-existing
  neighbor reopened the unchanged folder and moved nothing.
- In a separate `en-US` two-member Gmail → Photos fixture,
  the right edge previewed page two; cancel kept the exact
  order and one page. Valid drop placed Gmail bottom-right
  on page two, leaving Photos in the former folder cell.
  Both placements and the dock survived restart, matching
  Nova's LTR result from OBS-VESNA-100.
- Restored original locale, grid, layouts and docks; kept
  the signed v0.4.67 update. Keyboard, compact screens and
  the page cap were not exercised in this pass.
  Evidence: `EVI-VESNA-003/parity101-*`, `release-0.4.67/`.
  [Independent verification in #160](https://github.com/vladon/vesnalauncher/issues/160#issuecomment-7097791450).

## OBS-VESNA-106 — Signed whole-folder drag to an existing page

- Android 15 research AVD, `en-US`, 1080×2400 px at 420 dpi,
  100% text. Both launchers used a real 4×5 grid, confirmed
  on Nova's workspace (353 px rows). Page one held a closed
  folder named **Work** (Gmail → Photos) at top-right; page
  two held Files at bottom-left with a free cell beside it.
- v0.4.67 (71) kept page one visible while the folder was
  held at the right edge, and the drop landed on page one.
  Nova 9.0.1 Prime (90100) showed page two while held and
  placed the folder beside Files with name and order intact.
  That placement and the dock survived a launcher restart.
- In-place upgrade to published signed v0.4.68 (73).
  [Release/tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.68)
  `6bb25b6e86cf91382e554e7d36aa571f7323310b`, APK SHA-256
  `1e418386bb72aa5f7cfa56ade7616f08529d316e92ecc6047c379e9563be0fa6`,
  with the same verified v2 signer as v0.4.67.
- On v0.4.68 the held folder showed page two. Cancel and a
  release onto Files each kept every page and the dock
  exactly. A valid drop put **Work** beside Files with
  Gmail → Photos unchanged, and page one lost the folder.
  All of this survived process restart. An outward drag from
  the last page added nothing, and a left-edge drag back
  returned the folder to its original cell.
- After switching the system to `ar-EG`, cancel was again
  exact and the physical left edge moved **Work** beside the
  mirrored Files cell on page two, with the same order after
  restart.
- Nova in the same fixture: a cancelled cross-page drag left
  Nova on the previewed neighbor while the folder kept its
  source cell, and dropping the folder onto an occupied cell
  pushed the occupant (Nova Settings) to the next free cell
  instead of refusing. A short drag along the top row to
  x=1058 did not switch pages in Nova; moving lower first did.
- Reverted the grid and restored every layout through the
  public UI, checked against the pre-test captures. The
  original snapshot could not be loaded under the current
  emulator renderer. Evidence: `EVI-VESNA-003/parity106-*`,
  `release-0.4.68/`.
  [Independent verification in #163](https://github.com/vladon/vesnalauncher/issues/163#issuecomment-7101663076).

## OBS-VESNA-107 — Correction: Nova grid in earlier runs

- Several earlier entries describe Nova 9.0.1 Prime (90100)
  as using a 4×5 grid. The Desktop grid dialog did show 4×5
  after the step, but the saved captures prove the workspace
  kept its default four rows: item boxes were 441 px tall, as
  in the 4×4 layout. A real five-row grid gives 353 px rows,
  as in OBS-VESNA-087 and 090 and again in OBS-VESNA-106.
- Affected: OBS-VESNA-098, 099, 100, 101, 103, 104 and 105.
  OBS-VESNA-087, 090 and 091 used a real 4×5 Nova grid.
  Vesna was 4×5 throughout. The compared cells were the same
  screen cells (top-right folder or pin, bottom-left or
  bottom-right targets, the adjacent cell), so the recorded
  behaviors are not withdrawn; "matched 4×5" overstated the
  setup for Nova. Why the dialog change did not reach the
  workspace was not determined.
- Correction comments were left in #153, #151, #132, #160
  and #163. Later runs confirm the grid from the workspace
  geometry after OK and after a launcher restart.
- Evidence: box heights in `EVI-VESNA-003/parity95-*`,
  `parity96-*`, `parity97-*`, `parity98-*`, `parity100-*`
  and `parity101-*`, versus `parity86-*`, `parity88-*`,
  `parity89-*` and `parity106-*`.

## OBS-VESNA-108 — Icon pack fallback and removal in Nova

- Android 15 research AVD, `en-US`, Nova 9.0.1 Prime (90100)
  with official Arcticons Black 15.4.7 (1580) installed and
  selected as the Icon theme. Signed Vesna v0.4.68 (73) was
  present and still offers no pack selector (OBS-VESNA-095).
- Apps covered by the pack showed its artwork in the drawer,
  on Home and in the dock. **Vesna Launcher**, which the pack
  does not cover, kept its own full-colour icon in all three
  places: its centre pixel stayed green on Home and white in
  the dock while neighbouring icons were dark pack artwork.
  The drawer's app-action chips (Gmail's Compose and the Nova
  Settings entry) also kept their original artwork.
- After uninstalling the pack while it was selected, Nova
  kept the drawn pack icons on Home and in the dock until its
  process restarted. After the restart every surface showed
  the apps' normal artwork. The **Icon theme** row still named
  the removed pack; the chooser offered only **System**, and
  choosing it cleared the name.
- These are reference facts for #157's decision on unmatched
  icons and removed packs, not a Vesna comparison. Restored
  through the loadable baseline snapshot and re-checked both
  layouts, both docks and the System theme.
  Evidence: `EVI-VESNA-003/parity107-*`.
  [Follow-up in #157](https://github.com/vladon/vesnalauncher/issues/157#issuecomment-7102057099).

## OBS-VESNA-109 — Whole folder dragged past the last page

- Android 15 research AVD, `en-US`, real 4×5 grids (Nova's
  row height checked on the workspace). Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.68 (73), each with a single
  Home page holding one closed Gmail → Photos folder at
  top-right.
- Holding the whole folder at the physical right edge in Nova
  showed a blank second page with the add-page **+**.
  Releasing at the top-right coordinates placed the folder on
  the new page two; the other pin stayed on page one. Both
  pages survived a launcher restart.
- In Vesna, page one stayed visible while the folder was held.
  Releasing kept the folder in its original cell on the single
  page, with every other pin and the dock unchanged. PR #164
  deliberately excluded page creation for whole folders.
- Cancel and the page cap were not exercised for this case.
  Restored through the loadable baseline snapshot and re-checked
  both layouts, both docks and the 4×4 Nova grid.
  Evidence: `EVI-VESNA-003/parity109-*`.
  [Issue #167](https://github.com/vladon/vesnalauncher/issues/167).

## OBS-VESNA-110 — Drops onto occupied cells

- Android 15 research AVD, `en-US`, real 4×5 grids (Nova's
  row height checked on the workspace). Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.68 (73).
- **App onto an app, same page:** both created a two-app
  folder at the target cell (Vesna: Drive onto Files; Nova:
  Photos onto Gmail).
- **App onto an app, another page:** Drive was dragged from
  page one to the right edge and released on Files on page
  two. Nova created a folder with Drive and Files at the
  Files cell, and page one lost Drive. Vesna previewed page
  two while held but created nothing: Drive stayed in its
  original cell and both pages were unchanged.
- **Closed folder onto an app, same page:** Nova placed the
  folder in the occupied cell and moved the app to the free
  cell the folder had left. Vesna refused; folder, app and
  dock stayed exactly as before. The cross-page variant
  behaved the same way in OBS-VESNA-106.
- The two refusals are what #139 and #163 specified, so they
  are scope questions rather than regressions. Restart
  persistence of Nova's cross-page folder was not exercised.
  Restored through the loadable baseline snapshot and
  re-checked both layouts, both docks and the 4×4 Nova grid.
  Evidence: `EVI-VESNA-003/parity110-*`.
  [Issue #168](https://github.com/vladon/vesnalauncher/issues/168)
  and [#169](https://github.com/vladon/vesnalauncher/issues/169).

## OBS-VESNA-111 — Shortcut dropped onto a folder on another page

- Same Android 15 research AVD, real 4×5 grids, Nova 9.0.1
  Prime (90100) and signed Vesna v0.4.68 (73). Page two held
  a closed folder with Drive and Files at top-left; page one
  held Photos.
- Photos was dragged from page one to the right edge and
  released on the folder.
- Nova added Photos as the third member and opened the folder
  with Files, Drive and Photos in that order; page one lost
  Photos and the dock was unchanged.
- Vesna showed page two while Photos was held, then refused:
  it returned to page one, Photos stayed in its cell, and the
  folder kept its two members.
- On the same page Vesna does add a dropped app to a folder
  (OBS-VESNA-097). Restored through the loadable baseline
  snapshot and re-checked both layouts, both docks and the
  4×4 Nova grid. Evidence: `EVI-VESNA-003/parity111-*`.
  [New fact in #168](https://github.com/vladon/vesnalauncher/issues/168#issuecomment-7106555120).

## OBS-VESNA-112 — Slow upward swipe on signed v0.4.68

- Android 15 research AVD, `en-US`, gesture navigation,
  1080×2400 px at 420 dpi. Nova 9.0.1 Prime (90100) and
  signed Vesna v0.4.68 (73). Both started with the drawer
  closed from the same two empty bottom-row cells; each
  swipe was one continuous gesture of 1100 px or 400 px,
  repeated from both cells.
- **1100 px in 500, 665, 800 or 925 ms:** Vesna opened the
  workspace menu in 11 of 11 trials and never the drawer.
  Nova opened the drawer in 11 of 11.
- **400 px in 800 ms:** Vesna opened the menu in 3 of 3;
  Nova opened neither surface in 3 of 3.
- **Controls:** the long swipe in 300 ms opened the drawer
  for Vesna (2 of 2) and for Nova (7 of 8). At 390 ms Nova
  opened it in 3 of 4, and each miss opened nothing. The
  short swipe in 150 ms opened the drawer for Vesna (2 of 2)
  and nothing for Nova (3 of 3). A stationary hold opened the
  workspace menu in Vesna and the Widgets sheet in Nova.
- This is NT-GESTURE-03 again, last recorded on v0.4.16
  (OBS-VESNA-014). The slow swipes pass touch slop in their
  first frames, so movement should cancel the pending long
  press. The timings describe the repro, not product
  thresholds. No setting was changed.
  Evidence: `EVI-VESNA-003/parity112-*`.
  [Issue #170](https://github.com/vladon/vesnalauncher/issues/170).

## OBS-VESNA-113 — Uninstalled app with three pins

- Android 15 research AVD, `en-US`. Nova 9.0.1 Prime (90100)
  and signed Vesna v0.4.68 (73). Official Arcticons Black
  15.4.7 (1580) was installed and pinned three times in each
  launcher: on Home, in the dock and inside a two-app folder.
- Right after uninstalling, both launchers had removed all
  three pins. The folder kept its other two members in their
  cells, every other pin and dock slot was unchanged, and the
  app no longer appeared in the drawer. A launcher restart
  changed nothing.
- Side facts from building the fixture. With its four dock
  slots full, Vesna's **Add to dock** added nothing and showed
  the toast **The dock is full**; after one pin was removed it
  added the app in the free slot. Nova, with five dock pins,
  put a sixth app dropped from the drawer on a second dock
  page, and a swipe across the dock returned to the first page.
- In Nova a drop from the drawer onto a folder displaced the
  folder instead of adding the app; dragging an existing Home
  pin onto the folder added it.
- Restored through the loadable baseline snapshot and
  re-checked both layouts, both docks and the 4×4 Nova grid.
  Evidence: `EVI-VESNA-003/parity114-*`.
  [Dock facts in #115](https://github.com/vladon/vesnalauncher/issues/115#issuecomment-7107839244)
  and [unavailable-shortcut input in #116](https://github.com/vladon/vesnalauncher/issues/116#issuecomment-7107839768).

## OBS-VESNA-114 — Signed whole-folder page append

- Android 15 research AVD, real 4×5 grids, 1080×2400 px at
  420 dpi, 100% text. In-place upgrade from signed v0.4.68 to
  published signed v0.4.69 (74).
  [Release/tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.69)
  `2b2bcc0deb5580423cdafa9a4c247e4b0c6fe213`, APK SHA-256
  `e27b180b371a1603b2b51a046736dcd7b41c6a372ad340432ea7fad5dc5c6d1e`,
  with the same verified v2 signer as v0.4.68.
- **en-US:** a closed folder named **Work** (Gmail → Photos) was
  the test item on a single Home page. Held at the right edge,
  it showed a blank second page. Cancel and a release over the
  dock left one page and the exact layout. A valid release
  created page two with **Work** in the same cell; page one
  lost only the folder, and the name and member order were
  intact.
- Dragging the folder back through the left edge returned it
  to its original cell on page one and left page two empty, so
  the existing-neighbor route still works.
- **System `ar-EG`, RTL:** cancel at the physical left edge was
  exact. A valid release created page three with **Work** in
  the same physical cell and the same order.
- Both results were unchanged after a process restart. Nova
  9.0.1 Prime (90100) created a page in both directions as
  well, with the folder at the same coordinates (LTR in
  OBS-VESNA-109).
- The maintainer had closed #167 before this check. The
  eight-page cap and layout lock were not exercised.
  Reverted the locale and grid through the baseline snapshot,
  reinstalled v0.4.69 over the original state, and saved that
  state as the new loadable baseline.
  Evidence: `EVI-VESNA-003/parity115-*`, `release-0.4.69/`.
  [Independent verification in #167](https://github.com/vladon/vesnalauncher/issues/167#issuecomment-7108341615).

## OBS-VESNA-115 — Regressions on v0.4.69 and reinstall

- Android 15 research AVD, `en-US`, published signed Vesna
  v0.4.69 (74) and Nova 9.0.1 Prime (90100). Each open defect
  was replayed once on the new build.
- **Slow upward swipe (#170):** still reproduces. On the
  1100 px path the workspace menu opened in 3 of 3 trials at
  665 ms and in 1 of 1 at 925 ms. Added for symmetry with
  Nova, 390 ms opened the drawer in 3 of 3, as did the 300 ms
  control (1 of 1). On this path the menu appears from
  somewhere between 390 and 665 ms.
- **Shortcut onto an app on another page (#168):** still
  refused. Drive stayed in its original cell and both pages
  were unchanged.
- **Reinstall after uninstall:** with the app pinned on Home
  and in the dock, uninstalling removed both pins in each
  launcher, as in OBS-VESNA-113. Reinstalling the same APK
  did not bring either pin back, neither at once nor after a
  launcher restart. An unavailable shortcut therefore only
  comes from a saved layout.
- Restored through the loadable baseline snapshot and
  re-checked both layouts, both docks and the 4×4 Nova grid.
  Evidence: `EVI-VESNA-003/parity116-*`.
  [#170](https://github.com/vladon/vesnalauncher/issues/170#issuecomment-7108816589),
  [#168](https://github.com/vladon/vesnalauncher/issues/168#issuecomment-7108817240)
  and [#116](https://github.com/vladon/vesnalauncher/issues/116#issuecomment-7108817872).

## OBS-VESNA-116 — Home label typography in Nova

- Android 15 research AVD, `en-US`, dark wallpaper, 1080×2400
  px at 420 dpi. Nova 9.0.1 Prime (90100) and signed Vesna
  v0.4.69 (73). The measured label was Nova Settings; its ink
  was 184 px wide before any change.
- **Nova, Home screen → Label:** **Size** is a six-step slider
  (158 px at the smallest step). **Font** offers Normal,
  Medium, Condensed (the default) and Light; Normal measured
  176 px at the smallest size. At the largest size with
  Normal the label was 216 px and was cut to "Nova Setti…",
  because **Single line** was on. **Shadow** and
  **Single line** are separate checkboxes. Icons did not
  change.
- **Nova, Style → Home screen → Text:** five tone swatches, a
  custom color option and a **Minimum contrast** slider
  (40Δ by default). The fifth tone turned the label ink from
  white to gray (255,255,255 → 145,145,145).
- **Vesna:** Settings search for `font`, `shadow`, `text size`
  and `text color` returned **No settings found**. Searching
  for `label` or `name` finds only the show/hide switches.
- Every Nova change was restored. The check returned to the
  first tone, Minimum contrast stayed 40Δ, and the Home
  screen was pixel-identical to the pre-test capture.
  Evidence: `EVI-VESNA-003/parity117-*`.
  [Issue #173](https://github.com/vladon/vesnalauncher/issues/173)
  and [text tones in #155](https://github.com/vladon/vesnalauncher/issues/155#issuecomment-7109519114).

## OBS-VESNA-117 — Background controls and transparency

- Android 15 research AVD, `en-US`. Nova 9.0.1 Prime (90100)
  and signed Vesna v0.4.70 (75). No setting was changed in
  Nova; only its settings pages were read.
- **Nova app drawer surface:** Style → App drawer → Surface
  shows a grid of system-palette tones and a grid of opaque
  **Fixed colors**, with separate Surface, Text and Accent
  tabs. That page has no transparency control.
- **Nova dock background:** Home screen → Dock → Dock
  background has an on/off switch, **Draw behind navigation
  bar**, **Padding**, and a corner-radius slider with a shape
  dropdown for each corner. The page ends there.
- **Vesna:** Settings search for `transparent`, `opacity`,
  `dock background`, `corner`, `radius`, `animation` and
  `transition` returned **No settings found**.
- Transparency is therefore not a confirmed difference:
  neither launcher offered a control in the explored pages.
  The dock background shape is a Nova-only control, left as a
  reference fact in #115. Nova's transition and animation
  rows were only seen, not exercised.
- Both Home layouts were unchanged afterwards.
  Evidence: `EVI-VESNA-003/parity118-*`.
  [Dock background fact in #115](https://github.com/vladon/vesnalauncher/issues/115#issuecomment-7110009865).

## OBS-VESNA-118 — Signed v0.4.70 slow upward swipe

- Android 15 research AVD, `en-US`, gesture navigation,
  1080×2400 px at 420 dpi. In-place upgrade from signed
  v0.4.69 to published signed v0.4.70 (75).
  [Release/tag](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.70)
  `013cef2924a3a47ec0c9c13d0f4be180a9ef5883`, APK SHA-256
  `dedcc3a0a7a651991a6b01327c8ba1d7bd3f64e9065bb5b24d2d7a6803a539c5`,
  with the same verified v2 signer as v0.4.69.
- Same trajectories and starting cells as OBS-VESNA-112; each
  was repeated from both empty bottom-row cells.
- **1100 px in 500, 665, 800 or 925 ms:** the drawer opened in
  16 of 16 trials and the workspace menu never appeared. On
  v0.4.68 and v0.4.69 the menu had opened in every slow trial.
- **1100 px in 300 or 390 ms:** the drawer opened in 8 of 8
  trials.
- **400 px in 800 ms:** neither surface opened, in 4 of 4
  trials. **400 px in 150 ms:** the drawer opened in 2 of 2.
- **Stationary hold on an empty cell:** the workspace menu
  opened in 2 of 2 trials.
- **Tile long-press:** a plain hold on a Home shortcut still
  opened its own menu. A hold with the finger drifting 6–18 px
  also opened the shortcut menu and did not move the tile.
- Every criterion of AT-GESTURE-112.5 passed for the tested
  paths. Two-finger gestures and other screen sizes were not
  exercised. The maintainer's issue was still open at the
  time of this check. No layout changed; v0.4.70 was kept and
  saved as the new loadable baseline.
  Evidence: `EVI-VESNA-003/parity119-*`, `release-0.4.70/`.
  [Independent verification in #170](https://github.com/vladon/vesnalauncher/issues/170#issuecomment-7110009227).

## OBS-VESNA-119 — Page looping and page indicator

- Android 15 research AVD, `en-US`, two Home pages in each
  launcher with a single marker shortcut on the second page.
  Nova 9.0.1 Prime (90100) and signed Vesna v0.4.70 (75).
- **Outward swipes by default:** in both launchers a swipe
  away from the other page stayed on the same page, from the
  first page and from the last.
- **Nova, Home screen → Infinite scroll:** after switching it
  on, the outward swipe from the first page went to the last,
  and the outward swipe from the last page returned to the
  first. After switching it off again, the swipe stayed put.
- **Nova, Home screen → Page indicator:** the options are
  None, CIRCLE, DOT and LINE, with LINE selected by default.
  Right after a page swipe, LINE showed a thin line above the
  dock and None showed nothing.
- **Vesna:** the counter **Home page 1 of 2** or **Home page
  2 of 2**, with Previous and Next buttons, is always shown.
  Settings search for `infinite`, `indicator` and `loop`
  returned **No settings found**.
- Nova's transition and animation rows were only seen, not
  exercised. Restored through the loadable baseline snapshot:
  one page per launcher, Infinite scroll off, Page indicator
  LINE, both original layouts.
  Evidence: `EVI-VESNA-003/parity120-*`.
  [Issue #174](https://github.com/vladon/vesnalauncher/issues/174).

## OBS-VESNA-120 — Layout lock, page cap and emptied pages

- Android 15 research AVD, `en-US`, signed Vesna
  v0.4.70 (75) on a 4×5 grid and Nova 9.0.1 Prime
  (90100) on its default 4×4 grid. Each launcher
  had one closed folder that held Gmail → Photos.
  This closes the two acceptance points of #167
  that the earlier pass left open.
- **Vesna, layout lock on** (Settings › Home screen ›
  Lock workspace layout): the long-press on the
  closed folder showed **Workspace layout is
  locked**. The folder did not lift, the right edge
  previewed no page, and a release in the same cell
  or over an empty cell left the single page and
  every pin unchanged. With the lock off, the same
  hold showed **Home page 2 of 2**.
- **Vesna, page creation and cap:** seven
  consecutive outer-edge drops each showed
  **Home page n+1 of n+1** while held and moved the
  folder to the same cell of the new page. At
  **Home page 8 of 8** the outer edge previewed
  nothing, **Add home page** was absent from Home
  options, and the left edge still showed **Home
  page 7 of 8**. A cold restart kept the eight
  pages. A left-edge drag then moved the folder to
  the same cell on page 7.
- **Vesna, emptied pages:** pages 2 to 7 stayed in
  place as empty pages, also after the restart, and
  so did page 8 after the drag to page 7. Home
  options offered **Remove empty page** on them.
- **Nova, layout lock on** (Settings → Home screen
  → Advanced → Lock layout): the long-press opened a
  **Home screen is locked** sheet with a padlock and
  **Long press the padlock to allow changes to the
  home screen**, plus an **Automatically relock?**
  switch that was on. The folder did not move. The
  padlock was not pressed. A drag with a downward
  component while the sheet was up left the system
  notification shade open; the cause was not
  followed up.
- **Nova, page creation:** unlocked, eight
  consecutive outer-edge drops each showed the
  blank page and created a new page with the folder
  in the same cell. Nova has no eight-page cap on
  this path, as with shortcuts in 101.
- **Nova, emptied pages:** each page the folder
  left disappeared at once. After the eight drops
  two pages were left, and after a drag back to the
  first page the second page was gone.
- The folder kept its members in both launchers.
  Restored through the loadable baseline snapshot
  and checked both lock switches against their
  original frames.
- Evidence: `EVI-VESNA-003/parity121-*`.
  [#167 verification](https://github.com/vladon/vesnalauncher/issues/167#issuecomment-7111561732),
  [decision on emptied pages #176](https://github.com/vladon/vesnalauncher/issues/176),
  [unlock from the locked message #177](https://github.com/vladon/vesnalauncher/issues/177).

## OBS-VESNA-121 — Signed v0.4.71 keyboard on drawer open

- Android 15 research AVD, `en-US`. Published
  signed Vesna v0.4.71 (76), tag at PR #175 merge
  `a9708473331d374712df1f14bb27cb03a279ecc5`, APK
  SHA-256
  `84247477f27a6f5c9c3ecd6ecb1c920bede532e8e45d59da7176e1f2c70ea774`,
  with the same verified v2 signer as v0.4.70. It
  was installed in place over the restored original
  state, and both original layouts survived the
  upgrade. Nova 9.0.1 Prime (90100) is unchanged.
- **New setting:** Settings search for `keyboard`
  found **Vesna settings › App drawer › Show keyboard
  on open** ("Focus search and open the keyboard
  each time the app drawer opens.") under a
  **Search match** band. It was off by default.
- **Off:** an upward swipe on the workspace and the
  **Open app drawer** button both opened the drawer
  with the keyboard hidden and the search field
  unfocused.
- **On:** both entry points opened the drawer with
  the search field focused and the keyboard shown.
  Typing `se` left Settings and Voice Search. Back
  hid the keyboard first and closed the drawer
  second, with and without a typed query. The
  setting and the behaviour survived a force-stop.
- **Nova:** Settings → App drawer → **Show
  Keyboard** ("Immediately show keyboard when the
  drawer is opened") was on by default here. With
  it on, the swipe opened focused search with the
  keyboard, and Back acted the same way with an
  empty query. With it off, the swipe opened the
  drawer with the keyboard hidden and search
  unfocused.
- The two launchers differ only in the default:
  off in Vesna, on in Nova on this AVD.
- Not tested: an open drawer across recreation,
  Russian strings, Dark and compact RTL, and
  Nova's frequent-app predictions, which stay
  deferred by decision.
- Restored: the Vesna switch is pixel-identical to
  its default frame and Nova's to its pre-test
  frame. Saved `baseline_research_0.4.71`, proved
  that it loads, and deleted the 0.4.70 baseline.
  Evidence: `EVI-VESNA-003/parity122-*`,
  `release-0.4.71/`.
  [Independent verification in #112](https://github.com/vladon/vesnalauncher/issues/112#issuecomment-7111954487).

## OBS-VESNA-122 — Transition effect, wallpaper scrolling and a v0.4.71 regression check

- Android 15 research AVD, `en-US`, signed Vesna
  v0.4.71 (76) and Nova 9.0.1 Prime (90100)
  on its default 4×4 grid. Two Home pages in
  each launcher; the second one was blank in
  Vesna, which shows **Open Home options to
  customize this page** on it. Screen recordings
  were taken with `screenrecord` during the
  swipe.
- **Regression of #170 on v0.4.71,** whose only
  change touched the code that opens the drawer:
  1100 px swipes in 925 ms and in 665 ms, from
  both empty bottom-row cells, opened the drawer
  in 4 of 4 trials. 400 px in 800 ms opened
  neither surface, and a stationary hold opened
  the workspace menu.
- **Nova, page turn:** with the default effect
  the pages slide for about half a second. In a
  held 700 px drag the neighbouring page had
  already slid in by about 610 px. Settings →
  Home screen → **Transition effect** offers
  None, Simple (the default), Cube, Card stack,
  Tablet, Revolving door, Flip, Flip vertical and
  In and out. In recordings Cube turned the
  pages in perspective, and None gave a short
  slide with a still wallpaper. Returned to
  Simple and checked the radio button.
- **Vesna, page turn:** the page content does
  not follow the finger. After the same 700 px
  drag, still held, the page was unshifted and
  only the page indicator had moved. On release
  the next page appeared in a single frame; two
  recordings show no frame in between. Settings
  search for `transition`, `animation` and
  `effect` returned **No settings found**.
- **Nova, wallpaper:** with **Wallpaper
  scrolling** on, the default, the wallpaper on
  page 2 is shifted by **593 px** against page 1;
  two bands without icons match exactly at that
  shift. The options are On, Off and Reverse;
  with Off both pages show it at the same
  position. Returned to On; the row is
  pixel-identical to its pre-test frame.
- **Vesna, wallpaper:** the same position on
  both pages. The side margins, the band above
  the page controls and the dock area are
  pixel-identical. Settings search for
  `wallpaper` and `scroll` returned **No settings
  found**.
- Evidence: `EVI-VESNA-003/parity123-*`.
  [Transition effect #180](https://github.com/vladon/vesnalauncher/issues/180),
  [wallpaper scrolling #181](https://github.com/vladon/vesnalauncher/issues/181),
  [regression note in #170](https://github.com/vladon/vesnalauncher/issues/170#issuecomment-7112897485).

## OBS-VESNA-123 — Outward swipe on the first page opens Nova's drawer

- Same AVD, versions and settings as 122.
  Swipes started on empty cells or on a
  shortcut and ran for 600 to 700 px in 460 ms
  or 150 ms.
- **Nova:** on the first page a rightward swipe
  opens the app drawer, which slides in from the
  left while the page stays in place: 5 of 5
  trials with two pages and 2 of 2 with one
  page, including the trajectory used in 119. On
  the last page a leftward swipe did nothing, in
  2 of 2 trials. A leftward swipe inside the open
  drawer closed it.
- **Vesna:** on the first page a rightward swipe
  did nothing in 5 of 5 trials, with two pages
  and with one page, from empty cells and from
  the Settings shortcut. On the last page a
  leftward swipe did nothing, in 2 of 2 trials.
- **Errata for 119:** that pass read only the
  workspace nodes, which the dump also lists
  behind an open drawer. Its saved dump lists
  the drawer's nodes, so its first-page result
  for Nova is incomplete: the page did not
  wrap, but the drawer had opened. The looping
  and indicator results stand, and the fact was
  added to #174.
- Nova's gesture bindings on this AVD, read in
  Settings → Gestures & inputs: Home button →
  Nova Search, Swipe up → App drawer, Swipe down
  → Expand notifications, Two finger swipe up →
  Nova Settings, Two finger swipe down → Nova
  Search, Pinch in → Desktop previews, Pinch out
  → Recent apps; the rest is None, including
  Swipe left and Swipe right.
- The HOME intent returns Nova to its first
  page, so a helper that expects to stay on
  another page has to move there again.
- Restored both layouts through the baseline
  snapshot. Evidence: `EVI-VESNA-003/parity123-*`.
  [Issue #179](https://github.com/vladon/vesnalauncher/issues/179),
  [comment in #174](https://github.com/vladon/vesnalauncher/issues/174#issuecomment-7112896874).

## OBS-VESNA-124 — Signed v0.4.72 cross-page folder drops

- Android 15 research AVD, `en-US`. Published
  signed Vesna v0.4.72 (77), tag at PR #178
  merge `23caa4ee9d351730b718b203434ecae3675edcd3`,
  APK SHA-256
  `d872d81164f5a7792b3f9416721fa97c65f0d6c4547d07019364072ba28a3866`,
  with the same verified v2 signer as v0.4.71.
  Installed in place over the restored original
  state; both original layouts survived the
  upgrade. Nova 9.0.1 Prime (90100) was not
  re-run: its results are in 110/111 and the
  version is unchanged.
- **Empty-cell control:** Files dragged from
  page 1 through the right edge landed in the
  chosen top-left cell of page 2.
- **App onto an app:** Drive from page 1 was
  held at the right edge until **Home page 2 of
  2** appeared, then released on **Files**. A
  folder was created in the Files cell with
  Files, Drive in that order, and page 1 lost
  Drive.
- **App onto a folder:** Photos from page 1 was
  released on that closed folder and became the
  third member after Files and Drive.
- **Cancel and invalid release:** Android
  CANCEL and a release over the dock on the
  previewed page each returned to page 1 with
  Contacts in its cell and both pages unchanged.
- **Restart:** after a force-stop and relaunch
  the folder still held Files, Drive, Photos.
  The other pins and the dock were unchanged.
- **Regressions on the same build:** a closed
  folder dragged from page 2 onto an occupied
  cell of page 1 is still refused, and the
  folder stays on page 2 (#169 path). A closed
  folder dragged past the outer edge of the
  last page still created page 3 with the folder
  in the same cell (#167).
- **Remaining difference:** after the cross-page
  drop onto a folder Nova opens the folder;
  Vesna leaves it closed.
- Restored and installed again on the original
  state. Saved `baseline_research_0.4.72`,
  proved that it loads, and deleted the 0.4.71
  baseline. Evidence: `EVI-VESNA-003/parity124-*`,
  `release-0.4.72/`.
  [Independent verification in #168](https://github.com/vladon/vesnalauncher/issues/168#issuecomment-7113172553).

## OBS-VESNA-125 — Two-finger gestures and page management

- Android 15 research AVD, `en-US`, gesture
  navigation, default settings. Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.72 (77). Vesna
  v0.4.73 was published during this run; its
  single commit touches only the icon sizes.
- **Two-finger input:** the AVD image is a
  userdebug build, so `adb root` allowed writing
  to the first `virtio_input_multi_touch` device.
  Each SYN batch was written with one `printf`
  to the node, as 24-byte input_event records. A
  kernel event log showed gestures of 0.19 to
  0.45 s, and the pointer-location overlay showed
  two fingers at the intended coordinates.
  `adb root` was undone afterwards.
- **Nova, one finger:** a swipe down on an empty
  cell opened the notification shade. A double
  tap did nothing.
- **Nova, two fingers:** a swipe up opened Nova
  Settings. Fingers moving apart, bound to Recent
  apps, opened the **Enable Accessibility
  Service** dialog; it was cancelled. Fingers
  moving together, bound to Desktop previews,
  opened the same customize screen as a
  long-press, with the Widgets sheet. No page
  overview appeared in seven trials, horizontal
  and diagonal, lasting 0.22 to 0.34 s, with one
  page and with two. A two-finger tap did
  nothing. The long-press timeout on this AVD is
  400 ms, so the fastest trial ran under it.
- **Vesna:** a swipe down, a double tap, a
  two-finger swipe up and both pinch directions
  did nothing. Settings search for `gesture` and
  `swipe` returned **No settings found**.
- **Re-checked on signed v0.4.73 (78)** with
  three pages, a marker on the middle one: the
  pinch-in and a 600 px two-finger swipe stayed
  on the middle page, while the same 600 px
  swipe with one finger reached the next page.
  Two-finger input is ignored rather than
  treated as a one-finger swipe. **Go to
  page…**, the Home options entries and **Move
  to position 3** behaved as above on the new
  build.
- **Nova, edit mode:** the house button at the
  top of each page is outlined on ordinary pages
  and filled on the Home page. After a tap on
  page 2, Nova started on page 2 after a
  restart; a tap on page 1 restored the old
  start page. A swipe past the last page shows a
  **+** button. Tapping it added a blank page
  that was still there after a restart. No
  control to remove a blank page was found in
  edit mode.
- **Vesna pages:** **Add home page** creates the
  next page and selects it. **Go to page…**
  lists Page 1 to N, and choosing one shows it.
  **Reorder this page…** offers **Move to
  position** one step away. Page 1 has no
  reorder entry, and page 2 cannot move to
  position 1, so the first page is always the
  main one.
- **Helper pitfall:** on Nova's main page a
  second HOME intent opens Nova Search, as the
  Home button binding says. A helper that sends
  it after a restart has to press Back.
- Restored through the baseline snapshot.
  Evidence: `EVI-VESNA-003/parity125-*`.
  [Gestures #183](https://github.com/vladon/vesnalauncher/issues/183),
  [main page #184](https://github.com/vladon/vesnalauncher/issues/184).

## OBS-VESNA-126 — Signed v0.4.73 separate icon sizes

- Android 15 research AVD, `en-US`. Published
  signed Vesna v0.4.73 (78), tag at PR #182
  merge `00d7f63075b6594cfe846ae99695fd7355ed6e2a`,
  APK SHA-256
  `470a901cb59bf29348e3d211d175a6888fbc9aa2c1d1a5bf4ec1cd0e173d50ed`,
  with the same verified v2 signer as v0.4.72.
  Installed in place over the restored original
  state; both original layouts survived the
  upgrade. Nova 9.0.1 Prime (90100) was not
  re-run: its sizes are in 080 and the version is
  unchanged.
- **Settings:** Home screen → **Home icon size**
  ("App and folder artwork on Home. Cells, labels
  and shortcuts stay where they are.") is a
  six-step slider from 80% to 130%, 100% by
  default. App drawer → **Match Home icon size**
  ("The drawer uses the Home icon size. Turn off
  to choose its own size.") is on by default, and
  **Drawer icon size** ("App artwork in the
  drawer, kept for when it stops matching
  Home.") is disabled while matching. Settings
  search for `icon` found all three.
- Artwork was measured from screenshots as the
  bounding box inside a cell:

| State | Home artwork | Drawer grid |
|---|---|---|
| Default: Home 100%, Match on | 126 px | 126 px in 238 × 214 px tiles |
| Home 130%, Match on | 164 px | 164 px in 238 × 252 px tiles |
| Match off, drawer 80% | 164 px | 101 px in 238 × 214 px tiles |
| Home 110%, Match off | 139 px | 101 px |
| Match on again | 139 px | 139 px; the disabled slider still showed 80% |
| Match off again | 139 px | 101 px |
| After force-stop and relaunch | 139 px | 101 px |

- Home cells kept their 239 × 339 px bounds and
  the dock its 126 px artwork in every state; the
  whole dock band is pixel-identical between the
  100 %, 110 % and 130 % frames. The
  label sits lower under a larger artwork. The
  drawer tile height follows the artwork above
  100%; the release note does not mention this.
- Not exercised: the vertical list and
  horizontal pages layouts of the drawer, the
  other slider steps, and Dark or compact RTL.
- Restored Home 100%, drawer 100% and Match on;
  the Home frame equals the default frame. Saved
  `baseline_research_0.4.73`, proved that it
  loads, and deleted the 0.4.72 baseline.
  Evidence: `EVI-VESNA-003/parity126-*`,
  `release-0.4.73/`.
  [Independent verification in #146](https://github.com/vladon/vesnalauncher/issues/146#issuecomment-7114696573).

## OBS-VESNA-127 — Persistent search bar on Home

- Android 15 research AVD, `en-US`, gesture
  navigation, default settings. Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.73 (78) on a
  single Home page each.
- **Nova:** the workspace shows a persistent
  search bar below the dock: **Nova Search** at
  the left and the Assistant at the right. Tapping
  it opens a full-height search overlay with a
  search field, a predicted shortcuts row, an app
  grid and category rows. Typing `cal` filtered
  the apps to Calendar and offered Calendar
  actions and integration, a calculator
  microresult, and web suggestions with Google,
  Play Store, Wikipedia, Google Maps and YouTube.
  The first typed query was intercepted by the
  system stylus-handwriting dialog; the retype
  after tapping the field is the recorded result.
- **Nova, settings:** Settings → Search has
  **Desktop search bar**, **Drawer search bar**
  and **Nova Now search bar** toggles. The Desktop
  search bar page offers Placement (**Dock
  (Below icons)**, the default; also None, Widget,
  Dock (Above icons) and Persistent), Colors,
  Transparency, Shadow, Corner radius, Logo,
  Additional Actions, Weather and the Assistant
  icon. Further rows cover Contact card view, API
  integrations, Web suggestions (Google), Search
  More providers (Google, Play Store, Wikipedia,
  Google Maps, Spotify, YouTube), Nova AI in
  search and folder and shortcut search. Nothing
  was changed.
- **Vesna:** no persistent search bar on the
  workspace: the bottom controls are **Home
  options**, the page controls and **Open app
  drawer**. Search lives only inside the drawer.
  Settings search for `bar` returned **No settings
  found**; `search` offered only drawer rows.
- Restored through the baseline snapshot.
  Evidence: `EVI-VESNA-003/parity128-*`.
  [Issue #186](https://github.com/vladon/vesnalauncher/issues/186).

## OBS-VESNA-128 — Signed v0.4.74 icon shape setting

- Android 15 research AVD, `en-US`. Published
  signed Vesna v0.4.74 (79), tag at PR #185
  merge `10eecf41c423e27e56d9e639645be79af71ec143`,
  APK SHA-256
  `0e5fe328093214733a477c30126d7f3d3ca5265fe118591cb13811f0644f3c32`,
  with the same verified v2 signer as v0.4.73.
  Installed in place over the restored original
  state; both original layouts survived the
  upgrade. Nova 9.0.1 Prime (90100) was not
  re-run: its icon-shape handling is in 079 and
  the version is unchanged.
- **Settings:** Vesna settings → Appearance →
  **Icon shape** ("Mask for adaptive app icons on
  Home, in the dock, in the app drawer, and in
  folders. Other icons and work badges stay as
  they are."), with **System (default)** selected
  by default and **Rounded square** as the other
  choice. Settings search for `shape` finds it.
- Verified crops of Gmail on Home, Calendar in
  the dock and Calendar in the vertical drawer
  grid: System shows the circular mask everywhere;
  Rounded square changes all three to a rounded
  square with the artwork content and the 126 px
  bounding box unchanged. Home cells and the dock
  kept their bounds. Folders were not checked.
- After force-stop and relaunch the rounded square
  was still applied (the Home and drawer crops
  pixel-identical to the pre-restart rounded
  frames). Back on System the three crops equal
  the default frames again; the small crop
  difference behind the icon is the wallpaper.
- Nova's Look & feel offers many icon shapes;
  Vesna implements the two approved choices.
- Restored System (default). Saved
  `baseline_research_0.4.74`, proved that it
  loads, and deleted the 0.4.73 baseline.
  Evidence: `EVI-VESNA-003/parity128-*`,
  `release-0.4.74/`.
  [Independent verification in #144](https://github.com/vladon/vesnalauncher/issues/144#issuecomment-7115689678).

## OBS-VESNA-129 — Dock item management on v0.4.74

- Android 15 research AVD, `en-US`. Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.74 (79); the two
  interim releases changed only icon sizes and
  shapes. Docks: Vesna Calendar, Camera, Chrome,
  Clock; Nova Phone, Messages, Maps, Chrome,
  Camera. This fills the DOCK-004 gap of the
  inventory.
- **Reorder by drag:** dropping a Vesna dock pin
  on an adjacent dock pin swapped the two
  (Camera and Calendar), as in 041 on v0.4.52.
  Dropping a Nova dock pin on another dock pin
  made a two-item dock folder (041); reordering
  Nova dock pins without overlap was not
  exercised.
- **Remove:** Vesna shows no dock-pin menu while
  the finger is down; after release the menu
  offers **Remove from dock** and **App info**.
  Remove from dock deleted the Calendar shortcut
  from the dock and from the launcher (it did not
  move to the workspace) and the dock compacted to
  three pins. Nova's menu opens while the finger
  is down and offers Remove, App info and Edit;
  Remove removed Camera and the dock compacted to
  four pins.
- **Add:** Vesna's drawer item menu offers Add to
  workspace, **Add to dock** and App info; Add to
  dock appended Drive to the dock's free last
  slot (the dock had three pins after the
  removal). Nova's fifth slot, freed by the
  removal, took a Home shortcut dropped onto it.
- A dock long-press in Nova opens its menu while
  the finger is still down; Vesna's menu appears
  only after release.
- Restart persistence of these changes was not
  checked. The dock-add toast at full capacity is
  recorded in 113.
- Restored through the baseline snapshot.
  Evidence: `EVI-VESNA-003/parity129-*`.

## OBS-VESNA-130 — Dock icon size on v0.4.75

- Android 15 research AVD, `en-US`. Published
  signed Vesna v0.4.75 (80), tag at PR #187
  merge `cdfefd3f3e5d4717cbf7b3565870a8514aad0027`,
  APK SHA-256
  `4bee6637332506276a01cc99571d244687bcb7ec1a5cb6b6130b595a7522df97`,
  with the same verified v2 signer as v0.4.74.
  Installed in place over the restored original
  state; both original layouts survived the
  upgrade. Nova 9.0.1 Prime (90100) was not
  re-run: its dock icon size control is
  inventoried below and the version is unchanged.
- **Settings:** Home screen → **Dock icon size**
  ("Favorite app artwork in the dock. Pins keep
  their order; Home cells adjust so the dock
  stays below them."), a six-step slider from
  80% to 130% with 100% the default. Settings
  search for `icon` offered it together with the
  three icon-size settings of 126-era #146.
- **Default:** dock artwork 126 px in 265 × 214 px
  tiles.
- **130%:** dock artwork 164 px, the dock band
  grew and the workspace cells adjusted (a Home
  cell 332 px tall vs 339 px) so the dock stays
  below them. The Home artwork stayed 126 px and
  the pins kept their order.
- **80%:** dock artwork 101 px.
- **Restart:** after a force-stop and relaunch
  the 80% size was still applied; restoring 100%
  returned the 126 px artwork and the original
  339 px cell height.
- **Nova:** Settings → Home screen → Dock has a
  dock **Icon size** (125%) with **Match desktop
  size**, the same concept as Vesna's separate
  dock slider.
- Not exercised: the other slider steps, Dark and
  compact RTL.
- Restored 100%. Saved `baseline_research_0.4.75`,
  proved that it loads, and deleted the 0.4.74
  baseline. Evidence: `EVI-VESNA-003/parity129-*`,
  `release-0.4.75/`.
  [Stage 1 verification in #115](https://github.com/vladon/vesnalauncher/issues/115#issuecomment-7116536290).

## OBS-VESNA-131 — Home screen rotation

- Android 15 research AVD, `en-US`, gesture
  navigation, default settings. Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.75 (80). The
  device was forced to landscape (system
  auto-rotate off, user rotation set to 90°) and
  back.
- **Nova:** Settings → Look & feel → **Allow Home
  screen rotation** exists and is off by default.
  With it off, the forced landscape left the
  workspace portrait (1080 × 2400 kept). With it
  on, Nova rotated to landscape (2400 × 1080):
  the dock and search bar moved to the left edge,
  as in 040/047. Restored off.
- **Vesna:** the same forced landscape rotated the
  workspace to landscape (2400 × 1080): four to
  five pins per row, the page controls centred
  and the dock kept as a bottom row (as in 040).
  Settings search for `rotat` returned **No
  settings found**; no rotation control exists.
- **Difference:** Nova can lock the Home screen
  to portrait; Vesna follows the device
  orientation with no user control. Recorded in
  #189.
- Restored: Nova's toggle off, the system
  accelerometer rotation back to automatic and
  the user rotation to 0; both layouts equal the
  baselines. Evidence:
  `EVI-VESNA-003/parity129-*`.
  [Issue #189](https://github.com/vladon/vesnalauncher/issues/189).

## OBS-VESNA-132 — Show dock switch on v0.4.76

- Android 15 research AVD, `en-US`. Published
  signed Vesna v0.4.76 (81), tag at PR #188
  merge `5c45dca6bdda7530a32322aaf243f3fd6f80013f`,
  APK SHA-256
  `f7750a47739172de22854e9e0c40342b2eca5fcbad733250494cb488d2caf9b3`,
  with the same verified v2 signer as v0.4.75.
  Installed in place over the restored original
  state; both original layouts survived the
  upgrade. Nova 9.0.1 Prime (90100) was not
  re-run: its dock visibility gesture is in 037
  and the version is unchanged.
- **Setting found:** Settings search for `dock`
  offered **Vesna settings › Home screen › Show
  dock** ("Favorite apps at the bottom of Home. A
  hidden dock keeps its pins and returns with
  them."), on by default.
- **Hidden:** the dock disappeared, its space went
  back to Home (a workspace cell grew from 339 px
  to 566 px tall) and the bottom controls kept
  **Home options** and **Open app drawer**. The
  Home pin menu no longer offered **Add to dock**
  (Remove from workspace, Edit name and App info
  only).
- **Hidden across restart:** after a force-stop
  and relaunch the dock stayed hidden with the
  same expanded cells.
- **Re-shown:** the dock returned with the same
  four pins in the same order and the workspace
  cells restored to 339 px.
- Not exercised: drag behaviour toward the hidden
  dock area, Dark and compact RTL.
- Restored Show dock on; the workspace equals the
  default frame. Saved
  `baseline_research_0.4.76`, proved that it
  loads, and deleted the 0.4.75 baseline.
  Evidence: `EVI-VESNA-003/parity130-*`,
  `release-0.4.76/`.
  [Visibility verification in #115](https://github.com/vladon/vesnalauncher/issues/115#issuecomment-7117127934).

## OBS-VESNA-133 — Dock pin drag-swap not reproduced on v0.4.76

- Android 15 research AVD, `en-US`. Signed Vesna
  v0.4.76 (81) on a fresh baseline_research_0.4.76
  load. The dock-pin drag-swap recorded in 041
  (v0.4.52) and re-confirmed on v0.4.74 earlier in
  this session did not reproduce.
- Four attempts to drop the Calendar dock pin on
  the adjacent Camera dock pin (DROP helper, a
  stepwise drag with 0.15 s gaps and a 0.4 s pause
  over the target, the same with a 0.9 s pause,
  and the same with a 0.9 s pause and a slow
  lift) all left the dock order unchanged
  (Calendar, Camera, Chrome, Clock). The workspace
  drag (Contacts onto Drive) still created a
  folder on the same build, so the injection path
  works.
- The dock-pin drag to the workspace still works
  too: Calendar was moved from the dock to an
  empty workspace cell (the dock compacted and the
  workspace grew the pin).
- The dock-pin menu (Remove from dock, App info)
  still appears after a long-press-and-release.
- Not determined whether this is a v0.4.76
  behaviour change or a drop-geometry sensitivity:
  the successful v0.4.74 swap used the same
  technique, but no controlled v0.4.74-vs-v0.4.76
  comparison was run. Untested: dropping closer to
  or further from the pin centre, pin order
  dependence, and the Nova dock-pin routes on
  v0.4.76.
- Restored through the baseline snapshot. Dock
  back to Calendar, Camera, Chrome, Clock.
  Evidence: `EVI-VESNA-003/parity131-*`.

## OBS-VESNA-134 — Drawer search empty state on v0.4.76

- Android 15 research AVD, `en-US`, gesture
  navigation, default settings. Nova 9.0.1 Prime
  (90100) and signed Vesna v0.4.76 (81).
- **Query with no matches:** typing `zzzz` in the
  drawer search field. Vesna shows the query with
  **No matching apps** centred below the field and
  nothing else. Nova shows the query with provider
  icons (Google, Play Store, Wikipedia, Google
  Maps, YouTube), web suggestions from Google
  (`zzzz zzzz zzzz word` etc.) and a **Home**
  button, with no local-app results.
- This is consistent with 028: Nova separates
  provider buttons from web suggestions while
  Vesna shows only a no-results state. The
  external providers and suggestions gap is
  tracked in #113.
  Restored both launchers to the baseline.
  Evidence: `EVI-VESNA-003/parity132-*`.

## OBS-VESNA-135 — Search actions on v0.4.77

- Android 15 research AVD, `en-US`. Published
  signed Vesna v0.4.77 (82), tag at PR #190
  merge `62d437c6bdda7530a32322aaf243f3fd6f80013f`,
  APK SHA-256
  `57991653f4f38af28bf7ff7653a604b9e3e36292ade07af9fa68327a2f8ae007`,
  with the same verified v2 signer as v0.4.76.
  Installed in place over the restored original
  state; both original layouts survived the
  upgrade. Nova 9.0.1 Prime (90100) was not
  re-run: its web suggestions are in 028 and the
  version is unchanged.
- **Setting found:** Settings search for
  `search action` offered **Vesna settings › App
  drawer › Search actions** ("When a search
  matches no apps, offer to hand the query to
  other apps. Nothing is sent while typing and
  the local search is unchanged."), off by
  default.
- **Off:** a no-match query showed **No matching
  apps** with no external suggestions.
- **On:** the same query showed three action rows
  below the no-results text: **Search the web**,
  **Search in Maps** and **Search in YouTube**.
  Tapping **Search the web** opened the Google
  search app with the query. Typing and results
  were unchanged while on.
- **Toggle:** switching back to off restored the
  no-results state.
- Not exercised: the Maps and YouTube action
  rows, Dark and compact RTL.
- Restored Search actions off; the workspace
  equals the default frame. Saved
  `baseline_research_0.4.77`, proved that it
  loads, and deleted the 0.4.76 baseline.
  Evidence: `EVI-VESNA-003/parity133-*`,
  `release-0.4.77/`.
  [Stage 1 verification in #113](https://github.com/vladon/vesnalauncher/issues/113#issuecomment-7117861015).

## OBS-VESNA-136 — Dock pin drag inserts at the target on v0.4.77

- Android 15 research AVD, `en-US`. Signed Vesna
  v0.4.77 (82) on a fresh baseline load. This
  closes [OBS-VESNA-133](#obs-vesna-133--dock-pin-drag-swap-not-reproduced-on-v0476)
  for current builds: the "not reproduced" result
  there is explained by a hold-duration artifact
  — the `DROP` helper's 0.72 s hold does not lift
  a dock pin, while a hold of about 1.3 s with
  0.22 s move steps and a 0.9 s pause over the
  target reorders the dock on v0.4.77. OBS-133's
  build, v0.4.76, was not re-tested with the
  longer hold, so the explanation is unproven for
  that release.
- **Insert semantics.** A dock-pin drag released
  over another occupied dock tile removes the pin
  from its slot and inserts it at the target
  slot; the pins between shift one slot toward
  the source. Verified commits (slot positions):
  Camera 2→3 (Chrome shifted to 2), Calendar
  1→3 (Chrome→1, Camera→2), Calendar 3→2
  (Camera→3), Chrome 1→3, Clock 4→1, Clock 1→2,
  Clock 2→4. An adjacent drop exchanges two pins
  only, which is why the earlier sessions read it
  as "swap on drop"; a non-adjacent drop shows
  the shift.
- **Drop targeting.** A release beyond the last
  occupied tile resolves to the last occupied
  slot: with three pins, Camera released at
  x=937 committed at slot 3, while the same
  release with Chrome as the source (already the
  last pin) was a no-op. A release over the
  pin's own tile is a no-op. Live dumps taken
  during the hold read stale dock positions, so
  the release point decides, not the held dump.
- A free trailing slot is not a dock-internal
  target: with three pins, dragging the last pin
  to the free fourth slot left the order
  unchanged. From the workspace the free slot is
  honored: dragging Maps onto the free fourth
  slot landed it there (tile at x=805).
- **Full-dock rejection.** Dragging a workspace
  pin onto an occupied slot of the full 4-pin
  dock was rejected: the dock and the workspace
  stayed unchanged. No rejection message was
  caught in dumps taken about 0.7 s and 1.8 s
  after the release, so the message promised by
  the product README for a full dock was not
  observed (possibly a toast shorter than the
  dump cadence).
- **Persistence.** Remove from dock (menu)
  survived a force-stop and relaunch (Calendar
  stayed out; the dock compacted), and Add to
  dock from the drawer appended it to the free
  last slot. A dock reorder (Clock to slot 1)
  survived force-stop and relaunch too.
- Nova 9.0.1 comparison: Nova has no insert.
  Dropping a dock pin onto an occupied dock slot
  creates a folder (observed with Phone+Chrome
  and again with a 4-item folder), the same as
  drops from the workspace or an open folder
  popup. Nova does move a dock pin to a free
  slot (Chrome from slot 4 to the free slot 5
  after Camera was removed) and offers Remove in
  the dock-pin long-press menu. So on an
  occupied-slot drop Vesna reorders while Nova
  folders — Vesna's insert behaviour matches the
  product README's "drop onto a slot to insert
  it there".
- Both launchers were restored and verified
  positionally (the dock helper sorts by name, so
  an order check must compare bounds, not the
  name list). Evidence:
  `EVI-VESNA-003/parity135-*`.

## OBS-VESNA-137 — Curated palettes on v0.4.78

- Android 15 research AVD, `en-US`. Published
  signed Vesna v0.4.78 (83), main `e8804f43`
  (PR #155 merge), APK SHA-256
  `5156a8653f0adb753b8667514555f17d6ecef91379498201d4b4bf7dfe87be3a`,
  same verified v2 signer. Installed in place
  over the restored v0.4.77 original state; the
  Home, dock and drawer layouts survived the
  upgrade.
- **Setting:** Settings → Appearance shows a
  **Palette** section with three inline radio
  rows — **Vesna (default)**, **Ocean**,
  **Sunset** — between Icon shape and Show app
  names; Palette is a section header, not a
  sub-page. Vesna is selected out of the box.
- **Immediate switch.** Pixel probes on the
  Appearance page: the selected radio dot and
  the ON switch track follow the palette primary
  — Vesna green (73,103,46), Ocean blue
  (34,94,166), Sunset amber (144,72,2) — and the
  Settings surface follows the palette neutrals
  (249,250,239) / (246,250,255) / (253,248,246).
- **Surfaces outside Settings:** the drawer
  background and its search-field surface, and
  the Home floating Home options / drawer-entry
  buttons follow the palette; the dock's dark
  scrim over the wallpaper and the wallpaper
  itself are unaffected.
- **Independent of Theme:** with Sunset
  selected, Theme System → Dark kept Sunset
  selected and switched its primary to the
  dark-scheme amber (255,179,127) on a dark
  surface (22,19,17); System and Light restore
  the light-scheme values — one light and one
  dark scheme per palette, as the release note
  says.
- **Persistence:** Sunset survived force-stop
  and relaunch (radio still selected, colors
  unchanged); re-selecting Vesna restored every
  probed color.
- Nova 9.0.1 comparison: Look & feel offers
  **Palette: System** (System / Local Wallpaper
  / Custom — derived, not curated fixed pairs),
  **Mode: Follow system**, and per-surface color
  pages (Home screen, App drawer, Folders,
  Popup menu, Nova Now each with Surface / Text
  / Accent color). Vesna ships three curated
  whole-launcher palettes with per-palette
  Light/Dark schemes; per-surface overrides stay
  outside the scope [as filed in
  #155](https://github.com/vladon/vesnalauncher/issues/155).
  [Verification comment in
  #155](https://github.com/vladon/vesnalauncher/issues/155#issuecomment-7119194129).
- Restored palette Vesna and Theme System; the
  layout equals the baseline. Saved
  `baseline_research_0.4.78`, proved that it
  loads, and deleted the 0.4.77 baseline.
  Evidence: `EVI-VESNA-003/parity136-*`,
  `release-0.4.78/`.
