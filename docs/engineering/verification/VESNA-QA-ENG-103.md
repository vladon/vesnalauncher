# ENG-103: Large folders and drag-in persistence

**Verdict:** Both drag-in routes and long-folder scrolling pass on Android 8 and Android 15. The 0.4.5 baseline drew scrolled icons over the title and actions; clipping the embedded grid to its measured bounds fixes the overlap.

| Device | Test layout | Members | Drawer app added |
| --- | --- | ---: | --- |
| Android 8 / API 26 | 240 × 520 dp override, 200% text, Dark | 11 | Calendar |
| Android 15 / API 35 | 240 × 520 dp override, 200% text, Light | 12 | Chrome |

1. Drag Home shortcuts onto a folder (or onto the first shortcut to create it). Drag a separate shortcut from the drawer into the same folder. On API 26, use `input draganddrop` with a 1600 ms duration; manually injected MOVE events did not reliably start the drawer drag.
2. Compare saved item rows across `install -r` of the fixed APK and a force-stop/reopen. The full row snapshot remained identical on each device, including dialog member ordering and dock pins.
3. Open the folder and scroll through every member. The added app is last. Check the header and both action labels at the bottom of the list. Tap Rename folder, verify the editor opens, and dismiss without changes.

Compared baseline and fixed screenshots at large text: icons no longer paint over the header or actions, and the final row can be brought fully into view. Debug assembly and Lint passed. TalkBack and physical devices were not tested.

**Decision:** Keep the scrollable grid specified by the Vesna design system. Pagination is a separate product option, not a fix for this rendering defect. The published research covers creation, rename and dismissal, but does not validate pagination for long folders.
