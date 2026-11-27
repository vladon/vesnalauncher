# VESNA-QA-DOCK-LABELS-PILLS — dock label size and pill collision fix (#231, v0.4.99)

Issue #231 reported two regressions on the v0.4.93-95 window, verified on
the QA AVD (emulator-5560, Android 15/API 35, 1080×2400 @420dpi, Russian
locale, fresh default state after `pm clear`):

## Reproduction (before the fix, 0.4.97 debug build)

- **Defect 1 — dock labels blown up and truncated**: a dock label
  «Chrome» measured 235×122 px (the correct 12sp size is ≈149×47 px);
  longer names ellipsized to «Ca…». The #173 typography application
  assigned a px value to `TextView.setTextSize(float)`, which reads SP
  units — at 420 dpi every applied typography pass inflated the text
  ≈2.625×.
- **Defect 2 — the Home options/drawer pills overlapped the dock while
  the persistent search bar was shown**: the pills lifted by the dock
  height only (`updateWorkspaceBottom`), so the bar stacked below the
  dock pushed the icons into the pill row (the drawer pill bounds
  intersected the dock tiles). Reproduced with the default Below-the-dock
  placement.

## Fix (PR #235, v0.4.99 / versionCode 104)

1. `AppIconView.applyLabelTypography` passes the base size back with
   `TypedValue.COMPLEX_UNIT_PX`.
2. `HomeActivity.applyLabelTypography` runs at startup, so the saved
   label size/font survives a restart (it previously applied only when
   the settings wrote the pref while Home was resumed — the #231
   restart case).
3. `updateWorkspaceBottom` lifts the controls and reserves the workspace
   bottom by the whole `home_dock_area` height (dock + the optional
   search bar), following the area's layout changes.

## Verification (after the fix, same device)

| Check | Result |
| --- | --- |
| Default (100%, Inter Medium, bar below the dock) | Dock label «Chrome» = 149×47 px, full text, no truncation; the drawer/Home options pills sit fully above the dock (pill bottom 1958 < dock top 1968); the search bar visible below the dock. `bug231-after-0499.png` |
| Saved typography at startup | With the slider left at 130% the launcher restarted: labels render 186×61 px (exactly ×1.3), no settings interaction needed. Restored to 100% afterwards. |
| Placement = Above the dock | The bar stacks above the dock; the pills lift higher (y 1832); no overlap with the dock icons. `bug231-above-0499.png` |
| Placement = Off | The bar gone; the dock at the bottom; the pills above it (bottom 2103 < dock label top 2265); labels unchanged. `bug231-off-0499.png` |
| Slider scales in px steps | 130% = ×1.3 of the 100% label box (61 px vs 47 px height), 100% restores the exact layout default. |
| Regression suites | FolderDisplacementTest 3 + PageAutoRemovalTest 5 + NotificationListenerBadgeTest 6 = 14/14 green on the fix build. |

## Notes

- The workspace grid labels share `AppIconView` and were inflated by the
  same px/sp bug when a typography change applied live; the fix restores
  the exact layout default at 100% for every surface (workspace, dock,
  drawer grid tiles).
- The drawer list rows keep their own 16sp typography (they never call
  `setLabelTypography`), unaffected before and after.
- The release build and the physical devices need separate runs; the
  published APK is verified by digest/cert/badging in the release chain.
