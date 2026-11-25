# VESNA-QA-BADGE-COMPAT — badge compatibility deterministic checklist (#ROADMAP P3.3, v0.4.84)

Closes the research-matrix weak row "Badge compatibility — Inconclusive; no working conclusion" (ROADMAP P3.3: re-run the inconclusive research probe as a deterministic checklist once a work-profile AVD is part of the QA inventory — the condition became true with the #199 work on the managed-profile AVDs).

Devices: Android 15 research AVD `Nova_Reboot_API35` (emulator-5588, managed work profile user 11, Nova 9.0.1 Prime, Vesna v0.4.84 = 88) and Android 15 QA AVD (emulator-5560) for the control experiment.

## The deterministic probe (the missing piece since OBS-SYS-114)

`com.vesnaresearch.enumdiag` v2 (raw-SDK build; `<queries>` identical to Vesna's MAIN/LAUNCHER declaration; exported receiver actions POST/CANCEL): POST creates channel `badge` (IMPORTANCE_HIGH, `mShowBadge=true`) and posts notification id=42 "BadgeTest / Deterministic badge-compat probe notification" — importance 4, **no group**, flags=0. `POST_NOTIFICATIONS` granted headlessly (`pm grant`) for user 0 and user 11; verified active via `dumpsys notification` keys `0|com.vesnaresearch.enumdiag|42|null|10213` and `11|…|1110213`. CANCEL removes the record deterministically (record count 0 verified) — no shade-swipe flakiness.

## Checklist results

| Check | Evidence | Result |
|---|---|---|
| A deterministic badgeable third-party notification can be posted from adb | The active record above — the OBS-SYS-114 blocker (every prior route suppressed by design) is closed | PASS |
| Vesna v0.4.84 renders no notification badge while a badgeable notification is active | Drawer screenshot `vesna-drawer-badge.png` / `vesna-enumdiag-tiles.png`: EnumDiag (personal) and Work EnumDiag tiles show no dot/counter; the work briefcase badge stays | PASS (structural: no NotificationListenerService in the APK — #200 remains the privacy-gated feature ticket) |
| Work-profile badges unaffected by notification state | The Work EnumDiag tile keeps its briefcase badge with and without the notification | PASS |
| Nova's NotificationListener is grantable headlessly | `cmd notification allow_listener com.teslacoilsw.launcher/…notificationlistener.NotificationListener`; binding recorded in `dumpsys notification` (INotificationListener proxy) — OBS-VESNA-159's manual disallow/restore step is now scripted | PASS |
| Nova popup-menu notification content (the OBS-SYS-114 untested piece) | With the listener connected and the notification active: Nova's **Work-tab** long-press popup on the EnumDiag row shows only Edit / Hide / App info / Uninstall — no notification content section, no badge observed (`nova-hold2.png`, `nova-popup2.png`) | **NOVA-SIDE: content absent even with a live record** — either Nova's popup content does not cover Work-tab rows, or its badge pipeline needs further Nova-internal state; recorded as unconfirmed on Nova's own surfaces |
| Cleanup determinism | CANCEL action removes both records; `cmd notification disallow_listener` restores the listener set to AiAi + Pixel Launcher exactly as pre-experiment | PASS |

## Operational findings (recorded as OBS-SYS-117)

- Launching a second `emulator -avd Nova_Reboot_API35` against a running instance wedges the AVD: adb flaps offline, streamed installs and user-scoped dumpsys hang for minutes. The zombie **qemu-system-x86_64 child** survives its dead parent and holds the AVD locks — kill the qemu child (not just emulator.exe), clear `hardware-qemu.ini.lock`/`multiinstance.lock`, then cold-boot `-no-snapshot`.
- `userdata-qemu.img.qcow2` is an overlay on `userdata-qemu.img`; a backup needs both files beside each other. `qemu-img check` on the backed-up pair: no errors.
- `com.android.stk` (SIM Toolkit) does not resolve immediately after any reboot on these images — one saved tile stays hidden until the next refresh (pre-existing, noted in the #199 record).

## Limitations

- Nova badge rendering and popup content on Nova's own surfaces remain unconfirmed (the Drawer-groups mode collapses newly installed apps into categories, and the Work-tab row popup shows no notification section with a live record) — needs hands-on Nova-side state (badge config re-entry) at a future at-the-phone session.
- Any notification-badge FEATURE for Vesna remains #200 (NotificationListenerService privacy + product choices, recorded as blockers in the backlog).

Evidence: EVI-SYS-003 (`vesna-drawer-badge.png`, `vesna-enumdiag-tiles.png`, `nova-hold2.png`, `nova-popup2.png`), `dumpsys notification` keys quoted above. Research journal: OBS-SYS-116/117 (vesna-research branch, commit 505405b).
