# Acceptance Tests — Scenarios (Clean-Room)

Given/When/Then scenarios for approved specifications. Each scenario cites
its feature ID. These describe externally observable behavior only.

## Home screen

### AT-HOME-100 — Enter workspace customization
- Given the launcher is showing the workspace
- When the user long-presses an empty workspace area
- Then widgets, wallpapers, and settings entry points are offered and the
  dock is hidden or de-emphasized
- And pressing BACK collapses any open panel but keeps customization active
- And using the customization exit control returns to the normal workspace

### AT-HOME-101 — Widget picker grouping and search
- Given the user opened workspace customization and chose Widgets
- When the picker is shown
- Then widgets are grouped by app with per-app counts
- And typing in the picker search field narrows the groups
- And expanding an app shows widget previews with grid sizes

### AT-HOME-102 — Workspace icon popup menu
- Given an app icon on the workspace
- When the user long-presses the icon
- Then a popup anchored to the icon opens with launcher shortcuts and
  close/widgets/app-info/edit affordances
- When the user taps outside the popup
- Then the popup closes without launching anything

### AT-HOME-103 — Drag engagement
- Given an app icon on the workspace
- When the user touches the icon and moves immediately without holding
- Then no drag starts
- When the user holds the icon stationary past the long-press timeout and
  then moves
- Then the icon follows the drag

### AT-HOME-106 — Try Vesna without changing the default launcher
- Given Vesna is freshly installed and another launcher is system Home
- When the user opens Vesna by tapping its ordinary app icon
- Then Vesna shows its workspace without a blocking default-launcher prompt
- And the user can open Vesna's drawer and launch an installed test app
- When the user uses the Android Home gesture or button
- Then Android opens the original launcher, still selected as system Home
- And tapping the Vesna icon again opens the Vesna workspace
- And Vesna settings are reachable from that workspace, while direct
  settings links still open their intended pages

### AT-HOME-107 — Lock layout without blocking app launch
- Given a workspace shortcut and an empty cell, verify that a drag can
  move the shortcut while unlocked, then restore it to its original cell
- When the user enables the layout lock and repeats the drag
- Then the shortcut stays in its original cell
- And its long-press menu offers app information but no layout edits
- And holding empty space explains the lock instead of entering customization
- And a normal tap still launches the app
- When the user disables the lock, then the shortcut can be moved again

## App drawer

### AT-DRAWER-100 — Drawer opens with focused search
- Given the launcher is showing the workspace and "show keyboard on open" is
  enabled
- When the user swipes up from the lower part of the screen
- Then the drawer opens with the search field focused and the keyboard shown
- And a frequently-used predictions section is visible above the app grid
- And apps are listed alphabetically

### AT-DRAWER-101 — Search filtering with emphasized matches
- Given the drawer is open with search focused
- When the user types a query matching several app names
- Then the app matches are shown with the matched prefix emphasized
- And external provider and web suggestion rows are available
- And enabling "limit apps to one row" caps the visible app matches to one row

### AT-DRAWER-102 — Drawer closes via BACK
- Given the drawer is open with text in the search field
- When the user presses BACK once
- Then the search field clears
- When the user presses BACK again
- Then the drawer closes and the workspace is restored with its previous page

### AT-DRAWER-103 — Contact suggestions opt-in
- Given contact search has never been enabled
- When the drawer is opened
- Then a contact-suggestions offer bar is visible with enable and dismiss
  controls
- When the user taps enable
- Then the standard system Contacts permission dialog is shown before any
  contact data is used

### AT-DRAWER-102b — Swipe-down closes the drawer
- Given the drawer is open with the keyboard dismissed
- When the user swipes downward over the app grid
- Then the drawer closes and the workspace is restored

## Cards

### AT-CARDS-100 — Condition-met card renders in Nova Now search
- Given a card configured with a Day/time condition whose window covers the
  current time, its data permission granted, and "Activate in Nova Now" on
- When the user opens Nova Now and types a search query
- Then the card renders in the search results with live data
- And with the query cleared, the plain drawer browse view shows no card

## Backup & restore

### AT-BACKUP-100 — Backup creation
- Given the Backup & restore settings page
- When the user taps Backup and confirms the prefilled timestamped filename
  in the system file picker
- Then a single backup file is written to the chosen location
- And canceling the picker leaves all state unchanged

### AT-BACKUP-101 — Destructive actions guard rails
- Given the Backup & restore settings page
- When the user taps Restore backup or Reset to default
- Then an explicit confirmation is required before any configuration change

## Appearance

### AT-APPPEAR-100 — Theme mode applies instantly
- Given the Style settings tab
- When the user changes Mode to Dark
- Then the settings UI, drawer, and workspace chrome re-theme immediately
  without restart
- When the user changes Mode back to Follow system
- Then the previous theme is restored immediately

## Gestures

### AT-GESTURE-100 — Preset bindings work unconfigured
- Given the launcher with default gesture settings
- When the user swipes up from mid-screen
- Then the app drawer opens
- When the user swipes down from mid-screen on the workspace
- Then the notification shade expands
- When the user long-presses an empty workspace area
- Then workspace customization opens
- And all assignment rows in gesture settings show their current bindings

## Widgets

### AT-WIDGET-100 — Placement flow
- Given the widget picker is open in workspace customization
- When the user drags a widget preview onto an empty workspace area
- Then a preview follows the pointer during the drag
- And the system widget-consent dialog is shown before binding
- And a widget that declares a configuration activity opens it before final
  placement
- And the placed widget shows resize handles immediately with a settings
  entry point

### AT-WIDGET-101 — Resize
- Given a placed widget in resize mode
- When the user drags an edge or corner handle
- Then the widget resizes live in grid-cell increments
- And the final size respects the widget's declared minimum and maximum

### AT-WIDGET-102 — Removal with undo
- Given a placed widget
- When the user long-presses it and taps Remove
- Then the widget is removed immediately without a confirmation dialog
- And an "item removed" snackbar with an Undo action appears for a short
  period

### AT-WIDGET-103 — Consent pre-approval is honored
- Given a widget provider the user has pre-approved via the consent
  dialog's "always allow" checkbox
- When the user places another widget from that provider
- Then placement completes without re-showing the consent dialog

## Dock

### AT-DOCK-100 — Dock configuration surface
- Given the dock settings page
- When the user inspects the dock controls
- Then the dock can be enabled/disabled, its icon count set via a discrete
  selector, and pages/infinite-scroll configured
- And enabling "match desktop size" disables the independent icon-size
  control
- And labels and the background plate default to off

### AT-DOCK-101 — Dock rendering defaults
- Given the dock is enabled with default settings
- When the workspace is shown
- Then the dock renders as a single row of icons without labels or a
  background plate, above the search bar

### AT-DOCK-102 — Dock-toggle gesture owns its stream
- Given a dock-toggle gesture is assigned (e.g. swipe up) and a
  conflicting gestures-section binding exists on the same gesture
- When the user performs the gesture on the workspace
- Then exactly ONE action fires: the dock toggle, never the conflicting
  system-panel action
- And the gesture result is identical regardless of the swipe's start
  zone on the workspace

### AT-DOCK-103 — Dock config persists across restart
- Given a customized dock (count, pages, background, presentation mode)
- When the launcher process is force-stopped and restarted
- Then every dock setting and its contents restore exactly

### AT-DOCK-104 — Dock wraps only when enabled
- Given three distinguishable dock pages and infinite scroll disabled
- When the user swipes outward from the first and last dock pages
- Then each swipe leaves the dock on the current page
- When the user enables infinite scroll and repeats the boundary swipes
- Then the dock wraps from first to last and from last to first
- And the workspace page does not change in either setting

## Folders

### AT-FOLDER-100 — Folder creation
- Given two app icons on adjacent workspace cells
- When the user drags one icon onto the other (hold-then-move, release on
  the occupied cell)
- Then a folder containing both apps replaces the target icon
- And the source cell is empty
- And the folder icon previews both contained apps
- And the folder has no label until the user names it

### AT-FOLDER-101 — Folder rename
- Given a folder on the workspace
- When the user taps it and types a new name in the name row
- Then the name commits live
- And after closing the window the desktop folder shows the new label

### AT-FOLDER-102 — Folder window dismissal
- Given an open folder window
- When the user presses BACK
- Then the window closes and the workspace is restored

## Cards

### AT-CARDS-100 — Render matrix enforcement
- Given a configured card with Activate ON and its data permission granted
- When a name-matching query is typed in the search surface
- Then the card renders with live data
- When the card's condition expires, is never satisfied, or becomes
  satisfied later
- Then the search-surface render state follows the condition exactly (no
  stale render, no silent change), and expiry removes the condition row
- And the plain drawer never renders the card in any condition state

### AT-CARDS-101 — Permission enables the card in one step
- Given a provider card whose data permission has not been granted
- When the user grants the runtime permission through the standard dialog
- Then the card enables immediately with no further connect step
- And its empty state names the exact missing prerequisite

### AT-CARDS-103 — Custom card lifecycle
- Given the card builder with a name and at least one action
- When the user saves
- Then the card persists in the manager's custom section across restarts
  and renders for name-matching queries with its action chips
- And queries matching only an action's target app surface the target app,
  not the card
- When the user deletes the card from its details overflow
- Then it disappears without a confirmation dialog

### AT-CARDS-104 — Builder save gating with reasons
- Given a builder with only a card name
- Then Save is disabled with the live reason "Add at least one action to
  save"
- When the user attaches an action
- Then Save enables and the reason disappears
- When the user dismisses the builder with unsaved changes
- Then the discard path provably destroys the draft (nothing persists)

### AT-CARDS-105 — Scheduled location surfaces at go-live
- Given a saved location with a schedule set to a future time and day
- When that moment arrives
- Then the location surfaces on the surfaces the schedule copy promises
- And before the schedule it does not render
- When the location is deleted
- Then an undo snackbar appears and the schedule is removed with it

## Drawer groups

### AT-DRAWER-106 — Auto-folders render on enable
- Given auto-organization generates folders with member apps
- When the master toggle is turned ON
- Then every generated folder renders in the drawer grid immediately —
  no per-folder Enable required
- And populated folders render labeled with their member apps; empty
  generated folders do not render at all
- When any folder's enabled state is changed
- Then it persists across an app restart
- And the folder's editor shows its rules and enabled state in one
  surface
- Given two non-empty generated folders, one enabled and one explicitly
  disabled in its editor
- When the master switch is turned OFF and the drawer is reopened
- Then neither folder appears
- When the master switch is turned back ON
- Then the previously enabled folder reappears without individual
  re-enablement and the explicitly disabled folder stays hidden

## Gestures

### AT-GESTURE-112 — Single-owner routing and assignment safety
- Given a gestures-section binding and a separate drawer-open configuration
  exist for the same gesture
- When the gesture is performed from any start zone on the workspace
- Then exactly ONE bound action fires, identically from every zone
- And the bound action never silently changes through a misread dialog
- And every gesture target remains reachable through a deterministic
  non-gesture affordance

### AT-GESTURE-112.5 — Slow swipe does not open the workspace menu
- Given the drawer is closed and the workspace has two unobstructed
  starting zones for an upward swipe
- When a slow, continuous upward swipe exceeds touch slop before the
  long-press timeout and travels far enough for the long-swipe path
- Then the drawer opens once and the workspace menu never appears
- When the same slow motion ends short of the distance criterion and
  remains below the short-swipe velocity criterion
- Then neither the drawer nor the workspace menu opens
- And a faster swipe of that shorter distance opens the drawer when it
  meets the short-swipe criteria
- And replaying each trajectory with the same distance and duration from
  the other starting zone gives the same result
- And a stationary hold on an empty area still opens the workspace menu
  as a control

## Settings

### AT-SETTING-112 — Settings opens clean and deep-links land true
- Given the settings app was previously left on a subpage with search text
  and focus
- When the user cold-opens settings
- Then it opens on the root page with no search text, no field focus, and
  no keyboard
- When the user opens a named page via its deep link from any prior state
- Then that exact page is shown
- And with the search field focused, BACK dismisses the keyboard without
  leaking keystrokes into other surfaces
