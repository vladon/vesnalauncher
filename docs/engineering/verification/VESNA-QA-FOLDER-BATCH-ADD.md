# Android verification: add several apps to a Home folder

**Ticket:** [#134](https://github.com/vladon/vesnalauncher/issues/134).  
**Candidate:** debug 0.4.60 (65), SHA-256
`d66e6a13084b23513ab42284e593386caa50392ec60f71e3ca7484e20a116dc6`.
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.

On a temporary second Home page, drawer drags created a folder at cell 0
with Photos and Gmail. Contacts, Drive and Files already had pins on the
primary page; the picker must not move those pins.

| Scenario | Observed result |
| --- | --- |
| One save | The unlocked closed-folder menu opened **Add apps**. Existing members were excluded and Add started disabled. Checking Drive then Contacts changed both checkbox states and enabled Add with no database write. One Add appended Contacts then Drive in picker order. The folder ID, page, cell, old member rows and all other pins were identical. |
| Cancel and Back | Each dismissal was exercised separately after checking two apps. All rows matched the pre-picker snapshot. Reopening started with no items checked. |
| Restart and drag | Force-stop killed the process; the next process showed all four saved members and the same placements. A regular drawer drag of Files then appended only Files at rank 4. |
| Layout lock | With the real Settings switch enabled, a folder hold did not expose Add apps and the placement snapshot was unchanged. |
| Dark, Russian and keyboard | At 320 × 640 dp and 200% font scale with three-button navigation, Tab focused the Home folder, Menu opened its options and D-pad Center opened the picker. Tab and D-pad Center toggled a named checkbox. Both footer targets stayed put when the list scrolled. Landscape kept the checked Chrome row, usable scrolling space and both actions. Android Back restored focus to the Home folder and left its rows unchanged. |
| Touch targets and contrast | The Android tree exposed one named clickable CheckBox per row with its actual check state. Contacts measured 56 dp high in Light; the compact Dark keyboard target was also 56 dp. Footer controls were at least 48 dp. Sampled screenshot roles yielded 13.92:1 / 11.10:1 for Light/Dark labels and 5.25:1 / 8.43:1 for enabled actions. The Dark focus outline was visible. |

The 39 focused Android instrumentation cases cover folder creation, the
batch transaction (including stale target, duplicate and lock rejection),
folder removal, dock/folder interactions, and Settings layout. QA removed
only the temporary page and pins and verified all 19 original Home and dock
rows exactly. The launcher role, theme, app language, display size, font
scale and gesture navigation were restored. Screenshots, accessibility
trees, placement snapshots and test logs are in
`D:/orca/scratch/vesna-folder-batch-add-qa/`.

TalkBack speech, RTL and a managed profile were not exercised on this AVD.
Physical-device and tablet verification remain open.
