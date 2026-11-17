# Observations — Settings Hierarchy (ENV-1, Nova 8.9.9 free)

Environment: ENV-1 (Pixel 9a, Android 17). All captures under
`EVI-SETT-001..010` in the restricted local store. Screenshots are the source
of truth; the settings app exposes limited semantics to uiautomator.

Evidence files: settings main (`EVI-SETT-001`), Home screen (`EVI-SETT-002`),
App drawer (`EVI-SETT-003`), Folders (`EVI-SETT-004`), Search (`EVI-SETT-005`),
Cards (`EVI-SETT-006`), Look & feel incl. Icon style + Popup menu
(`EVI-SETT-007`), Gestures & inputs (`EVI-SETT-008`), Nova AI (`EVI-SETT-009`),
Style tab (`EVI-SETT-010`).

Entry points observed:
- Workspace shortcut "Nova Settings" (placed by setup wizard).
- (To verify later: long-press home ▸ Settings; Play Store listing "Open".)

## OBS-SETT-001 — Settings main structure

- Date / Env: 2026-11-17 / ENV-1, Nova 8.9.9 (89900), free edition
- Navigation path: workspace ▸ Nova Settings
- Observed: full-screen settings app with bottom two-tab bar
  (Settings | Style). Settings tab, top to bottom:
  1. Search field (searches settings; behavior OBS-SETT-020, untested)
  2. Nova AI (subtitle: Chat and privacy) — opens Plus subscription page in
     free edition (OBS-SETT-011)
  3. Home screen — "Set grid, icon layout, dock settings, and more"
  4. App drawer — "Set layout, style, opening gesture, and more"
  5. Folders — "Set window styles, background colors, and icon layout"
  6. Search — "Nova's search window and bar configuration"
  7. Cards — card builder list (OBS-SETT-012)
  8. Look & feel — "Icon preferences, popup menu, and notification bar options"
  9. Gestures & inputs — "Define secondary home button action, along with
     swipe, tap, and pinch gestures on home screen"
  10. Feed page — "Control and customize the feed pages at the edge of your
      home screen"
  11. Integrations — "Connect other Apps to Nova, for example to enhance
      shortcut search content"
  12. Notification badges — "Choose from dynamic badges, notification dots, or
      numeric dots" — GREYED with Prime ribbon (OBS-SETT-013)
  13. Backup & restore — "Backup or restore existing Nova setup and settings
      or reset to default"
  14. Manage Subscription — "Unlock more with Nova Prime or Plus"
  15. Version row "Nova Launcher 89900 (8.9.9)" — "Tap to see the current
      changelog and to check for updates"
  16. Select default launcher — value: Nova Launcher
- Style tab: Palette [System]; Mode [Follow system]; per-surface color
  controls for 9 surfaces: Nova Settings, Home screen, App drawer, Folders,
  Popup menu, Nova Now, Desktop search bar, Drawer search bar, Nova Now search
  bar — each surface has a text-color control and a background-color control.
- Evidence: EVI-SETT-001 (main p1/p2), EVI-SETT-010
- Confidence: Observed

## OBS-SETT-002 — Home screen section inventory

- Navigation: Settings ▸ Home screen
- Observed (top to bottom; live preview strip pinned at top showing a mock
  workspace with app icons that visually reflects current settings):
  - **Layout**: Icon size slider (observed value 125%; minus/plus steppers);
    Label toggle (on) with expandable sub-options; Desktop grid picker
    (value 6 × 5, thumbnail preview); Padding (value "None × None"); Dock
    (row with subtitle "Icon layout, background, pages" — opens Dock subpage).
  - **Search**: Search bar placement (value chip "Dock (Below icons)" + gear
    affordance).
  - **Scroll**: Wallpaper scrolling dropdown (value On; subtitle "Scroll
    wallpaper when swiping between home screen pages"); Widget corner radius
    slider (16dp); Transition effect (subtitle "Animation when swiping between
    home screen pages"); Infinite scroll checkbox (unchecked; subtitle
    "Seamlessly swipe between first and last pages"); Page indicator dropdown
    (subtitle "Displays above the dock to show which home screen page is
    currently active").
  - **New apps**: Add icons to home screen toggle (on; subtitle "Automatically
    add newly installed apps to home screen").
  - **Advanced** (collapsible): Lock layout toggle (subtitle "Prevent changes
    to home screen layout"); Show shadow checkbox (subtitle about top/bottom
    screen shadows and status bar contrast); Widget overlap toggle (subtitle
    "Allow widgets to be placed under or on top of other home screen items when
    resizing"); Overlap when placing checkbox; Wallpaper Zoom Effects checkbox
    (checked).
- Note: toggle/checkbox boolean states were read visually; uiautomator could
  not expose them (Compose semantics). States above are as seen in captures;
  exact defaults to re-verify per-feature during behavioral testing.
- Evidence: EVI-SETT-002 (p1, p2, p3, advanced)
- Confidence: Observed (inventory), Not Tested (behaviors)

## OBS-SETT-003 — App drawer section inventory

- Navigation: Settings ▸ App drawer
- Observed:
  - Live preview strip (drawer search bar mock + icon row).
  - **Layout**: Icon size 125%; Match desktop size checkbox (checked); Label
    dropdown+toggle (on); Drawer App Grid 6 × 5; Colors (text-style picker);
    App drawer style dropdown (value Vertical); Transition animation.
  - Search bar placement [Top]; Tab bar placement [None]; Frequently used
    apps toggle (on); Frequently used shortcuts toggle (on); Smart Folders
    toggle (on); Help improve folders [Ask].
  - **Open App Drawer** dropdown [Swipe] + gear (opens drawer-gesture config;
    this is the free drawer-open gesture, distinct from Prime swipe actions).
  - Pull to search (subtitle about pulling down at drawer bottom).
  - Show Keyboard toggle (on; subtitle "Immediately show keyboard when the
    drawer is opened").
  - Fast scrollbar toggle (on; subtitle "Add a thumb slider to quickly
    navigate apps alphabetically").
  - **Apps**: Hide apps — greyed, Prime ribbon; Drawer groups — greyed, Prime
    ("Customize tabs and folders in the drawer"); Folders first — greyed,
    checked?, Prime ("Show folders before apps in the drawer"); Work apps in
    their own tab — checkbox (checked).
  - **Horizontal Scroll** (collapsible): Infinite scroll checkbox (unchecked);
    Transition effect.
  - **Advanced** (collapsible): Automatically close checkbox (unchecked;
    subtitle "Hide drawer once an app is launched"); Remember position
    checkbox (unchecked; subtitle "Don't return to the first page (or top) of
    the drawer after an app is launched, the screen is turned off, or the home
    button is pressed while in an app").
- Evidence: EVI-SETT-003 (p1..p4, hscroll, advanced, end)
- Confidence: Observed (inventory), Not Tested (behaviors)

## OBS-SETT-004 — Folders section inventory

- Navigation: Settings ▸ Folders
- Observed: live preview of an open folder card (grid of icons, name field,
  pager dots, overflow menu).
  - **Layout**: Icon size 125%; Match desktop size (checked); Label; Colors.
  - Mode dropdown [Window]; Shadow toggle (on); Corner radius slider (24dp);
    Show folder name toggle (on); Grid value [AUTO, greyed]; Scroll direction
    [Horizontal]; Folder icon appearance (mini stacked-icons preview row).
- Evidence: EVI-SETT-004 (p1..p3)
- Confidence: Observed (inventory), Not Tested (behaviors)

## OBS-SETT-005 — Search section inventory

- Navigation: Settings ▸ Search
- Observed: interactive search preview showing a typed query with app result,
  app-shortcut result, and a "Search with" provider row (several providers
  shown).
  - **Layout**: Mode [Immersive]; Limit apps to one row (checked; subtitle
    "only show the first row of app results"); Nova Microresults (checked,
    with info affordance; subtitle mentions on-device calculations, unit
    conversions, direct links); Folders checkbox (unchecked; "Search for your
    Nova folders"); Shortcut Search toggle (on; subtitle about local on-device
    indexing of app shortcuts and contacts).
  - Contact card view (checkbox; subtitle about card results; greyed while
    Contact Search is off).
  - API Integrations row (subtitle "Connect other apps and services to get
    more searchable links").
  - Web suggestions [Google] (subtitle "The service to query when searching
    the web").
  - Search More providers (Google, Play Store, Wikipedia, Google Maps,
    Spotify, YouTube observed in preview).
  - Nova AI in search [Tap to open] (subtitle about drawer search results).
  - Make shortcuts from my recent searches (checked).
  - Private keyboard (checked; subtitle about requesting a keyboard that does
    not track typing, with caveat that not all keyboards respect it).
  - Subpages: Desktop search bar, Drawer search bar, Nova Now search bar.
- Evidence: EVI-SETT-005 (p1..p3)
- Confidence: Observed (inventory), Not Tested (behaviors)

## OBS-SETT-006 — Cards section inventory

- Navigation: Settings ▸ Cards
- Observed: "Your cards" list with card previews and per-card configure
  (gear) buttons; observed default cards include a media player card, a
  weather card (weather-service branded), and a games card; a floating add
  button to create new cards.
- Evidence: EVI-SETT-006
- Confidence: Observed (inventory), Not Tested (behaviors)

## OBS-SETT-007 — Look & feel section inventory

- Navigation: Settings ▸ Look & feel
- Observed:
  - Icon style (subtitle "Theme and shape") — opens subpage OBS-SETT-007a.
  - Allow Home screen rotation toggle (off) with per-item overflow menu
    ("When phone is rotated").
  - App animation (subtitle "When launching an app or returning to the home
    screen") — opens radio dialog with options: System, Adaptive icon
    (default selected), Circle, Reveal, Slide up, Zoom, Blink, Slide left;
    Cancel/OK. Cancel leaves selection unchanged (verified: selection moved to
    Slide up then Cancel restored Adaptive icon).
  - Popup menu — opens subpage OBS-SETT-007b.
  - Drop targets toggle (off; subtitle about zooming out the desktop during
    drag and drop and showing Remove/Uninstall/App Info targets).
  - Show undo bar toggle (on; subtitle "Allow undoing for a short period after
    removing a desktop item").
  - Disable animations toggle.
  - **Notification bar**: Show notification bar toggle (on); Transparent
    notification bar checkbox (checked; subtitle "Disabling this makes the
    notification bar black while on home screen"); Transparent navigation bar
    checkbox (checked, same pattern); Dark icons dropdown [Auto] (subtitle
    "Use dark icons in the notification bar on home screen").

### OBS-SETT-007a — Icon style subpage

- Icon theme: "Choose the theme to apply to your app icons" (empty list until
  an icon pack is installed).
- Icon shape radio list: Round (Default) selected; Squircle; Rounded square;
  Flower; More (opens additional shapes); Custom.
- Reshape legacy icons (checked; subtitle "Reshape or add a background to make
  all icons match").
- Adaptive icon animations (checked; subtitle "Wiggle icons during drag and
  drop").
- Shadow (checked; subtitle "Show shadows behind icons").
- Android 7.1 icons (unchecked; subtitle "Prefer icons meant for Android 7.1
  or lower instead of modern icons if the app provides both").

### OBS-SETT-007b — Popup menu subpage

- Preview mockup of a popup menu.
- Colors (text-style picker); Card background toggle (on); Visually group
  shortcuts toggle (off); Corner radius slider (8dp).
- Per-item visibility matrix with Home / Drawer columns for: Uninstall
  (Home unchecked, Drawer checked), Play Store (unchecked/unchecked), Close
  app, Relaunch, Save APK, Hide (Drawer checked; Home column not applicable).
  (Full row list to verify during behavioral testing; captures show rows up
  to Hide.)
- Evidence: EVI-SETT-007 (all pages)
- Confidence: Observed (inventory), Not Tested (behaviors)

## OBS-SETT-008 — Gestures & inputs section inventory

- Navigation: Settings ▸ Gestures & inputs
- Observed: **every listed customizable gesture is greyed with a Prime
  ribbon** in the free edition; each row shows its current binding:
  Swipe up [App drawer], Swipe down [Expand notifications], Swipe left [None],
  Swipe right [None], Double tap [None], Double tap + swipe up [None],
  Double tap + swipe down [None], Two finger swipe up [None], Two finger swipe
  down [None], Pinch in [None], Pinch out [None], Two finger rotation
  (counterclockwise) [None], Two finger rotation (clockwise) [None].
  Free items: Button actions section (Home button → Nova Search + "Only on
  main home page" checkbox).
- Implication (to verify behaviorally): the free edition keeps basic
  navigation gestures (drawer open via swipe) but hides custom gesture
  assignment behind Prime.
- Evidence: EVI-SETT-008 (p1..p4)
- Confidence: Observed (inventory); behavior Not Tested

## OBS-SETT-009 — Nova AI section (free edition gating)

- Navigation: Settings ▸ Nova AI
- Observed: opening the section in the free edition presents a full-screen
  dark-themed subscription page (not a settings page): product name, marketing
  bullets (smart insights tailored to your day; built into your launcher; you
  control your data, delete anytime), plan chooser with $99.99/year ("Save
  44%") and $14.99/month radios (month preselected), benefit list ("Everything
  in Prime and: Personalized AI insights daily; AI assistant on your home
  screen"), primary button "Try 3 days free", footnotes ("Free 3 days trial,
  then $14.99/month." / "Cancel anytime"), links Privacy Policy / Terms of
  Service / Restore purchase, and a close (X) control top-right.
- No purchase was made; page closed via X.
- Evidence: EVI-SETT-009
- Confidence: Observed

## OBS-SETT-010 — Style tab (appearance hub)

- Navigation: Settings app bottom bar ▸ Style
- Observed: heading "Nova Launcher" + settings search field; Palette [System]
  with color-wheel control; Mode [Follow system] (light/dark); per-surface
  color rows (text color + background color) for 9 surfaces: Nova Settings,
  Home screen, App drawer, Folders, Popup menu, Nova Now, Desktop search bar,
  Drawer search bar, Nova Now search bar.
- Note: "Nova Now" appears both as a Style surface and a Search subpage —
  a distinct search/overlay surface in this version.
- Evidence: EVI-SETT-010 (p1, p2)
- Confidence: Observed (inventory); behaviors Not Tested

## OBS-SETT-011 — Edition gating pattern (free vs Prime/Plus)

- Observed across sections: paid features are rendered greyed with a blue
  "Prime" ribbon on the row (Notification badges, Hide apps, Drawer groups,
  Folders first, all customizable gestures). Nova AI opens the Plus upsell
  instead of a settings page. Tapping a Prime-locked row was NOT attempted
  yet (to verify: does it open subscription page?).
- Evidence: EVI-SETT-003, EVI-SETT-008, EVI-SETT-009, EVI-SETT-001
- Confidence: Observed (visual); interaction Not Tested

## Cross-cutting notes

- The settings app renders controls with large touch targets, grouped section
  cards, and a persistent bottom tab bar; content that overflows one screen is
  collapsed into expandable sections (Advanced, Horizontal Scroll) rather than
  subpages in most cases.
- uiautomator exposes little semantic state for toggles (the app uses a
  Compose UI); screenshots are required as evidence for boolean states.
