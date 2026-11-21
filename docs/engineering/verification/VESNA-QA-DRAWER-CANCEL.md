# App drawer — a cancelled hold does not affect the next swipe

On the [published 0.4.7 build](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.7), an ordinary upward swipe closed the drawer after Android cancelled a stationary long press on an app. A control swipe without the cancelled hold scrolled the same viewport normally. The disposable Android 15 test used an actual touch stream (`input motionevent DOWN`, hold, `CANCEL`, then a new swipe) with 120 launchable fixture aliases.

The root now handles cancellation of the incoming touch stream, including when a drawer child still owns it. A synthetic cancellation sent only to the child during normal drag takeover does not cancel the root drag.

| Check | Android 8 / API 26 | Android 15 / API 35 |
| --- | --- | --- |
| `ShortcutActionsTest` + `DrawerSearchTest` | 14/14 passed | 14/14 passed |
| Drawer visual and accessibility smoke | Dark, 200% fonts, 320 dp wide | Light, 130% fonts, about 411 dp wide |

The same instrumentation APK failed the new “next swipe must not close the drawer” assertion against the original debug APK on Android 15. The instrumentation starts at the armed-drag handoff and sends touch events through the launcher root; it does not depend on long-press timing. The fixed build also passed a separate Android 15 touch replay: cancel a stationary hold and scroll normally; release a stationary hold and see the shortcut menu; move a held tile toward an empty cell then cancel without a Home change; repeat and release to add exactly one tile, with the dock unchanged. The added shortcut survived process stop and relaunch.

Both font-scale smoke checks showed visibly distinct search and close controls, labelled 48 dp close targets, and the full accessible app names despite ellipsized visible captions. The instrumentation also covered keyboard search and accessible shortcut options. No layout or visual tokens changed. TalkBack and physical devices were not exercised. Raw red and green logs, screenshots, and drag traces are in `D:/orca/scratch/vesna-drawer-cancel/`, outside Git.
