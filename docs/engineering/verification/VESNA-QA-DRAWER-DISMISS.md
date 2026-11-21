# App drawer — scrolling back to the top does not dismiss it

**Verdict:** On Android 15, a touchscreen swipe starting below the top closed the drawer in the published signed 0.4.6 APK. The same regression test failed against the unchanged code and passed with the fix. The fixed drawer still closes on a second quick downward swipe from the top; an Android `ACTION_CANCEL` does not dismiss it.

| Runtime check | Android 8 / API 26 | Android 15 / API 35 |
| --- | --- | --- |
| `DrawerSearchTest`, including the new gesture case | 10/10 | 10/10 |
| Real Home → drawer, screenshot and accessible names | Dark, Android 200% font scale, 320 dp wide | Light, Android 130% font scale, about 411 dp wide |

The regression places a scrollable grid just below the top, then sends a fast downward touch stream through the grid. It checks that the first swipe reached the top without hiding the drawer from accessibility, that system cancellation leaves it open, and that another quick completed swipe from the top dismisses it. The same instrumentation APK was used for the Android 15 red run and both green runs. The class also exercises drawer keyboard focus, keyboard search, app naming, and viewport preservation.

Both real drawer captures showed intact search and close controls, with the visible app labels ellipsized at larger text sizes while their full accessible names remained available. The close control was 48 dp on both configurations. No visual tokens or layout rules changed. TalkBack interaction and physical devices were not exercised. Raw red/green output, screenshots and the signed baseline touch trace are in `D:/orca/scratch/vesna-drawer-gesture` outside Git.
