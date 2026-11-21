# Home — a second finger does not switch workspace pages

On the [published 0.4.9 APK](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.9), replacing the first finger with a second one on the workspace switched from page 1 to page 2, although neither finger moved far enough to start a swipe. Adding a second finger after the first had already crossed the swipe threshold also switched pages on release. Reproduced both on a read-only Android 15 emulator with two Home pages using a separate test-only APK to inject Android touchscreen events.

The workspace now disqualifies a horizontal page swipe for the remainder of a touch stream when a second finger joins it. A swipe already intercepted by the workspace still consumes its remaining events without changing the page. Existing single-finger thresholds and layout-direction handling are unchanged.

| Check | Android 8 / API 26 | Android 15 / API 35 |
| --- | --- | --- |
| `WorkspacePageSwipeTest`, `WorkspaceLabelLayoutTest`, `ShortcutActionsTest` | 8/8 passed | 8/8 passed |
| Real Home pager | Dark, 200% text; Tab and D-pad Center advanced one page | Light, 130% text; both two-pointer replays stayed on the original page |

The same instrumentation APK failed both new assertions with unchanged code on Android 15; the single-finger control already passed in LTR and RTL. With the fix, a real one-finger swipe still advanced the page, the selected page survived a process restart, and the Previous button restored the first page and its original visible layout. The dock did not change. The named pager targets were at least 48 dp high in both layouts, and the dark pager showed a visible keyboard focus ring. TalkBack and physical devices were not exercised; no visual tokens or controls changed.

The injector source, before/after accessibility trees, screenshots, and red/green logs are in `D:/orca/scratch/vesna-page-touch/`, outside Git.
