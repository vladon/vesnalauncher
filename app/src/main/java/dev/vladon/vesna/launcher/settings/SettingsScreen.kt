package dev.vladon.vesna.launcher.settings

import android.content.Intent
import android.view.View
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp
import dev.vladon.vesna.launcher.search.searchKey
import dev.vladon.vesna.launcher.ui.VesnaFocusRingOutset
import dev.vladon.vesna.launcher.ui.vesnaFocusRing
import dev.vladon.vesna.launcher.workspace.GridCanvas
import dev.vladon.vesna.launcher.workspace.GridResizeResult
import kotlinx.coroutines.launch

internal enum class DefaultHomeState { ACTIVE, AVAILABLE, UNAVAILABLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    page: SettingsPage,
    query: String,
    onQueryChange: (String) -> Unit,
    searchTarget: Int?,
    onNavigate: (SettingsPage, Int?) -> Unit,
    onResetInputFocus: () -> Unit,
    versionName: String,
    defaultHome: DefaultHomeState,
    onSetDefaultHome: () -> Unit,
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    iconShape: IconShapeMode,
    onIconShapeSelected: (IconShapeMode) -> Unit,
    showAppNames: Boolean,
    onShowAppNamesChanged: (Boolean) -> Unit,
    layoutLocked: Boolean,
    onLayoutLockedChanged: (Boolean) -> Unit,
    grid: GridCanvas?,
    onApplyGridResize: suspend (GridCanvas) -> GridResizeResult,
    pinNewApps: Boolean,
    onPinNewAppsChanged: (Boolean) -> Unit,
    hideHomeAppNames: Boolean,
    onHideHomeAppNamesChanged: (Boolean) -> Unit,
    homeIconPercent: Int,
    onHomeIconPercentChanged: (Int) -> Unit,
    dockIconPercent: Int,
    onDockIconPercentChanged: (Int) -> Unit,
    drawerLayoutMode: DrawerLayoutMode,
    onDrawerLayoutModeSelected: (DrawerLayoutMode) -> Unit,
    closeDrawerOnLaunch: Boolean,
    onCloseDrawerOnLaunchChanged: (Boolean) -> Unit,
    rememberDrawerPosition: Boolean,
    onRememberDrawerPositionChanged: (Boolean) -> Unit,
    showKeyboardOnDrawerOpen: Boolean,
    onShowKeyboardOnDrawerOpenChanged: (Boolean) -> Unit,
    drawerIconPercent: Int,
    onDrawerIconPercentChanged: (Int) -> Unit,
    drawerFollowsHome: Boolean,
    onDrawerFollowsHomeChanged: (Boolean) -> Unit,
    drawerApps: List<LauncherApp>,
    hiddenDrawerApps: Set<AppKey>,
    onDrawerAppHiddenChanged: (AppKey, Boolean) -> Unit,
) {
    val inSubpage = page != SettingsPage.ROOT
    val up = { onNavigate(SettingsPage.ROOT, null) }
    BackHandler(enabled = inSubpage, onBack = up)
    val density = LocalDensity.current
    val appBarHeight = maxOf(64.dp, with(density) {
        MaterialTheme.typography.titleLarge.lineHeight.toDp() * 2 + 8.dp
    })
    val searchHeight = maxOf(56.dp, with(density) {
        MaterialTheme.typography.bodyLarge.lineHeight.toDp() + 32.dp
    })
    val title = stringResource(page.title)
    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).safeDrawingPadding(),
    ) {
        // Keep the root search usable when the IME leaves no room for both
        // the visual heading and the directory. Subpages keep their Up action.
        val searchBarHeight = searchHeight + VesnaFocusRingOutset * 2
        val standardChrome = appBarHeight + searchBarHeight + 12.dp + 12.dp + 24.dp
        val shortDirectory = !inSubpage && maxHeight < standardChrome + 48.dp
        val narrow = maxWidth < 320.dp
        val availableSearchBarHeight = minOf(searchBarHeight, maxHeight)
        // Match the directory's centered content width and 16 dp side margins.
        val searchWidth = (minOf(maxWidth, 560.dp) - 32.dp).coerceAtLeast(0.dp)
        // Put the app bars before the scrollable body in keyboard traversal order.
        Column(Modifier.fillMaxSize()) {
            if (!shortDirectory) {
                TopAppBar(
                    title = {
                        Text(
                            title,
                            modifier = Modifier.semantics { heading() },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (inSubpage) {
                            IconButton(onClick = up, modifier = Modifier.vesnaFocusRing()) {
                                Icon(
                                    painterResource(R.drawable.ic_settings_back),
                                    contentDescription = stringResource(R.string.settings_back_to_root),
                                )
                            }
                        }
                    },
                    expandedHeight = appBarHeight,
                )
            }
            if (!inSubpage) {
                CenterAlignedTopAppBar(
                    modifier = if (shortDirectory) Modifier.semantics { paneTitle = title }
                        else Modifier.padding(top = 12.dp),
                    expandedHeight = availableSearchBarHeight,
                    title = {
                        SettingsSearchField(
                            query = query,
                            onQueryChange = onQueryChange,
                            onResetInputFocus = onResetInputFocus,
                            short = shortDirectory,
                            narrow = narrow,
                            modifier = Modifier
                                .width(searchWidth)
                                .padding(vertical = VesnaFocusRingOutset),
                        )
                    },
                )
            }
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                key(page) {
                    Column(
                        Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .then(if (inSubpage && page != SettingsPage.APP_DRAWER)
                                Modifier.verticalScroll(rememberScrollState()) else Modifier)
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = when {
                                    inSubpage -> 16.dp
                                    shortDirectory -> VesnaFocusRingOutset
                                    else -> 12.dp
                                },
                                bottom = if (shortDirectory) VesnaFocusRingOutset else 24.dp,
                            ),
                    ) {
                        when (page) {
                            SettingsPage.ROOT -> SettingsDirectory(defaultHome, query, onNavigate)
                            SettingsPage.DEFAULT_LAUNCHER -> SearchTarget(searchTarget == R.string.settings_default_launcher) {
                                DefaultLauncherSetup(defaultHome, onSetDefaultHome)
                            }
                            SettingsPage.HOME_SCREEN -> {
                                if (grid != null) {
                                    // The row waits for the persisted grid instead of
                                    // flashing the default dimensions while loading.
                                    SearchTarget(searchTarget == R.string.settings_workspace_grid) {
                                        WorkspaceGridSettings(grid, layoutLocked, onApplyGridResize)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                                SearchTarget(searchTarget == R.string.settings_lock_layout) {
                                    SettingsSwitch(
                                        R.string.settings_lock_layout, R.string.settings_lock_layout_detail,
                                        layoutLocked, onLayoutLockedChanged,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                SearchTarget(searchTarget == R.string.settings_pin_new_apps) {
                                    SettingsSwitch(
                                        R.string.settings_pin_new_apps, R.string.settings_pin_new_apps_detail,
                                        pinNewApps, onPinNewAppsChanged,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                SearchTarget(searchTarget == R.string.settings_hide_home_app_names) {
                                    SettingsSwitch(
                                        R.string.settings_hide_home_app_names, R.string.settings_hide_home_app_names_detail,
                                        hideHomeAppNames, onHideHomeAppNamesChanged,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                SearchTarget(searchTarget == R.string.settings_home_icon_size) {
                                    SettingsSlider(
                                        R.string.settings_home_icon_size, R.string.settings_home_icon_size_detail,
                                        homeIconPercent, onHomeIconPercentChanged,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                SearchTarget(searchTarget == R.string.settings_dock_icon_size) {
                                    SettingsSlider(
                                        R.string.settings_dock_icon_size, R.string.settings_dock_icon_size_detail,
                                        dockIconPercent, onDockIconPercentChanged,
                                    )
                                }
                            }
                            SettingsPage.APP_DRAWER -> DrawerAppSettings(
                                drawerLayoutMode, onDrawerLayoutModeSelected,
                                closeDrawerOnLaunch, onCloseDrawerOnLaunchChanged,
                                rememberDrawerPosition, onRememberDrawerPositionChanged,
                                showKeyboardOnDrawerOpen, onShowKeyboardOnDrawerOpenChanged,
                                drawerIconPercent, onDrawerIconPercentChanged,
                                drawerFollowsHome, onDrawerFollowsHomeChanged,
                                drawerApps, hiddenDrawerApps, onDrawerAppHiddenChanged,
                                searchTarget == R.string.settings_hide_drawer_apps,
                                searchTarget == R.string.settings_drawer_layout,
                                searchTarget == R.string.settings_drawer_close_on_launch,
                                searchTarget == R.string.settings_drawer_remember_position,
                                searchTarget == R.string.settings_drawer_show_keyboard,
                                searchTarget == R.string.settings_drawer_follow_home,
                                searchTarget == R.string.settings_drawer_icon_size,
                            )
                            SettingsPage.APPEARANCE -> {
                                SearchTarget(searchTarget == R.string.settings_theme) { ThemeSettings(themeMode, onThemeSelected) }
                                Spacer(Modifier.height(8.dp))
                                SearchTarget(searchTarget == R.string.settings_icon_shape) {
                                    IconShapeSettings(iconShape, onIconShapeSelected)
                                }
                                Spacer(Modifier.height(8.dp))
                                SearchTarget(searchTarget == R.string.settings_show_app_names) {
                                    SettingsSwitch(
                                        R.string.settings_show_app_names, R.string.settings_show_app_names_detail,
                                        showAppNames, onShowAppNamesChanged,
                                    )
                                }
                            }
                            SettingsPage.ABOUT -> AboutContent(versionName)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onResetInputFocus: () -> Unit,
    short: Boolean,
    narrow: Boolean,
    modifier: Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val editorFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val searchLabel = stringResource(R.string.settings_search)
    val colors = MaterialTheme.colorScheme
    // While the IME is visible, let Android handle Back before changing the search state.
    BackHandler(enabled = !WindowInsets.isImeVisible && (focused || query.isNotEmpty())) {
        onResetInputFocus()
        onQueryChange("")
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .focusRequester(editorFocus)
            .vesnaFocusRing()
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = searchLabel },
        placeholder = {
            Text(
                if (narrow) stringResource(R.string.settings_search_compact) else searchLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = if (short && narrow) null else {
            { Icon(painterResource(R.drawable.ic_search), contentDescription = null) }
        },
        trailingIcon = if (query.isEmpty()) null else {
            {
                var clearFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        // Clear removes this button. Return keyboard focus before that happens.
                        if (clearFocused) editorFocus.requestFocus()
                        onQueryChange("")
                    },
                    modifier = Modifier.vesnaFocusRing().onFocusChanged { clearFocused = it.isFocused },
                ) {
                    Icon(
                        painterResource(R.drawable.ic_clear_search),
                        contentDescription = stringResource(R.string.drawer_clear_search),
                    )
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHigh,
            unfocusedContainerColor = colors.surfaceContainerHigh,
        ),
    )
}

@Composable
private fun SettingsDirectory(
    defaultHome: DefaultHomeState,
    query: String,
    onNavigate: (SettingsPage, Int?) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    val term = remember(query) { searchKey(query) }
    val open: (SettingsPage, Int?) -> Unit = { page, target ->
        keyboard?.hide()
        focusManager.clearFocus()
        onNavigate(page, target)
    }
    key(term) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var found = false
            if (term.isNotEmpty()) {
                val entries = listOf(
                    SearchEntry(
                        SettingsPage.DEFAULT_LAUNCHER,
                        if (defaultHome == DefaultHomeState.AVAILABLE) R.string.menu_set_default_home
                        else R.string.settings_default_launcher,
                        defaultHome.summary,
                    ),
                    SearchEntry(SettingsPage.HOME_SCREEN, R.string.settings_workspace_grid, R.string.settings_workspace_grid_detail),
                    SearchEntry(SettingsPage.HOME_SCREEN, R.string.settings_lock_layout, R.string.settings_lock_layout_detail),
                    SearchEntry(SettingsPage.HOME_SCREEN, R.string.settings_pin_new_apps, R.string.settings_pin_new_apps_detail),
                    SearchEntry(SettingsPage.HOME_SCREEN, R.string.settings_hide_home_app_names, R.string.settings_hide_home_app_names_detail),
                    SearchEntry(SettingsPage.HOME_SCREEN, R.string.settings_home_icon_size, R.string.settings_home_icon_size_detail),
                    SearchEntry(SettingsPage.HOME_SCREEN, R.string.settings_dock_icon_size, R.string.settings_dock_icon_size_detail),
                    SearchEntry(SettingsPage.APP_DRAWER, R.string.settings_drawer_layout, R.string.settings_drawer_layout_detail),
                    SearchEntry(SettingsPage.APP_DRAWER, R.string.settings_drawer_close_on_launch, R.string.settings_drawer_close_on_launch_detail),
                    SearchEntry(SettingsPage.APP_DRAWER, R.string.settings_drawer_remember_position, R.string.settings_drawer_remember_position_detail),
                    SearchEntry(SettingsPage.APP_DRAWER, R.string.settings_drawer_show_keyboard, R.string.settings_drawer_show_keyboard_detail),
                    SearchEntry(SettingsPage.APP_DRAWER, R.string.settings_drawer_follow_home, R.string.settings_drawer_follow_home_detail),
                    SearchEntry(SettingsPage.APP_DRAWER, R.string.settings_drawer_icon_size, R.string.settings_drawer_icon_size_detail),
                    SearchEntry(SettingsPage.APP_DRAWER, R.string.settings_hide_drawer_apps, R.string.settings_hide_drawer_apps_detail),
                    SearchEntry(SettingsPage.APPEARANCE, R.string.settings_theme, R.string.settings_theme_detail),
                    SearchEntry(SettingsPage.APPEARANCE, R.string.settings_icon_shape, R.string.settings_icon_shape_detail),
                    SearchEntry(SettingsPage.APPEARANCE, R.string.settings_show_app_names, R.string.settings_show_app_names_detail),
                )
                for (entry in entries) {
                    val title = stringResource(entry.title)
                    val summary = stringResource(entry.summary)
                    if (term !in searchKey(title) && term !in searchKey(summary)) continue
                    found = true
                    OutlinedCard(
                        onClick = {
                            open(entry.page, if (entry.page == SettingsPage.DEFAULT_LAUNCHER)
                                R.string.settings_default_launcher else entry.title)
                        },
                        modifier = Modifier.fillMaxWidth().vesnaFocusRing(radius = 12.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.settings_search_breadcrumb, stringResource(entry.page.title)),
                                style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                            Text(title, style = MaterialTheme.typography.bodyLarge)
                            Text(summary, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
            for (page in SettingsPage.entries) {
                val summary = stringResource(when (page) {
                    SettingsPage.ROOT -> continue
                    SettingsPage.DEFAULT_LAUNCHER -> defaultHome.summary
                    SettingsPage.HOME_SCREEN -> R.string.settings_home_screen_detail
                    SettingsPage.APP_DRAWER -> R.string.settings_app_drawer_detail
                    SettingsPage.APPEARANCE -> R.string.settings_appearance_detail
                    SettingsPage.ABOUT -> R.string.settings_about_detail
                })
                val title = stringResource(page.title)
                if (term.isNotEmpty() && (page == SettingsPage.DEFAULT_LAUNCHER ||
                        term !in searchKey(title) && term !in searchKey(summary))) continue
                found = true
                ListItem(
                    headlineContent = { Text(title) },
                    supportingContent = { Text(summary) },
                    trailingContent = { Icon(painterResource(R.drawable.ic_settings_forward), contentDescription = null) },
                    modifier = Modifier.vesnaFocusRing(radius = 12.dp).clickable(role = Role.Button) { open(page, null) },
                )
            }
            if (!found) {
                Text(stringResource(R.string.settings_search_empty),
                    Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

private data class SearchEntry(val page: SettingsPage, val title: Int, val summary: Int)

@Composable
private fun SearchTarget(highlighted: Boolean, content: @Composable () -> Unit) {
    if (!highlighted) {
        content()
        return
    }
    val requester = remember { BringIntoViewRequester() }
    var placed by remember { mutableStateOf(false) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(placed) {
        // A search result scrolls into view once, not over a restored reading position.
        if (placed && !revealed) {
            requester.bringIntoView()
            revealed = true
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(requester).onPlaced { placed = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column {
            Text(stringResource(R.string.settings_search_match), Modifier.padding(start = 16.dp, top = 8.dp),
                style = MaterialTheme.typography.labelMedium)
            content()
        }
    }
}

private val DefaultHomeState.summary: Int
    get() = when (this) {
        DefaultHomeState.ACTIVE -> R.string.settings_default_home_active
        DefaultHomeState.AVAILABLE -> R.string.settings_default_home_setup
        DefaultHomeState.UNAVAILABLE -> R.string.settings_default_home_unavailable
    }

@Composable
private fun ThemeSettings(mode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Text(
        text = stringResource(R.string.settings_theme),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(8.dp))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (choice in ThemeMode.entries) {
            val selected = choice == mode
            val label = stringResource(when (choice) {
                ThemeMode.SYSTEM -> R.string.settings_theme_system
                ThemeMode.LIGHT -> R.string.settings_theme_light
                ThemeMode.DARK -> R.string.settings_theme_dark
            })
            ListItem(
                headlineContent = { Text(label) },
                leadingContent = { RadioButton(selected = selected, onClick = null) },
                modifier = Modifier
                    .vesnaFocusRing(radius = 12.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(choice) }),
            )
        }
    }
}

@Composable
private fun IconShapeSettings(shape: IconShapeMode, onSelect: (IconShapeMode) -> Unit) {
    Text(
        text = stringResource(R.string.settings_icon_shape),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(8.dp))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (choice in IconShapeMode.entries) {
            val selected = choice == shape
            val label = stringResource(
                when (choice) {
                    IconShapeMode.SYSTEM -> R.string.settings_icon_shape_system
                    IconShapeMode.ROUNDED_SQUARE -> R.string.settings_icon_shape_rounded_square
                },
            )
            ListItem(
                headlineContent = { Text(label) },
                leadingContent = { RadioButton(selected = selected, onClick = null) },
                modifier = Modifier
                    .vesnaFocusRing(radius = 12.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(choice) }),
            )
        }
    }
}

@Composable
private fun SettingsSwitch(
    @StringRes title: Int,
    @StringRes summary: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier
            .vesnaFocusRing(radius = 12.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** A discrete size choice over the offered percentage steps; the artwork alone changes. */
@Composable
private fun SettingsSlider(
    @StringRes title: Int,
    @StringRes summary: Int,
    percent: Int,
    onValueChange: (Int) -> Unit,
    enabled: Boolean = true,
) {
    val value = stringResource(R.string.settings_icon_size_value, percent)
    val description = stringResource(title)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(title),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(summary),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
        Slider(
            value = percent.toFloat(),
            onValueChange = { onValueChange(IconScale.coerce(it.toInt())) },
            valueRange = IconScale.steps.first().toFloat()..IconScale.steps.last().toFloat(),
            steps = IconScale.steps.size - 2,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description; stateDescription = value },
        )
    }
}

@Composable
private fun DrawerAppSettings(
    layoutMode: DrawerLayoutMode,
    onLayoutModeSelected: (DrawerLayoutMode) -> Unit,
    closeOnLaunch: Boolean,
    onCloseOnLaunchChanged: (Boolean) -> Unit,
    rememberPosition: Boolean,
    onRememberPositionChanged: (Boolean) -> Unit,
    showKeyboard: Boolean,
    onShowKeyboardChanged: (Boolean) -> Unit,
    iconPercent: Int,
    onIconPercentChanged: (Int) -> Unit,
    followHome: Boolean,
    onFollowHomeChanged: (Boolean) -> Unit,
    apps: List<LauncherApp>,
    hiddenApps: Set<AppKey>,
    onHiddenChanged: (AppKey, Boolean) -> Unit,
    hideHighlighted: Boolean,
    layoutHighlighted: Boolean,
    closeOnLaunchHighlighted: Boolean,
    rememberPositionHighlighted: Boolean,
    showKeyboardHighlighted: Boolean,
    followHomeHighlighted: Boolean,
    iconSizeHighlighted: Boolean,
) {
    // A search target may not be composed yet in a short, scaled viewport.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = when {
        hideHighlighted -> 6
        iconSizeHighlighted -> 5
        followHomeHighlighted -> 4
        showKeyboardHighlighted -> 3
        rememberPositionHighlighted -> 2
        closeOnLaunchHighlighted -> 1
        else -> 0
    })
    LazyColumn(Modifier.fillMaxWidth(), state = listState) {
        item(key = "drawer-layout") {
            SearchTarget(layoutHighlighted) {
                DrawerLayoutSettings(layoutMode, onLayoutModeSelected)
            }
        }
        item(key = "close-on-launch") {
            SearchTarget(closeOnLaunchHighlighted) {
                SettingsSwitch(
                    R.string.settings_drawer_close_on_launch, R.string.settings_drawer_close_on_launch_detail,
                    closeOnLaunch, onCloseOnLaunchChanged,
                )
            }
        }
        item(key = "remember-position") {
            SearchTarget(rememberPositionHighlighted) {
                SettingsSwitch(
                    R.string.settings_drawer_remember_position, R.string.settings_drawer_remember_position_detail,
                    rememberPosition, onRememberPositionChanged,
                )
            }
        }
        item(key = "show-keyboard") {
            SearchTarget(showKeyboardHighlighted) {
                SettingsSwitch(
                    R.string.settings_drawer_show_keyboard, R.string.settings_drawer_show_keyboard_detail,
                    showKeyboard, onShowKeyboardChanged,
                )
            }
        }
        item(key = "follow-home") {
            SearchTarget(followHomeHighlighted) {
                SettingsSwitch(
                    R.string.settings_drawer_follow_home, R.string.settings_drawer_follow_home_detail,
                    followHome, onFollowHomeChanged,
                )
            }
        }
        item(key = "icon-size") {
            SearchTarget(iconSizeHighlighted) {
                SettingsSlider(
                    R.string.settings_drawer_icon_size, R.string.settings_drawer_icon_size_detail,
                    iconPercent, onIconPercentChanged, enabled = !followHome,
                )
            }
        }
        item(key = "hide-apps") {
            SearchTarget(hideHighlighted) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        stringResource(R.string.settings_hide_drawer_apps),
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_hide_drawer_apps_detail),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        items(apps, key = { it.key.drawerPreferenceKey() }) { app ->
            val hidden = app.key in hiddenApps
            val visibility = stringResource(
                if (hidden) R.string.settings_drawer_app_hidden else R.string.settings_drawer_app_shown,
            )
            val resources = LocalContext.current.resources
            val icon = remember(app.icon, resources) { app.icon?.constantState?.newDrawable(resources)?.mutate() }
            ListItem(
                headlineContent = { Text(app.accessibilityLabel) },
                leadingContent = if (icon == null) null else {
                    {
                        AndroidView(
                            factory = { context ->
                                ImageView(context).apply {
                                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                                }
                            },
                            modifier = Modifier.size(24.dp),
                            update = { it.setImageDrawable(icon) },
                        )
                    }
                },
                trailingContent = { Switch(checked = hidden, onCheckedChange = null) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .vesnaFocusRing(radius = 12.dp)
                    .toggleable(value = hidden, role = Role.Switch) { onHiddenChanged(app.key, it) }
                    .semantics { stateDescription = visibility },
            )
        }
    }
}

@Composable
private fun DrawerLayoutSettings(mode: DrawerLayoutMode, onSelect: (DrawerLayoutMode) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.settings_drawer_layout),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_drawer_layout_detail),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (choice in DrawerLayoutMode.entries) {
                val selected = choice == mode
                ListItem(
                    headlineContent = { Text(stringResource(choice.title)) },
                    leadingContent = { RadioButton(selected = selected, onClick = null) },
                    modifier = Modifier
                        .vesnaFocusRing(radius = 12.dp)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(choice) }),
                )
            }
        }
    }
}

@Composable
private fun WorkspaceGridSettings(
    grid: GridCanvas,
    locked: Boolean,
    onResize: suspend (GridCanvas) -> GridResizeResult,
) {
    // The draft survives recreation; navigating away keys the page content and
    // starts a new session, matching the rest of Settings.
    var editing by rememberSaveable { mutableStateOf(false) }
    var draftColumns by rememberSaveable { mutableIntStateOf(grid.columns) }
    var draftRows by rememberSaveable { mutableIntStateOf(grid.rows) }
    var rejection by rememberSaveable { mutableIntStateOf(GRID_REJECTION_NONE) }
    // Not saveable: a recreation cancels the in-flight submit with its scope and
    // re-enables Apply on the restored editor instead of leaving it stuck.
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_workspace_grid)) },
        supportingContent = {
            Column {
                Text(workspaceGridSummary(grid))
                if (locked) Text(stringResource(R.string.workspace_layout_locked))
            }
        },
        trailingContent = if (locked || editing) null else {
            { Icon(painterResource(R.drawable.ic_settings_forward), contentDescription = null) }
        },
        modifier = Modifier.vesnaFocusRing(radius = 12.dp).then(
            // Locked layouts disable the row; the supporting text names the reason.
            // While the editor is open, the row stays inert so a stray tap cannot
            // discard the draft.
            if (locked || editing) Modifier else Modifier.clickable(role = Role.Button) {
                draftColumns = grid.columns
                draftRows = grid.rows
                rejection = GRID_REJECTION_NONE
                editing = true
            },
        ),
    )
    if (!editing) return

    Spacer(Modifier.height(8.dp))
    Column {
        Text(
            stringResource(R.string.settings_workspace_grid),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_workspace_grid_editor_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        WorkspaceGridDimensionChoice(
            label = stringResource(R.string.settings_workspace_grid_columns),
            selected = draftColumns,
            range = GridCanvas.MIN_COLUMNS..GridCanvas.MAX_COLUMNS,
            enabled = !submitting,
        ) {
            draftColumns = it
            rejection = GRID_REJECTION_NONE
        }
        Spacer(Modifier.height(8.dp))
        WorkspaceGridDimensionChoice(
            label = stringResource(R.string.settings_workspace_grid_rows),
            selected = draftRows,
            range = GridCanvas.MIN_ROWS..GridCanvas.MAX_ROWS,
            enabled = !submitting,
        ) {
            draftRows = it
            rejection = GRID_REJECTION_NONE
        }
        val notice = when {
            locked -> stringResource(R.string.workspace_layout_locked) to true
            rejection == GRID_REJECTION_TOO_MANY_ITEMS ->
                stringResource(R.string.settings_workspace_grid_too_many_items) to false
            rejection == GRID_REJECTION_LOCKED -> stringResource(R.string.workspace_layout_locked) to false
            else -> null
        }
        if (notice != null) {
            Spacer(Modifier.height(8.dp))
            WorkspaceGridNotice(notice.first, informational = notice.second)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (!submitting) {
                    scope.launch {
                        submitting = true
                        val result = onResize(GridCanvas(columns = draftColumns, rows = draftRows))
                        submitting = false
                        when (result) {
                            GridResizeResult.APPLIED -> {
                                rejection = GRID_REJECTION_NONE
                                editing = false
                            }
                            GridResizeResult.TOO_MANY_ITEMS -> rejection = GRID_REJECTION_TOO_MANY_ITEMS
                            GridResizeResult.LOCKED -> rejection = GRID_REJECTION_LOCKED
                        }
                    }
                }
            },
            enabled = !submitting && !locked,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).vesnaFocusRing(),
        ) {
            Text(stringResource(R.string.settings_workspace_grid_apply), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                editing = false
                rejection = GRID_REJECTION_NONE
            },
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).vesnaFocusRing(),
        ) {
            Text(stringResource(R.string.settings_workspace_grid_cancel), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun WorkspaceGridDimensionChoice(
    label: String,
    selected: Int,
    range: IntRange,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(4.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        val count = range.count()
        range.forEachIndexed { index, value ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = count),
                modifier = Modifier.vesnaFocusRing(),
            ) {
                Text(value.toString())
            }
        }
    }
}

@Composable
private fun WorkspaceGridNotice(text: String, informational: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = if (informational) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (informational) MaterialTheme.colorScheme.onTertiaryContainer
        else MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun workspaceGridSummary(grid: GridCanvas): String = stringResource(
    R.string.settings_workspace_grid_summary_fmt,
    pluralStringResource(R.plurals.workspace_grid_columns, grid.columns, grid.columns),
    pluralStringResource(R.plurals.workspace_grid_rows, grid.rows, grid.rows),
)

private const val GRID_REJECTION_NONE = 0
private const val GRID_REJECTION_TOO_MANY_ITEMS = 1
private const val GRID_REJECTION_LOCKED = 2

@Composable
private fun DefaultLauncherSetup(state: DefaultHomeState, onSetDefaultHome: () -> Unit) {
    Text(
        text = stringResource(state.summary),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state == DefaultHomeState.AVAILABLE) {
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSetDefaultHome,
            modifier = Modifier.heightIn(min = 48.dp).vesnaFocusRing(),
        ) {
            Text(stringResource(R.string.menu_set_default_home), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AboutContent(versionName: String) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Android renders the same adaptive artwork and mask as the installed app icon.
        AndroidView(
            factory = { viewContext ->
                ImageView(viewContext).apply {
                    setImageResource(R.mipmap.ic_launcher)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            },
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.about_version_fmt, versionName),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.about_license),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, REPOSITORY_URL.toUri())) },
            modifier = Modifier.heightIn(min = 48.dp).vesnaFocusRing(),
        ) {
            Text(stringResource(R.string.about_github), textAlign = TextAlign.Center)
        }
    }
}

private const val REPOSITORY_URL = "https://github.com/vladon/vesnalauncher"
