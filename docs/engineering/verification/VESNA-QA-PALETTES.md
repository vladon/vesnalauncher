# VESNA-QA-PALETTES — curated launcher palettes (#155, v0.4.78)

Device: Android 15 emulator (API 35, emulator-5560), debug build 0.4.78 (commit under review).
Start state: Vesna palette, Light, English, 1.0 font scale, 15 workspace + 4 dock pins.

| Check | Evidence | Result |
|---|---|---|
| Default unchanged: Home with no `palette` pref renders the Vesna scheme | Capture `s-p01-vesna-light-home` | PASS |
| Settings → Appearance lists **Palette** radios: Vesna (default), Ocean, Sunset | Capture `s-p02-appearance` | PASS |
| Ocean Light: drawer View surface #F6FAFF (sampled), Compose drawer/search re-themed | Pixel sample + `s-p06-ocean-light-drawer`, `s-p04-ocean-light-home` | PASS |
| Sunset Light: drawer View surface #FDF8F6 (sampled) | Pixel sample `q02` | PASS |
| Ocean Dark: drawer View surface #111417 (sampled) | Pixel sample `q04`, capture `s-p07-ocean-dark-drawer` | PASS |
| Sunset Dark: drawer View surface #161311 (sampled) | Pixel sample `q03` | PASS |
| Preference persists: each radio tap wrote `palette` to `vesna_launcher.xml` (ocean/sunset/ocean/vesna observed on disk) | `run-as` pref reads after each tap | PASS |
| Cold start applies the stored palette (ocean after a locale-driven restart; vesna #11140E after force-stop + relaunch) | Pixel samples `q07`, `q08` | PASS |
| Settings search (en) finds "Palette" with the Appearance breadcrumb | Capture `s-p09` path, node dump | PASS |
| Russian: Appearance page shows «Палитра», «Vesna (по умолчанию)», «Ocean», «Sunset»; drawer «Поиск приложений» | Captures `s-p10-ru-appearance`, `s-p08-ocean-dark-drawer-ru` | PASS |
| `PaletteContrastTest`: 3 palettes × 2 themes, text pairs ≥ 4.5:1, outline ≥ 3:1 | JVM run in the 49-test suite | PASS |
| Regression: full instrumentation suite (ui, workspace, settings) | 168/168 on this build | PASS |

Notes and limits:

- Russian search was verified by opening the Appearance entry directly; Cyrillic could not be typed into the settings search over `adb shell input text` (ASCII-only), and the English search already exercises the same searchable-entry wiring.
- Palette colors are fixed curated values, not wallpaper-derived; the palette choice is independent of System/Light/Dark and applies to the workspace, dock, drawer, dialogs and Settings in both themes.
- Screenshots and raw samples: `D:/orca/scratch/vesna-icon-shape-qa/` (p01–p11, q01–q08).
