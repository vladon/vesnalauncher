# Vesna Design Lessons from Nova 8.9.9 Black-Box Research

Four verified quality gaps in Nova 8.9.9 (Prime), observed across 2026-11-18/19
research rounds on the Pixel 9a. Each is a shipping failure a Vesna build should
never repeat. Evidence lives in `observations/cards.md`, `observations/app-drawer.md`,
`observations/gestures.md` (OBS-CARDS-112..119, OBS-DRAWER-112, OBS-GESTURE-112).

## Gap 1 — Conditions are decorative

Nova's card conditions ("when all met, will cause this card to show in the app
drawer") do nothing at runtime:

- An expired Day/time condition stays attached 2 h+ past its window (one lasted
  17 h+; one vanished unexplained) and never blocks rendering.
- A never-satisfied Wi-Fi condition renders anyway.
- A later-satisfied false→true transition never adds the card to the plain drawer.

The only render requirements are Activate + Location permission + a typed query —
conditions are configuration theatre.

**Vesna rule**: every condition must be evaluated continuously against every
surface it claims to gate, and its expiry must remove it. If a gate cannot be
enforced on a surface, the surface's copy must not claim the gate.

## Gap 2 — The manual-location promise is unfulfilled

The Nova Now welcome sheet says "Alternatively, set a manual location in Nova
Settings" for the Weather card. No such setting exists — verified against four
search queries, both card config pages, the overlay long-press/gears/overflow,
the full Integrations inventory, and the Cards list (the sixth built-in,
"Map locations", was found later and has its own location editor — for a
different card).

**Vesna rule**: every advertised fallback must be implemented AND discoverable
before the copy ships. Search-test the fallback's own keywords; walk every
surface the copy names.

## Gap 3 — Smart folders ship dark-by-default

Nova auto-categorizes apps into folders, writes them with members, shows a
master "Smart Folders" toggle — and then hides every folder. Each ships
per-folder DISABLED behind a per-item ⋮ ▸ Enable menu; the master toggle alone
is insufficient. The drawer render was "inert" until the per-folder Enable was
discovered.

**Vesna rule**: auto-generated structures must surface their own enablement (a
banner, a badge, a first-run choice) — never a buried per-item menu. And an
empty folder should not render as a bare unlabeled icon.

## Gap 4 — Builder save gating without feedback

The custom-card builder disables Save for a name-only card with no visible
reason, and a name-only card still ended up persisted through the discard-dialog
path. Save state never explains what is missing.

**Vesna rule**: a disabled primary action must say why ("Add at least one
action to save"), and the discard path must never silently create.

## Cross-cutting lessons

1. **Copy must match behavior.** Gaps 1–3 are all shipping copy promising
   behavior that does not exist. Test every sentence of feature copy as an
   acceptance criterion.
2. **Permissions are not integrations.** Nova's Calendar card ignored an adb
   -granted calendar permission until its in-app "Connect" flow ran. Runtime
   grants are necessary, not sufficient, for provider cards.
3. **State gates need state tests.** The condition matrix (expired /
   never-satisfied / later-satisfied) caught what a single happy-path test
   never would: every gate needs its false, expired, and later-true cases.
4. **Search-state traps.** Nova's Settings persists page, scroll, and query
   across force-stops; dead config pages look identical to live ones in
   accessibility dumps. Verify with screenshots, and design Vesna's own
   settings to reset cleanly.
5. **Inventory completeness.** The sixth built-in card was found only after
   scrolling to the TRUE bottom — inventories that stop at an assumed tail
   poison downstream conclusions ("no location setting anywhere").

## Status

- All four gaps documented as Observed with evidence IDs; the condition matrix
  and the render model are complete (see COVERAGE_MATRIX.md CARDS-100..105,
  DRAWER-106).
- Remaining open questions: Map locations render with a future-day schedule;
  smart-folder Enable persistence across restart (device work — blocked while
  the Pixel 9a is unplugged).
