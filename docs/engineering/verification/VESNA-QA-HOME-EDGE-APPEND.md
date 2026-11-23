# Android verification: create a Home page at the drag edge

**Ticket:** [#151](https://github.com/vladon/vesnalauncher/issues/151).  
**Candidate:** debug 0.4.65 (70), SHA-256
`321388146bb8cb4f5fa6b7f06eae71d2b6c35b6524ac3cc2e123cc3f5d285c56`.  
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.

| Scenario | Observed result |
| --- | --- |
| Light, LTR, cancel and invalid release | Holding Drive at the physical right edge of page 2 showed a blank **Home page 3 of 3** while the pointer was still down. The saved pages and all pins were unchanged during the preview. Both Android `CANCEL` and a release over the dock restored **Home page 2 of 2** with exactly the original placement rows. |
| Light, valid drop and existing neighbor | Releasing on the highlighted empty cell created page 24 and moved Drive's existing row (280) to cell 18, without moving Contacts, changing the dock, or editing any other row. Dwelling toward the previous page from the new page still previewed the Contacts marker without a write. |
| Cold restart | After force-stop, the new process (PID 13756 → 14632) showed Drive at the same cell on the newly created page. The saved layout and dock matched the pre-restart snapshot. |
| Compact Dark, RTL | At 320 × 640 dp, 200% text, Arabic (Egypt) and three-button navigation, the physical left edge previewed the blank page even from an initial one-page layout. Returning to the physical right edge from a blank preview revealed the original page without a write. Dropping at Drive's original screen coordinates created page 26 with the same Drive row (282) in mirrored logical cell 0. Chrome stayed on the source page. The page indicator retained a 48 dp target and an accessible page count; the highlight was visible in both themes. A cold restart (PID 14632 → 15980) kept every saved row and the new placement. |

All 38 focused Android instrumentation tests passed. They covered exact source
IDs and cells, invalid or stale source/tail/grid, locked layout, the page cap,
app-action exclusion, existing page controls and placement, folder operations,
and Home/folder focus restoration. The UI scenarios used real Android motion
events rather than direct data writes.

QA removed only disposable pins and pages. All 19 original Home and dock rows
and the original page matched exactly. Light, original display size, font scale,
gesture navigation and the default app locale were restored. Pixel remained
the Home role holder. Evidence: `D:/orca/scratch/vesna-edge-append-page-qa/`.
Physical devices, TalkBack speech and a physical two-finger gesture were not
exercised.
