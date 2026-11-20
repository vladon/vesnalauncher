# Settings navigation: `NT-SETTING-01`

**Verdict: pass** on Android 8 (API 26, compact, dark, 200% text) and
Android 15 (API 35, light, 130% and 100% text). Test scope:
[`NT-SETTING-01`](https://github.com/vladon/vesnalauncher/blob/9954a278758958da8d0b0d878bc18e6a9fbeb0e8/docs/clean-room/implementation-tickets.md#nt-setting-01-p1--settings-opens-clean-every-page-deep-linkable)
and [`AT-SETTING-112`](https://github.com/vladon/vesnalauncher/blob/9954a278758958da8d0b0d878bc18e6a9fbeb0e8/docs/clean-room/acceptance-tests/scenarios.md#at-setting-112--settings-opens-clean-and-deep-links-land-true).

| Destination | Android link |
| --- | --- |
| Vesna settings | `vesna://settings/` |
| Default launcher | `vesna://settings/default-launcher` |
| Appearance | `vesna://settings/appearance` |
| About | `vesna://settings/about` |

Run against an installed debug build with `adb -s <serial>` pinned to the QA
AVD. For example, open Appearance without specifying an activity:

```text
adb -s <serial> shell am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d vesna://settings/appearance -p dev.vladon.vesna.launcher
```

1. Open the app icon. The root is visible with an empty, unfocused search
   field and no keyboard. Repeat after force-stopping from a focused search
   and from an open subpage.
2. Type `Theme`; only Appearance remains. Press Back with the keyboard up:
   the keyboard closes and the query still filters the list. Press Back
   again: the query and focus clear without leaving Settings.
3. Type a query with no results and leave the keyboard up. Launch each
   of the three named links from this state; the exact page and its contents
   appear, with no old search field or keyboard. Launch the root link and
   confirm it clears state. Switch directly between two named pages, then
   open the app icon and confirm a clean root.
4. Tab to Appearance and activate it with Enter. Confirm the labeled Up
   control has a target at least 48 dp wide and tall and returns to the
   directory. On the compact device, scroll to About and to the app-names
   switch. In Russian on Android 15, confirm the root and Appearance titles
   and theme labels render.

The checks used device input, the Android accessibility hierarchy, IME
visibility, and screenshots. The four Gradle CI gates passed locally. This
verifies destination search and navigation, not `SETT-110` breadcrumbs or
individual control highlights. TalkBack, RTL and a physical device were not
part of this run.
