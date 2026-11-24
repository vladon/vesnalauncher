# VESNA-QA-DRAWER-BACKGROUND — drawer-only background (#158, v0.4.79)

Device: Android 15 emulator (API 35, emulator-5560), debug build 0.4.79.
Start state: Vesna palette, Light, English, 1.0 font scale, 15 workspace + 4 dock pins.

| Check | Evidence | Result |
|---|---|---|
| Default unchanged: Follow theme keeps the drawer surface #F9FAEF | Pixel sample `d01` | PASS |
| Settings → App drawer lists **Drawer background** radios: Follow theme, Rose, Sage | Node dump + captures | PASS |
| Rose Light: drawer surface #FEF7FD (sampled) | Pixel sample `d02` | PASS |
| Sage Light: drawer surface #F5FCF4 (sampled) | Pixel sample `d03`, capture `s-p13-sage-light-drawer` | PASS |
| Rose Dark: drawer surface #171116 (sampled) | Pixel sample `d06`, capture `s-p16-rose-dark-drawer` | PASS |
| Sage Dark: drawer surface #101510 (sampled) | Pixel sample `d04`/`d05` | PASS |
| Preference persists across taps and a cold start (sage survived force-stop + relaunch in Dark) | `run-as` pref reads + sample `d05` | PASS |
| Home/dock untouched: the 19 launcher tiles (15 workspace + 4 dock) stay listed while a background is active | Node dump | PASS |
| Settings search finds **Drawer background** (query "background") | Capture `s-p14-search-background` | PASS |
| Russian: «Фон списка приложений», «Как в теме», «Розовый», «Шалфейный» (per-app locale ru-RU) | Capture `s-p17-ru-drawer-background` | PASS |
| `DrawerBackgroundContrastTest`: Rose and Sage at 4.5:1 or more against the Vesna onSurface/onSurfaceVariant in both themes | JVM run in the 50-test suite | PASS |
| Regression: full instrumentation suite 168/168 (ui 68, workspace 86, settings 14), 50 JVM tests, lintDebug | This build | PASS |

Notes and limits:

- The choice changes only the drawer panel surface, per the approved scope; text/accent overrides, other surfaces and opacity stay out of scope.
- Follow theme writes the stable name `theme` and renders the palette surface either way.
- Screenshots and raw samples: `D:/orca/scratch/vesna-palettes/` (d01–d06, p13–p17).
