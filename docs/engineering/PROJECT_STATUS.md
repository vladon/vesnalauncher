# Engineering Project Status

**Baseline commit:** `888f557` — `test: cover drawer search on Android (#9)` (QA PR #9,
squash merge). Post-merge CI run 44385854347 is green.
**App version:** 0.4.1 (`versionName`), `versionCode` 6, application ID
`dev.vladon.vesna.launcher`.
**Date of baseline:** 2026-11-18.

This document is the engineering handoff baseline: what exists in the code today,
what is verified, and what is not. Requirement-level traceability lives in
[REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md); the research input
is summarized in [RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md).

## Product shape

Single Gradle module `:app`, Kotlin. The launcher hot path (workspace, dock,
drawer) is Android Views; the About/settings surface is Jetpack Compose with
Material 3. `minSdk` 26, `compileSdk`/`targetSdk` 37. See
[../architecture/OVERVIEW.md](../architecture/OVERVIEW.md) for package layout,
key decisions, and data flow.

## Implemented capabilities (source-verified)

Every item below was verified by reading the implementation at the baseline
commit. The "Verification" column names exactly what the baseline audit
exercised; everything else is source-verified only.

| Capability | Implementation anchors | Verification |
|---|---|---|
| HOME role | `HomeActivity` declares `CATEGORY_HOME` (`singleTask`, `excludeFromRecents`); `HomeRoleChecker` (RoleManager on API 29+, `resolveActivity` fallback) and the system "choose home app" intent | Source only at the audit baseline; **device-verified** afterwards by the [VESNA-QA-002 run](verification/VESNA-QA-002.md) (2026-11-18, API 35 AVD) |
| Workspace | Fixed 4×5 grid (`GridCanvas`), placement via `WorkspaceResolver` + `WorkspaceRepository`, Room-backed; compact cells make room for enlarged labels without moving pins | `GridCanvasTest`, `WorkspaceResolverTest`, and [Home labels at large text sizes](#home-labels-at-large-text-sizes) |
| Workspace pages (unreleased) | **Home options** adds, reorders, and removes eligible pages. Swipe or use the arrows to step through up to eight 4×5 pages, or use the dots or **Home options → Go to page…** to jump directly. **Shortcut options → Move to page…** moves a shortcut or folder to another page with room. Vesna restores the last selected page. | Android 8 and 15 emulator verification; see [Workspace page controls](#workspace-page-controls), [Jump to a Home page](#jump-to-a-home-page), [Reorder Home pages](#reorder-home-pages), [Move a shortcut between pages](#move-a-shortcut-between-pages), and [Move a folder between pages](#move-a-folder-between-pages). |
| Dock | Four slots aligned with workspace columns, ordering and reorder rules in `DockOrdering` | `DockOrderingTest` (9 tests) |
| First-run seeding | `SeedPlanner` fills dock and first page once; a persisted "seeded" flag prevents re-seeding; user-emptied workspaces are never re-seeded | `SeedPlannerTest` (4 tests) |
| App discovery | `LauncherApps`-based `AppDiscovery` across the main and managed profiles, sorted by `label.lowercase(Locale.ROOT)`, single `<queries>` manifest entry (no `QUERY_ALL_PACKAGES`) | Source only; discovery needs system services and is deliberately not exercised by tests |
| App drawer access (unreleased) | Tap the labeled app-grid button above the dock or swipe up. Close with the drawer button or Android Back. The drawer fits up to four columns to the available width and keeps its scroll position when apps change. | Android 8 and 15 emulator verification; see [App drawer access](#app-drawer-access), [Narrow drawer layouts](#narrow-drawer-layouts), and [Drawer refresh](#drawer-refresh). |
| Home options (unreleased) | A named overflow button above the dock opens Settings, wallpaper, and applicable launcher and page actions without a long-press. | Android 8 and 15 emulator verification; see [Home options](#home-options). |
| Drawer search | Start typing with a keyboard anywhere in the open drawer. Press **Enter** in the search field to open the sole match. Substring search ignores case, repeated spaces, Latin accents and the dots on Russian `ё`, without changing labels or drawer order. **Clear search** restores the grid and returns focus to the field. | See [Keyboard search](#keyboard-search), [Drawer search matching](#drawer-search-matching) and [Drawer search recovery](#drawer-search-recovery). |
| Drag-and-drop | App pins move across workspace and dock. Folders can be dragged to empty cells on their current Home page. | Android 8 and 15 folder-drag verification; see [Move a folder on the same page](#move-a-folder-on-the-same-page) and [Requirements traceability](REQUIREMENTS_TRACEABILITY.md#data-and-platform-behaviors-without-research-ids). |
| Shortcut options (unreleased) | On a focused app or folder tile, press **Shift+F10** or the **Menu** key. Android accessibility exposes **Shortcut options**. Touch long-press and drag remain available. | See [Shortcut options](#shortcut-options) for Android 8 and 15 emulator verification. |
| Folders | `Folder` cell and a Material 3 `FolderDialog` with a responsive member grid; both rename actions use `FolderRenameDialog` | `WorkspaceResolverFolderTest` covers the projection. Device and Room tests cover member removal, rollback, and uninstall-driven dissolution. See [Compact folder layouts](#compact-folder-layouts), [Rename a folder](#rename-a-folder), and [Folder verification](REQUIREMENTS_TRACEABILITY.md#folders). |
| Package events | `PackageChangeMonitor` uses profile-scoped `LauncherApps` callbacks; disabled apps retain their pins | [VESNA-QA-003A](verification/VESNA-QA-003.md) verified drawer changes after install, update, disable/re-enable, and uninstall. [VESNA-QA-003C](verification/VESNA-QA-003C.md) verified work-profile pinning and uninstall. [VESNA-QA-003D](verification/VESNA-QA-003D.md) verified uninstall across all three containers. [VESNA-QA-003F](verification/VESNA-QA-003F.md) verified disable/re-enable retains placements across a launcher restart. All four records use API 35. |
| Persistence | Room `vesna-launcher.db`, schema version 4. The 3→4 migration adds stable workspace page IDs without changing the dock or existing cells. | Production Room migrated a copy of the signed 0.4.2 database on API 35 with all 18 placements, the folder, and the seeding flag intact. See [ADR-PAGE-1](../adr/0001-workspace-page-identity.md). Historical API 35 migration and reboot evidence is in [VESNA-QA-004](BACKLOG.md#vesna-qa-004-migration-and-restart-verification-done). |
| Settings / About | The app icon opens a clean settings directory with search and separate Default launcher, Appearance, and About pages. Named Android links select the exact page from any previous Settings state. Back dismisses the keyboard before clearing search. | [Android 8 and 15 Settings navigation QA](verification/VESNA-QA-SETTINGS-NAVIGATION.md). For the system picker itself, see [Default launcher setup](#default-launcher-setup). |
| Appearance | **Vesna settings → Appearance → Theme** offers System, Light and Dark. Vesna saves the mode locally and applies it to Settings and native launcher surfaces independently of the wallpaper. | See [Appearance](#appearance) for Android 8 and 15 emulator verification. |
| App names | **Vesna settings → Appearance → Show app names** hides or restores app labels on Home, in the dock, in the drawer, and in folders. Folder titles, accessible app names, and icon positions stay the same. | See [App names](#app-names) for Android 8 and 15 emulator verification. |

**Explicitly not implemented:** widgets, icon packs, fuzzy search, drawer
search-result match highlighting, search beyond substring. The Android manifest
sets `android:allowBackup="true"` (platform default behavior), but a dedicated
backup/restore feature does not exist; the backup/privacy ADR candidate in
[BACKLOG.md](BACKLOG.md#adr-backup-1-backup-and-privacy-policy) must decide
whether platform backup stays enabled and what its scope is. There is no
monetization of any kind — no ads, subscriptions, paid features, in-app
purchases, or analytics/tracking SDKs.

## Quality status at the baseline

| Check | Result |
|---|---|
| `assembleDebug` | Success |
| `:app:assembleDebugAndroidTest` | Success |
| JVM unit tests | 42 tests, 6 suites, all passing |
| Android Lint | 0 errors, 18 warnings (3 of them are test-dependency notices) |
| Post-merge CI | Run 44385854347 green (build, JVM tests, lint; no instrumentation in CI) |

JVM suites: `DockOrderingTest`, `GridCanvasTest`, `SeedPlannerTest`,
`WorkspaceResolverTest`, `WorkspaceResolverFolderTest` (workspace package) and
`AppSearchTest` (apps package). At the baseline one instrumentation suite
existed: `DrawerSearchTest` (single test covering mixed-case query narrowing,
empty state, and clear/restore), run locally, not in CI; `RoomMigrationQaTest`
was added afterwards by [PR #13](https://github.com/vladon/vesnalauncher/pull/13)
(recorded in the instrumentation verification record below).

### Instrumentation verification record

- `DrawerSearchTest` passed 3 consecutive runs on the dedicated API 35 AVD.
- `RoomMigrationQaTest` (introduced by
  [PR #13](https://github.com/vladon/vesnalauncher/pull/13), merge `14ec712`):
  both cases (`migrationFromV1PreservesLayoutAndDerivesDefaults`,
  `migrationFromV2PreservesUserSerialsAndAddsFolders`) plus `DrawerSearchTest`
  were executed by the coordinator on the dedicated API 35 AVD (2026-11-19).
  The test seeds authentic v1/v2 schemas into isolated on-device databases and
  migrates through the production `LauncherDatabase.build`; its own doc states
  process-death/reboot durability is a separate task — covered by
  [VESNA-QA-004B](verification/VESNA-QA-004B.md).
- The full existing instrumentation suite passed on the declared minimum OS:
  [VESNA-QA-005-API26](verification/VESNA-QA-005-API26.md) (2026-11-19,
  dedicated API 26 AVD) ran `RoomMigrationQaTest` (both cases) and
  `DrawerSearchTest` — 3/3 passed, 0 failures/errors/skipped; the per-case
  JUnit XML (`TEST-Vesna_QA_MinSdk_API_26-AVD-8.0.0.xml`) is listed in the
  record's evidence index. Narrow smoke scope: build, HOME discoverability,
  cold start, workspace/drawer render, one fixture drawer round-trip. The
  same run exposed a fifth-row dock overlap (VESNA-BUG-004), fixed in
  [PR #23](https://github.com/vladon/vesnalauncher/pull/23) and replayed on API 26 and API 35.
- At audit time, an API 36.1 AVD was provisioned but did not boot, so API 36.1
  was not covered by the audit. That gap was closed afterwards by
  [VESNA-QA-006-API36-1](verification/VESNA-QA-006-API36-1.md)
  (2026-11-19, dedicated API 36.1 AVD on Windows): the full existing
  instrumentation suite passed 5/5, with HOME selection and launch-return
  smoke-verified through the system RoleManager dialog. Run 2 on the same AVD
  exercised the BUG-005 candidate `d8d6a748` with the added
  `WorkspaceRepositoryTopLevelRemovalTest`: 7/7 passed; these device runs are
  scoped to that branch candidate, which merged as
  [PR #27](https://github.com/vladon/vesnalauncher/pull/27) (`81cea18`).
- On Android 12/API 31, the 12-test candidate `75ca3ed` passed. The run
  verified workspace and drawer rendering and HOME registration while
  NexusLauncher remained default.
  [VESNA-QA-010](verification/VESNA-QA-010-API31.md)
- The full 16-test instrumentation suite passed on dedicated API 26, 31,
  35, and 36.1 emulators using the same reviewed debug candidate
  `1b5dc2e`. Each run used the serial-pinned runner and restored its
  fixture. [Requirements traceability](REQUIREMENTS_TRACEABILITY.md#data-and-platform-behaviors-without-research-ids)
- Dock capacity, top-level unpin, and dock-to-workspace drag regressions were
  verified on dedicated emulators. The verification scope for each fix is
  recorded in [Requirements traceability](REQUIREMENTS_TRACEABILITY.md#data-and-platform-behaviors-without-research-ids).
- [VESNA-QA-008](https://github.com/vladon/vesnalauncher/pull/34) strengthened
  the approved order-preservation and local-filtering coverage. The test
  supplies an unsorted fixture and verifies the order of two mixed-case
  substring matches. The full suite passed 8/8 on the dedicated API 35 AVD
  with candidate `18dd2d1`.
- The eight-test suite passed on API 26 at build `4c5f0c1`.
  [VESNA-QA-005B](verification/VESNA-QA-005-API26.md#follow-up-on-the-eight-test-suite)
- The legacy HOME chooser and return from Calculator were verified on API 26.
  [VESNA-QA-002B](verification/VESNA-QA-002.md#api-26-follow-up-vesna-qa-002b)
- Focused regression records cover the two folder transactions and the
  selected-pin workspace move.
  [Requirements traceability](REQUIREMENTS_TRACEABILITY.md#folders)

At audit commit `888f557`, startup, scroll, and drag had no baseline
measurements. [VESNA-QA-007](verification/VESNA-QA-007-COLD-START.md) now
records a process-cold debug ActivityManager timing series on an Android 15
shared-host emulator, and
[VESNA-QA-009-LAUNCH-STATES](verification/VESNA-QA-009-LAUNCH-STATES.md)
adds paired cold and warm timings on the dedicated Android 15 AVD (COLD and
WARM only; no HOT state was measured). Drawer scrolling, drag latency, time to
interactivity, release builds, and real devices remain open.

## Audit status discipline

Statements in this file and in [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md)
are either (a) verified against the source at the pinned commit, or (b) marked
as covered/not covered by the recorded audit runs. "Source-verified" means the
code implements the behavior as described; it is not a claim of on-device QA.
The audit ran the checks listed above and no others; absence of a check from the
list means it was not performed, nothing more.

## Known gaps and risks

- Platform coverage comes from dedicated API 26, API 31, API 35, and API 36.1
  emulators. See the instrumentation record for the tested builds and
  device configurations.
- CI does not run instrumentation; `DrawerSearchTest` depends on a local AVD and
  a manual runner.
- Managed-profile behavior (badged work apps, profile-aware removal) is
  implemented via `LauncherApps` and `userSerial` and is outside the audit;
  profile-scoped discovery, pinning, and removal were device-verified
  afterwards by [VESNA-QA-003C](verification/VESNA-QA-003C.md) (2026-11-19,
  API 35 AVD).
- [Profile-aware App info](#profile-aware-app-info) was verified later with
  identical Personal and Work copies of the same app on Android 15.
- The last published release is `0.4.2` (version code 7, Room schema 3).
  The signed [0.4.1 → 0.4.2 upgrade](verification/VESNA-QA-0.4.2-UPGRADE.md)
  preserved all workspace, dock, and folder data on Android 15.
- The local `0.4.3` candidate (version code 8, Room schema 4) passed 41 JVM
  tests and all 29 instrumentation tests on each of two dedicated emulators:
  Android 8/API 26 (`Vesna_Release_043_API26_QA`, fresh install) and Android
  15/API 35 (`Vesna_Pages_V4_QA`). Home rendered on both. On API 35, the debug
  candidate migrated a copy of the signed 0.4.2 database, preserving all 18
  placements, the folder and its members, and the seeding flag. Lint reported
  0 errors and 20 warnings. The minified release APK was built unsigned;
  its package, version, and ZIP alignment were checked locally.

### Workspace page controls

The local `workspace-page-controls` branch builds on the 0.4.3 storage work; it
does not change the Room schema or the last published release. New pages are
empty; ordinary pinning and drag-and-drop act on the displayed page. On an
empty extra page, open **Home options → Remove empty page** to free a slot and return
to the first Home page. The first page and pages with saved shortcuts or folders
cannot be removed, even if a saved shortcut is not currently visible. Dragging
between pages is not available.

- All 41 JVM tests and 34 instrumentation tests passed; the instrumentation
  suite ran on both Android 8 (API 26) and Android 15 (API 35). Debug lint had no
  errors.
- On Android 15, a drawer drag and a drawer-menu pin landed on page two. A
  force-stop and relaunch restored that page and both pins. The original
  18 placement rows, the folder, and the shared dock were unchanged. The Home
  menu stopped offering creation at eight pages.
- The page controls were checked at 320, 411, and 610 dp widths, with gesture
  and three-button navigation, and in Light and Dark at up to 1.6× font scale.
  Verified 48 dp targets, color contrast, visible keyboard focus, English and
  Russian labels, accessibility position/count, and mirrored RTL gestures.
  Spoken TalkBack announcements and physical-device behavior were not verified.
- The eight focused page tests passed on both emulators for empty-page removal.
  The Home menu restored **Add home page** after a removal at the cap, retained
  the first page across restart, and preserved all 20 saved placements and the
  folder on Android 15. Android 8 hid the pager after removing the only extra
  page. The new menu row fit at 320 dp and 1.6× type in English and Russian;
  touch selection and visible D-pad focus and activation were verified.

### Jump to a Home page

Tap the page dots above the dock or choose **Home options → Go to page…** to
jump directly to any Home page. The list marks the current page and includes
full and empty pages. Both entry points are hidden when only one page exists.

- On Android 8, touch navigated through both entry points, including a jump to
  a full page. The dots measured 48 × 48 dp with two pages. Removing the extra
  page hid the picker again and left the original layout unchanged.
- On Android 15, Tab reached the dots, Back dismissed the menu with focus
  restored, and D-pad selection jumped from the first page to the last.
  That selection survived a force-stop and restart.
- Checked Light and Dark, three-button and gesture navigation, checked-row
  semantics, all eight rows at 320 dp in Russian at 200% text, and localized
  RTL numerals and controls at 610 dp. Debug lint passed. TalkBack speech and
  physical devices were not checked.

### Reorder Home pages

With at least three pages, go to the page you want to move and open
**Home options → Reorder this page…**. Pick its new position. Pages in between
shift; the first page stays fixed. The moved page stays selected, with its
shortcuts and folders in the same cells and the shared dock unchanged.

- Android 8 touch verification moved page three to position two. The action
  was hidden on the first page and when fewer than three pages existed.
- Android 15 D-pad verification moved a populated page from position two
  to six. Back canceled the chooser and returned focus to Home options.
  The new order and selected page survived a force-stop and restart without
  changes to any shortcut, folder, or dock rows.
- All eleven `WorkspaceRepositoryPagesTest` cases passed on both emulators.
  Debug lint passed. The native menu was checked in Light and Dark, at compact
  and wide widths, at 200% text size, in English and Russian, and with RTL
  page controls and localized digits. TalkBack speech and physical devices
  were not checked.

### Move a shortcut between pages

Long-press a Home app shortcut, or focus it and press **Shift+F10** or the
**Menu** key. Choose **Move to page…** and select a destination with room.
Vesna moves the existing shortcut to the first free cell and keeps the
current page selected. It does not copy the shortcut or replace another item.
With only one page, the action is hidden.

- Android 8: the touch chooser skipped a full page and moved the same pin
  to the eligible page. Android 15: D-pad selected a destination; Back
  canceled without moving the pin and returned keyboard focus to it.
- The moved pin ID, destination and selected source page survived a force-stop
  and restart. All pre-existing layout rows were unchanged after QA cleanup.
- Checked the Dark 320 dp and Light 610 dp choosers, Russian at 200% text,
  RTL with localized page numbers, and both navigation modes. Debug lint and
  all ten `WorkspaceRepositoryPagesTest` cases passed on both emulators.
  TalkBack speech and physical devices were not checked.

### Move a folder between pages

Long-press a folder on Home, or open **Shortcut options** with **Shift+F10**,
the **Menu** key, or Android accessibility. Choose **Move to page…** and
select another page with room. The folder moves into the first free cell
with its name and apps intact. The source page stays selected.

- On Android 8, touch selection moved a two-app folder and skipped a full
  page. On Android 15, D-pad selection moved the folder after a canceled
  choice restored focus. The folder reopened with both apps after a
  force-stop and restart, and no shortcut rows changed during the move.
- All 13 page tests passed on both emulators, including stale source and
  full or removed destination handling. Debug lint passed.
- Checked Light and Dark with gesture and three-button navigation, English,
  compact Russian at 200% type, and wide RTL. New menu labels fit, and the
  destination rows measured 48 dp high. The original Russian rename row
  truncated at 320 dp and 200% type; it was fixed in
  [0.4.17](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.17).

### Context menu labels at enlarged text sizes

- In [PR #73](https://github.com/vladon/vesnalauncher/pull/73), the Android 8
  and 15 shortcut suites passed five checks each, including a wrapped row
  that accepted the Android accessibility click action. Debug lint passed.
- Verified Russian labels without truncation at 320 dp and 198 dp with
  200% text, Light and Dark, RTL positioning, touch, D-pad selection of a
  checked page, Back, and the chained Move to page menu.
- The exact signed 0.4.16 → 0.4.17 update retained 13 Home pins, four dock pins
  and the renamed folder and its member order. Tapping its wrapped menu item
  opened the focused rename editor. TalkBack speech and physical devices
  were not checked.

### Move a folder on the same page

Hold a folder and drag it to an empty Home cell. The target highlights during
the drag, and the folder keeps its name and apps. Dropping on an occupied
cell, over the dock, or outside the grid leaves it in place. Holding without
moving still opens the folder menu.

- Android 8 and 15 verified valid and invalid drops, the stationary-hold
  menu, unchanged member rows, and placement across restart. On Android 15,
  a grid resize canceled a pending drag, and Shift+F10 still opened the
  folder menu and returned focus after dismissal.
- Checked the empty target highlight and no highlight on an occupied cell.
  Verified Light and Dark at 200% text with gesture and three-button
  navigation.
- All 14 `WorkspaceRepositoryPagesTest` cases passed on both emulators;
  debug lint passed. TalkBack speech and physical devices were not checked.

### Rename a folder

Choose **Rename folder** from the Home folder's menu or from inside the folder.
The editor selects the current name. **Save** or keyboard **Done** removes
surrounding whitespace; a blank name shows an inline error without closing.
**Cancel** discards edits. Renaming from inside a folder leaves it open.

- Android 8 and 15 verified both entry points, blank input, cancellation,
  and the saved name across a launcher restart. Folder identity, cell, and
  member rows were unchanged by renaming.
- Verified Android 8's on-screen **Done** for both valid and blank input.
  On Android 15, Shift+F10 and D-pad opened the editor, and Enter saved and
  returned focus to the same Home tile.
- Checked Light and Dark, gesture and three-button navigation, and English
  and Russian at 320 dp and 200% text. The localized buttons remained at
  least 48 dp in each dimension; field and error text were exposed together
  in the Android accessibility tree. TalkBack speech, RTL, and physical
  devices were not checked. Debug build and lint passed.
- One Android 8 run crashed in Android's accessibility-state callback
  during an automated UI dump. Later open, cancel, and save checks on the
  same emulator did not reproduce the crash.
- The [0.4.16 follow-up](https://github.com/vladon/vesnalauncher/pull/72)
  kept the field above the keyboard at 198 × 339 dp and 200% text, where
  signed 0.4.15 hid it. Android 8 and 15 passed four focused shortcut tests
  each; Light and Russian Dark, Save, Done, blank validation, IME-first Back,
  and Cancel were checked. The signed upgrade preserved the folder's member
  order and Home and dock placement. No spoken TalkBack or physical tests ran.
- Signed 0.4.24 lost an unsaved draft when Android switched themes. The
  fix kept the draft, editing focus, and keyboard on Android 15 at 200%
  text from Light to Dark. Cancel returned to the same open folder with
  no name change. From the Home folder menu, Dark to Light preserved the
  draft through Save and updated the original tile in place. A Home
  intent still closed the editor and folder. Build and lint passed, as
  did 18 focused tests each on Android 8 and 15. The live theme-switch
  check was on Android 15; physical devices and TalkBack were not tested.
- Signed 0.4.27 kept a blank draft during a theme switch but lost the
  validation message. The editor now retains both, including the field's
  accessibility error. On Android 15 at 200% text, Light to Dark kept the
  message; entering a name cleared it, and Cancel left the folder unchanged.
- `FolderRenameValidationTest` verified the visual text and accessibility
  error through editor recreation, whitespace rejection, and saving a
  corrected Cyrillic name. Build, lint and 19 focused tests passed on
  Android 8 and 15. Physical devices and spoken TalkBack were not tested.

### Compact folder layouts

An open folder uses up to four columns, like the drawer, with at least
64 dp per cell when space allows. Narrow windows show fewer columns without
changing the member order. Members scroll independently of the dialog actions.

- Android 8 and 15 showed 64 dp tiles at 240 dp and 200% text, compared
  with the previous clipped 52 dp tiles. At 210 × 480 dp, the member list
  scrolled to the last row and both actions stayed visible and usable.
- Resized an open folder between two, three, and four columns while retaining
  member order and keyboard focus. Checked member launch, shortcut options,
  rename, removal, and focus return to the invoking Home tile.
- Checked Light and Dark at normal and enlarged text, gesture and
  three-button navigation, and Arabic RTL alignment. The existing drawer
  resize test passed on both emulators; debug lint passed. TalkBack speech
  and physical devices were not checked.

### Keep an open folder through Home recreation

- On signed 0.4.28, switching the system theme closed the folder and
  returned to Home. On Android 15 at 200% text, the fix kept the same
  folder and member order from Light to Dark.
- Back closed the folder, and the next theme switch did not reopen it.
  A Home intent still dismissed it, and Cancel on a restored rename
  returned to the invoking folder.
- The integration regression uses an isolated folder on a temporary
  secondary page. It reproduced the loss on the previous debug APK.
  The 20 focused tests passed on Android 8 and 15; build and lint passed.
  Physical devices and TalkBack speech were not tested.

### Keep keyboard focus inside an open folder through Home recreation

- Signed 0.4.29 kept the folder open but moved focus from Files to Drive
  when switching from Light to Dark on Android 15. The fix restores focus
  to the same saved shortcut, not the first tile or another copy of the app.
- On Android 15 at 200% text, the focus outline stayed on Files across the
  Light-to-Dark switch. D-pad Left moved focus to Drive, which stayed
  focused on the switch back. Both themes showed the full accessible app
  names and 48 dp dialog actions. Home closed the folder, and reopening
  did not reclaim the previous focus.
- The 42-member regression failed on the previous debug APK at the focus
  assertion. It verifies the focused member, existing scroll restoration,
  and that an intentional dismissal does not reopen the folder. The 20
  focused tests passed on Android 8 and 15; debug build and lint passed.
  TalkBack speech and physical devices were not tested.

### Return keyboard focus to the workspace folder after Back

- On signed 0.4.32, Back returned focus to the folder's Home shortcut in
  the original activity, but jumped to Contacts if the system theme
  changed while the folder was open.
- On Android 15 at 200% text, after switching themes in either direction
  and pressing Back, the folder tile regained focus. D-pad Right moved
  to the next shortcut. Dismissing by touch did not force keyboard focus.
  The tested folder tile was 91 × 90 dp, with its full accessible name.
  Focus outline-to-fill contrast was 4.96:1 in Light and 7.29:1 in Dark.
- The updated recreation regression failed on 0.4.32 at the Back focus
  assertion. With the fix, 23 focused tests passed on each of Android 8
  and 15. Debug build and lint passed; TalkBack speech and physical
  devices were not tested.

### Keep keyboard focus on Home and in the dock through recreation

- With signed 0.4.30 on Android 15, a system theme switch moved focus
  from Gmail on the workspace or Chrome in the dock to Contacts.
- At 200% text on Android 15, verified focus retention on the workspace
  app and folder tiles and on a dock favorite, in Light and Dark. D-pad
  moved focus normally after recreation. The tested app tiles measured
  at least 90 dp on each axis; the outline-to-focus-fill contrast was
  4.96:1 in Light and 7.29:1 in Dark on the fixture wallpaper.
- Duplicate shortcuts in the regression distinguish the exact focused
  copy from another copy of the same app. Both cases failed on 0.4.30.
  With the fix, 11 focused tests passed on each of Android 8 and Android 15;
  debug build and lint passed. TalkBack speech and physical devices were not tested.

### Home labels at large text sizes

On compact Home grids, app and folder artwork shrinks when needed to fit a
complete line of scaled text. Long names still ellipsize horizontally; the
full accessible names, tap targets, and saved positions stay unchanged.

- Android 8 (Dark, three-button) and 15 (Light, gesture) showed the complete
  line at 320 dp and 200% text. Measured Home targets were at least
  68 × 71 dp. Normal text kept full-size artwork at 320 and 600 dp.
- Verified app launch, folder opening, and the keyboard shortcut menu with
  focus return. The white text and opaque wallpaper backing provide
  15.88:1 contrast. TalkBack speech and physical devices were not checked.
- `WorkspaceLabelLayoutTest` reproduced clipping on the previous build and
  passed on both emulators with English app and Cyrillic folder names.
  Debug build and lint passed.

### Home shortcuts in landscape

- On signed 0.4.22 in Android 15 landscape (1920 × 1080 px, 420 dpi),
  the right navigation bar began at x=1794, but the last workspace and
  dock tiles extended to x=1887 and x=1910. Workspace label lines were
  cut off even at normal text size.
- Home now uses safe side insets for its grid, dock, and controls without
  moving the wallpaper or changing saved cells. Wide, short tiles put
  the artwork beside a complete label line, with the same 4 dp gap.
- Android 15 showed both the artwork and a 91 px label line at 200% in
  Light with a right bar and Dark with a left bar. At normal text size
  in portrait, the artwork returned to 48 dp above the label. Drawer-to-Home
  and workspace drags next to a left inset landed in the intended cells.
  Android 8 showed full label lines on a populated landscape Home page.
- The focused Home, paging, dock and shortcut tests passed 14/14 on
  each Android version, including the compact app and folder tiles in
  LTR and RTL. The artwork regression failed on the earlier candidate
  as expected. Build and lint passed. Physical cutouts and TalkBack
  speech were not tested.

### Cancel a drag when a second finger touches

On the signed 0.4.20 APK, letting a second finger take over a drawer drag
added a Drive shortcut to an empty Home cell. The drag layer now cancels an
active drag before the new pointer can be routed to the drop handler. It clears
the preview and waits for a fresh touch before another drag can start.

- On Android 15, the same two-pointer drawer-to-Home gesture kept the cell
  empty. A one-finger retry added the shortcut. In Dark at 200% text, an
  interrupted dock drag preserved all four slots; one-finger reorder worked.
- The folder drag regression failed on the 0.4.20 debug build and passed on
  the fix: the second finger no longer moved the folder, and the next
  one-finger drag worked. The shortcut, page swipe and dock tests passed 12/12
  on each of Android 8 and 15. Debug assembly and lint passed.
- No visual styles, touch target sizes or accessibility actions changed.
  Physical multitouch and TalkBack were not tested.

### Home options

Tap the **Home options** button above the dock or long-press empty Home space
to open the same menu. The button works even when all 20 workspace cells
are occupied. Use it to reach Vesna Settings, the wallpaper picker, the
default-launcher chooser, and page actions when available.

- Android 8: tapped the new button on a full Home, opened Settings, and compared
  the menu with long-press. The drawer hid the button while open. Icon positions
  and persisted placements did not change.
- Android 15: Tab and D-pad opened the menu and selected Settings, and Back
  returned focus to the button when dismissing the menu. At eight pages, all
  four controls kept 48 dp targets in a 320 dp window. **Add home page** was
  hidden at the cap, **Remove empty page** was absent on populated Home, and
  adding/removing two test pages left the original pages and placements intact.
- Checked Light and Dark at 320 dp and 610 dp, English and Russian at up to
  200% text, RTL, and gesture and three-button navigation. Opened the wallpaper
  chooser and canceled without changing the wallpaper. Debug lint passed, as
  did all eight `WorkspaceRepositoryPagesTest` cases on both emulators.
  TalkBack speech and physical devices were not checked.

### Return to the current Home page

Android **Home** closes launcher menus, the drawer, and folder dialogs while
keeping the selected workspace page. An unsaved folder name is discarded.
**Back** still dismisses the frontmost layer first.

- On Android 15, the Home key closed Home options, the page picker, move and
  reorder menus, a folder member menu, and a rename draft. Returning from
  Clock launched in the drawer kept page 4 in the same Home activity.
- On Android 8, a Home intent closed the rename editor, keyboard, and folder
  without saving the draft. Using Back instead returned to the open folder.
- Checked Light with gesture navigation and Dark with three-button navigation
  at enlarged text sizes. The Home options regression verifies that the same
  activity regains window focus, rather than relying on Android 8's active
  accessibility root immediately after the popup closes. All three shortcut
  tests passed on Android 8 and 15; the Home options test failed on both when
  dismissal was deliberately disabled. Debug lint passed; physical devices
  were not checked.

### App launch feedback

If Android rejects an app launch, Vesna shows a Material snackbar and refreshes
the app list. In the drawer, the search query, focus, and open keyboard stay in
place. If launched from a folder, feedback appears inside the open dialog.

- On Android 8 (Dark, 320 dp, 2× text, three-button navigation) and Android 15
  (Light, 1.3× text, gesture navigation), pressing Enter on a stale search
  result showed accessible feedback above the keyboard without clearing the
  query or closing the keyboard. The successful Enter path still opened the app.
- On Android 15, a rejected folder launch displayed feedback alongside the
  folder contents and kept the rename and remove buttons visible.
  Debug lint passed. TalkBack speech, Russian and RTL rendering, and physical
  devices were not checked.

### App drawer access

The Home and drawer buttons are 48 dp targets. The Home button is available on
a single page and next to the pager with more pages. The drawer stays opaque
and keeps Home out of keyboard and accessibility traversal while open.

- `DrawerSearchTest` passed on Android 8 (API 26) and 15 (API 35); debug lint
  reported no errors. Both emulators exercised touch opening and closing;
  Android 15 also verified swipe-up, Tab/D-pad entry, and focus restoration
  from both the close button and Android Back.
- The controls fit 320, 411, and 610 dp layouts, with gesture and three-button
  navigation, English and Russian labels, and RTL ordering. The 320 dp Home
  retained all 20 grid tiles and a complete label line at 1.6× type. Android 15
  repainted the controls on a live system night-mode change without a manual
  relaunch.
- A drawer drag on a secondary Home page still landed on that page. Removing
  the test pin restored the exact original placement and folder rows. TalkBack
  speech and handheld-device behavior were not verified.

### Drawer search through Home recreation

- Changing the Android system theme while searching for Calendar on signed
  0.4.23 returned to Home, hiding the open drawer and its results.
- On Android 15 at 200% text, the drawer stayed open from Light to Dark
  with the query, search focus, keyboard and matching app intact. Rotation
  after hiding the keyboard retained the result; closing and reopening
  started an empty search.
- `DrawerRecreationTest` failed on the previous debug build and verified
  the query, cursor, focus, empty result, and clean reopen on the fix.
  The 13 focused tests passed on Android 8 and 15. Debug build and lint
  passed. Physical devices and TalkBack speech were not tested.

### Keyboard focus in drawer results through Home recreation

- On signed 0.4.31, changing the system theme kept the `ca` search but
  moved keyboard focus from Camera to Close. The fix restores the same
  app once the restored search results are ready.
- On Android 15 at 200% text, Camera kept focus from Light to Dark.
  D-pad Left moved to Calendar, which kept focus on the switch back.
  Also checked a scrolled result below the initial viewport in a
  shorter window. Closing and reopening cleared the query and focus.
- The Camera tile measured 103 × 98 dp, with its full accessible name
  and focus outline-to-fill contrast of 4.96:1 in Light and 7.29:1 in Dark.
  The new regression failed on 0.4.31; 22 focused tests passed on each
  of Android 8 and 15 with the fix. Debug build and lint passed.
  TalkBack speech and physical devices were not tested.

### Narrow drawer layouts

The drawer keeps four columns when they fit. In narrower windows, it uses
fewer columns to leave at least 64 dp per tile wherever that much space is
available. System bars and cutouts are outside the grid and search area.
The compact search bar uses a short localized hint and smaller side margins.

- Android 8 verified two columns at 184 dp, three at 232 dp, and four at
  320 dp. The 48 dp app art stayed intact, labels kept their full accessible
  names, and keyboard search and touch Clear still worked.
- Android 15 kept keyboard focus on the same app when shrinking from four
  columns at 610 dp to three at 240 dp. Shift+F10 still opened its menu.
  Checked the Russian search hint and mirrored RTL order at the narrow width.
- Verified both themes at 200% text, both navigation modes, and unchanged
  Home placement rows. All seven `DrawerSearchTest` cases passed on both
  emulators, including reflow around side insets. Debug lint passed.
  Physical devices and TalkBack speech were not checked.

### Inline keyboard for landscape drawer search

- On Android 8, Gboard replaced the 0.4.25 drawer with a full-screen
  editor in landscape. The matching apps were hidden until the keyboard closed.
- With the fix, the in-app search and Clock result stayed visible, and
  tapping the result with the keyboard still open launched Clock.
- Android 15 in Dark at 200% showed Calendar in the scrollable area
  above the keyboard. Back hid the keyboard without clearing the search.
- The editor-contract assertion failed on the old app and passed with the
  fix. Debug build, lint and 13 focused tests passed on Android 8 and 15.
  Only the emulators' Gboard versions were tested; no physical or TalkBack checks ran.

### No-results feedback above the drawer keyboard

- On signed 0.4.26 at 200% text on Android 15, the short landscape
  viewport had room for the message, but its fixed padding clipped it.
- The candidate shows the full “No matching apps” line above Gboard in
  Light and Dark at 200% on Android 15, and at standard text size on
  Android 8. Clear still restores the app grid and keeps field focus.
- The short-viewport test failed at the clipping assertion on the old
  build. Build, lint and 13 focused tests passed on both emulator versions.
  Physical devices, other keyboards, and TalkBack speech were not tested.

### Drawer refresh

Installing, updating, or removing an app no longer sends the open drawer
back to the first row. Editing or clearing search still starts at the top
of the new results, even if an app refresh arrives at the same time.

- On Android 8 and 15, a scroll away from the top survived installing,
  updating, and uninstalling the signed lifecycle fixture. Clear search
  returned to the first row. The saved Home layout stayed unchanged.
- The regression test failed before the fix: refreshing jumped from Tool 40
  to Tool 0. The fixed build kept the same partly visible row across inserts
  and removals. All six `DrawerSearchTest` cases passed on both emulators.
- Checked compact Dark and wide Light at 200% text, with three-button and
  gesture navigation. Debug lint passed. TalkBack speech and physical
  devices were not checked.

When one installed app changes the column of a visible shortcut, the drawer
keeps that shortcut at the same height. Scrolling to a different row changes
what the next refresh preserves. Editing or clearing search still resets to
the top.

- Removing one app above the visible results previously made Tool 40 vanish
  from the viewport. Now Tool 40 stays at its original offset during removal
  and reinsertion, and a later scroll anchors the newly visible row. Five
  focused drawer tests passed on both Android 8 and 15, as did debug lint.
- On Android 8 in compact Dark at 200% and Android 15 in Light at 130%,
  uninstalling and reinstalling a test app above the visible rows moved
  Chrome and Clock across columns respectively without changing their
  vertical bounds. Clearing the query returned to the top with search
  focused. Named 48 dp controls and both palettes were checked; TalkBack
  speech was not tested.

### Keyboard search

With the app drawer open, start typing to focus search and filter the grid.
If you were browsing results, typing continues the existing query. This
doesn't change Home, arrow-key navigation, or shortcut activation.

With focus in the search field, press **Enter** or **numpad Enter** to open
the only match for a nonblank query. Blank, unmatched, and ambiguous
queries stay in the search field. On-screen **Done** still dismisses the
keyboard.

- On Android 8 and 15, typing after opening the drawer by touch entered the
  first character and filtered the list. On Android 15, Shift+F10 still
  opened the focused app's menu; after Back, typing from that tile continued
  the query. The saved Home layout was unchanged.
- All three `DrawerSearchTest` cases passed on both Android versions. Debug
  lint passed. Checked Dark 320 dp and Light 610 dp at 200% text, including
  Russian and RTL with the filtered results above the on-screen keyboard.
  Physical keyboards and TalkBack speech were not checked.
- Quick launch was verified with the real Clock app on Android 8 (Enter)
  and Android 15 (numpad Enter). Repeated Enter with zero or multiple matches
  kept focus in search; tapping Done with one match dismissed the keyboard
  without launching. All five drawer tests passed on both emulators, including
  a stale visible result and a match that changed while Enter was held.
  Checked Light at 130% text and compact Dark at 200%. Debug lint passed.

### Drawer search recovery

The trailing 48 dp clear button appears only when there is text, including on
the no-results screen. It restores the full grid and gives the search field
focus so another query can be typed immediately.

- The updated `DrawerSearchTest` passed on API 26 and API 35, clearing through
  the actual button. Touch clearing, a second mixed-case query, and Android 15
  Tab/D-pad clearing were also exercised in Home. Debug lint reported no errors.
- Checked the Light and Dark search controls at 320 dp and 1.6× type, English
  and Russian labels, and RTL placement. The drawer grid ended at the visible
  keyboard boundary on both Android versions; the final app could be scrolled
  fully above the keyboard on Android 15. TalkBack speech was not verified.

### Search in short drawer windows

When the keyboard leaves a short viewport, Close and Search share a row.
The query and Clear stay accessible rather than being vertically clipped.
In the shortest window, hiding the keyboard reveals the full matching
results without clearing the query.

- Signed 0.4.17 clipped the search text at 198 × 339 dp and 200% type. The
  new regression failed on the old build and all 11 drawer tests passed
  on Android 8 and 15 with the fix.
- Checked the real keyboard at 198 dp, Light and Russian Dark, Android 8
  three-button and Android 15 gesture navigation, mirrored RTL controls,
  Clear, keyboard-first Back, and on-screen Done. The standard-height
  drawer kept its stacked header and live results.
- Spoken TalkBack, physical hardware, and floating keyboards were not tested.

### Drawer search matching

Type `cafe reader` to find **Café Reader**, or `елки` to find **Ёлки**.
Extra spaces and Unicode width variants are accepted. Search still matches
one contiguous substring in the app label; it does not reorder words or
results. App labels and the text you type are shown unchanged.

- Eight `AppSearchTest` JVM cases passed, including Greek sigma and preserving
  distinctions such as Russian `й` and Japanese voicing. Both
  `DrawerSearchTest` cases passed on Android 8 and 15: accented labels matched
  in order, and a label refresh updated the results without rewriting the query.
- In the live drawer, mixed case and repeated spaces found **Custom Locale**
  on Android 8 and **YT Music** on Android 15. Clear restored the original
  visible order and search focus. Debug lint passed.

### Default launcher setup

Open **Vesna Launcher** from the current launcher, then choose **Set as default
launcher** in Vesna Settings. Android owns the selection. **Home options →
Default launcher** opens the same system selection when Vesna is not selected.
The old Home banner no longer covers shortcuts or consumes grid space.

- Android 15 opened the Home role chooser from Settings with D-pad input;
  cancellation kept the old default. Confirming through the Home menu's
  chooser selected Vesna, and reopening Settings showed the selected status.
  All 20 workspace and dock placement rows and the folder were unchanged.
- Android 8 opened Android's **Default apps → Home app** picker from Settings.
  The compact 20-shortcut Home preserved all its tiles, and tapping a shortcut
  in the formerly covered first row launched its app.
- Verified 48 dp targets, visible keyboard focus, Light and Dark, 320 dp and
  610 dp layouts, 1.6× text, English and Russian, right-to-left alignment, and
  scrollable About content. `DrawerSearchTest` passed on both emulators and
  lint reported no errors. TalkBack speech and physical devices were not checked.

### Appearance

**System** follows Android night mode; **Light** and **Dark** use the fixed
Vesna palettes even when Android uses the opposite mode. The app keeps the
user's wallpaper, installed app artwork, and workspace layout. Wallpaper labels
use the design system's dark backing; drawer and folder labels use the active
surface text color.

- On Android 8 and 15, exercised manual overrides against the opposite Android
  mode, restored the selected mode after stopping the process, and switched
  System live from Android Dark to Light. Returning from Settings updated an
  existing Home on Android 15. Workspace, dock, and folder records were unchanged.
- Checked the rendered Settings and drawer in both palettes, on compact and
  wide windows, with enlarged type, English and Russian labels, and RTL layout.
  On Android 15, verified keyboard focus plus D-pad selection, opaque folder
  surfaces, and both gesture and three-button navigation.
- Debug APKs built, lint passed, and `DrawerSearchTest` passed on both
  emulators. Physical devices, TalkBack speech, work-profile badges, and a
  bright/busy wallpaper matrix were not rerun for this change.

### Keep the reading position in Settings

- On signed 0.4.33, changing the system theme sent both the Settings
  directory and Appearance back to the top. It also lost the reading
  position after entering Appearance from a highlighted Theme result.
- On Android 15 at 200% text in short windows, the directory kept About
  at its exact scrolled bounds, and Appearance kept Show app names in
  place across Light and Dark, both from a direct link and after opening
  a search match. Opening a fresh link to the same page started at the
  top without the old highlight. No setting was changed.
- Both compact scroll regressions failed on 0.4.33. With the fix, 10
  focused tests passed on each of Android 8 and 15; debug build and lint
  passed. TalkBack speech and physical devices were not tested.

### App names

Turn **Show app names** off in Vesna Settings to hide app labels. The choice
survives restarts and defaults to on. It does not rename apps, change drawer
search, or hide user-created folder titles.

- Android 8 and 15 retained the same Home activity, icon and touch-target bounds,
  accessible app names, and saved placements after toggling. Android 8 kept
  labels off after a process restart and launched Clock from an unlabeled result.
- On Android 15, both folder members kept their accessible names with labels
  hidden. Restoring labels kept an active drawer query and updated the results
  and additional tiles brought into view by scrolling.
- Checked the switch in Light and Dark, 320 dp and 610 dp, English and Russian,
  up to 200% text, plus RTL alignment and visible keyboard focus with D-pad
  activation. Android 15 used gesture and three-button navigation. Debug lint
  and `DrawerSearchTest` passed; TalkBack speech and physical devices were not
  checked.

### Shortcut options

On a focused tile in the workspace, dock, app drawer, or an open folder, press
**Shift+F10** or the **Menu** key to open the same actions as touch long-press.
Android accessibility exposes those actions as **Shortcut options**. Keyboard
focus has a visible ring, and a non-touch action never starts a drag.

- Android 8: the Menu key opened workspace and dock options and Back returned
  focus to the tile. Android 15: Shift+F10 opened folder and drawer options.
  Back preserved the focused search result and its query; keyboard activation
  pinned that result to the current workspace page.
- Injected touch events still opened the touch-hold menu, moved a shortcut
  between workspace cells, and dragged a drawer result to Home. Only test
  placements were removed during cleanup; original placements were unchanged.
- Checked Light and Dark focus rings and popups, compact 320 dp and wide 610 dp
  layouts, English and Russian up to 200% text, RTL, and both navigation modes.
  Debug lint passed; `ShortcutActionsTest` and `DrawerSearchTest` passed on
  both emulators. TalkBack speech and physical devices were not checked.

### Profile-aware App info

**App info** on a workspace shortcut, dock shortcut, drawer entry, or
folder member opens Android Settings for that app's profile. Vesna uses
the selected entry's user serial and does not fall back to another profile.

- Android 15 with the same fixture app installed in Personal and Work:
  the previous build opened user 0's Settings from the Work Home shortcut.
  The fixed build opened Work Settings (user 10) from all four surfaces.
  Personal entries from the drawer and the same folder opened user 0.
- Android 8 opened personal Clock's App info from Home. Neither emulator
  needed Vesna to hold the default Home role. Debug lint passed. Physical
  devices and organization-managed profile policies were not tested.

### Accessible Work app names

App tiles on Home, in the dock, in the drawer, and in folders use Android's
localized profile label for accessibility. Visible captions and drawer
search still use the app's original name. Turning off **Show app names**
does not hide the accessible name or the visual profile badge.

- Android 15 exposed `QA Fixture` and `Work QA Fixture` as distinct
  accessible names when the same app was installed in both profiles.
  Verified Work names across all four surfaces, both copies together in
  the drawer and in a folder, and the drawer with visual captions hidden
  at 200% text. Searching for the accessibility-only prefix did not match
  the fixture.
- Android 8 retained the personal app name. The focused drawer regression
  for profile-only name updates and unchanged search passed on Android 8
  and 15; debug lint passed. TalkBack speech, physical devices, and custom
  organization-provided labels were not checked.

### Batched app catalogue refresh

Android can report several apps becoming available or unavailable at once.
Vesna now refreshes its catalogue once per nonempty batch. Real uninstall
events still retain the package and profile so only its own pins are removed.

- An Android 15 callback probe with 12 package names reduced refresh requests
  from 12 to one for each availability callback, with and without the
  replacing flag. Empty batches emitted no request. This measured requests,
  not refresh latency; a live multi-package OS event was not exercised.
- On Android 15, a real fixture appeared in the open drawer after installation.
  Updating and temporarily disabling it preserved its saved Home pin.
  Re-enabling it restored the search result; uninstall removed only the
  fixture pin. The search query and launcher process stayed active during the
  update, disable, enable, and uninstall checks. Debug lint passed.

### Shared app index for Home

The workspace, dock, and folders use one app lookup built when Vesna refreshes
its app catalogue. Changing pages or placements reuses that lookup. The drawer
still uses the discovery order, and shortcuts still match by component and
profile.

- With 500 fixture apps and ten Home page switches, the controller read
  15,000 catalogue entries on the prior build and none on the new build after
  the initial refresh, on both Android 8 and 15. This counts catalogue reads,
  not page-switch latency.
- The refresh regression kept Personal and Work copies separate and restored a
  temporarily unavailable Work copy to the workspace, dock, and folder without
  changing saved placements. All 15 page tests passed on each emulator, as did
  the 12 JVM resolver tests and debug lint. Real Home and drawer search rendered
  on both emulators; Android 15 also retained dock and folder order across page
  switches. The regression used simulated profile entries, not a live Work profile.

### Reuse dock and folder projections during page switches

Home page selection updates the grid without rebuilding the dock or folder
contents. Changes to the saved layout or installed apps still refresh those
surfaces.

- In a two-page fixture with four dock shortcuts and twelve three-member
  folders, ten switches previously rebuilt 40 dock shortcuts, 360 folder
  shortcuts, and ten folder lookup maps. The new build rebuilt none of those
  objects on Android 8 and 15. The probe counted rebuilt objects, not
  end-to-end latency.
- Five focused page and catalogue tests passed on both versions, including
  hidden and restored Work shortcuts without changing saved placements.
  Debug lint passed.
- On Android 15 in Light at 130% text, dock order and positions stayed fixed
  through two round trips between Home pages, and the folder reopened with
  Drive and Files in the original order. On Android 8 in Dark at 200% text,
  creating and removing a temporary page preserved the dock, and keyboard
  navigation kept focus on an enabled, named page control. Original layouts
  and settings were restored. TalkBack and physical-device timing were not
  tested.

### Dock reordering in RTL

Dock drag targets now use the same padded columns and layout direction as
the visible icons. Mirroring the dock does not change the stored order.

- Android 15 Light, 1.3× text and gesture navigation: dragging Calendar from
  the rightmost to the highlighted leftmost slot left it on the right in the
  previous build. The fixed build moved it to the left and saved it as the
  final dock item. An LTR drag restored the original order.
- Android 8 Dark, 320 dp, 2× text and three-button navigation: LTR drag and
  restoration both worked. Dock tiles stayed named, keyboard-focusable,
  and at least 48 dp on both emulators. The two dock geometry tests failed
  on the baseline and passed after the fix on both, covering RTL and LTR,
  padded tile edges, and an empty highlight slot. Debug lint passed;
  TalkBack speech and physical devices were not checked.

### Dock drop previews

The dock previews the position a dropped icon will actually occupy. In a
partial dock, trailing space targets the last visible position when reordering
or the next open position when adding. Hidden but saved pins still use capacity.

- On Android 15, dragging one of three icons over the fourth column highlighted
  the fourth column before the fix but saved the icon in the third. The fixed
  reorder highlighted the third column. Inserting into a two-icon dock also
  highlighted and filled the third column. A Dark RTL reorder verified the
  mirrored result.
- A drawer drag filled the fourth saved slot. Disabling that test app hid its
  icon without freeing its slot; another drop showed no insertion highlight
  and changed no saved rows. Re-enabling the app restored the same pin.
- All 21 focused instrumentation tests passed on Android 8 and 15. The 15
  focused JVM tests and debug lint passed. Physical devices and TalkBack were
  not checked.

### Atomic dock insertion

A drawer drop now writes the new dock pin and the shifted slots in one Room
transaction. If a slot update fails, the saved dock stays unchanged.

- An in-memory SQLite trigger aborted the last slot shift on Android 8 and 15.
  The previous build left the incoming pin at slot zero. The fixed build
  preserved every original row and completed the same drop once the trigger
  was removed. All five dock repository tests and debug lint passed.
- On Android 15, a real drawer drop filled the second slot and shifted the
  existing icons without replacing their saved pins. The original fixture
  layout was restored afterward.

### Atomic uninstall cleanup

When an uninstall leaves a folder empty or with one member, Vesna saves the
pin removal and the resulting folder layout together. This covers both live
package events and startup cleanup.

- On Android 8 and 15, forcing folder deletion to fail left partial placements
  on the prior build. The fixed build rolled back every affected row. Retrying
  removed only the uninstalled profile's pins and returned the remaining app
  to the folder's original cell on its original page. The six focused package
  and folder tests passed on both versions; debug lint passed.
- On Android 15, uninstalling a test app while Vesna was running or force-stopped
  dissolved the two-app folder and restored Camera to its original slot. All
  original fixture placements were preserved.

### Exact folder shortcut removal

**Remove from folder** moves the selected shortcut, even when a folder contains
another copy of the same app. The tile and its menu use the saved pin ID; an
outdated action cannot move a different member.

- On Android 15, removing the third tile from `[QA Fixture, Camera, QA Fixture]`
  moved the first copy on the prior build. The fixed build moved the third
  copy to the first free cell on its Home page and left `[QA Fixture, Camera]`
  in the folder. No other placement changed.
- The native replay passed with touch in light mode at 130% font scale and
  with keyboard focus and the Menu key in dark mode at 200%. The shortcut
  targets were at least 68 × 86 dp and held their full accessible names;
  screenshots matched the Vesna menu and dialog color roles. TalkBack speech
  was not tested.
- Seven folder projection tests, six focused instrumentation tests on each of
  Android 8 and 15, and debug lint passed. The original emulator layouts and
  settings were restored.

### Feedback when a Home page cannot fit folder shortcuts

If a page cannot fit the shortcuts, **Remove from folder** and **Remove folder**
leave the layout untouched. Vesna explains how to make room on the surface
where the action started. An open folder stays open when removal fails.

- On Android 15 with all 20 cells occupied, both actions previously failed
  silently, and **Remove folder** closed the dialog. The fixed build showed
  the message from both dialog actions and the Home folder menu. Removing one
  Home shortcut let the same two-member folder be removed; the members took
  the freed cell and the old folder cell.
- In Light at 130% and Dark at 200%, the full message and folder controls were
  visible, shortcut names stayed accessible, and the keyboard Menu key opened
  the selected shortcut's actions. The displayed snackbar colors matched the
  Vesna inverse roles. TalkBack speech and the Russian layout were not tested.
- Six focused tests passed each on Android 8 and 15. They covered preserving
  every saved row on a full secondary page and retrying both actions after
  freeing space. Debug lint passed.

### Read Home folders and shortcuts together

Home reads saved folders and shortcuts in the same Room transaction before
rendering either. Grouping or removing a folder can no longer combine newly
saved memberships with an older list of folders, or vice versa.

- The previous build emitted an empty grid on Android 8 and kept the loose
  shortcuts alongside new folder metadata on Android 15. The fixed build
  emitted only complete layouts through 12 group-and-remove cycles on each
  version. This checked UI state emissions, not individual display frames.
- On Android 15 in Light at 130% and Dark at 200%, dragging a test shortcut
  onto Camera created a two-member folder. Renaming updated the open dialog
  without changing its members. Uninstalling the test app closed the dialog
  and restored the original Camera pin and dock. The dialog retained full
  accessible shortcut names and at least 48 dp targets; Dark keyboard focus
  was visible. TalkBack speech was not checked.
- Eight focused tests passed on each Android version, as did debug lint.
  No database schema change was needed.

## Where to go next

- Completed since the baseline: [VESNA-QA-002](verification/VESNA-QA-002.md) —
  HOME registration/selection and launch-return device-verified on the
  dedicated API 35 AVD (a Vesna platform baseline item; no approved research ID).
- Completed since the baseline: [VESNA-QA-003](BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial)
  — installed app lifecycle verified at API 35, including a work-only
  uninstall and retention of workspace, dock, and folder pins while
  a personal-profile app is disabled.
- Completed since the baseline: [VESNA-QA-005](verification/VESNA-QA-005-API26.md)
  — minimum SDK (API 26) smoke on the dedicated API 26 AVD: build, HOME
  discoverability, cold start, workspace/drawer render, one fixture drawer
  round-trip, and the full instrumentation suite 3/3 (per-case JUnit XML in
  the record's evidence index). The layout fix for VESNA-BUG-004 is recorded
  in [PR #23](https://github.com/vladon/vesnalauncher/pull/23).
- Completed since the baseline: [VESNA-QA-004](BACKLOG.md#vesna-qa-004-migration-and-restart-verification-done)
  — Room migrations (`RoomMigrationQaTest`, PR #13, merge `14ec712`) and
  persistence (process death, full guest reboot, no re-seed;
  [VESNA-QA-004B](verification/VESNA-QA-004B.md)) device-verified on the
  dedicated API 35 AVD as two separate experiments.
- Prioritized plan: [ROADMAP.md](ROADMAP.md).
- Requirement-level pointers: [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).
