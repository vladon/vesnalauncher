# VESNA-QA-ICON-PACK — installed icon pack (#157, v0.4.80)

Device: Android 15 emulator (API 35, emulator-5560), debug build 0.4.80.
Pack fixture: [Arcticons Black 15.4.7](https://github.com/Arcticons-Team/Arcticons/releases/tag/15.4.7) (`com.donnnno.arcticons.light`, versionCode 1580), SHA-256 prefix `3cf9c829848a…`, installed without opening its activity.
Start state: Vesna palette, Light, English, 1.0 font scale, 15 workspace + 4 dock pins (the 0.4.68 baseline data).

| Check | Evidence | Result |
|---|---|---|
| Format verification: pack declares `org.adw.launcher.THEMES`, ships `assets/appfilter.xml` (51 060 parsed component entries) and drawable names that resolve in the pack package (`drawable/c_pla`, `drawable/google_chrome`, …) | Static dump of the APK (aapt2 resources + asset listing); discovery logcat during QA | PASS |
| Settings → Appearance lists **Icon pack** with System (default selected) and **Arcticons Black** | Node dump + captures `qa-i01` | PASS |
| Selecting the pack themes covered apps on Home, in the dock, in the drawer, and in a folder | Pixel diffs vs the System baseline: Home Gmail region 12 621 px, dock Chrome 12 620 px, workspace Maps 12 598 px changed; drawer Calendar sample shows the black-outline pack art; folder Gmail member shows pack art (`qa-i01`–`qa-i03`) | PASS |
| Uncovered app keeps system artwork | `dev.vesna.qa.drawerframe/Entry001–008` logged `no entry` and take the system path; the System-restore pixel check shows the identical system artwork the fallback draws (`qa-i05`) | PASS |
| Labels, accessibility descriptions and pin positions unchanged (19 launcher tiles listed before and after) | Node dumps | PASS |
| No #144 mask on pack artwork: with the rounded-square icon shape enabled the pack's own outline drawing shows unmasked, no squircle plate | Capture `qa-i06d` | PASS |
| Preference survives a launcher process restart (pack art present after force-stop + relaunch) | `run-as` pref read + sample `qa-i04` | PASS |
| Selecting **System** restores the original artwork while Arcticons stays installed (dock Chrome pixel-diff 0; folder Gmail returns to the multicolor system glyph) | Samples `qa-i05`, `qa-i05b` | PASS |
| Settings search finds **Icon pack** (query "pack" → result card with breadcrumb "Vesna settings › Appearance") | Capture `qa-i08` + instrumentation `iconPackIsFoundFromSearchAndShowsTheSystemChoice` | PASS |
| Russian: «Набор иконок», «Как в системе (по умолчанию)», «Arcticons Black» (per-app locale ru-RU) | Capture `qa-i07` | PASS |
| `IconPackResolverTest`: mapping keys, first-entry-wins on duplicates, malformed lines skipped, whitespace tolerated | JVM run in the unit suite (54 tests) | PASS |
| Regression: instrumentation 169/169 (ui 68, workspace 86, settings 15), 54 JVM tests, lintDebug | This build | PASS |

Notes and limits:

- Pack artwork is substituted at catalogue build, before profile badging, so a work-profile app would keep its badge on top of the pack's drawing; no work profile was installed on this AVD, so badging itself is covered by the existing pipeline tests only.
- Android-published action pins (`AppShortcuts`) keep their Android-provided artwork; only the launchable-app catalogue is themed, matching the accepted scope.
- The Arcticons appfilter table (6.7 MB) is parsed line-by-line on IO: ~5.5 s once per selected pack, then O(1) lookups. A regex parse of the same table never completed on-device, which is why the parser is a linear scanner.
- The catalogue rebuild that applies a fresh selection is triggered on Home resume, before the theme-recreate check, so a recreation cannot swallow it; the rebuild takes a few seconds on-device while the launcher stays usable.
- A removed pack falls back to system artwork on the next catalogue rebuild (package events already trigger one).
- Screenshots and raw samples: `D:/orca/scratch/arcticons/` (`qa-i00` … `qa-i09`).
