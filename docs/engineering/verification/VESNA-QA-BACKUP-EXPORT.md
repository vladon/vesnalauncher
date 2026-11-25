# VESNA-QA-BACKUP-EXPORT — layout export/restore verification (#116, v0.4.88)

User-controlled layout export and restore per
[ADR-BACKUP-1 part 1](../../adr/0002-backup-and-privacy.md): the approved
[#116](https://github.com/vladon/vesnalauncher/issues/116) scope. Build:
debug 0.4.88 (versionCode 93), `Vesna_QA_Menu_API_35` (emulator-5560,
Android 15/API 35), Settings › Home screen › Export layout / Restore layout
(system file picker, Downloads destination).

## Instrumentation

`LayoutTransferTest` (in-memory Room, isolated preference files): **10/10**.
Codec round trip with labels, shortcut ids, folders, multi-page; unknown
version, cell outside the grid, unknown page reference, unknown folder
reference, and two pins on one cell all rejected; restore replaces the whole
layout (previous pins and dock rows gone); the preview lists unavailable
packages; a 9-pin dock file is rejected wholesale on a capacity-4
destination (the dock capacity is a destination setting the snapshot never
carries, so the rejection is all-or-nothing rather than stranding overflow
rows); restored pins for absent packages survive `pruneUninstalled` until
the package reports installed, then follow the normal rules.

The four-package instrumentation run on the same build: workspace **OK
(101 tests)**, apps OK (no suite). The settings (15) and ui (81) suites
failed 9 + 5 in class context and pass individually on both this build and
the unmodified `d10abe9` baseline (the baseline class run crashed the app
process outright, worse than this build), reproducing the documented
post-reboot instrumentation interference
(OBS-SYS-118, vesna-research branch); the changed surfaces —
the new Export/Restore rows and their search targets — pass individually.

## End-to-end (UI, evidence in EVI-VESNA-003)

| Scenario (ADR table) | Result |
| --- | --- |
| Export | Settings › Home screen › Export layout: the system picker prefills `vesna-layout.json` in Downloads; the file carries `version: 1`, grid 4×5, 15 pins + 4 dock pins, real package/class pairs, no work-profile rows or serials. |
| Preview and confirmation | Restore of that file shows the counts from the file ("1 pages, 15 Home pins, 4 dock pins and 0 folders"), the replace disclosure with the destination pin count ("replaces all 19 pins … including work-profile pins. Work-profile apps stay in the drawer."), and Confirm/Cancel. `layout-restore-preview.png`. |
| Round trip | Export → restore (confirmed) → export again: the two snapshot files are equal as parsed JSON (identical pages, pins, dock, folders, grid, seeding flag). |
| Corrupt and unsupported input | A hand-written file with `cell: 999` is rejected with "File rejected — This file is not a valid layout export. Your current layout is unchanged." plus the validation reason ("pins carries cell 999 outside the grid"); the layout stays unchanged. `layout-restore-rejected.png`. |
| Failure containment | Covered by the restore transaction (single `db.withTransaction`) and the preview gate; the UI shows a failure dialog and keeps the previous layout if the apply throws. |

## Design-system acceptance (AGENTS.md, added after release)

Run on the same AVD against the **signed release APK** (installed over the
uninstalled debug build; `DEBUGGABLE` absent), device locale Russian
(surface strings render in the shipped values-ru translations):

| Check | Result |
| --- | --- |
| Dark theme | Rows and the restore preview dialog render on the Vesna dark surface (sampled (17,20,14)); buttons and titles follow the dark palette. `layout-rows-dark.png`, `layout-preview-dark.png`. |
| Russian labels | «Экспорт макета» / «Восстановить макет» rows, the dialog title, summary («страниц — 1, значков на рабочем столе — 15, в доке — 4, папок — 0») and buttons render in Russian. |
| Enlarged font (1.5×) | Rows and the preview dialog wrap and scale without clipping: the longest Russian disclosure renders fully inside the AlertDialog with both buttons visible. `layout-rows-dark-font15.png`, `layout-preview-dark-font15.png`. |
| Touch targets | The row containers measure 88 dp and 121 dp high (minimum 48 dp). |
| D-pad focus | **Partially verified**: synthetic DPAD after touch input only re-focuses the window root (the a11y dump shows `focused="true"` on the window, never on a row), so the ring on the new rows was not conclusively exercised; the rows use the same `vesnaFocusRing` modifier as the verified sibling rows. A hardware-keyboard focus pass remains open. |
| TalkBack | Not run (needs a manual gesture pass); the rows are standard M3 `ListItem`s with headline/supporting text and a clickable action. |

The AVD state (font scale 1.0, night mode off) was restored after the pass.

## Limitations

- Single AVD instance, debug build; the SAF flows ran on Downloads only.
- The work-profile isolation is covered at the format level (the codec
  carries no serials and the export filters to `userSerial == 0`); a live
  managed-profile device run stays open for the research cycle.
- The pending-app UX (a restored pin whose package arrives later) is
  verified by instrumentation, not by a live install on this AVD.
