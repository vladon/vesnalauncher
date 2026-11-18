# VESNA-QA-003C — managed-profile package lifecycle verification

**Result:** PASS. On the dedicated disposable AVD a managed profile was created
and started with shell commands only (no DPC). A work-profile-only fixture was
discovered in Vesna's drawer, pinned by drawer drag, and its persisted row
carried the managed profile's serial. Launching the pinned icon opened the
fixture's activity in user 10 via `LauncherApps.startMainActivity`. With a
personal copy also staged, uninstalling the package from the work profile only
deleted exactly the work-serial placements while the personal placement and
drawer entry survived, stable across a launcher restart. Teardown removed the
task's profile and fixture. User 0 remained the only user, and the final
SQLite row dump matched the baseline. Run recorded 2026-11-19.

**Backlog:** [VESNA-QA-003](../BACKLOG.md#vesna-qa-003-package-lifecycle-verification-partial).

**Scope:** QA of existing behavior only; no product change, no research
approval claimed, no shared status edited, no commits. Only this file and
artifacts under `D:/orca/workspaces/vesnalauncher/qa-evidence/qa003c/` were
written. No accounts were enrolled, no physical device touched, no DPC
installed, no other user or app removed.

**Code under test:** current main after PR #17 — commit
`e392f6bc3645b1e9730ac773d240841873a73ebe` ("docs: record placed-app package
lifecycle verification (#17)"), clean checkout in the `qa-managed-profile`
worktree. `app-debug.apk` built from this checkout
(SHA-256 `89d6dde2d0d40d059b80d00f5066ae1c292aa97d54a8eb98df08eaa752c81a30`)
and installed with `install -r` over the pre-staged build.
**Package under test:** `dev.vladon.vesna.launcher`, `versionName` 0.4.1,
`versionCode` 6.

## Environment

| Property | Value | Source |
| --- | --- | --- |
| Serial | `emulator-5560` (pinned on every adb call) | `adb devices` |
| AVD name | `Vesna_QA_Menu_API_35` | `emu avd name` |
| Image | sdk_gphone64_x86_64, API 35, Android 15, `emu64xa:15/AE3A.240806.043/12960925:userdebug` | `getprop` |
| Users before run | `UserInfo{0:Owner:4c13} running` — user 0 only | `pm list users` |
| Users after teardown | `UserInfo{0:Owner:4c13} running` — user 0 only | `pm list users` |
| HOME intent resolution (before/after) | `com.google.android.apps.nexuslauncher/.NexusLauncherActivity` | `cmd package resolve-activity` with the MAIN action and HOME category |
| Device timestamps | device-local as reported by the device (`lastUpdateTime=2026-11-18 22:27:05` corresponds to the 2026-11-19 ~01:27 host-side install) | `dumpsys package` |

Every adb command in this run pinned the serial (`-s emulator-5560`); no other
device, AVD, or worktree was touched.

## Fixture validation (before any install)

Reusable signed fixture `D:/orca/scratch/vesna-qa-003/fixture-v1.apk`
(SHA-256 `f6fb89981616580d8485a728449020fc8236b1386c90901119ec47755eefcebf`):
package `dev.vladon.qa.lifecyclefixture`, `versionCode` 1, `versionName` 0.1,
launchable `dev.vladon.qa.lifecyclefixture.MainActivity`. Signer certificate
SHA-256 `582314987823bd90e8e66af56e5fa32d95528a368c85d3cbe0095949a80a82e4` —
identical for the fixture, the freshly built launcher APK, and the previously
installed launcher base.apk (`2c07c384374eddb5ba3f02bb1b7ee06fe05ccff0610e8d18dde01b13070b9745`).
Label as discovered by the launcher: **"QA Fixture"**.

## Profile provisioning (shell only)

```bash
adb -s emulator-5560 shell pm create-user --profileOf 0 --managed VesnaQaWork
# → Success: created user id 10
adb -s emulator-5560 shell am start-user 10
# → Success: user started
```

- `pm list users`: `UserInfo{10:VesnaQaWork:1020}` before start, `…1030`
  running after start (0x20 = managed profile flag set).
- Authoritative serial: `dumpsys user` →
  `UserInfo{10:VesnaQaWork:1030} serialNo=10 isPrimary=false parentId=0`.
  (`am get-serial-number --user` and a `cmd user` serial subcommand do not
  exist on this image; see command failures.)
- No DPC installed; plain shell provisioning was sufficient for
  `LauncherApps.profiles` discovery from user 0.

## Baseline (before any fixture change)

`run-as dev.vladon.vesna.launcher sqlite3 -readonly databases/vesna-launcher.db`:
**19 `workspace_items` rows, 0 folders** — dock `calendar 0, camera2 1,
chrome 2, deskclock 3`; workspace cells 0–8 and 10–15 occupied, **cell 9
(row 2, col 1) and all of row 4 (cells 16–19) free**. Note: this AVD's stock
layout differs from the QA-003B device (there cells 14/15 were free); the grid
is 4 columns × 5 rows (`GridCanvas`: cells 0–19, dock = 4 slots).

Foreground enabler used throughout:
`am start -W -n dev.vladon.vesna.launcher/.ui.HomeActivity`. HOME role was
never modified.

## Case 1 — work-only fixture is discovered in the drawer

Fixture installed into the managed profile only:

```bash
adb -s emulator-5560 push "$(cygpath -w …/fixture-v1.apk)" /data/local/tmp/qa003c-fixture-v1.apk
adb -s emulator-5560 shell pm install -r --user 10 /data/local/tmp/qa003c-fixture-v1.apk
```

`pm list packages --user 10` lists the fixture; `--user 0` does not. After
`am start` of Vesna, the drawer (flung open from an empty workspace area)
lists exactly one **"QA Fixture"** at label bounds `[555,1779][795,1822]`
(artifact `05-drawer-workonly.xml/.png`). **PASS** — `launcherApps.profiles`
discovery reaches the managed profile without any DPC.

## Case 2 — drawer drag pins the work copy; serial matches the profile

Continuous injected gesture (per QA-003B recipe): `DOWN 675 1800` (label
center) → 1.0 s long-press arm → five `MOVE` steps → dwell 0.4 s →
`UP 409 1228` (free cell 9). Drawer closed mid-gesture; drop committed:

```text
22|dev.vladon.qa.lifecyclefixture|dev.vladon.qa.lifecyclefixture.MainActivity|WORKSPACE|9|10|
```

The trailing `userSerial=10` equals the managed profile's `serialNo=10`
(`parentId=0`). **PASS.**

Cross-profile launch (bonus evidence): tapping the pinned icon put
`ActivityRecord{… u10 dev.vladon.qa.lifecyclefixture/.MainActivity}` on top
(`startMainActivity` path for serial ≠ 0); BACK returned to Vesna
(`07-launch-work-fixture.png`).

Restart persistence: `am force-stop` + relaunch → row 22 unchanged, drawer
entry intact (`08-restart-persistence-workpin.txt`). **PASS.**

## Case 3 — coinciding labels: staging the personal copy

`pm install -r --user 0` of the same fixture; the drawer then shows two
"QA Fixture" entries. The labels coincide, so each entry was identified from
rendered profile badges (read-only visual state; the launcher badges icons via
`getBadgedIcon`):

| Drawer node (label bounds) | Icon | Identity |
| --- | --- | --- |
| `[555,1779][795,1822]` (center 675,1800) | plain robot | **personal copy (user 0)** |
| `[825,1779][1065,1822]` (center 945,1800) | robot + briefcase badge | **work copy (user 10)** |

Zoom evidence: `09-badge-zoom-left-vs-right.png`, `11-badge-zoom-retry.png`.
Dragging the plain entry to free cell 16 (UP 150,1980) pinned a second row:

```text
22|…|WORKSPACE|9|10|   ← work copy (unchanged)
23|…|WORKSPACE|16|0|   ← personal copy
```

**PASS** — same package, two profiles, two independent placements. The first
attempt of this drag dropped at y=2116, 9 px inside the dock band
(`dock.top=2107`, "The dock is full" toast, no row created) — QA technique
error, corrected by reading the drop-resolution source (`wy >= dock.top`);
see command failures.

## Case 4 — work-only uninstall is scoped by profile

```bash
adb -s emulator-5560 shell pm uninstall --user 10 dev.vladon.qa.lifecyclefixture
# → Success
```

Observed 2.5 s after the uninstall (`PackageEvent.Removed(pkg, serial=10)` →
`removeAllForPackage(pkg, 10)`):

- `pm list packages --user 10`: fixture gone; `--user 0`: fixture still present.
- DB: **row 22 (work, serial 10) deleted; row 23 (personal, serial 0)
  retained** — same-package cleanup did not leak across profiles.
- Drawer: exactly one "QA Fixture" (plain personal entry at `[555,1779]`);
  the cell-16 pin remains, plain icon (`12-…`, `13-cell16-pin-zoom.png`).

Restart persistence (`am force-stop` + relaunch): only row 23 remains; one
drawer entry (`13-restart-after-uninstall.txt`, `13-drawer-after-uninstall.*`).
**PASS** — the work profile's pinned state and drawer row are removed and stay
removed; the personal copy is untouched.

## Teardown (actual end state)

The commands targeted the disposable AVD and the user ID returned by profile
creation. They removed the personal fixture, removed profile 10, and brought
NexusLauncher to the foreground.

```bash
adb -s emulator-5560 shell pm uninstall --user 0 dev.vladon.qa.lifecyclefixture
adb -s emulator-5560 shell pm remove-user 10
adb -s emulator-5560 shell am start -n com.google.android.apps.nexuslauncher/.NexusLauncherActivity
```

- `pm list users` → `UserInfo{0:Owner:4c13} running` — **user 0 only**; no
  other user existed, none deleted.
- Foreground → stock `NexusLauncherActivity` (`topResumedActivity`); HOME
  resolution unchanged (nexuslauncher).
- Final DB: `SELECT COUNT(*)` → **19 items / 0 folders**; full row dump
  diffed against the pre-run baseline: **empty diff** (`16-db-baseline-diff.txt`).
- No app data cleared, no settings changed, Vesna left installed and
  non-HOME as found.

## Command failures encountered (captured verbatim in artifacts)

1. `adb push /d/orca/scratch/…` — MSYS path not translated by adb.exe
   ("cannot stat"), first fixture install failed; recovered with
   `$(cygpath -w …)` (per runbook), one retry.
2. `am get-serial-number --user 10` → "Unknown command: get-serial-number";
   `cmd user` offers no serial query — replaced with `dumpsys user`
   (`serialNo=10 parentId=0`).
3. `cmd role holders android.app.role.HOME` → "Unknown command: holders" —
   replaced with `cmd package resolve-activity` HOME proxy.
4. Drawer-open swipes starting on the dock row (y=2300) did not open the
   drawer (dock children consume the touch); a fling from an empty workspace
   area (540,2100 → 540,500) worked.
5. Case-3 first drag: UP at (150,2116) resolved as a dock drop
   (`dock.top = 2107`, toast "The dock is full", no row written); recovered by
   aiming inside the row-4 band (150,1980). One retry.

No shell-provisioning failure occurred, so no bounded recovery or DPC question
was needed. No product defects found in the tested lifecycle paths.

## Hygiene

No DB edits (all queries `run-as … sqlite3 -readonly`), no app data clears, no
source or build-config changes, no shared status edited, no GitHub writes.
Transient device dumps (`/sdcard/qa003c-drawer.xml`, `/sdcard/x.xml`, pushed
APK) deleted; no transient files left in the worktree.

## Artifacts (`D:/orca/workspaces/vesnalauncher/qa-evidence/qa003c/`)

| File | Content |
| --- | --- |
| `00-emulator-identity.txt` | AVD name, props, fingerprint, users before, device list |
| `01-apk-and-fixture-identity.txt` | signer certs + SHA-256 of built/installed/fixture APKs, fixture badging |
| `02-profile-provision-and-install.txt` | create-user output (id 10), start-user, install -r, users after |
| `03-serial-baseline-fixture-workinstall.txt` | `dumpsys user` serial evidence, DB baseline (19 rows), first fixture-install failure + recovery |
| `04-ui-start.txt`, `04-workspace-pre.png` | Vesna cold start, pre-change workspace |
| `05-drawer-workonly.png/.xml` | drawer listing work-only fixture |
| `06-workspace-pinned-work.png/.xml` | pinned work copy at cell 9 (label `[305,1207]`) |
| `07-launch-work-fixture.png` | fixture activity running in u10 |
| `08-restart-persistence-workpin.txt/.png/.xml` | row 22 stable after restart |
| `09-personal-copy-staged.txt`, `09-drawer-two-copies.*`, `09-badge-zoom-left-vs-right.png` | two entries, badge identification |
| `10-two-rows-pinned.txt`, `10-workspace-two-pins.png` | rows 22/23 coexisting |
| `11-drawer-reclosed-retry.xml`, `11-drawer-retry.png`, `11-badge-zoom-retry.png` | pre-retry state re-check |
| `12-workonly-uninstall.txt`, `12-workspace-after-work-uninstall.png` | scoping evidence after `pm uninstall --user 10` |
| `13-restart-after-uninstall.txt`, `13-drawer-after-uninstall.*`, `13-cell16-pin-zoom.png` | post-uninstall restart persistence |
| `14-workspace-personal-only.png`, `14-cell16-pin-zoom.png` | surviving personal pin before teardown |
| `15-teardown.txt` | uninstall, remove-user, users after, foreground restore, tmp cleanup |
| `16-final-state.txt`, `16-db-baseline-diff.txt` | final DB/users/HOME; empty baseline diff |
