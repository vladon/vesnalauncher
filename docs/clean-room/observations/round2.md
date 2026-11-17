# Observations — Prime Gating, Subscription, Cards, Tabs (ENV-1, 8.9.9 free)

Second-round observations. Evidence: EVI-SYS-002, EVI-BACKUP-003,
EVI-SETT-003, EVI-DRAWER-002, EVI-CARDS-001.

## OBS-SYS-110 — Tapping a Prime-locked row opens the subscription page

- Date / Env: 2026-11-17 / ENV-1
- Action: tapped the Prime-locked "Notification badges" row in main settings.
- Observed: opens the full-screen "Manage Subscription — Choose your plan"
  page (back-arrow page, not a dialog): Nova Prime card ($4.99/year;
  ad-free, custom gestures & swipe actions, hide apps for privacy,
  themes/icons/animations) and Nova Plus card ($99.99/year "Save 44%" or
  $14.99/month radios; adds Personalized AI insights daily + AI assistant on
  your home screen), each with "Try 3 days free"; close (X) top-right.
- Same page opened when tapping Settings ▸ Nova AI (earlier OBS-SETT-009,
  dark themed there) and when tapping "Quick start" (see OBS-BACKUP-110).
  On ENV-2 the same page rendered with BLANK price amounts (no Play billing
  availability).
- Resolves OPEN_QUESTIONS #5.
- Evidence: EVI-SYS-002 (prime-row-tap)
- Confidence: Observed

## OBS-BACKUP-110 — Quick start and Reset to default flows (free edition)

- Date / Env: 2026-11-17 / ENV-1. A fresh backup
  (`2026-11-17_23-11.novabackup`) was created first as a guard rail.
- Quick start: tapping it opens the subscription page (PAYWALLED in free
  edition) — the setup re-run is a paid feature.
- Reset to default: tapping it launches the DARK-THEMED setup wizard
  directly (no separate confirmation dialog — the wizard itself is the
  flow; progress-bar steps: Search ▸ App drawer ▸ Icon style ▸ All done).
  The wizard pre-fills CURRENT values (observed: Mode Immersive, Shortcut
  Search ON, Contact Search OFF, search placements Dock (Below icons)/Top,
  icon size 125%). Completing it ("All done ▸ Go home") returned to the
  workspace with the LAYOUT INTACT (Media folder, Play Store, dock,
  shortcut all survived).
- Interpretation (honest limits): in this pass every touched setting was
  already at its default, so which values the reset actually changed is not
  measurable; the layout was NOT wiped by the reset (differs from the
  preconception that reset restores the out-of-box layout). Reset semantics
  remain partially undetermined.
- Wizard bonus: the Search page's "Search bar placement" dropdown exposed
  the full option list: None / Widget / Dock (Above icons) / Dock (Below
  icons) / Persistent.
- Evidence: EVI-BACKUP-003 (backup-page2/3, quickstart-flow, reset-flow,
  reset-wizard1..3, after-reset); fresh backup verified via shell ls
- Confidence: Observed (flows as far as run); reset semantics Partially
  Observed

## OBS-SETT-110 — Settings search field scope and deep-linking

- Action: tapped the pinned search field atop main settings, typed "dock".
- Observed:
  - results render as cards with a SECTION BREADCRUMB + title + subtitle:
    "Home screen ▸ Dock — Icon layout, background, pages" and
    "Home screen ▸ Page indicator — Displays above the dock…" (match on
    title AND subtitle text).
  - tapping a result deep-links into the section, pre-scrolled to the
    highlighted target row (Dock card highlighted at top of Home screen
    section).
- Evidence: EVI-SETT-003 (search-results, search-deeplink)
- Confidence: Observed

## OBS-DRAWER-110 — Drawer tabs in the free edition

- Action: App drawer settings ▸ Tab bar placement [None] → selected "Top";
  opened the drawer.
- Observed: options are None / Top / Bottom. With Top, the drawer shows a
  tab bar under the search area containing a single default tab "Apps"
  (pill-highlighted). Custom tab groups remain Prime ("Drawer groups"
  greyed) — the free edition gets the tab UI with the automatic Apps tab.
  Reverted to None after capture.
- Evidence: EVI-DRAWER-002 (tab-options, tabs-open)
- Confidence: Observed

## OBS-CARDS-110 — Cards internals: condition-triggered drawer cards

- Action: Settings ▸ Cards ▸ tapped AccuWeather's gear.
- Observed: "Card details" page (Save + overflow menu) with:
  - a live card preview;
  - an "Activate in Nova Now" section: "Configure a set of conditions
    that, when all met, will cause this card to show in the app drawer";
  - condition adders: Day/time (+); Wi-Fi network (+) "Requires Location
    permission"; When weather changes (+); Bluetooth connection (+)
    "Requires Bluetooth permission".
- Interpretation: Cards are condition-triggered content cards whose target
  surface is the APP DRAWER (the "Nova Now" branding on Style/Search
  surfaces refers to this drawer card layer). Conditions are composable
  ("all met") and permission-gated. Default card set: Media controls,
  AccuWeather, Games; a (+) FAB adds cards; per-card overflow exists.
- Evidence: EVI-CARDS-001 (cards-page2, card-config)
- Confidence: Observed (config page); condition behavior Not Tested

## OBS-WALL-002 — Third wallpaper image observed

- The workspace wallpaper changed again during the reset-wizard run
  (petals ▸ blue glass ▸ swirl variants observed across the session). No
  launcher wallpaper action was performed at any point. Consistent with a
  periodic OS/user-level wallpaper rotation on Android 17; not attributable
  to Nova. (Extends OBS-WALL-001.)
- Evidence: EVI-BACKUP-003 (after-reset), EVI-CARDS-001 (cards-page)
- Confidence: Partially Observed (rotation pattern; trigger external)
