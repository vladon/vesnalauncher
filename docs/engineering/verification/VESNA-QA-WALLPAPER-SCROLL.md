# VESNA-QA-WALLPAPER-SCROLL — wallpaper scrolling between pages (#181, v0.4.103)

The approved #181 scope: the wallpaper scrolls with the pages (on by
default, Nova's default) — the first page shows the wallpaper's left
edge, the last its right edge — with an option to turn it off; the
wallpaper stays edge to edge in both states. Implementation:
`WallpaperManager.setWallpaperOffsets` with the shown page's fraction
(`position / (pageCount - 1)`); the option off (or a single page) sends
the centered 0.5 offset, so the wallpaper never moves. Build: debug
0.4.103 (versionCode 108) on `Vesna_QA_Menu_API_35` (emulator-5560,
Android 15/API 35, RU locale, dark theme, two content pages).

## Test fixture

The default AVD wallpaper is single-screen-width (no scrollable range —
the offsets clamp invisibly), so a distinctive wallpaper was required:
the one-activity fixture `dev.vesna.qa.wallpaperset` (raw-SDK build,
29 KB) calls `WallpaperManager.setBitmap` on launch with a 2160×2400
horizontal blue→orange gradient (twice the screen width — the scrollable
range). The same fixture serves future wallpaper QA.

## What is verified

| Check | Result |
| --- | --- |
| Scrolling on (the default) | Page 1 vs page 2 wallpaper zones: the left band reads (32,50,144) on page 1 and (113,83,88) on page 2 — the mid-gradient; the mean per-zone channel difference 142-176 (the wallpaper shifted by the full span). |
| The option off | Settings › Home screen › «Прокрутка обоев» OFF (the switch persists `home_wallpaper_scroll = false`): the same zones read identical on both pages (the total difference 11 — the icon anti-aliasing noise), the wallpaper put. |
| The option restores | The switch back ON persists `home_wallpaper_scroll = true` and the shift returns. |
| Persistence | The pref survives force-stop/relaunch; the offsets re-apply on the first state render of the fresh process (the alignment holds on the page shown at start). |
| Unchanged guarantees | The tiles, the page counter, the dock and the page controls never move with the wallpaper (only the offsets change); the wallpaper stays edge to edge (the offsets, not the sizing, are touched). |
| Regression suites | FolderDisplacement 3 + PageAutoRemoval 5 + Badges 6 = 14/14; WorkTabDrawerTest + SettingsSearchLayoutTest 18/18 on the same build (the first batch ran before the test-APK rebuild; the NoSuchMethodError it showed was the stale test APK, resolved by rebuilding — recorded for the loop discipline: rebuild BOTH APKs after any SettingsScreen signature change). |

## Known limits

- Landscape: the offsets are horizontal only; the vertical stays centered.
  The wallpaper sizing (edge to edge) is the system's default behaviour,
  untouched by this change.
- Nova's «Reverse» direction is not offered (the acceptance requires on
  and off only); a direction choice would be a separate decision.
- The release build and physical devices need separate runs.
