# Observations — Round 4 Log (2026-11-18, chronological index)

## OBS-SYS-112 — Badge pipeline fully enabled; Nova self-badge does not render

- Date / Env: 2026-11-18 / ENV-1 (Nova Prime)
- Enablement chain (all verified):
  1. After the user's reboot, Nova's notification listener IS BOUND
     (`com.teslacoilsw.launcher/.notificationlistener.NotificationListener`
     appears in the active listener list with a live binder proxy).
  2. Via system App info ▸ Notifications, "All Nova Launcher notifications"
     was toggled ON (`AppSettings … importance=DEFAULT userSet=true`; before:
     `importance=NONE userSet=false` — "Notifications: Off").
  3. The built-in "Test notification badge" button then posted a LIVE
     notification (importance=3): "This is a test notification, check the
     Nova Settings icon for its badge".
- RESULT: **no badge renders on any Nova Settings icon instance** — the
  workspace shortcut, the drawer frequently-used icon, and the drawer
  prediction chip were all zoom-verified (4× nearest-neighbor crops) with
  the notification live: no corner dot, no content circle.
- CROSS-EVIDENCE that the badge pipeline itself works: a Play Store
  notification rendered a dynamic badge (avatar content) on the Play Store
  icon minutes earlier, and cleared when the notification cleared.
- Conclusion: badges render for OTHER apps' notifications; Nova's SELF
  notification does not badge its own icon in this build/configuration
  (candidates: self-notification suppression, badge target resolution
  preferring the launcher shortcut definition, or Small-size dynamic style
  quirk). OPEN — re-test with a different badge style (numeric/dots) and
  with a third-party app notification for a fuller matrix.
- Bonus finding: `am start -n com.teslacoilsw.launcher/.NovaSettings` fails
  with "Error type 3 — Activity does not exist": **the `.NovaSettings`
  activity is not in the manifest**; earlier am-start attempts silently
  failed (stderr was suppressed) while the surface appeared to change due to
  the drawer. The reliable settings entries are the workspace "Nova
  Settings" shortcut and the drawer prediction chip.
- Evidence: EVI-SYS-002 (post-reboot-state, badges-nav-r3*, badges-page-r3*,
  badge-test-r3/final/success, drawer-badge-check, badge-zoom, zoom-chip,
  zoom-frequent)
- Confidence: Observed (enablement chain + absence); root cause open

## Interruption record correction (from round 3)

- The "Tilt your head down" face prompt encountered at ~23:25 on 2026-11-17
  was **Google Play's biometric verification for the user's Nova Prime
  lifetime purchase** (the purchase completed minutes later) — not a device
  unlock attempt as initially assumed. The research's Cancel tap aborted the
  user's first verification attempt; the user re-verified and the install
  proceeded. Recorded for an accurate session chronology.
