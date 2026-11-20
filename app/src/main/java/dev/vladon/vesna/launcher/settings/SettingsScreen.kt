package dev.vladon.vesna.launcher.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.vladon.vesna.launcher.R

internal enum class DefaultHomeState { ACTIVE, AVAILABLE, UNAVAILABLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    page: SettingsPage,
    onNavigate: (SettingsPage) -> Unit,
    onResetInputFocus: () -> Unit,
    versionName: String,
    defaultHome: DefaultHomeState,
    onSetDefaultHome: () -> Unit,
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    showAppNames: Boolean,
    onShowAppNamesChanged: (Boolean) -> Unit,
) {
    val inSubpage = page != SettingsPage.ROOT
    val up = { onNavigate(SettingsPage.ROOT) }
    BackHandler(enabled = inSubpage, onBack = up)
    val titleHeight = with(LocalDensity.current) { MaterialTheme.typography.titleLarge.lineHeight.toDp() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(page.title),
                        modifier = Modifier.semantics { heading() },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (inSubpage) {
                        IconButton(onClick = up, modifier = Modifier.settingsFocusRing()) {
                            Icon(
                                painterResource(R.drawable.ic_settings_back),
                                contentDescription = stringResource(R.string.settings_back_to_root),
                            )
                        }
                    }
                },
                expandedHeight = maxOf(64.dp, titleHeight * 2 + 8.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { contentPadding ->
        Box(
            Modifier.fillMaxSize().padding(contentPadding).imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            key(page) {
                Column(
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .verticalScroll(remember { ScrollState(0) })
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                ) {
                    when (page) {
                        SettingsPage.ROOT -> SettingsDirectory(defaultHome, onNavigate, onResetInputFocus)
                        SettingsPage.DEFAULT_LAUNCHER -> DefaultLauncherSetup(defaultHome, onSetDefaultHome)
                        SettingsPage.APPEARANCE -> {
                            ThemeSettings(themeMode, onThemeSelected)
                            Spacer(Modifier.height(8.dp))
                            AppNamesSetting(showAppNames, onShowAppNamesChanged)
                        }
                        SettingsPage.ABOUT -> AboutContent(versionName)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDirectory(
    defaultHome: DefaultHomeState,
    onNavigate: (SettingsPage) -> Unit,
    onResetInputFocus: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val searchLabel = stringResource(R.string.settings_search)
    val colors = MaterialTheme.colorScheme
    // While the IME is visible, let Android handle Back before changing the search state.
    BackHandler(enabled = !WindowInsets.isImeVisible && (focused || query.isNotEmpty())) {
        onResetInputFocus()
        query = ""
    }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
            .fillMaxWidth()
            .settingsFocusRing()
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = searchLabel },
        placeholder = { Text(searchLabel) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }, modifier = Modifier.settingsFocusRing()) {
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
    Spacer(Modifier.height(16.dp))
    val term = query.trim()
    var found = false
    for (destination in SettingsPage.entries) {
        val summary = when (destination) {
            SettingsPage.ROOT -> continue
            SettingsPage.DEFAULT_LAUNCHER -> stringResource(defaultHome.summary)
            SettingsPage.APPEARANCE -> stringResource(R.string.settings_appearance_detail)
            SettingsPage.ABOUT -> stringResource(R.string.settings_about_detail)
        }
        val title = stringResource(destination.title)
        if (!title.contains(term, ignoreCase = true) &&
            !summary.contains(term, ignoreCase = true) &&
            !(destination == SettingsPage.APPEARANCE &&
                stringResource(R.string.settings_show_app_names).contains(term, ignoreCase = true))
        ) continue
        found = true
        key(destination) {
            ListItem(
                headlineContent = { Text(title) },
                supportingContent = { Text(summary) },
                trailingContent = { Icon(painterResource(R.drawable.ic_settings_forward), contentDescription = null) },
                modifier = Modifier.settingsFocusRing(radius = 12.dp).clickable(role = Role.Button) {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    onNavigate(destination)
                },
            )
        }
    }
    if (!found) {
        Text(
            stringResource(R.string.settings_search_empty),
            modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
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
                    .settingsFocusRing(radius = 12.dp)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(choice) }),
            )
        }
    }
}

@Composable
private fun AppNamesSetting(visible: Boolean, onVisibilityChanged: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_show_app_names)) },
        supportingContent = { Text(stringResource(R.string.settings_show_app_names_detail)) },
        trailingContent = { Switch(checked = visible, onCheckedChange = null) },
        modifier = Modifier
            .settingsFocusRing(radius = 12.dp)
            .toggleable(value = visible, role = Role.Switch, onValueChange = onVisibilityChanged),
    )
}

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
            modifier = Modifier.heightIn(min = 48.dp).settingsFocusRing(),
        ) {
            Text(stringResource(R.string.menu_set_default_home), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AboutContent(versionName: String) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(16.dp),
            )
        }
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
            modifier = Modifier.heightIn(min = 48.dp).settingsFocusRing(),
        ) {
            Text(stringResource(R.string.about_github), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Modifier.settingsFocusRing(radius: Dp? = null): Modifier {
    var focused by remember { mutableStateOf(false) }
    val focusColor = MaterialTheme.colorScheme.secondary
    return onFocusChanged { focused = it.isFocused }.drawBehind {
        if (focused) {
            // The center of the 2 dp stroke is 3 dp outside the control.
            val offset = 3.dp.toPx()
            drawRoundRect(
                color = focusColor,
                topLeft = Offset(-offset, -offset),
                size = Size(size.width + 2 * offset, size.height + 2 * offset),
                cornerRadius = CornerRadius((radius?.toPx() ?: size.height / 2) + offset),
                style = Stroke(2.dp.toPx()),
            )
        }
    }
}

private const val REPOSITORY_URL = "https://github.com/vladon/vesnalauncher"
