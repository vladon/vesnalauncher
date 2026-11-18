# VESNA-QA-002 — HOME registration/selection and launch-return verification

**Verdict:** PASS — all four acceptance behaviors from the backlog item were
observed on the dedicated QA device. Run recorded 2026-11-18.

**Item:** [VESNA-QA-002 in BACKLOG.md](../BACKLOG.md#vesna-qa-002-home-and-launch-return-verification-done)
(P0, Vesna platform baseline, no approved research ID — QA of existing
behavior only; no product change, no research approval claimed).
**Code under test:** commit `902c1f6` (`902c1f60b078dbb122f719e00be03b4bd9704e14`,
"docs: record the Vesna engineering handoff (#10)"), branch `vladon/qa-home-return`
(isolated worktree `qa-home-return`). The APK was built from this checkout.
**Package under test:** `dev.vladon.vesna.launcher`, `versionName` 0.4.1,
`versionCode` 6 (matches the engineering baseline in PROJECT_STATUS.md).

## Acceptance assertions (what "HOME works" means for 0.4.1)

1. The system HOME selection (system UI) **offers Vesna Launcher** alongside
   the stock launcher.
2. After selection through that system UI, Vesna is the **default HOME holder**;
   a launched app's HOME press **returns to the Vesna workspace**.
3. Launching a known stock app **through Vesna's drawer** reaches its real
   foreground (verified by activity state and screen content, not by
   `am start` exit codes); pressing system HOME afterwards **restores the
   workspace as left, drawer closed** (stable fixture).
4. Cleanup **restores the previous HOME holder** exactly; no data clear, stock
   launcher untouched.

## Environment

| Property | Value | Source |
|---|---|---|
| Dedicated device | AVD `Vesna_QA_API_35`, adb serial `emulator-5556` | `adb -s emulator-5556 emu avd name` → `Vesna_QA_API_35` |
| OS / API | Android 15, API 35 | `getprop ro.build.version.release/sdk` |
| Build fingerprint | `google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.043/12960925:userdebug/dev-keys` | `getprop ro.build.fingerprint` |
| Screen | 1080×2400 @ 420 dpi | `wm size`, `wm density` |
| User | 0 (`am get-current-user`) | no managed profile exercised |
| Boot / lock | `sys.boot_completed=1`; keyguard `mIsShowing=false` (awake, unlocked) | `getprop`, `dumpsys window policy` |
| Other launchers | `com.google.android.apps.nexuslauncher` (Pixel Launcher, `/system_ext`) present, **left installed** | `pm path` |

Every adb command in this run pinned the serial (`-s emulator-5556`); no other
device, AVD, or research branch was touched.

## Initial HOME state (recorded before any change)

| Probe | Command | Result |
|---|---|---|
| HOME role holder | `cmd role get-role-holders android.app.role.HOME` | **empty** — no holder |
| Default home resolution | `cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME` | `com.android.internal.app.ResolverActivity`, `isDefault=false` |
| Acting/visible home | `dumpsys activity activities` → `topResumedActivity` | `com.google.android.apps.nexuslauncher/.NexusLauncherActivity` (task t7, record `314b1a0`) — the fallback on screen, not a persisted default |

So the baseline to restore was: **no HOME role holder, no preferred home;
Pixel Launcher merely visible as fallback.**

## Build and install

```bash
./gradlew.bat :app:assembleDebug          # BUILD successful; app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
# → "Performing Streamed Install / Success"
```

Post-install: `versionName=0.4.1`, `versionCode=6`, `installerPackageName=null`
(sideload). `install -r` only — **no data clear** at any point; an existing
installation of the same version from an earlier session was replaced in place.

## Procedure and observations

Steps 1–3 use Android's own system UI for the HOME decision (no
`cmd package set-home-activity` shortcut was used to establish the default).

1. **Vesna is offered by the HOME selection.** With no default set, pressing
   system HOME (`input keyevent KEYCODE_HOME`) raised the system resolver
   "Select a Home app" listing **Pixel Launcher** and **Vesna Launcher**
   (evidence `01`). `topResumedActivity=android/com.android.internal.app.ResolverActivity`.
2. **Vesna selected via the system UI.** Tapping the Vesna Launcher row enabled
   *Just once*/*Always* (evidence `02`); tapping **Always** made Vesna the
   default home. Observed afterwards:
   - `topResumedActivity=dev.vladon.vesna.launcher/.ui.HomeActivity`
   - `cmd role get-role-holders android.app.role.HOME` → `dev.vladon.vesna.launcher`
   - resolve-activity `isDefault=true`, component `dev.vladon.vesna.launcher.ui.HomeActivity`
   The Vesna workspace was visible (evidence `03`, fixture below).
3. **Drawer opens.** Swipe-up mid-screen (`input swipe 540 1500 540 600 350`)
   opened the drawer: "Search apps" bar + alphabetical 4-column grid of 18 apps
   (Calendar…YT Music), workspace dimmed behind (evidence `04`). Foreground
   remained `.ui.HomeActivity` — the drawer is the launcher's own surface.
4. **Stock app launched through the drawer.** Tapping the visible **Settings**
   icon (grid column 3, row 4) launched the real Settings app:
   `topResumedActivity=com.android.settings/.Settings` in its own task (t16);
   the Settings main page was on screen (evidence `05`). Vesna's task stayed
   alive behind it (t15, `HomeActivity` paused at history #0).
5. **HOME returns to the workspace as left.** Pressing system HOME resumed
   `dev.vladon.vesna.launcher/.ui.HomeActivity` — **the same task t15 and the
   same ActivityRecord instance** (`299a3b`) as before the launch, i.e. the
   launcher task was brought back rather than recreated. The workspace was
   fully visible, **drawer closed**, fixture identical to evidence `03`
   (evidence `06`). The Settings task remained in the background
   (`visible=false`), paused — standard launch-return behavior.

### Workspace fixture invariants (evidence `03` vs `06`)

Fixed 4-column layout, identical before and after the round-trip:

- Grid rows: `Contacts, Drive, Files, Gmail` · `Google, Maps, Messages, Phone`
  · `Photos, Safety, Settings, Voice Search` · `YouTube, YT Music` (14 icons)
- Dock: `Calendar, Camera, Chrome, Clock` (4 slots)
- No drawer/search surface visible on return; layout matches the seeded
  first-page/dock model described in PROJECT_STATUS.md (4×5 grid, 4-slot dock).

## Cleanup / restore (finally)

1. `cmd package clear-package-preferred-activities dev.vladon.vesna.launcher`
   cleared the preferred-home mapping (`isDefault=false` again) **but the HOME
   role holder still listed Vesna** — a real divergence on API 35 (see note
   below). A HOME press at this point still raised the resolver (evidence
   `07`), so the preferred mapping was the operative default.
2. `cmd role remove-role-holder android.app.role.HOME dev.vladon.vesna.launcher`
   cleared the role holder. Verified: `cmd role get-role-holders` → empty;
   `dumpsys role` HOME block → no `holders=` entry; resolve-activity →
   `ResolverActivity`, `isDefault=false` — exactly the initial state.
3. Cosmetic restore of the visible surface: with the resolver up (state of the
   probe), selecting **Pixel Launcher → Just once** put
   `NexusLauncherActivity` back in the foreground — the **same task t7 /
   ActivityRecord `314b1a0`** as at session start (evidence `08`). *Just once*
   adds no persistent default: role holders still empty, `isDefault=false`.

End state: original HOME holder (none) restored and verified from all three
sources; Vesna still installed (0.4.1/6, no data cleared); stock launcher
untouched.

> **Operational finding for later QA cleanup scripts (QA-003/QA-004):** on
> API 35, `cmd package clear-package-preferred-activities` alone does **not**
> clear the HOME role holder. `cmd role remove-role-holder` (an undocumented
> subcommand — `cmd role help` lists only `get-role-holders`) is required to
> restore a no-default HOME state.

## Verdict per acceptance item

| # | Assertion | Result | Evidence |
|---|---|---|---|
| 1 | Vesna offered in system HOME selection | **PASS** | `01`, resolver foreground probe |
| 2 | Default after system-UI selection; HOME returns to workspace | **PASS** | role holder + `isDefault=true` + resumed task probes; `03`, `06` |
| 3 | Drawer launch of stock app reaches real foreground; HOME restores workspace as left, drawer closed | **PASS** | `com.android.settings/.Settings` resumed + visible; `04`, `05`, `06`; fixture identical `03`↔`06` |
| 4 | Cleanup restores previous HOME holder; no data clear; stock launcher kept | **PASS** | restore probes (empty holder, resolver, same fallback record); `07`, `08` |

## Notes and limitations

- Manual scripted run (`adb input`, shell probes) on one device, one user
  (user 0); not an automated test suite. Managed profiles and other API levels
  were not exercised (out of scope for this item).
- Foreground claims are backed by `dumpsys activity activities`
  (`topResumedActivity`/task records) **and** screenshots, never by `am start`
  exit codes (no `am start` was used in the whole run).
- Evidence captures (PNG, device-attributed to `emulator-5556`/`Vesna_QA_API_35`)
  live **outside the repository** at
  `/d/orca/workspaces/vesnalauncher/evidence/qa-home-return/`:
  `01_home_selection.png`, `02_vesna_row_selected.png`,
  `03_vesna_workspace_after_always.png`, `04_drawer_open.png`,
  `05_settings_launched.png`, `06_home_return_after_settings.png`,
  `07_home_after_clear_probe.png`, `08_restored_state.png`.
- Status documents are updated only to the extent observed here (HOME-role row
  in REQUIREMENTS_TRACEABILITY.md, the HOME capability row and "Where to go
  next" in PROJECT_STATUS.md, and the item status in BACKLOG.md / ROADMAP.md).
