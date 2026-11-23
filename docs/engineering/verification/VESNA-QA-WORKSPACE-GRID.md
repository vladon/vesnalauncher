# Android verification: resize the workspace grid without losing shortcuts

**Ticket:** [#106 / NT-HOME-03](https://github.com/vladon/vesnalauncher/issues/106).
**Candidate:** debug 0.4.53 (58), APK SHA-256
`c492771e707e9f1f12386d086fd22a2b53f6470e56c87dfd340e9ce9022296cf`.
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.
Pixel Launcher remained the default Home app.

Vesna keeps valid coordinates where possible and fills a shortcut or folder's
own page first. Remaining items take free cells on other pages in page order;
Vesna adds pages when needed, up to eight total. It rejects a smaller grid
without changing the layout if all items cannot fit. The dock keeps its
capacity and placements. Expanding keeps the current placements where they
fit; it does not restore a prior layout or claim exact Nova parity.

| Scenario | Observed result |
| --- | --- |
| Upgrade from 0.4.52 (57) in place | Room changed from version 4 to 5. All 15 workspace pins, four favorites, and the original page row were unchanged. The default remained four columns and five rows. |
| Search, Cancel, and layout lock | Searching “workspace grid” found the highlighted setting. Tab focused the row and Enter opened the editor. Changing both dimensions and then choosing Cancel left Room unchanged. Locking Home disabled the row and explained why without hiding the saved size. |
| Shrink a populated workspace | Applied 2 × 2 with occupied right-edge and lower cells. The 15 original IDs appeared once each, at valid, distinct cells across four reachable pages (4, 4, 4, 3); all four dock rows were identical. Real swipes reached every page. The page picker selected both the first and a relocated page. |
| Restart, expand, and launch | Force-stop/reopen retained the selected fourth page, grid, and exact saved placements. Expanding to 4 × 5 preserved every shortcut; another restart kept the expanded size and placements. The relocated Settings shortcut launched Android Settings from page three. |
| Material and accessibility | Verified Light English, Dark Russian at 200% on a compact 320 × 640 dp display, and a wider Dark Arabic RTL layout with English fallback. The compact editor scrolled to readable choices, Apply, and Cancel. RTL mirrored both Home cells and dimension choices. Tested gesture and three-button navigation, keyboard focus/activation, and exposed selection states. Captured title/detail text contrast was 16.2:1 / 8.9:1 in Light and 14.35:1 / 10.9:1 in Dark. The first Dark replay exposed black editor text; explicit Vesna content roles fixed it. |

Passed 27 focused JVM cases. On Android 15, 28 Room/page/install cases and 17
workspace/Settings cases passed across the focused runs. In addition to the live
replay, these cover folder and member IDs, final-cell drops, invalidation of a
stale drag, refusing a 33-item layout at 2 × 2, preserving the locked layout,
and rolling back the added page and moved pins when the final grid write fails.
No physical device or TalkBack speech verification was used.

Restored the dedicated emulator's original placements, default grid, app locale,
font scale, display size, system theme, and gesture navigation. Local screenshots,
SQLite snapshots, and test output: `D:/orca/scratch/vesna-workspace-grid-resize-qa/`.
