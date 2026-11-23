# Android verification: drag a shortcut to a neighboring Home page

**Ticket:** [#139](https://github.com/vladon/vesnalauncher/issues/139).  
**Candidate:** debug 0.4.61 (66), SHA-256
`1ffa8b4f740eac5f909700b96924e461932dcf2537d55b15d65b5f49748fa150`.  
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.

Two temporary pages followed the original Home page. Page 2 (ID 13) held
Maps at cell 5 and a folder at cell 0 with Photos, Gmail and Files. Page 3
(ID 14) had a Drive pin at cell 3. This kept all original placements out of
the test. Real Android DOWN, MOVE, UP and CANCEL events drove the gestures.

| Scenario | Observed result |
| --- | --- |
| Regular Home shortcut | Holding Maps in the right edge band revealed the real Drive marker on page 3 while the pointer was still down. All placement rows and the saved page were unchanged. Dropping on a highlighted empty cell moved that same Maps row to cell 18 on page 3. |
| Folder shortcut | Moving Files out of the folder and dwelling at the right edge revealed the same page without making any database change. One release moved that same Files row to cell 19 on page 3. The folder stayed in its original cell with the same Photos and Gmail rows. |
| Cancel, occupied cell and last page | Android CANCEL for Maps and Back for Files restored page 2 without a placement write. An attempted drop on the Drive cell left all rows unchanged for each source, and the canceled folder drag restored the open folder. Dwelling toward a nonexistent page from page 3 did not create one. |
| Cold start | After force-stop, a new process showed Drive, Maps and Files on page 3 in their exact saved cells. Page 2 still had the original folder with Photos and Gmail. |
| Existing editing flows | The actual Settings lock prevented both sources from previewing or moving. Unlocked, a same-page extraction moved Gmail to cell 10 and put Photos in the former folder cell. An ordinary same-page Maps drag landed in cell 17, and a stationary hold still opened its menu. |
| Compact Dark, RTL and keyboard | With an Arabic app locale, 320 × 640 dp, 200% text and three-button navigation, the physical right edge previewed the previous logical page. The empty-cell highlight was visible on the wallpaper in Light and Dark. Cancel restored the origin with exact rows. Tab/D-pad could reach Maps and Menu still exposed **Move to page…**: its row measured 54 dp and the page control target was 48 dp. |

All 43 focused Android instrumentation tests passed. They exercised precise
source IDs, occupied/stale/locked refusals, cross-page and same-page Room moves,
sparse-folder dissolution on the source page, page controls, mirrored hit
targets and folder recreation.

QA removed only the five temporary pins and two pages. All 19 original Home
and dock rows matched the initial snapshot. The fixture was returned to Light,
1080 × 2400, default app locale, normal font scale and gesture navigation;
its Pixel Home role was unchanged. Evidence is in
`D:/orca/scratch/vesna-home-edge-drag-qa/`. Physical devices, TalkBack speech,
and a physical two-finger gesture were not exercised.
