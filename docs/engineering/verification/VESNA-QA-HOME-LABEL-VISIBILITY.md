# Android verification: hide Home labels independently

**Ticket:** [#152](https://github.com/vladon/vesnalauncher/issues/152).  
**Candidate:** debug 0.4.63 (68), SHA-256
`a123741b2b2c769555ebbf456b4a78d45a803540b82dbbc7df4df568cd3de6c3`.  
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.

| Scenario | Observed result |
| --- | --- |
| Default and discovery | Before changing anything, Gmail and Photos on Home and Calendar in the dock displayed names. Settings search for `hide app names` opened the highlighted **Hide app names on Home** switch. |
| Independent labels | Turning the switch on removed visual Home and dock labels. The original clickable, focusable tiles still exposed Gmail, Photos and Calendar as accessible names; their bounds and screenshot crops of the actual artwork were unchanged. The vertical drawer list retained its visible Calendar label and the same row bounds. |
| Global switch and restart | A new process after force-stop restored the Home-only setting. Turning **Show app names** off still hid drawer names; turning it on restored drawer names without overriding the Home-only choice. Turning the scoped switch off restored Home and dock names. |
| Folder names | A temporary folder's title and its Photos and Gmail member labels remained visible when Home labels were hidden. |
| Compact Dark and access | At 320 × 640 dp, 200% text and three-button navigation, the full Russian title and supporting text fit in one labeled, checkable row. Tab focused it with a visible outline and D-pad Center changed its state. Home still exposed the accessible Gmail name after hiding its visual label, and the drawer showed Chrome with its label. |

All 31 instrumentation tests passed on the same emulator:
`SettingsSearchLayoutTest`, `WorkspaceLabelLayoutTest`, `HomeArtworkLayoutTest`,
and `DrawerSearchTest`.

QA removed only the temporary folder and page. All 19 original Home and dock
rows matched exactly, including cell positions. Light, normal size and text,
no app locale override, gesture navigation, grid layout, and the effective
original label settings were restored. The Pixel Home role never changed.
Evidence is in `D:/orca/scratch/vesna-home-label-visibility-qa/`. Physical
devices and TalkBack speech were not exercised.
