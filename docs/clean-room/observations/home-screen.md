# Observations — Home Screen (ENV-1, Nova 8.9.9 free)

Evidence under `EVI-HOME-001/002` in the restricted local store. Environment:
Pixel 9a, Android 17, gesture navigation, Nova grid 6×5, icon size 125%.

## OBS-HOME-001 — Long-press on empty workspace opens customize mode

- Date / Env: 2026-11-17 / ENV-1
- Initial state: home screen page 1; workspace icons Gmail, Photos, Play
  Store, Nova Settings; dock with 5 apps; search bar below dock.
- Action: long-press (1.2 s zero-distance touch) on empty wallpaper area.
- Observed: the launcher enters a customization state:
  - a Widgets bottom sheet slides up from the bottom (rounded, drag handle,
    app-grouped widget list, e.g. "Nova Launcher — 1 widget, 3 shortcuts",
    "Calendar — 2 widgets", "Chrome — 4 widgets");
  - floating shortcut buttons "Wallpapers" and "Settings" appear over the
    wallpaper where the dock was (dock hidden while customizing);
  - a home-button circle appears top-center;
  - workspace icons remain visible above the sheet.
- Variations: BACK collapses the sheet but the customization state persists
  (Wallpapers/Settings buttons remain). Tapping the home-circle button exits
  customization back to the normal home screen.
- Persistence: n/a (transient UI state).
- Dependencies: none observed.
- Evidence: EVI-HOME-001 (longpress-empty, widgets-expanded, widgets-drag)
- Confidence: Observed
- Open questions: does a second long-press-elsewhere also exit? Do the
  Wallpapers/Settings buttons open system wallpaper picker / Nova settings?

## OBS-HOME-002 — Widgets sheet expands to a full-screen picker

- Action: drag the collapsed widgets sheet upward (or tap its drag area).
- Observed: the sheet expands to full-screen widget picker:
  - "Search Widgets" field at top;
  - list grouped by app with per-app counts ("1 widget, 3 shortcuts");
  - rows expand via chevron/tap to show widget previews with size labels
    (e.g. "Search bar 4 × 1") and launcher-provided shortcuts (Nova action,
    Activity, Apps or folder).
- Evidence: EVI-HOME-001 (widgets-drag, widgets-nova-group)
- Confidence: Observed

## OBS-HOME-003 — App icon long-press popup

- Initial state: normal home screen.
- Action: long-press the Gmail workspace icon.
- Observed: a popup anchored above the icon with:
  - a header action row: close (X), widgets grid icon, app-info icon, edit
    (pencil) icon;
  - the app's launcher shortcuts as rows (e.g. Gmail "Compose") each with a
    drag-handle affordance on the right;
  - NO Uninstall / Play Store rows — consistent with the default popup menu
    configuration where those items are disabled for the Home surface
    (see observations/settings.md OBS-SETT-007b).
- Dismissal: tapping elsewhere dismisses the popup.
- Evidence: EVI-HOME-002 (gmail-popup, popup-dismissed)
- Confidence: Observed
- Open questions: exact behavior of header buttons (widgets/info/edit).

## OBS-HOME-004 — Drag and drop requires hold-then-move

- Action A: continuous swipe from the Photos icon to the Gmail cell
  (1.5 s interpolated) — NOT registered as a drag; icons unchanged.
- Action B: `input draganddrop` (native press-long-then-move gesture) from
  Photos cell to Gmail cell — drag engaged and completed.
- Conclusion: drag mode engages after a stationary hold on the item; a
  gesture that starts moving immediately does not start a drag. (Behavioral
  requirement for Vesna's touch handling; implementation-independent.)
- Evidence: EVI-FOLDER-001 (drag-onto-icon vs dnd-attempt)
- Confidence: Observed

## OBS-HOME-005 — Layout persistence across launcher restart

- Action: `am force-stop com.teslacoilsw.launcher`, then press HOME.
- Observed: workspace restored with the Media folder (both apps, custom
  label), Play Store position, dock, and search bar exactly as before the
  restart.
- Evidence: EVI-HOME-003 (after-restart, workspace-after-restart,
  back-to-workspace)
- Confidence: Observed

## OBS-HOME-006 — HOME key while on the workspace opens Nova Search (free default)

- Context: Gestures & inputs ▸ Button actions shows "Home button → Nova
  Search" with an "Only on main home page" checkbox (checked) as the free
  edition default.
- Observed:
  - HOME pressed while the workspace is showing: the search-focused drawer
    (Nova Search) opens instead of a no-op; repeated HOME presses keep it
    open; BACK closes it to the workspace.
  - HOME pressed from another app (system Settings): returns to the
    workspace (standard HOME role; binding does not fire from other apps).
  - HOME pressed while the launcher process is dead: the launcher cold-starts
    into the search-focused drawer (the binding applies to the incoming HOME
    intent too).
- Cross-version note: on the Android 16 AVD (ENV-2), the same HOME-press
  sequence while on the workspace did NOT open Nova Search — the workspace
  stayed. See OBS-SYS-102 for the comparison.
- Evidence: EVI-HOME-003 (home-after-back), EVI-SYS-001 (home-from-app),
  EVI-ENV2-001 (avd-home-test)
- Confidence: Observed
- Open questions: is this binding present in Prime/Plus editions or only
  free? Does gesture-navigation HOME (swipe) behave identically to the
  KEYCODE_HOME event?

## OBS-WALL-001 — Wallpaper anomaly resolved (external, not launcher)

- Summary: early-session workspace captures show a Material-style wallpaper;
  captures from the widget-research phase onward show a stock-style abstract
  image. The image changed once, mid-session, without any wallpaper UI being
  invoked by the research (the Wallpapers button was never opened; no
  wallpaper intent was fired).
- Forensics: `dumpsys wallpaper` shows the active wallpaper is the platform
  ImageWallpaper (SystemUI) with a GradientColorWallpaper fallback; the
  stock-style image is also the default wallpaper of the Android 16 AVD used
  later in the session (both devices showing stock-style art).
- Conclusion: the change is external to Nova — an OS/user-level mechanism
  (e.g. platform wallpaper rotation or another app) altered the image; Nova
  only renders it. No launcher-side wallpaper write was observed. Marked
  resolved as "not attributable to Nova"; the exact OS trigger remains
  unknown (outside clean-room scope).
- Evidence: EVI-HOME-003 vs EVI-WIDGET-001/EVI-WALL-001 captures; dumpsys
  wallpaper excerpt
- Confidence: Partially Observed (change confirmed external; OS trigger
  unidentified)

## OBS-HOME-007 — Page creation by edge drag and page navigation (verified)

- Action: dragged the Play Store icon from page 1 toward the right screen
  edge (hold-then-move, ~3 s; release after the page flipped).
- Observed:
  1. Hovering the icon at the right edge flipped to a NEW empty page;
     releasing placed Play Store there — page 2 created by drag.
  2. "Wallpaper scrolling" visibly panned the wallpaper between pages.
  3. Swiping horizontally navigates between the two pages; dock and search
     bar stay fixed across pages.
  4. A too-short drag (~25 px movement) does not engage drag mode and opens
     the item popup instead (consistent with OBS-HOME-004).
  5. No page-indicator dots were visible in either state (indicator style
     per its setting; value unconfirmed).
- Evidence: EVI-HOME-005 (page2-drop, page2-attempt2, page1-check)
- Confidence: Observed
