# ADR-WIDGET-1 — Widget host ownership and cleanup

**Status:** Proposed for review. This is a lifecycle contract, not a shipped widget feature.

**Gates:** [ENG-102](../engineering/BACKLOG.md#eng-102-widget-host--lifecycle-only).

## Context

Vesna's active page has stable Room IDs and a fixed grid. The repository serializes layout mutations and checks occupancy at commit time. Android separately owns AppWidget bindings; allocating an ID and committing a Room row cannot be one atomic operation. A cancellation, crash or package change must never leave a ghost cell, leak a host ID, or disturb app shortcuts and folders.

[Android host contract](https://developer.android.com/develop/ui/views/appwidgets/host) · [Host API](https://developer.android.com/reference/android/appwidget/AppWidgetHost) · [Provider configuration](https://developer.android.com/develop/ui/views/appwidgets/configuration) · [Page identity](0001-workspace-page-identity.md)

## Proposed decision

- One stable `AppWidgetHost` ID owned by Vesna. Start listening while Home is visible; stop when it is not. Render the platform `AppWidgetHostView` and report the actual rendered size to the provider. The host ID is independent of page order, activity instances and app versions.
- A committed widget record contains the host instance ID, provider identity, stable page ID, top-left cell and spans. Apply an additive Room migration; ship no destructive fallback. The single page occupancy check must include every widget cell alongside shortcuts and folders, including during placement, ordinary icon moves, folder dissolution and page removal. Reject a rectangle outside the grid or intersecting any occupied cell; never displace icons or auto-create a page.
- Obtain an ID only once the user chooses an eligible placement. Try Android's binding API first and launch the system binding UI only if it is needed. Follow the platform configuration contract for the bound provider. Do not add a Vesna consent prompt. Revalidate the pending operation and rectangle after external results; only then write the completed placement. Decline, cancellation, a missing configuration activity, a stale result or an invalid final destination must not commit a placement.
- Keep at most one placement in flight. Preserve its ID and operation identity through activity recreation; ignore callbacks for an operation that is no longer current. Cancellation deletes only that ID. For a committed removal, delete the Room record first, then the binding. A restart may clean up a temporarily orphaned binding without leaving a visible widget that refers to a deleted ID.
- At startup, after restoring any active placement, reconcile this host's IDs against committed rows and that pending operation. Delete unused IDs individually, not with package-wide `deleteAllHosts`. For an established local layout, remove records for confirmed-lost bindings without touching other rows. Do not treat a temporarily unavailable provider as an uninstall. Reconcile again when Android confirms provider removal, and test provider replacement separately.
- IDs are device-local. Do not bind a restored row to an arbitrary provider just because an integer matches. Use an install-local identity excluded from backup to distinguish restored from live placement state. Do not promise migration of widget configuration between devices; require an explicit restore policy and transport verification before publishing widget support. See [ADR-BACKUP-1](0002-backup-and-privacy.md).

## Why this approach

A single transaction cannot cover both the Android widget service and Room. A committed placement plus reconciliation keeps the durable layout authoritative and fixes crashes in either ordering. Bulk deletion would also remove valid widgets. Persisting an unconfirmed placeholder would let an abandoned activity occupy workspace cells indefinitely.

## Acceptance before enabling widget placement

| Scenario | Observable result |
| --- | --- |
| Cancel Android binding; cancel or fail provider configuration | No Room row, no occupied cell, no residual host ID. Existing widgets and shortcuts are unchanged. |
| Kill the process after allocation, before Room commit, and after a removal commit but before deleting the ID | On reopen, orphaned IDs are removed; a widget with a committed row survives. |
| Rotate or recreate Home while binding or configuration is active | No duplicate allocation. A successful result commits once to the originally requested page and rectangle if still valid. |
| Occupy the target or remove its page while awaiting configuration | Commit rejects the placement and releases its ID. |
| Restart and reboot with a valid widget; uninstall and separately update a provider | The valid placement survives. Uninstall frees its cells and binding; update does not remove the placement. |
| Restore app data with a missing or mismatched install-local identity | Never attach an unrelated ID or alter app and folder placements. |

Run on API 26 and a current Android release with a fixture provider that has a deterministic configure/cancel path. Record Room rows, host IDs and relevant Home behavior. The [published widget scenario](https://github.com/vladon/vesnalauncher/blob/9954a278758958da8d0b0d878bc18e6a9fbeb0e8/docs/clean-room/acceptance-tests/scenarios.md#at-widget-100--placement-flow) covers launching configuration; it does not demonstrate cleanup when configuration is canceled. This document defines Vesna's acceptance requirement, not an observation about another launcher.

Resize handles, undo, overlap controls and widget-specific appearance are separate decisions. In particular, preserving provider configuration for Undo requires retaining an ID until removal becomes final; do not implement Undo by allocating a replacement ID.

## Android platform probe

On dedicated Android 8 (API 26) and Android 15 (API 35) emulators, a
temporary instrumentation probe passed once on each device. It read the
initial host ID set, allocated an ID without binding, confirmed no provider
info existed, and enumerated the same ID from a new `AppWidgetHost` instance
with the same host key. After `deleteAppWidgetId`, the ID set matched the
baseline again. This establishes the enumeration and cleanup mechanism on
the tested images; it does not verify process interruption, binding consent,
configuration cancellation, or restore.
