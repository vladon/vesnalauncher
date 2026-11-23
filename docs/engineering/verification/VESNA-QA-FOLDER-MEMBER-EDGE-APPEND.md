# Android verification: drag a folder member to a new Home page

**Ticket:** [#160](https://github.com/vladon/vesnalauncher/issues/160).  
**Candidate:** debug 0.4.67 (72), SHA-256
`888a04592a2f044bb8f3ef2f68a8c5bc5f469075fa5bb2e50222e018fb54fec4`.  
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.

The settled Home page had the default 4 × 5 grid. QA created folders and pins
through the launcher and sent real Android motion events. The temporary folders
occupied cell 9; the original 19 Home/dock rows were not repurposed.

| Scenario | Observed result |
| --- | --- |
| One-page Light LTR: preview, cancel and invalid release | Holding Gmail at the physical right edge revealed a blank second page and retained the dock. Moving to the bottom-right cell showed its Material drag highlight. No saved row or page selection changed while held. Android `CANCEL` and a separate release over the dock each restored the open folder with the exact original pins, member order and page. |
| Two-member folder and cold start | Dropping Gmail at bottom-right created page ID 31 and moved pin 420 to cell 19. Photos, pin 421, took the former folder cell 9 on page 1. No other row changed. After force-stop (PID 22770 → 23784), Gmail rendered at the same bounds on page 2 and Photos on the original page. |
| Compact Dark RTL: three-member folder | At 320 × 640 dp, 200% text, Arabic (Egypt) and three-button navigation, the physical left edge previewed a blank page; the highlight mapped bottom-left to logical cell 19. Only Drive's row (423) moved to new page ID 32. The source folder ID, cell, and the other two member rows and order were unchanged. A cold restart (PID 23784 → 25587) retained that layout. |
| Existing neighbor and keyboard | Tab focused the existing 48 × 48 dp page control with its accessible page count, and D-pad Center opened the picker. After selecting the source page, another folder drag previewed the real Drive page instead of appending. Dropping on Drive's occupied cell restored the open folder and changed no row or page selection. |

All 39 focused Android instrumentation tests passed. The folder and page Room
suites verified the stable row and action metadata, stale source/tail/grid,
invalid cell, layout lock and page cap. They also covered regular Home pin
appends and existing folder moves. Folder recreation and rename validation
passed on the same candidate.

QA restored the original single page and all 19 Home and dock rows, Light,
normal text/display size, gesture navigation, the default app locale and the
unlocked layout. Pixel remained the Home role holder. Evidence is in
`D:/orca/scratch/vesna-folder-member-edge-append-qa/`. TalkBack speech, physical
devices and live publisher-only app actions were not exercised.
