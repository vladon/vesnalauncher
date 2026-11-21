# Observations — Vesna Launcher release comparison

Installed on the research Pixel (55021JEBF03248) from the GitHub release
`v0.4.3` (2026-11-20). Purpose: the feature-state baseline against the Nova
findings and the Vesna specifications. Vesna runs as a foreground activity;
Nova remains the system HOME (comparison-only install).

## OBS-VESNA-001 — v0.4.3 feature baseline

- Date / Env: 2026-11-20 17:33 / ENV-1 (the Pixel, Vesna 0.4.3 over Nova)
- UI surface (single-container design): a workspace/app-list page with the
  search pill (grid icon + more button) and a 4-icon dock; a popup menu
  (Vesna settings / Set wallpaper / Default launcher / Add home page); a
  ONE-PAGE settings screen: Default launcher button, Theme
  (System/Light/Dark), Show app names toggle, MIT license + GitHub link.
- Feature state vs the researched areas:
  - Card system (conditions, schedules, provider cards, builder): NOT YET
    PRESENT.
  - Drawer groups / smart folders: NOT YET PRESENT.
  - Settings search field: absent; search-state reset was not applicable.
  - Widget placement: not yet observed in this pass.
- Settings opened on the single root page, without a search field or
  keyboard. This did not verify the full R-SETT-112 contract.
- The spec requirements (CARDS-100..105, DRAWER-106) are the design target
  for future releases; 0.4.3 predates them.
- Evidence: EVI-CARDS-001 (vesna-first-launch, vesna-explore-1,
  vesna-settings, vesna-settings-btm)
- Confidence: Observed

## OBS-VESNA-002 — v0.4.8 follow-up

- Upgraded the research Pixel in place from 0.4.3 to the
  [GitHub release v0.4.8](https://github.com/vladon/vesnalauncher/releases/tag/v0.4.8).
  The APK matched the published SHA-256; Vesna launched and Nova remained
  the system HOME.
- Observed on the Pixel: opening settings from Vesna's menu showed the
  Default launcher, Appearance, and About directory, with an empty,
  unfocused search field. Searching for `theme` returned the Theme control
  under Appearance. The first Back preserved the query and results; the
  second cleared the query and restored the directory.
- Force-stop check on the Pixel: with `theme` entered, the search field
  focused, and the keyboard visible, force-stopped Vesna. The process
  exited. After a cold launch and reopening settings from the menu, the
  full directory returned with an empty, unfocused field and no keyboard.
- Evidence: adb UI dumps before and after, input-method state, and
  `am start -W` reporting `LaunchState: COLD`. Confidence: observed.
- Direct links on the Pixel: Android resolved unscoped browsable `VIEW`
  intents for `vesna://settings/`, `/default-launcher`, `/appearance`,
  and `/about` to Vesna. UI dumps confirmed the expected page for each
  link, with no keyboard. Appearance was tested from a cold start; the
  other three links were opened while Settings was already running.
- Drawer cancellation on the Pixel: at stock display density the app grid
  fit on screen. At a temporary density of 560 dpi, held the AICore tile
  for one second, injected `ACTION_CANCEL`, then swiped upward. UI dumps
  showed the drawer stayed open and the top visible row moved from
  AICore/Calculator to Files/Find Hub. Restored the stock 420 dpi.
- The [release diff](https://github.com/vladon/vesnalauncher/compare/v0.4.3...v0.4.8)
  showed no implementation of cards, smart folders, or the card builder.
  Those acceptance scenarios remain untested.

## Comparison: Nova 8.9.9 vs Vesna releases

| Researched area | Nova 8.9.9 | Vesna 0.4.3 | Vesna 0.4.8 | Spec requirement |
|---|---|---|---|---|
| Card conditions render gate | Decorative; expiry is silent | No cards | No implementation found | Enforce conditions and surface expiry (NT-CARDS-01/02) |
| Met-later surface promise | Never surfaced | Not applicable | Not applicable | Enqueue within one refresh (NT-CARDS-03) |
| Manual-location fallback | Promised but absent | Not applicable | Not applicable | Only promise reachable surfaces (NT-CARDS-04) |
| Scheduled location go-live | Did not render at activation | Not applicable | Not applicable | Surface at go-live (NT-CARDS-03; CARDS-105) |
| Calendar connect gate | Runtime grant ignored | No Calendar card | No implementation found | One-step enable (NT-CARDS-06) |
| Smart folders | Off until enabled per folder | No smart folders | No implementation found | Master toggle and first-run choice (NT-DRAWER-01/02) |
| Empty generated folders | Unlabeled icons | Not applicable | Not applicable | Do not render empty groups (NT-DRAWER-03) |
| Builder save gating | Disabled Save; silent persistence | No builder | No implementation found | Explain disabled Save and define save behavior (NT-CARDS-10/11) |
| Hidden defaults | Activate on with a pre-attached ineffective condition | Not applicable | Not applicable | No hidden defaults (NT-CARDS-12) |
| Settings state hygiene | Focused search survives force-stop | Single page; no search | Clean directory after force-stop; two-step Back and all four direct links verified | R-SETT-112 partially verified |

## OBS-VESNA-003 — Workspace gesture comparison on the Pixel

| Gesture | Nova 8.9.9, current bindings | Vesna 0.4.8 |
|---|---|---|
| Swipe up from mid-screen (y=1500→700) | Drawer viewport stayed collapsed on retry | Opened the app drawer |
| Swipe up from lower screen (Nova y=2000→900; Vesna y=1950→850) | Opened the app drawer | Opened the app drawer |
| Swipe down from mid-screen on Home (y=700→1500) | Opened Android's notification shade | Stayed on Home |
| Double-tap between icons | No visible action | No visible action |
| Swipe left from the workspace interior | No visible change | No visible change |
| Swipe right from the workspace interior | Opened the Nova Now panel (Search apps, Nova AI) | No visible change |

- Existing [Nova gesture observations](gestures.md) document that a
  configured Quick Settings action can preempt drawer opening. No Nova
  bindings were changed in this pass. Vesna 0.4.8 wires workspace swipe-up
  directly to drawer opening; the settings directory has no gesture
  assignment page. These checks do not cover the full GESTURE-112 contract.
- The horizontal swipes started at x=850 and x=230 respectively, at
  y=1150. Vesna's release code routes horizontal swipes to page navigation;
  no page transition was observed on this layout. Multi-page traversal
  was not tested.
- Evidence: adb UI dumps for the initial vertical checks and focused
  Android window for the notification shade; retained screenshots and
  UI dumps in `EVI-VESNA-003` for the lower Vesna swipe, double-tap, and
  horizontal checks. Confidence: observed for the listed test conditions.
