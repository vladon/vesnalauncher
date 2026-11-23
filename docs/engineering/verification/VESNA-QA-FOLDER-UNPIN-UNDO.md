# Android verification: unpin a Home folder and its shortcuts

**Ticket:** [#132](https://github.com/vladon/vesnalauncher/issues/132).  
**Final candidate:** debug 0.4.66 (71), SHA-256
`5891e9ba162401355f09a153e85e5265af271f687144673d4a69f282cad99c46`.  
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35`, 420 dpi.

| Scenario | Observed result |
| --- | --- |
| Separate actions in Light | The closed folder menu showed both **Ungroup folder** and **Remove folder and shortcuts**. The latter removed only the test folder and its Gmail, Photos and Files pins. Drive on the source page and the original Home/dock rows were unchanged. The three package paths were unchanged, and Gmail remained searchable in the drawer. |
| Undo and restart | Touch Undo restored the same folder ID 14, title, bounds, page ID 28, cell 1 and all 23 saved placement rows, including the original Gmail → Photos → Files order. A cold restart (PID 17076 → 19160) kept that exact state. On the final candidate a second touch Undo also restored the exact snapshot. |
| Existing ungroup | The reopened folder still listed all three members in order. Its **Ungroup folder** action returned the three same pin IDs to page 28; it did not unpin them. |
| Layout lock | Enabling the real Settings lock hid both closed-folder editing actions and showed the locked layout message. All saved rows stayed unchanged. Unlocking restored the actions. |
| Compact Dark and keyboard | At 320 × 640 dp, 200% text, Russian and three-button navigation, both localized menu actions wrapped within their 85 dp rows. Tab and Menu opened the folder menu; D-pad selected removal. The Material snackbar used two readable message lines and focused **Отменить** (Undo) on its own action row, a 53 dp-high clickable target. D-pad Center restored the exact saved snapshot. |
| Dismiss and restart | Explicitly dismissing the snackbar left the folder and its shortcuts unpinned. A cold restart (PID 20772 → 21764) did not restore them. The unrelated Drive pin, original copies of the app shortcuts and dock remained unchanged. |

The 37 focused Android instrumentation tests passed before the final snackbar
layout and focus adjustment. They included the packed **2 × 2** Room fixture:
Ungroup refused, unpin removed only the folder rows, unrelated inserts during
Undo did not reuse row IDs, and restore recovered exactly the same folder and
ordered members with action and custom-label metadata. Additional cases
verified that lock or an occupied original cell never overwrites other pins.
On the final candidate, both folder UI regression tests (recreation and
rename validation) passed again with the revised Material snackbar.

QA restored the exact original 19 Home/dock rows, page and settings, including
Light, normal size/text, gesture navigation, default app locale and accessibility
timeout. Pixel was the Home role holder throughout. Evidence is in
`D:/orca/scratch/vesna-unpin-folder-qa/`. Physical devices, TalkBack speech and
live publisher-only Android pin retention were not exercised; the Room tests
cover the latter's saved row metadata, not the platform pin API.
