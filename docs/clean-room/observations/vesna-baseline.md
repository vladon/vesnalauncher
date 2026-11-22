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
