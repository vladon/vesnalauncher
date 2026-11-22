# Android verification: selectable app drawer layouts

**Ticket:** [#108 / NT-DRAWER-05](https://github.com/vladon/vesnalauncher/issues/108).
**Build:** debug 0.4.51 (56), APK SHA-256
`eaa676b2b054b37cc2284d48fb00d39aff8e45d7ba7cb7a119c38cfd8f197d20`.
**Device:** Android 15 (API 35), `Vesna_QA_Menu_API_35` emulator, 420 dpi.
The fixture was the stock app list plus 120 launchable aliases. Pixel Launcher
remained the default Home app.

| Scenario | Observed result |
| --- | --- |
| Light, English, normal text and gesture navigation | All three choices worked from **Settings → App drawer**. The vertical grid used four columns. The vertical list used one app per row, preserved the starting alphabetical order, and scrolled vertically. |
| Horizontal browsing | The first page ran from Calendar to Frame App 029 in row order. A horizontal thumb swipe moved to Frame App 030–065 with no overlap between those pages. The vertical fast-scroll rail was absent. |
| Global search, clear, and launch | From page two, searching Calendar found the item from page one. Clearing the field and dismissing Gboard returned to the same page two. An exact Clock search opened the Android Clock app. |
| Close and restart | Closing and reopening the drawer started on page one. An app process restart also started on page one while retaining the Horizontal pages selection. Workspace, dock, folder and page rows were unchanged. |
| Dark and 200% text, three-button navigation | On 320 × 640 dp, selected the Russian list radio row and checked full-width app targets. In Arabic RTL the paged grid mirrored row order, a mirrored swipe advanced a page, and Tab focused a tile with a visible ring. Settings search still jumped to the highlighted **Hide apps** section. A wider 549 × 686 dp viewport kept four columns. |

All 23 cases in `SettingsSearchLayoutTest`, `DrawerSearchTest`, and
`DrawerRecreationTest` passed. The new pager regression checks a visible
app stays on the browsing page through a saved search and a temporary short
viewport, then checks a fresh drawer starts at the beginning. An earlier
live IME replay exposed the page drifting on clear; preserving the app
anchor across viewport-size changes fixed the real-Gboard replay above.

Restored the vertical grid, locale, font scale, display size, theme, and
navigation mode. Local screenshots, UI dumps, and instrumentation output:
`D:/orca/scratch/vesna-drawer-layout-qa/`. No physical device or live
TalkBack speech verification was used.
