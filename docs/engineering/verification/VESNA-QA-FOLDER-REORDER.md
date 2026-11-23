# Android verification: reorder shortcuts inside a Home folder

**Ticket:** [#131](https://github.com/vladon/vesnalauncher/issues/131).
**Candidate:** debug 0.4.57 (62), Android 15 (API 35), `Vesna_QA_Menu_API_35`,
420 dpi, gesture navigation. The temporary second Home page held Contacts,
Drive, and Files in one folder and a separate Calendar pin.

| Scenario | Observed result |
| --- | --- |
| Insert, not swap | Moving Files over Contacts in Light kept the folder visible and produced Files → Contacts → Drive, with the original row IDs. Moving Files in the other direction over Drive restored the original row order. The changed order survived closing and reopening the folder and a force-stop/cold launch. |
| Cancel, invalid, locked | `ACTION_CANCEL`, Back during the drag, and release on a blank position in the panel all left every saved row unchanged and restored the open folder. Layout lock blocked edits. A stationary hold and keyboard Menu still opened the member options; App info stayed available when locked. |
| Compact Dark RTL | At 320 × 640 dp, Arabic RTL and 200% font scale, the target followed the mirrored reading order. Dropping rightmost Files onto leftmost Drive wrote the exact intended ranks. The folder exposed full accessible names and 48 dp icon actions. Measured label contrast on the dragged state layer was 11.3:1 in Light and 7.6:1 in Dark. |
| Overflow | In a 12-member folder at 320 × 404 dp and 200% text, dragging near the lower member-grid edge reached Frame App 009, initially offscreen. Dropping on its tile moved Contacts to rank 11; all other members retained their relative order. Releasing in the grid's bottom padding did not change any rows. |
| Exact Home extraction | After moving outside the folder and back over a part of Home previously covered by the panel, the chosen empty cell highlighted and accepted the original Files row at cell 10 on page 2. The remaining two members stayed in the folder and no extra row was created. |

All 33 focused Android instrumentation tests passed. The new tests check
both reorder directions with tied and sparse ranks, preserve profile/label
fields and unrelated folders, and reject locked, stale, or cross-folder drops.
Existing folder creation, removal, recreation, and exact drop tests also ran.

The tested debug APK SHA-256 was
`c177578488fe655b40f5931f75db57ab246a688757a3c269d98ca4bac7a3610b`.
All fixture pins and the temporary page were removed. The original Home and
dock rows matched the initial snapshot exactly. Screenshots, accessibility
hierarchies, database snapshots, and logs are in
`D:/orca/scratch/vesna-folder-reorder-qa/`. TalkBack speech and physical devices
were not exercised.
