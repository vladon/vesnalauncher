# Android verification: drag a closed folder to a new Home page

- **Ticket:** [#167](https://github.com/vladon/vesnalauncher/issues/167)
- **Candidate:** debug 0.4.69 (74), SHA-256
  `8b03d8207875d87705cda1b6d07acafda60cdb883d8f7d6a557860fc0fb3ceed`
- **Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi

A real drag of Photos onto Gmail created folder ID 21, **Folder**, at page 1,
cell 3, with Gmail (row 8) before Photos (row 13) on the only Home page. The
original 19 Home/dock rows were not repurposed. On the installed debug 0.4.68
(73), holding that closed folder at the right edge kept page 1 visible.

| Scenario | Observed result |
| --- | --- |
| Light LTR: preview, cancel and invalid release | Holding the closed folder at the physical right edge revealed a blank second page with **Home page 2 of 2** while the pointer was down. The dock stayed, and no saved row changed while held. Android `CANCEL` and a separate release over the dock each restored the single page and the exact saved layout. |
| Light LTR: exact drop and restart | Releasing at the top-right cell created page ID 34 and moved only folder 21 to its cell 3. The title, both member rows and their order, and the other 17 rows were unchanged. Page 1 kept its 13 shortcuts, page 2 was selected, and the folder opened with Gmail before Photos. After force-stop (PID 4906 → 5468), the same pages and rows rendered. |
| Existing neighbor and page cap | From page 2, the left edge previewed page 1, and a drop moved the folder to cell 8 without adding a page. With eight pages and the folder on page 8, the outer edge showed no preview and the release changed nothing. |
| Compact Dark RTL | At 320 × 640 dp, 200% text, Arabic (Egypt) and three-button navigation, the physical left edge previewed **Home page ٢ of ٢**. Cancel was exact. A drop on the mirrored top-right cell created the page with the folder at logical cell 0; its name and Gmail → Photos order opened correctly after a cold restart (PID 6355 → 7725). |
| Layout lock | With **Lock workspace layout** enabled in Settings, the closed folder's edge drag previewed nothing and wrote nothing. |

All 59 focused Android instrumentation tests passed, including the new
`appendPageWithFolder` case for a stale source cell, unknown folder, invalid
cell, stale grid, layout lock, non-trailing page and the eight-page cap. They
also covered regular pin and folder member appends and the existing folder
moves.

The eight-page fixture was written with SQL before the drag; every other
change came from the launcher. QA restored the original single page and all
19 Home/dock rows, Light, normal text/display size, gesture navigation, the
default app locale and the unlocked layout. Pixel remained the Home role
holder. Evidence is in `D:/orca/scratch/vesna-whole-folder-edge-append-qa/`.
TalkBack speech and physical devices were not exercised.
