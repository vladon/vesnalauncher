# ADR-0003 — Workspace gesture-only desktop (pills removal)

**Status:** Accepted (2026-11-27 loop session; the decision delegated to
the implementer by [#228](https://github.com/vladon/vesnalauncher/issues/228)
acceptance: «Document the decision (ADR or a note in the design docs)»)

## Context

The workspace renders two persistent bottom pills: the **Home options**
button (⋯) and the **Open app drawer** button (3×3 grid). Nova's clean
desktop shows only pins and the dock; the same capabilities are reachable
by gesture: the drawer opens with an upward swipe from any page, and the
Home options menu opens with a long-press on empty workspace area
(`HomeActivity.onEmptyAreaLongClick`). Removing the pills removes only the
visible entry points, not any capability.

[#228](https://github.com/vladon/vesnalauncher/issues/228) asks for a
decision between keeping the pills and going gesture-only, with the
decision documented.

## Decision

The workspace goes **gesture-only** (Nova-style), per the standing
full-copy Nova-parity direction:

- The persistent bottom pills (**Home options**, **Open app drawer**) are
  removed from the workspace bottom bar.
- The page navigation controls (Previous / page indicator / Next) stay:
  they are page-management affordances, not the pills in scope.
- The drawer still opens with an upward swipe from any page (#170), and
  the Home options menu still opens with a long-press on empty workspace
  area — both gesture routes keep working unchanged.
- The workspace root node gains custom accessibility actions («Open app
  drawer», «Home options») so TalkBack users keep equivalent, discoverable
  routes (the #228 a11y requirement).
- A one-time hint (a snackbar on the first Home entry after the upgrade)
  compensates the lost discoverability: «Swipe up for apps · Long-press
  for options».

## Consequences

- The desktop shows only pins, the dock and the page controls.
- The TalkBack users reach the drawer and the Home options through the
  workspace's custom accessibility actions.
- The pills do not return; the #228 acceptance's «keep both gesture
  routes working» is satisfied by the preserved swipe-up and long-press
  routes.
- The Home options menu itself (wallpaper, pages, settings) stays
  reachable through the long-press and the accessibility actions.
