# Android verification: adaptive icon shape

- **Ticket:** [#144](https://github.com/vladon/vesnalauncher/issues/144), approved scope
- **Candidate:** debug 0.4.74 (79), SHA-256
  `24ff45d08881127cb7174f3e816134871c2aae294b5063e20c50a2b56de3e844`. The
  folder, restart, Dark and Russian rows were measured on
  `1a7b01acaae06ac2df64d73428ac62ddca5760585169f4f0d51d2720cf44c186`, which
  differs by the description text, a test fixture and internal drawable
  plumbing that do not change drawing; the default, both shapes, switching
  back and the 130 % row were measured again on the final build.
- **Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 1080 × 2400 px, 420 dpi

Corner fill below is the share of a 12 × 12 px block, 4 to 16 px inside each
icon's top-left corner, that differs from the backdrop beside the icon. The
device's circle mask leaves that block empty; a rounded square fills about
three quarters of it at 126 px artwork. Icon bounds come from the view
hierarchy of the top activity.

| Scenario | Observed result |
| --- | --- |
| Default | Nothing is stored and **System (default)** is selected. All 15 Home icons and 4 dock icons showed 0 % and 126 px artwork: the appearance of earlier versions. |
| Rounded square on Home | **Rounded square** stores `rounded_square`. Returning from Settings redrew Home: the 13 adaptive Home icons and all 4 dock icons showed 75–79 %. The legacy **TMoble** and **Voice Search** artwork stayed at 0 %. |
| Drawer and folder | The drawer grid showed rounded squares for Calendar, Camera, Chrome, Clock, Contacts, Drive and Files, and unchanged outline artwork for the Frame Apps. A folder created from Drive and Files showed both as rounded squares; **Ungroup folder** returned both to their earlier cells. |
| Switching back | **System (default)** stores `system` and restored 0 % on every Home and dock icon. |
| Restart | After force-stop and a new process, Home still showed rounded squares at 75–79 %. |
| Artwork at 130 % | With **Home icon size** at 130 %, Home artwork was 164 px with filled corners (38–44 %, the corner radius grows with the icon); dock artwork stayed 126 px. |
| Settings search | `shape` offered **Icon shape**. Opening it showed Appearance with **System (default)** and **Rounded square**. |
| Dark, Russian and large text | In the Dark theme Home kept rounded squares. In Russian at 130 % text, **Как в системе (по умолчанию)** wrapped to two lines inside its row and **Скруглённый квадрат** was selected. |

All 65 `ui`, 86 `workspace` and 14 `settings` instrumentation tests, 48 JVM
tests and lint passed on the final build. The new `RoundedSquareIconTest`
covers filled corners with a transparent tip, foreground content and a badge
position kept in place, legacy artwork left alone, and the system default
leaving an adaptive icon unwrapped. A new Settings search test opens
**Icon shape** from a query.

QA restored System (default), 100 % Home icon size, Light, normal text and
the default app locale. After ungrouping the test folder, the single page
showed the same 19 Home and dock shortcuts in their earlier cells. Pixel remained the Home
role holder. Evidence is in `D:/orca/scratch/vesna-icon-shape-qa/`. Russian
labels that already ellipsize at 130 % text, such as **Контакты**, predate
this change. Not exercised on a device: work-profile badges, installed icon
packs, TalkBack speech, hardware keyboards and landscape.
