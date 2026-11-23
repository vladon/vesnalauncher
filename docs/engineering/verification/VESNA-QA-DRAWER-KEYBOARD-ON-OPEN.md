# Android verification: optional keyboard when the app drawer opens

- **Ticket:** [#112](https://github.com/vladon/vesnalauncher/issues/112), approved scope only
- **Candidate:** debug 0.4.71 (76), SHA-256
  `d6cb34e61b83f43f76a29bbb38291d144c7331defec3b56c1725c2f94bbf64db`
- **Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 1080 × 2400 px, 420 dpi

The drawer was opened by a 1100 px fling from an empty bottom-row cell and by the **Open app drawer** button, with the drawer closed before each trial. The keyboard state comes from `dumpsys input_method` (`mInputShown`) and focus from the UI dump.

| Scenario | Observed result |
| --- | --- |
| Off by default | No setting key is stored. A swipe and the button each opened the drawer with the keyboard hidden, as before. |
| Enabling | **App drawer** settings lists **Show keyboard on open** between **Remember position** and **Hide apps**, off. Tapping the switch turned it on and stored `drawer_keyboard_on_open=true`. |
| On | A swipe and the button each opened the drawer with search focused and the keyboard shown. Typing `se` narrowed the grid to Settings and Voice Search. Back hid the keyboard and kept the drawer open; Back again closed it. |
| Done and recreation | The keyboard's Done key hid the keyboard and cleared focus while the query stayed. A configuration change (font scale 1.0 → 1.05 → 1.0) kept the open drawer with its query: with the keyboard shown beforehand it was shown afterwards with search focused; after Done it stayed hidden. |
| Cold start | After force-stop (PID 28392 → 29254) the setting survived, and a swipe again opened the drawer with search focused and the keyboard shown. |
| Settings search | Searching `keyboard` offered **Vesna settings › App drawer — Show keyboard on open**. Opening it showed the App drawer page scrolled to the switch under a **Search match** band. With a Russian app locale the page showed **Показывать клавиатуру при открытии**, and `клавиатур` found it. |
| Dark and compact | In the Dark theme both entry points showed the keyboard. At 320 × 640 dp, 200 % text, Arabic (Egypt) and three-button navigation, the swipe opened the drawer with search focused, Close above it and two rows of results above the Arabic keyboard. At the same size the Settings row wrapped its title and supporting text around the switch without clipping. |
| Turning off | The switch returned to off and stored `false`; a swipe then opened the drawer with the keyboard hidden. |

All 59 `ui`, 12 `settings` and 85 `workspace` instrumentation tests passed on the candidate, including the new `DrawerSearchTest` focus case and the new search case. The search case first failed under the Russian app locale because it read strings from the application context; it now reads them from the activity. Nine older `SettingsSearchLayoutTest` cases assume English strings and fail under a per-app Russian locale on this English device; that predates this change and was left alone.

QA restored Light, normal text and display size, gesture navigation and the default app locale. The single page and all 19 saved Home and dock rows equal the baseline. Pixel remained the Home role holder. Evidence is in `D:/orca/scratch/vesna-drawer-keyboard-qa/`. TalkBack speech, hardware keyboards, landscape and the frequent-app suggestions deferred by the product decision were not exercised.
