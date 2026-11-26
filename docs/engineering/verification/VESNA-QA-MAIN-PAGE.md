# VESNA-QA-MAIN-PAGE — choosing the main Home page (#184, v0.4.96)

The Make-this-the-main-page action per the approved #184 scope: Home
options gains the action on every page except the current main; the Home
press, a launcher start and a restart open the main page; the page order,
pins, dock and the eight-page cap stay unchanged; Go to page… and Reorder
this page… keep working. Build: debug 0.4.96 (versionCode 101) on
`Vesna_QA_Menu_API_35` (emulator-5560, Android 15/API 35, Russian locale,
dark theme).

## End-to-end (UI, evidence in EVI-VESNA-003)

| Scenario | Result |
| --- | --- |
| The action and the gating | On page 1 (the current main) the Home options menu has no make-main item; on page 2 the item appears («Сделать главной страницей», `make-main-item-0494.png`). |
| Making page 2 main | The tap stores the main page (the pref `workspace_main_page = 2`) and confirms («Эта страница теперь главная»). |
| Restart opens the main page | force-stop → launcher start: the workspace opens the EMPTY page 2 (no Frame-app pins — they live on page 1; the dock stays). `restart-opens-main-0494.png`. |
| The counter | The page indicator keeps counting from the first position (main = page 2 of 2 renders as the second dot). |
| Restore | The action on page 1 makes it main again (the pref `workspace_main_page = 1`). |
| The lock interplay | The auto-removal of a page that was the main one falls back to the default page (the controller resets the main-page pref; covered by the code path and the workspace package). |

## Notes

- The Home press opens the main page on the launcher resume (the
  `selectMainPage` call in HomeActivity resume), which also covers the
  launcher start and the restart.
- Go to page… and Reorder this page… are untouched (the popup rows keep
  their positions and handlers).
- The session-unlock (#177) and the label typography (#173) surfaces were
  exercised in the same session without conflicts.
