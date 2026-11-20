# Engineering Backlog

The single authoritative list of engineering work items. Ownership of content:

- Item definitions and status live **here only**.
- Sequencing and dependencies live in [ROADMAP.md](ROADMAP.md).
- Research-ID correspondence and verification state live in
  [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).

Status vocabulary: **proposed** (defined, not yet approved for implementation),
**ready** (approved, may start when its dependencies are green),
**in-progress**, **done** (with a link to the verifying run or commit).
Listing an item here implies no approval and no product/privacy decision; gates
are named explicitly.

---

## VESNA-QA-002: HOME and launch-return verification (done)

**Status:** done — device run recorded 2026-11-18 on the dedicated API 35 AVD;
full reproducible record:
[verification/VESNA-QA-002.md](verification/VESNA-QA-002.md). The Android 8
(API 26) legacy follow-up — Settings chooser selection and the
Calculator → HOME return replay — is appended to the same report as
[VESNA-QA-002B](verification/VESNA-QA-002.md#api-26-follow-up-vesna-qa-002b)
(2026-11-19, [PR #37](https://github.com/vladon/vesnalauncher/pull/37)).
**Priority:** P0 (first item). **Depends on:** nothing; reuses the **existing
dedicated API 35 AVD** — no new device work is required to start.
**Research:** none — this is a Vesna platform baseline item and carries **no
approved research ID**.

Verify on the dedicated Vesna AVD (the stock launcher may stay installed;
removing it is not required):

1. Vesna registers as a HOME launcher: the system offers it in the home-app
   selection (`RoleManager` role request on API 29+, home settings on older).
2. After selection, Vesna is the default home: pressing HOME from any app
   returns to the Vesna workspace.
3. Launch an app from workspace/dock/drawer, press HOME, and the workspace is
   restored as left (drawer closed per `singleTask`/`onNewIntent` behavior).
4. **Cleanup restores the previous HOME holder** — the device is returned to the
   HOME selection it had before the run.

Record what "HOME works" means for the 0.4.1 product baseline **first**, since
no research scenario covers this: write down the acceptance assertions, then
automate or script them. On completion, add the run record link to
[REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md#home-role-and-launching).

<a id="vesna-qa-003-package-lifecycle-verification-partial"></a>

## VESNA-QA-003: package lifecycle verification (done)

**Status:** done — includes disabled-app pin retention verified on API 35 by
[VESNA-QA-003F](verification/VESNA-QA-003F.md). The earlier drawer-side run
(2026-11-19, dedicated API 35 AVD) is recorded as **VESNA-QA-003A**:
[verification/VESNA-QA-003.md](verification/VESNA-QA-003.md). Install →
drawer, same-package update, disable/re-enable, and uninstall (incl. launcher
process restart and uninstall-while-dead) passed. After
[PR #15](https://github.com/vladon/vesnalauncher/pull/15) restored drawer drops,
[VESNA-QA-003B](verification/VESNA-QA-003B.md) verified pinning, upgrade
retention, and folder cleanup on uninstall on API 35. Disabling an app removed
its workspace and dock placements. Re-enabling it restored drawer discovery.
[VESNA-QA-003C](verification/VESNA-QA-003C.md) (2026-11-19, API 35) verified
the managed-profile case end to end: a shell-provisioned work profile's
fixture was discovered in the drawer, a drawer-drag pin persisted with the
profile's `userSerial`, launching opened the fixture in the work profile, and
a work-only uninstall deleted exactly the work-serial placements while the
personal package and pin survived, stable across a launcher restart.
[VESNA-QA-003D](verification/VESNA-QA-003D.md) (2026-11-19, API 35) verified
uninstall with simultaneous workspace, dock, and folder placements: all three
fixture rows left within seconds, the folder dissolved, and a cold relaunch
showed no ghost entries. The dock folder-capacity defect found in this line
(VESNA-BUG-003) was fixed in [PR #26](https://github.com/vladon/vesnalauncher/pull/26)
and device-verified in [VESNA-QA-003E](verification/VESNA-QA-003E.md)
(2026-11-19, API 35). Disabled apps now keep their stored workspace and dock
positions and folder memberships. They reappear on re-enable; uninstall
still removes pins for the affected profile.
Fixture methodology is recorded at
`D:/orca/scratch/vesna-qa-003/` (hand-built debug APKs: same
package/component/signature, two versionCodes, no permissions).
**Priority:** P0. **Depends on:** the AVD inventory from
[VESNA-QA-002](#vesna-qa-002-home-and-launch-return-verification-done).

Verify the `PackageChangeMonitor` → stale-cleanup path end to end:

- Uninstall an app pinned to workspace, dock, and a folder: all entries leave the
  UI **and** the database (no ghost cells after restart).
- Update an app (same package, new version) in place: entries and order survive
  (`PackageEvent.Replaced` path).
- Disable an app with `pm disable-user`: it is hidden without changing its
  pins or folder memberships, including across a launcher restart.
  Re-enabling restores its icons.
- Repeat at least one case across a managed profile if the AVD supports one
  (also feeds the managed-profile row in
  [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md#data-and-platform-behaviors-without-research-ids)).

## VESNA-QA-004: migration and restart verification (done)

**Status:** done. Separate API 35 runs verified historical migrations and
restart behavior. **Part A,
migrations:** `RoomMigrationQaTest` (merged as
[PR #13](https://github.com/vladon/vesnalauncher/pull/13), merge `14ec712`)
seeds authentic v1 and v2 schemas into isolated on-device test databases and
migrates them through the unmodified production `LauncherDatabase.build`; the
coordinator's independent run executed both cases plus `DrawerSearchTest` on
the dedicated API 35 AVD (2026-11-19). **Part B, persistence:** process death,
full guest reboot, and no re-seed of an emptied workspace — reproducible
record: [verification/VESNA-QA-004B.md](verification/VESNA-QA-004B.md)
(2026-11-19, tested build `0c7269c`, 0.4.1/6).
**Priority:** P0. **Depends on:** AVD inventory.

- Room 1→2 and 2→3 migrations (`MIGRATION_1_2`, `MIGRATION_2_3`): run against
  representative v1 and v2 databases; layout, folders, and per-profile
  `userSerial` values survive intact (part A).
- The persisted placements and icon bounds matched after force-stop and
  relaunch (part B).
- The persisted placements and icon bounds matched after reboot. An emptied
  layout stayed empty (part B).

## VESNA-QA-005: minimum SDK (API 26) smoke verification (done)

**Status:** done — narrow smoke run recorded 2026-11-19 on the dedicated
API 26 AVD (`Vesna_QA_MinSdk_API_26`, Android 8.0, 1080×1920); reproducible
record: [verification/VESNA-QA-005-API26.md](verification/VESNA-QA-005-API26.md).
Scope: build (debug + androidTest), HOME discoverability, explicit cold start
with workspace and drawer render, one fixture drawer round-trip
(discover → launch → uninstall); no default-HOME change, no deep drawer or
workspace editing. The full existing instrumentation suite passed 3/3 on
API 26 — `RoomMigrationQaTest` (both cases) and `DrawerSearchTest`; the
per-case JUnit XML (`TEST-Vesna_QA_MinSdk_API_26-AVD-8.0.0.xml`) is listed in
the record's evidence index. The run exposed VESNA-BUG-004, where the dock
covered the fifth workspace row. [PR #23](https://github.com/vladon/vesnalauncher/pull/23) fixed the overlap and was replayed
on API 26 and API 35. The follow-up on the full eight-test suite — all eight
instrumentation tests passed on API 26 at build `4c5f0c1` — is appended to
the same report as
[VESNA-QA-005B](verification/VESNA-QA-005-API26.md#follow-up-on-the-eight-test-suite)
(2026-11-19, [PR #36](https://github.com/vladon/vesnalauncher/pull/36)); the
original smoke record above predates that suite.
**Priority:** P0. **Depends on:** the AVD inventory from
[VESNA-QA-002](#vesna-qa-002-home-and-launch-return-verification-done) (plus
the dedicated API 26 AVD).

## Engineering work items

### ENG-101: Workspace page identities and schema migration

Stable page identifiers and the additive Room 3→4 migration follow
[ADR-PAGE-1](../adr/0001-workspace-page-identity.md). The launcher still
shows a single 4×5 default page. Page-management interactions remain a separate
product decision; `HOME-104` is still a research candidate.

### ENG-102: Widget host — lifecycle only

Add/update/remove widgets; host cleanup on package removal and after restore.
Blocked by **ADR-WIDGET-1** and by ENG-101 (widgets occupy grid cells, so the
page identity model comes first). Out of scope until the research `WIDGET-100`
placement/provider config-cancel gap is resolved; `WIDGET-102` (removal/undo)
is the removal-side counterpart.

### ENG-103: Folder pagination and drag-into verification

Pagination is the implementation work: folder contents beyond one screen are
not implemented. Drag-into-folder already exists in source — drops onto a folder
cell route through `LauncherController.addToFolder` in two `HomeActivity` paths —
so the drag-into half of this item is **verification of the existing UI flow**,
not implementation, and folds naturally into the on-device QA pass. Research is
the gate for both: the `FOLDER-100`/`FOLDER-101` scenarios are missing
([RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#known-weaknesses-in-the-research-matrix)).
No hard dependency on page identities is assumed; if
[ENG-101](#eng-101-workspace-page-identities-and-schema-migration) work
establishes one, record it in this item with evidence.

### ENG-104: Settings search and structured preferences

Implement settings search/deep-linking (research `SETT-110`), pairing the
missing research scenario ([ENG-107](#eng-107-research-matrix-fix-ups-research-branch-not-trunk))
with a trunk settings structure to hang it on. Note: settings **reset** is not
part of this item — it belongs to the backup/reset policy
([ENG-106](#eng-106-backuprestore-policy), research `BACKUP-101`).

`NT-SETTING-01` implements clean Settings entry, named page links, and
keyboard-first Back for the current destinations. See the
[Android verification](verification/VESNA-QA-SETTINGS-NAVIGATION.md).
The broader `SETT-110` work for breadcrumb results and highlighting individual
controls remains separate from the destination search verified there.

### ENG-105: Cards surface

`CARDS-100` is condition-triggered content. Blocked on the **product/privacy
decision** — the location/manual-fallback handling is the open privacy question,
and notification indicators are a separate item. The `DRAWER-105`/`R-DRAWER-103.1`
ID reuse must be repaired in research first; contact opt-in (`R-DRAWER-103.1`)
stays an independent item, separate from the cards conflict. No planning beyond
the placeholder until the decision exists
([RESEARCH_HANDOFF.md](RESEARCH_HANDOFF.md#known-weaknesses-in-the-research-matrix)).

### ENG-106: Backup/restore policy

Resolve the backup/privacy policy via **ADR-BACKUP-1**: the ADR may conclude to
**keep platform backup enabled and define its scope** — an explicit in-app
backup/restore feature is one option, not a requirement. Today the manifest
sets `allowBackup="true"` without a feature; that is a default, not a decision.
Settings **reset** also lands here (research `BACKUP-101`, partial coverage);
it needs its research scenario completed via
[ENG-107](#eng-107-research-matrix-fix-ups-research-branch-not-trunk). Related
research gap: `BACKUP-102` has no dedicated scenario.

### ENG-107: Research-matrix fix-ups (research branch, not trunk)

Author the missing scenarios the matrix claims as complete (`HOME-104`,
`DRAWER-104`), resolve `DRAWER-102` Back/IME ambiguity, repair the `DRAWER-105`
ID reuse of `R-DRAWER-103.1` (contact opt-in stays independent), add
`SETT-110`/`BACKUP-102` scenarios and complete `BACKUP-101` reset coverage,
resolve the `WIDGET-100` placement/provider config-cancel gap and specify
`WIDGET-102` removal/undo. Done on `vladon/vesna-research`; trunk items
un-block as each lands.

## Candidate items (unapproved)

Initial product directions kept as candidates only — no approval implied; each
needs research scenarios (via [ENG-107](#eng-107-research-matrix-fix-ups-research-branch-not-trunk))
and an explicit approval before becoming engineering work:

| ID | Candidate | Notes |
|---|---|---|
| CAND-201 | Appearance/theming | Themes, icon customization; research `appearance` spec at the pinned commit |
| CAND-202 | Essential gestures | Drawer-open settings, gesture assignment; blocked on live binding-value validation (research follow-up `ee15ed5`) |
| CAND-203 | Icon packs | Depends on appearance direction |
| CAND-204 | Extended dock/grid/folder capacity | More slots/rows/folder sizes; interacts with `DOCK-100`/`FOLDER-*` research gaps |
| CAND-205 | Accessibility | Content descriptions, focus order, TalkTeX-style navigation audit |
| CAND-206 | Foldables | Multi-window/large-screen layout behavior of the fixed 4×5 grid |
| CAND-207 | Work profiles | UX depth beyond current badged-app support; relates to the managed-profile verification gap |

---

## ADR candidates

ADR drafts to write before the dependent work starts. Each becomes an
`docs/adr/NNNN-*.md` entry when accepted (directory to be created by the first
ADR).

### ADR-PAGE-1: Workspace page identity and its migration

Accepted for storage and migration: [ADR 0001](../adr/0001-workspace-page-identity.md)
defines stable page IDs, page-local cells, and Room schema version 4.

### ADR-WIDGET-1: Widget host cleanup

AppWidgetHost lifecycle: allocation, `deleteHost`/binding cleanup on package
removal, behavior across restore and reboot. Gates ENG-102.

### ADR-BACKUP-1: Backup and privacy policy

[ADR 0002 — Backup and privacy](../adr/0002-backup-and-privacy.md) drafts scoped
Android device-to-device transfer only. No cloud backup, analytics, tracking,
or in-app export. Implementation remains gated on review and transport tests.

---

## Done

| Item | Evidence |
|---|---|
| Drawer search QA baseline (`R-DRAWER-100.4` order-preservation subset, `R-DRAWER-101.1` local-filtering subset) | `DrawerSearchTest` added in PR #9 and strengthened by [VESNA-QA-008 (PR #34)](https://github.com/vladon/vesnalauncher/pull/34). The updated suite passed 8/8 on the dedicated API 35 AVD. |
| VESNA-QA-004 migration and restart verification | Part A: `RoomMigrationQaTest` ([PR #13](https://github.com/vladon/vesnalauncher/pull/13), merge `14ec712`), both migration cases plus `DrawerSearchTest` on the dedicated API 35 AVD; part B: [VESNA-QA-004B](verification/VESNA-QA-004B.md) (force-stop, full guest reboot, no re-seed) — see [PROJECT_STATUS.md](PROJECT_STATUS.md#instrumentation-verification-record) |
| VESNA-QA-005 minimum SDK (API 26) smoke verification | [VESNA-QA-005-API26](verification/VESNA-QA-005-API26.md) (2026-11-19, dedicated API 26 AVD): build, HOME discoverability, cold start, workspace/drawer render, fixture round-trip, instrumentation suite 3/3; VESNA-BUG-004 fixed in [PR #23](https://github.com/vladon/vesnalauncher/pull/23) |
| VESNA-QA-006 Android 16 (API 36.1) smoke verification | [VESNA-QA-006-API36-1](verification/VESNA-QA-006-API36-1.md) (2026-11-19, dedicated API 36.1 AVD on Windows): instrumentation 5/5 at `332fb03` and 7/7 with the integrated BUG-005 candidate `d8d6a748` (later merged as [PR #27](https://github.com/vladon/vesnalauncher/pull/27)); HOME/launch-return smoke; scope limited to that AVD configuration |
| VESNA-QA-007 debug cold-launch timing baseline | [VESNA-QA-007-COLD-START](verification/VESNA-QA-007-COLD-START.md) (2026-11-19, PR #33): ten process-cold `HomeActivity` launches, debug 0.4.1 at `81cea18`, Android 15/API 35 shared-host emulator; see the run record for ActivityManager `TotalTime` observations |
| VESNA-QA-009 launch-state timing verification | [VESNA-QA-009-LAUNCH-STATES](verification/VESNA-QA-009-LAUNCH-STATES.md) (2026-11-19, [PR #40](https://github.com/vladon/vesnalauncher/pull/40)): paired cold and warm ActivityManager timings for one Android 15 debug-emulator configuration |
| VESNA-QA-010 Android 12 (API 31) smoke verification | [VESNA-QA-010-API31](verification/VESNA-QA-010-API31.md) (2026-11-19, [PR #43](https://github.com/vladon/vesnalauncher/pull/43)): 12/12 instrumentation tests on the dedicated API 31 AVD at the debug `75ca3ed` candidate from [PR #39](https://github.com/vladon/vesnalauncher/pull/39); workspace/drawer render; HOME-registration check kept the stock HOME; boot fallback and teardown in the record. The follow-up passed 16/16 on the same API 31 AVD at `1b5dc2e`. |
