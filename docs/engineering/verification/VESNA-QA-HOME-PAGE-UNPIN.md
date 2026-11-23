# Android verification: remove a Home page emptied by unpinning

**Ticket:** [#153](https://github.com/vladon/vesnalauncher/issues/153).  
**Candidate:** debug 0.4.64 (69), SHA-256
`123d92670a0242069731913e56986922b39cad431b6b6ed719dcc325aa826938`.  
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.

| Scenario | Observed result |
| --- | --- |
| Secondary page with other content | On Light Home, test page 2 (ID 17) held Drive in cell 0 and Contacts in cell 1. Removing Contacts through **Remove from workspace** left all three pages and Drive's exact cell unchanged. **Remove empty page** was not offered. |
| Last pin, other pages and dock | The same action on Drive removed page 17 immediately and returned to **Home page 1 of 2**. The 19 original Home and dock rows matched exactly. The separately added, never-populated page (ID 18) kept its ID and rank. |
| Cold restart | Force-stop and relaunch changed the process ID from 11089 to 11913. Home still started on the first page, and its next-page control opened the retained blank page. That page still supported manual removal. |
| Compact Dark, RTL and keyboard | At 320 × 640 dp, 200% text and three-button navigation with Arabic (Egypt), Tab focused Drive on the mirrored page. Menu and D-pad reached and activated the fully visible two-line removal row (85 dp tall). Focus moved to Contacts on the first page with a visible outline; the dock's layout and Calendar's accessible name and bounds were unchanged. Page arrows were no longer exposed with only one page. |

All 20 focused instrumentation tests passed (`WorkspaceRepositoryPagesTest`
and `WorkspaceRepositoryTopLevelRemovalTest`). They additionally exercised the
first-page guard, stale and locked removals, saved but unavailable pins, folder
occupancy, and preservation of an empty page after removing an app action.
The current regular shortcut menu does not offer Undo.

QA removed only the temporary pins and pages and restored the exact original
layout, Light, font scale, screen size, gesture navigation, and locale. Pixel
remained the Android Home role holder. Evidence is in
`D:/orca/scratch/vesna-prune-home-page-qa/`. Physical devices and TalkBack
speech were not exercised.
