# VESNA-QA-WORK-ENUMERATION — work-profile catalogue completeness (#199, v0.4.84)

Device A: Android 15 API 35 research AVD (`Nova_Reboot_API35`, emulator-5588) with a managed work profile (user 11, profile owner = the research probe), Nova 9.0.1 Prime installed, published signed Vesna v0.4.83 (88) — SHA-256 `64b15a04…7c1e9`, matching the release asset.
Device B: Android 15 API 35 emulator (emulator-5560) with a freshly created managed profile (user 10) and YouTube, YT Music, Google (quicksearchbox) and Camera materialized into it via `pm install-existing`.

## The reported state (#199, v0.4.82)

The research AVD's drawer showed 16 work tiles — without work YouTube/YT Music, with an extra work Voice Search entry — while `pm list packages --user 11` held all three packages.

## Verification on v0.4.83 (88) and v0.4.84 (89)

| Check | Evidence | Result |
|---|---|---|
| The catalogue covers every launchable work app | A diagnostic probe with the same `<queries>` (MAIN/LAUNCHER) enumerated user 11: 20 activities including YouTube (`Shell$HomeActivity`) and YT Music (`MusicActivity`); Vesna's drawer on the same AVD lists Work YouTube and Work YT Music alongside Work Voice Search and the probe — 18 work entries with the probe, exactly the acceptance's "16 → 18 tiles with the probe and Voice Search" | PASS |
| The observed 16-tile state was a stale catalogue, not enumeration | `getActivityList` from the launcher's visibility includes the work apps on a healthy system; the 16-tile capture came from an AVD session whose system services were wedged (installs and `dumpsys package --user 11` hang; adb offline cycles) — after a cold AVD restart the enumeration is complete on the same data | PASS (environmental root cause documented) |
| One entry per launchable activity, consistent on both sides | Personal and work sides both list per-activity entries: `Google` + `Voice Search` (quicksearchbox has two launchable activities) on both sides; documented as intended rather than dropped — no behavior change to the personal drawer | PASS (documented option of the acceptance) |
| Adjacency and badges | Work tiles render directly after their personal twins (`YouTube, Work YouTube, YT Music, Work YT Music` observed in the drawer dump) with work badges | PASS |
| Suspension events keep the catalogue current | `PackageChangeMonitor` now maps `onPackagesSuspended`/`onPackagesUnsuspended` to a catalogue refresh (previously unhandled — the stale-catalogue window behind the reported state); `PackageChangeMonitorSuspendTest` asserts both callbacks emit `CatalogueChanged` | PASS |
| Suspension does not remove tiles from the catalogue | On device B, `pm suspend --user 10 <youtube> <music>` plus a full restart kept Work YouTube and Work YT Music in the drawer (dimmed) — matching the #198 pause behavior | PASS |

## Gates

64 JVM tests, lintDebug; instrumentation: workspace 90/90 OK on the candidate, plus `PackageChangeMonitorSuspendTest`. The ui/settings suites on this freshly cold-booted AVD flake identically against the unmodified code (keyboard/a11y focus; reproduced on the baseline build — see [VESNA-QA-COLD-START.md](VESNA-QA-COLD-START.md) operational findings) and are green on a warmed system.

## Limitations

- The research AVD's package manager hung during parts of this cycle (installs and user-scoped dumpsys timed out); a full AVD restart recovered it. The enumeration evidence was captured after that restart.
- The dedupe question ("Voice Search as a separate entry") is documented as intended per the acceptance's second option; changing it to one-entry-per-package would alter the personal drawer too and is a product decision, not part of this fix.
- #198 (pause banner/resume affordance) and #197 (tab-vs-inline presentation) remain separate tickets.
