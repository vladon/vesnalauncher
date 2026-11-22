# Implementation Tickets — from the Nova 8.9.9 design-lessons research

Derived from `design-lessons.md` (per-gap fix guidance). Each ticket cites the
Nova counter-example it prevents and the acceptance scenario that proves the
fix. IDs are prefixed NT (Nova-translated) and map to the spec requirements in
`specifications/cards.md` (R-CARDS-10x) and `specifications/app-drawer.md`
(R-DRAWER-106.x) and `specifications/dock.md` (R-DOCK-100.x).

Priority: P1 = ship-blocker for the cards/folders feature; P2 = required
before the feature leaves beta; P3 = polish.

---

## NT-CARDS-01 (P1) — Single card-gate evaluation engine
- Gap: 1 (conditions decorative). Spec: R-CARDS-100.3, R-CARDS-105.2.
- Build one `CardGate.evaluate(card, surface, now)` consumed by both the
  drawer and the search/card overlay. No surface may implement its own gate.
- Accept: AT-CARDS-100 (the full render matrix, both surfaces).
- Nova counter-evidence: expired/never-met conditions rendered; met-later
  never surfaced the card on the promised surface.

## NT-CARDS-02 (P1) — Condition state machine with loud expiry
- Gap: 1. Spec: the condition state model (`never-met → active → expired →
  removed`).
- Expiry REMOVES the condition row from config with a visible toast. A
  condition never silently outlives its window (Nova: 2 h+ and 17 h+
  persistence observed) and never disappears unexplained (one Nova vanish
  remains unexplained).
- Accept: expiry asserts the row is gone AND the config reflects it; the
  disappearance is never observed without a user action.

## NT-CARDS-03 (P2) — Met-later enqueue on the promised surface
- Gap: 1. Spec: R-CARDS-105.2.
- When a condition transitions to met, the card appears on the promised
  surface within one UI refresh.
- Accept: the later-satisfied case asserted (Nova never surfaced the card —
  the drawer stayed empty through an active go-live).

## NT-CARDS-04 (P1) — Fallback copy is a ticket-gated surface
- Gap: 2 (manual-location promise unfulfilled). Spec: R-CARDS-100.4.
- Every fallback string ships with a linked, implemented, discoverable
  settings surface. CI/lint: each fallback string maps to a verified
  settings-search hit (search under the card name, the feature word, and
  the fallback word).
- Accept: the settings search for the fallback's keywords returns the
  working surface (Nova: four queries returned nothing).

## NT-CARDS-05 (P2) — Shared location model for provider cards
- Gap: 2 (the location editor existed only on an unrelated card).
  Spec: R-CARDS-102.1.
- All location-consuming cards read one location store with per-card
  overrides; a location editor is reachable from every card that renders
  location-dependent content.
- Accept: the weather card's location editor is reachable from the weather
  card's own config (Nova: its editor existed solely under a different
  card).

## NT-CARDS-06 (P1) — One-step permission enable for provider cards
- Gap: Calendar connect gate. Spec: R-CARDS-101.1, R-CARDS-101.2.
- Granting the runtime permission enables the card immediately; the empty
  state names the exact missing prerequisite and offers the one-step action.
- Accept: AT-CARDS-101. Nova counter-evidence: granted READ/WRITE_CALENDAR
  ignored across restarts until the in-app "Connect" flow ran.

## NT-CARDS-07 (P2) — Provider card content-matching render
- Gap: Map locations render inert. Spec: R-CARDS-102.1, R-CARDS-102.2.
- A provider card with data renders for content-matching queries; the
  visible timestamp never contradicts the shown data.
- Accept: with a saved location, the card renders for the location query,
  the card-name query, and partials (Nova: all negative in every state).

## NT-CARDS-08 (P1) — Custom card name-matching render
- Gap: verified WORKING in Nova (keep the behavior). Spec: R-CARDS-103.1,
  R-CARDS-103.2.
- Custom cards render for name-matching queries with a distinct marker and
  their action chips; action-target queries surface the target app only.
- Accept: AT-CARDS-103 (verified pattern — preserve it in Vesna).

## NT-CARDS-09 (P2) — Custom card persistence and clean delete
- Spec: R-CARDS-103.3.
- Custom cards persist across restarts; deletion from the details overflow
  removes them without confirmation and provably leaves no residue.
- Accept: AT-CARDS-103 delete branch + a post-delete restart check.

## NT-CARDS-10 (P1) — Builder save gating with live reasons
- Gap: 4 (grey Save, no reason). Spec: R-CARDS-104.1.
- A disabled Save carries its live reason inline; the reason list updates as
  requirements are met.
- Accept: AT-CARDS-104 middle branch. Nova counter-evidence: grey Save with
  no explanation, and the reason never surfaced anywhere.

## NT-CARDS-11 (P1) — Total save semantics
- Gap: 4 (silent creation through the discard path). Spec: R-CARDS-104.2.
- Commit or provably discard — never both-neither. The discard path destroys
  the draft deterministically.
- Accept: AT-CARDS-104 discard branch + a restart check proving no residue
  (Nova: a name-only card persisted through its grey-Save discard path).

## NT-CARDS-12 (P2) — No hidden defaults on new cards
- Gap: 4 (Activate-ON + a pre-attached never-satisfiable Wi-Fi condition).
  Spec: R-CARDS-104.3.
- New cards start with no conditions attached and no implicit activation
  state beyond the user's explicit choices.
- Accept: a fresh builder's card config equals the documented empty default.

## NT-DRAWER-01 (P1) — Master toggle is sufficient for generated folders
- Gap: 3 (folders dark-by-default). Spec: R-DRAWER-106.1, R-DRAWER-106.4.
- Master toggle ON = every generated folder renders immediately (live, no
  restart). Enable state persists across restarts. No per-folder Enable may
  gate what the master toggle claims to control.
- Accept: AT-DRAWER-106 (Nova counter-evidence: seven folders, master ON,
  zero rendered until each was individually enabled).

## NT-DRAWER-02 (P1) — First-run choice for auto-organization
- Gap: 3 (discoverability). Spec: R-DRAWER-106.2.
- Auto-organization presents its choice once, surfaced prominently (not a
  buried per-item menu), and shows the generated result on acceptance.
- Accept: the first-run flow demonstrates the rendered outcome before
  dismissal.

## NT-DRAWER-03 (P2) — Empty generated folders do not render
- Gap: 3 (bare unlabeled icons). Spec: R-DRAWER-106.3.
- Empty generated folders are hidden; populated ones render labeled with
  their member apps.
- Accept: AT-DRAWER-106 folder branch (Nova: empty folders rendered as bare
  unlabeled icons).

## NT-DOCK-01 (P1) — One owner per gesture stream
- Gap: gesture-binding precedence (system-panel actions preempting launcher
  gestures from any swipe zone). Spec: R-DOCK-100.10, R-DRAWER-106 context.
- The gesture router assigns ONE owner per gesture stream; system-panel
  actions (quick settings, notifications) route through the same dispatcher
  as launcher gestures. The gesture result must be identical regardless of
  the swipe's start zone.
- Accept: AT-DOCK-102 (Nova counter-evidence: the quick-settings binding
  preempted the drawer gesture from both zones while the notifications
  binding fired from neither).

## NT-DOCK-02 (P2) — Dock config persistence across restart
- Spec: R-DOCK-100.9.
- Dock contents, count, pages, background, and presentation mode restore
  exactly after a force-stop restart.
- Accept: AT-DOCK-103.

## NT-DOCK-03 (P3) — Widget consent pre-approval honored
- Spec: R-WIDGET-100.5.
- A provider pre-approved via "always allow" never sees the consent dialog
  again; Vesna adds no re-prompt of its own.
- Accept: AT-WIDGET-103 (verified working in the platform — preserve it).

## NT-DOCK-04 (P2) — Dock fixed during workspace page swipes
- Spec: R-DOCK-100.11.
- Page navigation never moves or hides the dock; multi-page docks scroll
  only via their own page gesture.
- Accept: page-swipe asserted with the dock pixel-stable.

## NT-DOCK-05 (P2) — Dependent options visibly explain their dependency
- Spec: R-DOCK-100.12.
- Options disabled by an unmet dependency (e.g. "auto-close on app launch"
  without a toggle gesture) are greyed WITH their explanatory subtitle —
  never silently disabled without explanation.
- Accept: the dependency state is asserted visible next to the control.

## NT-WIDGET-01 (P2) — Resize-mode settings affordance
- Spec: R-WIDGET-101.5.
- Resize mode entry pairs the handles with an explicit settings affordance
  (edit control + discovery toast) — widget settings reachable without a
  long-press.
- Accept: the affordance is asserted present in placement-time resize mode.

## NT-DRAWER-04 (P1) — One explicit grid-dimension notation
- Gap: Nova's drawer label "6 ✕ 5" reads rows×columns while its desktop
  label "6 ✕ 5" renders columns×rows — identical notation, opposite
  meanings, adjacent pages. Spec: R-DRAWER-101.5.
- Every grid dimension label uses explicit units ("6 columns × 5 rows").
- Accept: no bare A ✕ B dimension label anywhere in settings.

## NT-SETTING-01 (P1) — Settings opens clean, every page deep-linkable
- Gap: settings state (page/scroll/query/focus) persisted across
  force-stops, landing sessions in a focused garbage search field that
  swallowed navigation. Spec: R-SETT-112.1, R-SETT-112.2, R-SETT-112.3.
- Cold open lands on the root with no focus; every named page has a deep
  link; BACK dismisses the keyboard before leaking keystrokes.
- Accept: cold-open asserts root+no-focus; each page's deep link asserted
  from a dirty prior state.

## NT-SETTING-02 (P2) — Unsaved-changes dialogs fully actionable
- Spec: R-SETT-112.4.
- Discard/Save dialogs keep both options enabled and state what would be
  lost.
- Accept: the dialog asserted with both options actionable (Nova's pattern —
  preserve; its greyed-Save variant in the builder is covered by
  NT-CARDS-10).

## NT-GESTURE-01 (P1) — Single-owner gesture routing
- Gap: binding-dependent precedence (QS binding preempted the drawer gesture
  zone-independently; notifications fired from neither). Spec:
  R-GESTURE-112.1, R-GESTURE-112.2, R-GESTURE-112.4.
- ONE owner per gesture stream, zone-independent; system-panel actions route
  through the same dispatcher; every gesture target has a deterministic
  non-gesture affordance.
- Accept: AT-DOCK-102's ownership branch + the grid-icon affordance
  asserted reachable.

## NT-GESTURE-02 (P2) — Assignment dialogs confirm or undo
- Spec: R-GESTURE-112.3.
- Assignment changes apply only with confirmation (or an undo path); the
  dialog names its target gesture prominently beyond the title.
- Accept: a wrong-gesture tap asserted recoverable without reassignment
  through the menu.

---

Traceability: every ticket's acceptance scenario lives in
`acceptance-tests/scenarios.md`. The Nova counter-evidence lives in
`observations/cards.md` (OBS-CARDS-117..119), `observations/app-drawer.md`
(OBS-DRAWER-112), and `observations/gestures.md` (OBS-GESTURE-112).

## NT-GESTURE-03 (P1) — Slow upward swipe opens the workspace menu

- Gap: Vesna 0.4.10 can open the long-press workspace menu during a
  continuous 1100 px upward swipe. On the research Pixel, the menu
  opened at 725, 750, and 775 ms in both trials at each duration
  (OBS-VESNA-004; EVI-VESNA-003). These timings describe the repro,
  not production thresholds.
- Spec: R-GESTURE-112.1 and R-GESTURE-112.5.
- Expected: cancel the pending workspace long press when movement
  exceeds touch slop. A qualifying slow, long swipe opens only the
  drawer; an unqualified moving swipe opens neither surface.
  A stationary hold still opens the workspace menu.
- Accept: AT-GESTURE-112.5, including matched start zones, a shorter
  slow swipe, a faster short swipe, and the stationary-hold control.

## NT-HOME-01 (P1) — Open the workspace from Vesna's app icon

- Gap: with another system Home selected, Nova's app icon opens its
  workspace; Vesna 0.4.44 opens Settings (OBS-VESNA-016).
- Expected: the normal Vesna icon opens an interactive workspace and
  drawer without requesting a Home-role change. Keep system Home and
  Settings entry points independent.
- Spec: R-HOME-106.1–3. Accept: AT-HOME-106, starting from a fresh
  installation on an emulator with a different system Home.
