# Vesna Launcher Design System

**Version 1.1 · Material Design 3 · Proposed final specification**

Vesna Launcher is a fast, free, open-source Android home launcher. Material Design 3 supplies the product UI's color roles, type scale, shapes, elevation and component patterns. Vesna's spring identity supplies the artwork, color seed and type families. The launcher gives installed apps and the user's wallpaper first priority.

[Material 3 component preview](preview.html) · [Design tokens](tokens.json) · [Android Adaptive Icon files](adaptive-icon/res/) · [Material 3 on Android](https://developer.android.com/develop/ui/compose/designsystems/material3)

## Reference review

| Reference | What carries forward | What changes in the specification |
| --- | --- | --- |
| [Spring-inspired branding board](../vesna_launcher_spring_inspired_branding_board.png) | Sprouting V, stacked Vesna Launcher wordmark, five core brand colors, and flowing spring artwork. | The lockup uses one vector drawing and a fixed relationship between the symbol and letterforms. The vertical rule is dropped at all sizes to simplify the lockup. |
| [App icon exploration board](../vesna_launcher_icon_exploration_board.png) | Off-white field, two-tone V and an independent leaf. | The Android icon uses the simplified C composition for a clear silhouette at launcher size. Waves remain available as a visual motif for optional wallpaper and brand artwork. Android supplies the mask. |
| [Visual style guide](../vesna_launcher_visual_style_guide.png) | Nunito and Inter, rounded controls, visible drawer search and an 8 dp spacing rhythm. | Fresh Teal is normalized to the branding board's `#2EC6B8`. The style guide shows `#2EC6BB`. The Material 3 type scale, color roles and components define the interface treatments in both themes. |

White on the brand's Spring Green, Fresh Teal and Sky Blue measures 1.48:1, 2.12:1 and 2.12:1 respectively. Use the theme's foreground and container pairs for controls. The concept home screen's clock, weather, Google-branded search bar and decorative Vesna mark are visual references. The current checkout contains the workspace, dock, searchable drawer, folders, and Settings with theme selection and About. [Product overview](../../PRODUCT.md) · [Launcher UI source](../../app/src/main/java/dev/vladon/vesna/launcher/ui/) · [Settings source](../../app/src/main/java/dev/vladon/vesna/launcher/settings/SettingsScreen.kt)

## Logo

![Vesna Launcher primary lockup](brand/vesna-logo.svg)

| Asset | Use |
| --- | --- |
| [Primary lockup](brand/vesna-logo.svg) | The full-color mark and Charcoal wordmark on Off-White, white or an equally quiet light surface. |
| [Standalone color mark](brand/vesna-mark.svg) | Compact Vesna-owned placements on quiet light backgrounds. |
| [Charcoal lockup](brand/vesna-logo-mono.svg) | Single-color production and light backgrounds that need a high-contrast mark. |
| [Reversed lockup](brand/vesna-logo-reverse.svg) | Single-color Off-White artwork on dark surfaces. |

The lockup uses a folded V and a detached leaf with a cutout vein. The wordmark is drawn from Nunito at weights 850 and 500, with optical scaling and tracking baked into outlines. Keep the stacked relationship and the leaf orientation in all locales. Use “Vesna Launcher” as the accessible name where the logo communicates identity on its own. Treat it as decorative when the same name is already adjacent.

The lockup SVG includes at least 21 vector units of clear space on every side. Preserve the full view box or reserve one third of the visible mark height around cropped art. The minimum rendered width of the supplied lockup is 144 dp or CSS px. At smaller sizes, use the standalone mark at 24 dp or CSS px or larger. Preserve aspect ratio. Use a single-color version for unfamiliar backgrounds where the gradient lacks definition. [Nunito font and license](https://github.com/googlefonts/nunito)

## Android Adaptive Icon

The icon pairs a full-bleed Off-White background with the color mark. Keep the same standard icon across Vesna's light and dark themes. Android and the host launcher determine the system-themed rendering from the separate opaque monochrome silhouette. The monochrome version retains the leaf cutout.

| Layer | Vector master | Android handoff |
| --- | --- | --- |
| Background | [SVG](adaptive-icon/background.svg) | [VectorDrawable](adaptive-icon/res/drawable/vesna_icon_background.xml) |
| Foreground | [SVG](adaptive-icon/foreground.svg) | [VectorDrawable](adaptive-icon/res/drawable/vesna_icon_foreground.xml) |
| Monochrome | [SVG](adaptive-icon/monochrome.svg) | [VectorDrawable](adaptive-icon/res/drawable/vesna_icon_monochrome.xml) |
| Adaptive wrapper | [Mask examples](preview.html#adaptive-icon) | [Default](adaptive-icon/res/mipmap-anydpi-v26/ic_launcher.xml) · [Round alias](adaptive-icon/res/mipmap-anydpi-v26/ic_launcher_round.xml) |

All layers use a 108 × 108 dp viewport. Keep essential silhouette geometry inside the centered 66 × 66 dp safe zone at x/y 21–87. The centered 72 × 72 dp crop in the preview represents the visible mask viewport before OEM shaping. The outer 18 dp on each edge is available to the platform for masking and motion. The square, rounded-square, squircle and circular examples are design checks. The monochrome examples illustrate system tinting. The launcher may choose other colors and masks.

The app packages the Android handoff directory directly under the existing manifest resource names. Adaptive icons are available on the project's minimum SDK (API 26), and the dedicated monochrome layer supports compatible themed launchers from API 33. About renders the same adaptive mark without following the host launcher's themed-icon preference. [Android adaptive icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) · [Current manifest](../../app/src/main/AndroidManifest.xml)

## Material 3 color

| Brand color | Value | Use |
| --- | --- | --- |
| Spring Green | `#A8E66E` | Signature artwork highlight and Material theme seed. |
| Fresh Teal | `#2EC6B8` | Spring artwork and supporting brand accents. |
| Sky Blue | `#6FB7FF` | Supporting artwork and gradient fields. |
| Off-White | `#F8FAF6` | Adaptive icon background, reversed logo and brand canvas. |
| Charcoal | `#2E3338` | Primary wordmark and single-color brand artwork. |

Leaf Green `#90D442`, Deep Teal `#006D68` and Icon Teal `#128C7D` are artwork stops. Spring waves and gradients belong to optional wallpaper and brand artwork. Use semantic Material roles for Vesna-owned controls and surfaces. Preserve external app icon colors and work-profile badges.

Both branded schemes use Spring Green as the HCT seed in Material Color Utilities 0.3.0, with the Tonal Spot scheme and standard contrast. Primary is green, secondary is a quieter green and tertiary is teal. In **Vesna settings → Theme**, the default **System** preference follows Android night mode; **Light** and **Dark** override it with their fixed Vesna palettes. Wallpaper-derived UI colors would be a separate product preference. The Android themed app icon follows the host launcher independently. [Material Color Utilities](https://github.com/material-foundation/material-color-utilities) · [Material 3 theming on Android](https://developer.android.com/develop/ui/compose/designsystems/material3)

### Material color roles

| Role | Light | Dark |
| --- | --- | --- |
| `primary` | `#49672E` | `#AFD18C` |
| `onPrimary` | `#FFFFFF` | `#1D3703` |
| `primaryContainer` | `#CAEEA6` | `#324E18` |
| `onPrimaryContainer` | `#324E18` | `#CAEEA6` |
| `inversePrimary` | `#AFD18C` | `#49672E` |
| `secondary` | `#57624A` | `#BFCBAE` |
| `onSecondary` | `#FFFFFF` | `#29341F` |
| `secondaryContainer` | `#DBE7C9` | `#3F4A34` |
| `onSecondaryContainer` | `#3F4A34` | `#DBE7C9` |
| `tertiary` | `#386664` | `#A0CFCD` |
| `onTertiary` | `#FFFFFF` | `#003736` |
| `tertiaryContainer` | `#BBECE9` | `#1E4E4C` |
| `onTertiaryContainer` | `#1E4E4C` | `#BBECE9` |
| `error` | `#BA1A1A` | `#FFB4AB` |
| `onError` | `#FFFFFF` | `#690005` |
| `errorContainer` | `#FFDAD6` | `#93000A` |
| `onErrorContainer` | `#93000A` | `#FFDAD6` |
| `surface` | `#F9FAEF` | `#11140E` |
| `onSurface` | `#1A1D16` | `#E2E3D9` |
| `onSurfaceVariant` | `#44483E` | `#C4C8BA` |
| `surfaceDim` | `#D9DBD0` | `#11140E` |
| `surfaceBright` | `#F9FAEF` | `#373A33` |
| `surfaceContainerLowest` | `#FFFFFF` | `#0C0F09` |
| `surfaceContainerLow` | `#F3F5E9` | `#1A1D16` |
| `surfaceContainer` | `#EDEFE4` | `#1E211A` |
| `surfaceContainerHigh` | `#E8E9DE` | `#282B24` |
| `surfaceContainerHighest` | `#E2E3D9` | `#33362E` |
| `surfaceTint` | `#49672E` | `#AFD18C` |
| `outline` | `#74796D` | `#8E9286` |
| `outlineVariant` | `#C4C8BA` | `#44483E` |
| `inverseSurface` | `#2F312A` | `#E2E3D9` |
| `inverseOnSurface` | `#F0F2E7` | `#2F312A` |
| `scrim` | `#000000` | `#000000` |

[tokens.json](tokens.json) includes the fixed roles and compatibility aliases. Prefer `surface` and `onSurface` for screens and the opaque app drawer. Use `surfaceContainerLow` for subtle grouped sections, `surfaceContainer` for menus, `surfaceContainerHigh` for search and dialogs, and `surfaceContainerHighest` for the unselected switch track. Use `onSurfaceVariant` for supporting text and secondary icons. Use `outline` for visible control boundaries and `outlineVariant` for separators. Use `inverseSurface`, `inverseOnSurface` and `inversePrimary` for snackbars. [Compose ColorScheme](https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme)

### Measured contrast

Ratios use sRGB relative luminance. The design targets 4.5:1 for normal text and 3:1 for essential control boundaries, switch indicators and focus indicators. Pressed rows include the 10% state layer over the resting fill. Disabled components use their Material component defaults and expose disabled semantics.

| Pair | Light | Dark |
| --- | --- | --- |
| Screen text | 16.20:1 | 14.35:1 |
| Search text | 13.92:1 | 11.10:1 |
| Search placeholder | 7.64:1 | 8.43:1 |
| Filled button label | 6.44:1 | 7.70:1 |
| Pressed filled button label | 5.10:1 | 6.57:1 |
| Filled tonal button label | 7.27:1 | 7.27:1 |
| Pressed filled tonal button label | 6.23:1 | 5.66:1 |
| Snackbar text | 11.65:1 | 10.18:1 |
| Snackbar action | 7.73:1 | 4.97:1 |
| Dialog text action | 5.25:1 | 8.43:1 |
| Pressed dialog text action | 4.61:1 | 6.69:1 |
| Error message | 7.24:1 | 7.24:1 |
| Themed information message | 7.24:1 | 7.24:1 |
| Search outline | 3.65:1 | 4.52:1 |
| Switch off thumb and track outline | 3.45:1 | 3.87:1 |
| Switch on thumb | 6.44:1 | 7.70:1 |
| Focus ring on dialog and search surfaces | 5.27:1 | 8.46:1 |

On arbitrary wallpaper use the Vesna label backing `#18251E` with white text when needed. Resolve the status and navigation bar icon appearance against their actual background. Use the app surface behind system bars when wallpaper cannot provide reliable contrast. Respect all system bar and IME insets. [WCAG 2.2 text contrast](https://www.w3.org/TR/WCAG22/#contrast-minimum) · [WCAG 2.2 non-text contrast](https://www.w3.org/TR/WCAG22/#non-text-contrast)

## Material 3 typography

Use the 15 Material roles and the AndroidX baseline metrics with Vesna's font families. Nunito is used for display and headlines. Inter is used for titles, body and labels. Bundle the fonts locally during implementation, include the OFL notices with any redistributed font files and use Android sans-serif for missing families or glyphs.

The bundled files are static Latin and Cyrillic subsets of [Inter from Google Fonts](https://github.com/google/fonts/tree/0b58fb370093f9a9f4ff785d94405710b79de67c/ofl/inter) (weights 400 and 500, optical size 14) and [Nunito from Google Fonts](https://github.com/google/fonts/tree/8b0a1d0f5983c89bc2b93f1b5fb55f9e252744b5/ofl/nunito) (weight 400). The original OFL notices ship in [app assets](../../app/src/main/assets/licenses/).

| Role | Family | Size / line height in sp | Weight | Tracking in sp | Use |
| --- | --- | --- | --- | --- | --- |
| `displayLarge` | Nunito | 57 / 64 | 400 | -0.2 | Large brand-led display |
| `displayMedium` | Nunito | 45 / 52 | 400 | 0 | Large-format promotional heading |
| `displaySmall` | Nunito | 36 / 44 | 400 | 0 | Compact branded display |
| `headlineLarge` | Nunito | 32 / 40 | 400 | 0 | About title with generous space |
| `headlineMedium` | Nunito | 28 / 36 | 400 | 0 | Large section heading |
| `headlineSmall` | Nunito | 24 / 32 | 400 | 0 | Basic dialog and folder panel title |
| `titleLarge` | Inter | 22 / 28 | 400 | 0 | Small top app bar and screen title |
| `titleMedium` | Inter | 16 / 24 | 500 | 0.2 | Settings group title |
| `titleSmall` | Inter | 14 / 20 | 500 | 0.1 | Compact section title |
| `bodyLarge` | Inter | 16 / 24 | 400 | 0.5 | Search input and list-item headline |
| `bodyMedium` | Inter | 14 / 20 | 400 | 0.2 | Dialog body, list support and snackbar |
| `bodySmall` | Inter | 12 / 16 | 400 | 0.4 | Compact supporting copy and field label |
| `labelLarge` | Inter | 14 / 20 | 500 | 0.1 | Buttons and menu items |
| `labelMedium` | Inter | 12 / 16 | 500 | 0.5 | Workspace and drawer app labels |
| `labelSmall` | Inter | 11 / 16 | 500 | 0.5 | Short, nonessential captions |

Use sentence case and scalable `sp` for type. Allow row and button labels to wrap. Ellipsize app grid labels after their available lines and expose the full app name and profile to accessibility services. Verify Latin and Cyrillic, long localized labels, mixed-script names and Android's 200% font setting. [Compose typography tokens](https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/TypeScaleTokens.kt) · [Nunito](https://fonts.google.com/specimen/Nunito) · [Inter](https://github.com/rsms/inter) · [Android scalable content](https://developer.android.com/develop/ui/compose/accessibility/scalable-content)

Use Android's localized badged label for an app tile's accessible name. Keep the visible label, sorting, and search based on the unbadged app name. Continue exposing the accessible name when **Show app names** is off.

## Shape, elevation and motion

Use the Material shape scale of 0, 4, 8, 12, 16 and 28 dp, plus Full for pills. Use Extra Large (28 dp) for basic dialogs and top sheet corners, and Medium (12 dp) for Vesna app-owned icon wells and drag highlights. In Compose, Full is a percentage-based shape. Let each Material component supply its default shape and elevations.

Elevation levels 0–5 correspond to 0, 1, 3, 6, 8 and 12 dp. For custom surfaces use the named surface-container roles for tonal hierarchy. When a Material component already resolves its container color and elevation, let it own both.

Use 8 dp spacing for Vesna screen layout, with 4 and 12 dp adjustments where needed. Default to 16 dp horizontal content padding on phones and 24–32 dp on wider windows. Use at least 48 × 48 dp touch targets. Visual bounds can be smaller than the touch target. Use 24 dp Material Symbols Rounded for common UI actions such as back, search, clear and settings. Use local vector drawables for the small set needed in the launcher. [Material Symbols](https://fonts.google.com/icons) · [Android touch target guidance](https://developer.android.com/guide/topics/ui/accessibility/apps)

Use the native Material interaction feedback on standard components. Where Vesna draws custom state layers, use 8% hover, 10% focus, 10% press and 16% drag in the relevant content color. Vesna adds a visible 2 dp `secondary` keyboard focus ring with a 2 dp offset on its controls and custom Views. Use the stock disabled appearance for each component. Preserve selection, checked and disabled semantics. Use a label, shape or position change alongside color when it communicates state.

Use Material component motion defaults. For the custom drawer, track the finger and settle over 250 ms with Material standard easing `cubic-bezier(0.2, 0, 0, 1)`. Honor Android animator settings and reduced motion. [AndroidX Material motion tokens](https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/MotionTokens.kt)

## Components

| Pattern | Material 3 specification | Vesna behavior |
| --- | --- | --- |
| Common buttons | **Filled** uses `primary` and `onPrimary`. **Filled tonal** uses `secondaryContainer` and `onSecondaryContainer`. **Outlined** and **Text** use `primary` content. Outlined uses a 1 dp `outline` border. Use `labelLarge`, Full shape, 40 dp baseline visual height and at least a 48 dp touch target. | Use Filled for the primary action, Filled tonal or Outlined for secondary actions, and Text for low-emphasis actions and basic dialog actions. Grow or stack controls at large type sizes. |
| Top app bar | Small `TopAppBar` with a `titleLarge` title, `surface` and a standard navigation `IconButton`. Use the stock scrolled-container treatment. | Use for Settings and About where up navigation exists. Respect the Android back stack and mirror directional icons for RTL. |
| Icon button | Standard `IconButton` with a 24 dp glyph and 48 dp minimum target. Use `IconToggleButton` for a persistent binary control. | Give each control an accessible action label. A selected toggle also exposes checked semantics. |
| Drawer search | Material `SearchBar` appearance with a 56 dp Full container, `surfaceContainerHigh`, `bodyLarge`, and `onSurfaceVariant` placeholder and icons. Vesna adds a 1 dp `outline` boundary to make the field easy to locate in both themes. | Search filters the existing drawer grid inline. Typing on a physical keyboard while the drawer is open focuses the field and continues the query without changing navigation or shortcut keys. In the field, Enter opens the only match for a nonblank query; the on-screen keyboard’s Done action still dismisses the keyboard. Normalize case, spacing and equivalent spellings for matching without changing the query, displayed app names or drawer order. Clear restores the results and focus. Expose an accessible field name and keep results above the IME. In Views, reproduce the Material tokens and search semantics. |
| Settings item | Material `ListItem` anatomy with `bodyLarge` headline, optional `bodyMedium` supporting text, leading glyph and trailing value, chevron or switch. Use a 56 dp single-line or 72 dp two-line baseline. | Make the row one accessible target when it performs one action. Use three `RadioButton` choices for System, Light and Dark and announce the selected mode. |
| Switch | Standard Material `Switch`. The visual track is 52 × 32 dp. Without thumb icons, the selected thumb is 24 dp and the unselected thumb is 16 dp. Selected uses `primary` and `onPrimary`. Unselected uses `surfaceContainerHighest` and an `outline` thumb and 2 dp track border. | Toggle a single binary preference and expose the label and checked state together. Keep a 48 dp minimum target. **Show app names** controls visible labels in the workspace, dock, drawer and folder contents. It defaults to on. |
| Folder menu and dialog | Material `DropdownMenu` / `DropdownMenuItem` for contextual actions using `surfaceContainer`. A Material basic `AlertDialog` uses a 28 dp shape, 24 dp content inset, `surfaceContainerHigh`, `headlineSmall`, `bodyMedium` and trailing `TextButton` actions. For custom modal overlays use `scrim` at 32%. | Preserve open, rename, edit and dismiss behavior. Use a Material outlined text field for the folder name. Move focus into the dialog and restore it to the invoking control on dismissal. |
| Feedback | Material `Snackbar` for transient feedback, with inverse roles and an optional action. Use `errorContainer` with `onErrorContainer` for persistent errors and `tertiaryContainer` with `onTertiaryContainer` for an emphasized informational state. | Announce meaningful changes. Provide a clear-search action for no results and explain how to recover from unavailable apps or invalid drops. |
| Workspace and drawer app tile | Vesna launcher pattern with 48 dp app art, at least 64 dp cell width, a 4 dp icon-to-label gap and `labelMedium`. Apply Material state layers and focus around the tile. | Preserve installed app artwork, badges, launch and touch long-press actions. Keep drag effects outside the icon artwork. A focused tile opens its anchored menu with Shift+F10 or Menu. Android accessibility exposes **Shortcut options** on the tile; neither action should start a drag. |
| Dock and page indicator | Vesna launcher patterns built from Material roles. The dock uses full app targets. Indicators use `primary` and `outline`, with an additional shape or size cue for the active page. | Preserve up to four favorites and their order. Keep named 48 dp Home options and drawer-entry targets. With multiple pages, the dots are a 48 dp minimum button that announces position and count. The page picker is also available through Home options and marks the current page with a checked radio item. |

**Folder rename.** Use the same dialog from the Home folder menu and from an open folder. Focus and select the current name in the outlined field. **Save** and the keyboard's **Done** action trim surrounding whitespace; a blank name shows an inline error and leaves the editor open. **Cancel** discards the draft. Return to the invoking surface, keeping the folder open if renaming started there.
Keep the draft, selection, and any visible validation message when Android recreates Home during a configuration change. Clear the validation message when the name is corrected. Do not save the draft as the folder name until the user confirms it.

[Material buttons](https://developer.android.com/develop/ui/compose/components/button) · [Search bar](https://developer.android.com/develop/ui/compose/components/search-bar) · [Switch](https://developer.android.com/develop/ui/compose/components/switch) · [Dialogs](https://developer.android.com/develop/ui/compose/components/dialog)

### Screen composition

- **Home.** Put wallpaper behind the workspace and dock. Preserve saved cell coordinates and spans in the current 4 × 5 layout. Use the Vesna wallpaper label backing where necessary. Holding a folder and dragging it highlights only empty cells on its current page. An invalid or canceled drop retains the original cell; a stationary hold opens the folder menu. The overflow button and long-pressing empty space open the same Home options menu for Settings, wallpaper, and page actions. **Reorder this page…** lists the available positions, keeps the first page fixed, and keeps the moved page selected. The **Move to page…** menu on an app shortcut or folder lists other pages with room in display order and keeps the source page selected.
  Keep Home's workspace, dock, and controls inside the safe side insets in landscape, with the wallpaper still edge to edge. Resolve drop targets in the same inset layout that draws them.
  Fit artwork inside the available width of a narrow workspace or dock tile without cropping it. When a short workspace cell would clip a scaled label, shrink the artwork to make room for the line. Keep the cell and its touch target unchanged. Long names still ellipsize horizontally and retain their full accessible names.
  In a short, wide cell, place the artwork beside the label when stacking would leave insufficient room for the artwork. Keep the 4 dp gap and mirror the order in RTL.
  When the Home control row is too narrow for both page arrows at full size, show only the 48 dp minimum targets for Home options, the page picker, and the app drawer. Keep swipe navigation and the picker available, and move keyboard focus from a disappearing arrow to the picker.
  Reduce the Home control row's outer spacing before touching its 48 dp targets. Keep page picker labels short enough to show the number at 184 dp with enlarged text, while exposing the full page name and checked radio state to accessibility.
  Android Home returns to the same workspace page and closes launcher menus, the drawer, and open folder dialogs without saving a rename draft. Back still dismisses the frontmost layer first.
  When Android recreates Home, retain keyboard focus on the same workspace shortcut on the active page or the same dock favorite if it is still visible. Leave focus within an open drawer or dialog.
  If a second finger joins an active drag, clear the highlight and restore the starting layout. The remaining touches cannot finish the drop; a new touch can start another drag.
  Workspace columns and dock slots run from the reading start edge, including in RTL.
  Drag hit testing and highlights follow the rendered columns. Changing layout direction
  mirrors the display without changing the saved cells or slot order.
  The dock packs visible icons toward the start. Dragging across trailing space
  highlights the last possible position for that drag. Temporarily unavailable
  favorites remain saved and count toward capacity; a full dock does not show
  an insertion highlight.
- **App drawer.** Use an opaque Material surface with inset search and a scrollable app grid. Use up to four columns, choosing the largest count that gives each cell 64 dp after horizontal insets. In layouts with fewer than four columns, use 8 dp search margins and the short **Search** hint. Keep the viewport in place when installed apps refresh. Start at the top of the new results when the query changes or clears. Restore focus by app identity when that app remains visible. If an active drag cannot continue through a layout change, restore the original position.
  In short available heights, including above the keyboard, put Close and Search in one row before compressing the field. In a narrow one-row header, omit the decorative search glyph to leave room for the query and Clear. Keep the query when the keyboard hides so results can use the expanded viewport.
  Request an inline keyboard for drawer search in landscape so a full-screen editor does not replace the drawer.
  Keep the no-results message visible above the keyboard. Let its vertical whitespace yield before clipping scaled text, and keep Clear available in the search field.
  Keep the drawer and its active search open when Android recreates Home, including when the system theme changes. Closing the drawer or returning to Home clears the search.
  If an app tile had keyboard focus, restore focus to that same app in the restored results. Scroll it into view if needed. Closing the drawer discards that focus.
- **Folders.** Keep apps in their saved order in a scrollable grid inside the Material dialog. Use the same column rule as the drawer: up to four columns, 64 dp per cell after the dialog's content insets when space permits. Reflow the grid when the open dialog changes size. Keep rename and remove actions visible while the apps scroll.
  Keep the actions in one trailing row of Material `TextButton`s. If the localized labels do not fit at the current text size, use 24 dp rounded symbols in 48 dp targets, with the full action names as tooltips and accessible labels. The member grid remains the scrollable region.
  When Android recreates Home, reopen the current folder only if it still belongs to the active page. Back and Home dismiss it. Restore a pending rename above its invoking folder.
  Restore keyboard focus to the same folder member if it is still present. A fresh open uses normal focus traversal.
  When the folder closes, return keyboard focus to its Home tile if it is still on the active page, including after recreation. In touch mode, do not force keyboard focus.
- **Settings and About.** Use a small Material app bar, a `surface` canvas and standard list items for **Default launcher**, **Appearance**, and **About**. Use an accessible back button on each named page. The root has a rounded, outlined search field and opens with no query, field focus, or keyboard. Search filters the destination rows by their labels and supporting text. Android Back hides the keyboard before acting on the query. Place the Vesna lockup or mark on About. Appearance offers a single-select Theme group and a **Show app names** switch. Hiding app labels preserves icon positions, touch targets, accessible app names, and folder titles.
  Keep Settings search above the scrolling results. Match setting titles and descriptions. Show the parent path on each outlined result card, and scroll the destination to the matching control. Mark that control with a labeled `tertiaryContainer` treatment without changing its value.
  Use the same case, spacing, and spelling normalization as app search for matching; keep the typed query and translated labels unchanged.
  In a short Settings directory viewport, keep the outlined search field and its focus ring in the Material app bar. Show the visual heading when space allows; otherwise keep the Settings pane name available to accessibility. Named subpages retain their Up button. Narrow fields use a brief placeholder without changing the accessible field name.
  Keyboard traversal follows the app bar before the directory. If keyboard activation removes the Clear button, return focus to the search field.
  Keep the scroll position and current query or highlighted search match when Android recreates Settings. Scroll to a highlighted match only on entry, without replacing a restored reading position. A new Settings entry starts clean, including when a link opens the same page again.

## Android handoff and acceptance

Apply the same named roles to Compose and Android Views. Compose settings already use the Material 3 dependency. At implementation, populate `MaterialTheme` with the Vesna `ColorScheme`, `Typography` and `Shapes` and use the stable Material composables where available. Keep the existing Views for the workspace, drawer and dock and map their styling and state feedback to the same tokens. [Current Android build configuration](../../app/build.gradle.kts) · [Product principles](../../PRODUCT.md)

Verify System, Light and Dark at normal and enlarged font scales, on compact and wide screens, with both Android navigation modes. Check English and Russian labels, RTL navigation, D-pad focus and TalkBack. Use bright, dark and busy wallpapers to check label and system bar contrast. Verify invalid drops, the full dock, folder actions, empty search results and profile badges. Check the adaptive icon on API 26 and themed rendering from API 33 on a compatible launcher.

The web preview illustrates the proposed Material components and uses system font fallbacks when Nunito and Inter are unavailable locally. Android integration and device-level behavior are acceptance checks for implementation.
