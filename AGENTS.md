# Vesna Launcher agent instructions

## Use the Vesna Material Design 3 system

Apply the Vesna design system to every new or revised Android launcher surface, including the workspace, dock, app drawer, search, folders, menus, dialogs, Settings and About.

- Follow the [Vesna design specification](design/system/README.md) for component anatomy, layout, motion, states and accessibility.
- Use [tokens.json](design/system/tokens.json) for exact color roles, typography, shapes, elevation, spacing and component values. Use the fixed Vesna color schemes in both themes. System follows Android night mode. Light and Dark use their corresponding Vesna schemes.
- Use the [interactive preview](design/system/preview.html) to compare the light and dark treatments. Apply the specification and tokens to the features in scope. Ask for owner approval before departing from the design system, and record approved changes in the specification and tokens.
- Use Material 3 components and the Vesna `MaterialTheme` in Compose. Style the Android Views used by the workspace, dock and drawer with the same roles, typography and interaction states. Use Nunito for display and headlines and Inter for titles, body text and labels as specified.
- Use the supplied [SVG logo assets](design/system/brand/) for Vesna branding and the [Adaptive Icon resources](design/system/adaptive-icon/res/) when implementing the launcher icon. Preserve installed app artwork, work-profile badges and user wallpaper as described in the specification.

For each affected surface, verify both themes and the applicable [Android acceptance checks](design/system/README.md#android-handoff-and-acceptance). Check layout at enlarged font sizes, touch targets, contrast, keyboard focus and accessibility semantics. Record which checks ran and which still need device verification.
