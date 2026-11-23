# Android verification: optional search actions in drawer search

- **Ticket:** [#113](https://github.com/vladon/vesnalauncher/issues/113), approved
  split scope (explicit hand-off actions only; no network suggestions while
  typing; offline local search unchanged).
- **Candidate:** debug 0.4.77 (82), SHA-256
  `9fba923b6f9c1aa43d9c930ef135022a35f9b6cd9889a88e9b853d0a19db4a15`.
- **Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 1080 × 2400 px, 420 dpi

Search actions were enabled in Vesna settings → App drawer → **Search
actions**, and exercised by typing into the drawer search field.

| Scenario | Observed result |
| --- | --- |
| Empty search with the setting on | Query `zzzq` showed **No matching apps** and three resolved rows: **Search the web**, **Search in Maps**, **Search in YouTube**. Play Store had no handler on this device and was correctly left out. |
| Hand-off to Maps | Tapping **Search in Maps** opened Google Maps with the exact query; after skipping its sign-in screen, Maps showed its search results for the query. |
| Hand-off to the web | Tapping **Search the web** opened the Google search app showing `zzzq - Google Search` results. |
| Local match | Query `ca` matched Calendar, Camera and Clock and showed no action rows. |
| Setting off | With **Search actions** off, the same `zzzq` query showed only **No matching apps** with no rows. |
| Settings search | `Search actions` is on the App drawer page with its description; the switch state follows the setting. |
| Dark and Russian | In the Dark theme the rows render on the drawer surface with light text. In Russian the rows read **Найти в интернете**, **Найти в Картах**, **Найти на YouTube**. |

Nothing is sent while typing: the rows are built from the typed query and fire
only on tap; the launcher itself performs no network requests. All 66 `ui`,
86 `workspace` and 14 `settings` instrumentation tests, 48 JVM tests and lint
passed on this build; the new `DrawerSearchTest` cases cover the rows appearing
only when enabled on an empty search, hiding on a local match, and the tap
handing the exact query to the launcher's callback.

QA restored Search actions off, Light, the default app locale and day mode;
the single page and all 19 Home and dock pins equal the baseline. Pixel
remained the Home role holder. Evidence is in
`D:/orca/scratch/vesna-dock-icon-size-qa/`. Not exercised: TalkBack speech,
work profiles, and web suggestions during typing (out of the approved scope).
