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
