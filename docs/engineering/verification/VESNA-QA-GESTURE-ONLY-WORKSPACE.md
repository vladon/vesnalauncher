# VESNA-QA-GESTURE-ONLY-WORKSPACE — the pills removal per #228 decision (v0.4.109)

The #228 decision documented in ADR-0003: the workspace goes gesture-only
(Nova-style) — the persistent bottom pills (**Home options** ⋯ and
**Open app drawer**) are removed; the same capabilities stay reachable:
the drawer via the upward swipe from any page (#170), the Home options
menu via a long-press on empty workspace area, and the TalkBack users via
the workspace's custom accessibility actions. Build: debug 0.4.109
(versionCode 114) on `Vesna_QA_Menu_API_35`.

## What is verified

| Check | Result |
| --- | --- |
| The pills removed | The workspace bottom bar shows only the page controls (the arrows + the indicator); no ⋯ and no 3×3 drawer pill (`gesture-only-home.png`). |
| The swipe-up route | The upward swipe from the workspace opens the app drawer with the search, the Apps \| Work tab bar and the app grid (`v-combo.png` right). |
| The long-press route | The long-press on empty workspace area opens the Home options menu (the settings, the wallpaper, the pages). |
| The ANR regression fixed | The removal surfaced the launcher-menu popup anchored to a removed view: the popup height rendered at 97810 px (beyond the 32768 px render limit) and ANR'd the main thread. The menu anchor is now the dedicated 1 dp `menu_anchor` view; the long-press opens the menu without ANR (`menu2-crop.png`). |
| The a11y equivalent | The workspace root exposes the custom accessibility actions «Открыть список приложений» and «Меню рабочего стола» for TalkBack users. |
| Regression suites | GestureDispatchTest 4/4 + FolderDisplacement/PageAutoRemoval/Badges 14/14 on the same build. |

## Known limits

- The TalkBack linear traversal walkthrough (the full surface) needs a
  manual TalkBack session (the injected swipes are not recognized as
  TalkBack gestures — documented in the phase-2 record).
- The release build and physical devices need separate runs.