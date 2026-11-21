# Dock — small finger movement does not suppress the shortcut menu

On the [published 0.4.8 APK](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.8), a stationary hold opened the dock shortcut menu. Repeating the hold but adding a two-pixel horizontal motion before release left the menu closed. Both runs used the same pinned shortcut on a disposable Android 15 emulator. The dock order was unchanged.

The dock now uses Android's scaled touch slop from the original down position, like the workspace. Movement inside that threshold opens the menu on release without previewing a drop. Once movement has crossed the threshold, returning to the starting point does not reclassify a drag as a hold.

| Check | Android 8 / API 26 | Android 15 / API 35 |
| --- | --- | --- |
| `DockDropTargetTest` and `ShortcutActionsTest` | 7/7 passed | 7/7 passed |
| Menu and accessible dock labels | Dark, 200% text; Tab reached the shortcut and Menu opened options | Light, 130% text; touch hold with two-pixel movement opened options |

The same instrumentation APK first failed the new small-movement assertion on the unchanged Android 15 build. Its fixed run covers sub-slop and above-slop movement in both LTR and RTL, plus the existing slot target and highlight behavior. A separate Android 15 touch replay moved the first shortcut to the last slot of a full dock, with the other three in order, no workspace changes, and the resulting order retained after force-stop and relaunch.

The visible menu actions remained legible in both screenshots. Dock shortcut targets measured 78 × 99 dp on API 26 and approximately 101 × 86 dp on API 35. The accessible labels kept the full app names even where the visible captions ellipsized. TalkBack and physical devices were not exercised; no visual tokens changed. Raw screenshots, logs, and before/after layouts are in `D:/orca/scratch/vesna-dock-hold/`, outside Git.
