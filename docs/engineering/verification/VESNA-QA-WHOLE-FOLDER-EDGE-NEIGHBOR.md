# Android verification: move a closed folder to a neighboring Home page

**Ticket:** [#163](https://github.com/vladon/vesnalauncher/issues/163).  
**Candidate:** debug 0.4.68 (73), SHA-256
`6111457ffbab3a97e470c6385e8525fd6a0b3581191d57dd0a7da1c9e42e5672`.  
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.

QA used the native launcher to name folder ID 20 **Weekend tools** and place
Gmail (row 424) before Photos (row 425) at page 1, cell 9. Existing page 2
(ID 33) held Files (row 426) at cell 19, with free cell 18 beside it. The
original 19 Home/dock rows were not repurposed.

| Scenario | Observed result |
| --- | --- |
| Light LTR preview, cancel and occupied cell | A real long-press on the closed folder and dwell at the right edge previewed the existing Files page with the pointer still down. No row or saved selection changed. The Material highlight appeared only at the empty cell. Android `CANCEL` and a separate release onto Files each restored the source page and exact saved layout. |
| Light LTR exact drop and restart | Dropping beside Files moved folder ID 20 to page ID 33, cell 18, and selected that page. The name and all shortcut rows, including members and dock, stayed unchanged. Dwelling outward from the last page did not preview or create another page. After force-stop (PID 27077 → 28682), the folder occupied the same cell and opened with Gmail before Photos. A left-edge drag back to page 1 also landed in the original cell. |
| Compact Dark RTL | At 320 × 640 dp, 200% text, Arabic (Egypt) and three-button navigation, the physical left edge previewed the Files page. The highlighted cell was now to the right of Files, still logical cell 18. Only the folder placement changed. Cold restart (PID 28682 → 29398) retained the exact rows and the Gmail → Photos reading order. |
| Keyboard and lock | At the compact size, Tab focused the folder under its full accessible name and Menu exposed **Move to page…**. Enabling the real layout lock prevented the closed folder edge drag from previewing or writing a placement. |

All 45 focused Android instrumentation tests passed. They cover exact
same-page and cross-page folder moves, old source and missing destination,
occupied folders/pins, stale grid, lock, hidden reserved cells, second-finger
cancellation and recovery, and the existing regular/member page routes.

QA restored the original page and all 19 saved Home/dock rows, plus Light,
normal font and display size, gesture navigation, default app locale and the
unlocked layout. Pixel remained the Home role holder. Evidence is in
`D:/orca/scratch/vesna-whole-folder-edge-neighbor-qa/`. Physical devices and
TalkBack speech were not exercised.
