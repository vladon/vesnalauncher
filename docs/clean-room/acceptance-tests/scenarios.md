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
