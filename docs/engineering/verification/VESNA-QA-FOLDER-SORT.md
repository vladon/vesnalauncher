# Android verification: sort shortcuts inside a Home folder

**Ticket:** [#135](https://github.com/vladon/vesnalauncher/issues/135).
**Candidate:** debug 0.4.58 (63), Android 15 (API 35), `Vesna_QA_Menu_API_35`,
420 dpi, gesture navigation. The disposable page held Gmail, Photos and Files
in that nonalphabetical order; the Photos pin had the custom name **A Photos**.

| Scenario | Observed result |
| --- | --- |
| Explicit sort by visible label | Opening the folder and dismissing its options without selecting a command left the original order. Tapping **Sort alphabetically** in the closed-folder menu produced A Photos → Files → Gmail. The same saved pin IDs, custom label, folder and Home cell were retained. |
| Persistence and manual editing | The sorted order survived reopening and force-stop/cold start with a new process. Dragging Gmail back to the first position restored the manual order; reopening did not auto-sort. |
| Layout lock | In the locked workspace the sort command was unavailable, and the original saved rows were unchanged. |
| Dark Russian and keyboard | At 320 × 640 dp and 200% font scale the localized sort label wrapped without clipping. Tab focused the Home folder, Menu opened its options, and D-pad Down and Center selected and applied the sort. The resulting Russian display order was Файлы → A Photos → Gmail. Only member ranks changed. |
| Touch and contrast | The action was one labeled clickable row. Its target measured 48 dp in Light and 85 dp in enlarged Dark; sampled text contrast was 14.7:1 and 10.8:1 respectively. |

All 35 focused Android instrumentation tests passed. The new rank tests
cover locale-sensitive Swedish collation, case-insensitive ties, pinned
action metadata, unavailable members, unrelated folders, and rejection
when locked or when the selected folder or members are stale.

The tested debug APK SHA-256 was
`3c4075e773aee46e8cfa497fa7e8b27ef4d57f320bdc6b62bf780389948ca3c4`.
QA removed the temporary page and pins. All original Home and dock rows
matched the initial snapshot. Screenshots, accessibility trees, database
snapshots and test logs are in `D:/orca/scratch/vesna-folder-sort-qa/`.
TalkBack speech and physical devices were not exercised.
