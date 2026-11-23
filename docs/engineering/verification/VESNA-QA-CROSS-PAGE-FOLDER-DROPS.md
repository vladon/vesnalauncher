# Android verification: cross-page drops onto an occupied cell

- **Ticket:** [#168](https://github.com/vladon/vesnalauncher/issues/168)
- **Candidate:** debug 0.4.72 (77), SHA-256
  `4c8877a9e8421925e7e59db1e6e771b4c5c4803d87564deef2de30c0a5b3d9d0`
- **Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi

QA built the fixture through the launcher: **Move to page…** sent Files to cell 0, Drive to cell 1 and Settings to cell 2 of a new second page (ID 34), and Drive was dropped onto Files to form folder 21. The original 19 Home and dock rows supplied every pin. On the installed debug 0.4.68 (73), holding Safety at the right edge previewed page 2, but the release onto Settings left the layout unchanged.

| Scenario | Observed result |
| --- | --- |
| App onto an app on another page | Holding Messages (page 1, cell 6) at the right edge previewed page 2. The release on **Settings** created folder 22 at page 2, cell 2, with Settings and Messages. Messages left page 1, page 2 became selected, and the other 17 rows and both pages were unchanged. |
| App onto a folder on another page | From page 1, dragging Maps to the right edge highlighted folder 21 under the pointer, and the release added Maps after Files and Drive. The folder opened in that order. |
| Cancel and invalid release | On the page 2 preview, Android `CANCEL` and a release over the dock each returned to page 1 with the layout unchanged. |
| The pin's own cell | Dragging Photos to the right edge and then back to the left edge previewed page 1 again. Releasing on Photos' own cell changed nothing. |
| Restart | After force-stop (PID 1038 → 2533), page 2 still held both folders with the same members. |
| Compact RTL | At 320 × 640 dp, 200 % text, Arabic (Egypt) and three-button navigation, the physical left edge previewed page 2. Phone first moved to the mirrored cell 4, and YouTube dropped onto it, highlighted under the pointer, created folder 23 there. |

The new `dropsFromAnotherPageCreateOrExtendFoldersOnTheTargetPage` case checks the moved pin's page and cell, the occupied target and the unchanged rows. All 86 `workspace`, 59 `ui` and 12 `settings` instrumentation tests, 48 JVM tests and lint passed. One existing case named a moved pin without its page and now passes that page.

Observed, outside this ticket's acceptance: a shortcut added to a folder keeps its former cell as its position inside the folder, as with a same-page drop. Contacts from cell 0 joined folder 22 in front of Settings and Messages.

QA restored the original single page and all 19 saved Home and dock rows, Light, normal text and display size, gesture navigation and the default app locale. Pixel remained the Home role holder. Evidence is in `D:/orca/scratch/vesna-cross-page-folder-qa/`. Dropping a closed folder onto an occupied cell (#169), drawer and dock sources, and physical devices were not exercised.
