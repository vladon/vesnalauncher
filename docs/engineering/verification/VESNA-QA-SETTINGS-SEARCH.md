# Settings search: SETT-110

[Published requirements](https://github.com/vladon/vesnalauncher/blob/9954a278758958da8d0b0d878bc18e6a9fbeb0e8/docs/clean-room/specifications/settings.md#sett-110--settings-search-with-section-deep-linking)

## Acceptance scenario

1. Open Vesna Settings. The field is blank and unfocused. At 200% font size, scroll the directory; the search field stays fixed while the rows scroll.
2. Search for `tHeMe`. The Theme result includes its title, descriptive text, and the Appearance breadcrumb. Open the card: Appearance opens at the highlighted Theme group without selecting a theme.
3. Return to Settings and search for `folders`. This word appears in the description of Show app names, not its title. Select the result from Appearance. The page scrolls to the highlighted switch without changing its value, even when the switch starts below the viewport.
4. Return to Settings and search for a nonsense term. The empty state appears. Back first hides the keyboard and keeps the query; Back again clears the search. A named page link still opens the page without a search highlight.

## Results

Passed on Android 8 (320 dp, dark, 200% font scale) and Android 15
(light, 130%). Device input and accessibility dumps confirmed the two
matching paths, breadcrumb, visible target, empty state, and Back order.
On the compact fixture, dismiss the keyboard to see the complete result card.
Screenshots confirmed the target treatment in both themes. Debug assembly and
Lint passed. TalkBack and RTL were not exercised.
