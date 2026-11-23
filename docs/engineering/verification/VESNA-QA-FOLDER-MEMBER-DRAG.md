# Android verification: drag a member out of a Home folder

**Ticket:** [#130](https://github.com/vladon/vesnalauncher/issues/130).
**Candidate:** debug 0.4.55 (60), Android 15 (API 35), `Vesna_QA_Menu_API_35`,
420 dpi, gesture navigation. The fixture put Contacts, Drive, and Files in a
folder at cell 0 of a temporary Home page, with Calendar in cell 3. Cell
indices below are zero-based saved positions, not screen coordinates.

| Scenario | Observed result |
| --- | --- |
| Exact destination in Light | Holding Files and moving revealed Home. A release over Calendar did not highlight or overwrite it. A release over cell 16 highlighted that cell and moved the original Files row there, leaving Contacts and Drive in the same folder. Earlier empty cells were not used. |
| Cancel and invalid targets | A second finger, injected `ACTION_CANCEL`, and Back during a drag all restored the same open folder. Releasing on Calendar or on the dock also restored it. In every case the entire saved layout was unchanged. |
| Lock and menu fallback | A stationary hold and the keyboard Menu key opened the member options with **Remove from folder**. Back returned keyboard focus to the member and then to the Home folder tile. With **Lock workspace layout** enabled, the drag did not change the layout, **App info** remained available, and the removal and rename controls were absent. |
| Dark RTL, dissolution, and restart | On a compact 320 × 640 dp display in Arabic RTL at 200% font scale, the leftmost bottom cell highlighted and accepted Drive as saved cell 19. The two-member folder disappeared and Contacts retained its row ID at the former folder cell 0. Files stayed at cell 16. Relaunch after force-stop confirmed these exact rows and cells with no duplicates. |
| Accessibility and design | The enlarged Dark folder and its member grid stayed usable. Ellipsized names retained their full accessible descriptions. The compact action icons exposed **Remove folder** and **Rename folder** in 48 dp targets. Sampled text contrast was 14.7:1 in the Light options menu and 11.1:1 in the Dark folder title. |

All 31 targeted Android instrumentation tests passed, covering exact folder
placement and rejection, folder creation/removal, top-level removal, workspace
hit testing, folder recreation and scroll, and accessible shortcut options.
The tested debug APK's SHA-256 was
`8b6fd6700638b72ee3c1b0d6626fbef251f502d547740a0e31a2987ab4389cf2`.

The original Home and dock were byte-for-byte unchanged. Native screenshots,
UI hierarchies, database snapshots, and test logs are in
`D:/orca/scratch/vesna-drag-folder-member-qa/`. Physical devices and TalkBack
speech were not exercised.
