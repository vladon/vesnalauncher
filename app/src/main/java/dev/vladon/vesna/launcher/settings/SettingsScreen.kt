package dev.vladon.vesna.launcher.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.vladon.vesna.launcher.R

private val VesnaLightScheme = lightColorScheme(
    primary = Color(0xFF49672E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCAEEA6),
    secondary = Color(0xFF57624A),
    surface = Color(0xFFF9FAEF),
    onSurface = Color(0xFF1A1D16),
    onSurfaceVariant = Color(0xFF44483E),
    outline = Color(0xFF74796D),
    outlineVariant = Color(0xFFC4C8BA),
)

private val VesnaDarkScheme = darkColorScheme(
    primary = Color(0xFFAFD18C),
    onPrimary = Color(0xFF1D3703),
    primaryContainer = Color(0xFF324E18),
    secondary = Color(0xFFBFCBAE),
    surface = Color(0xFF11140E),
    onSurface = Color(0xFFE2E3D9),
    onSurfaceVariant = Color(0xFFC4C8BA),
    outline = Color(0xFF8E9286),
    outlineVariant = Color(0xFF44483E),
)

@Composable
fun VesnaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) VesnaDarkScheme else VesnaLightScheme,
        content = content,
    )
}

internal enum class DefaultHomeState { ACTIVE, AVAILABLE, UNAVAILABLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    versionName: String,
    defaultHome: DefaultHomeState,
    onSetDefaultHome: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.menu_launcher_settings)) }) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { contentPadding ->
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            ) {
                DefaultLauncherSetup(defaultHome, onSetDefaultHome)
                HorizontalDivider(Modifier.padding(vertical = 24.dp))
                AboutContent(versionName)
            }
        }
    }
}

@Composable
private fun DefaultLauncherSetup(state: DefaultHomeState, onSetDefaultHome: () -> Unit) {
    Text(
        text = stringResource(R.string.settings_default_launcher),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(when (state) {
            DefaultHomeState.ACTIVE -> R.string.settings_default_home_active
            DefaultHomeState.AVAILABLE -> R.string.settings_default_home_setup
            DefaultHomeState.UNAVAILABLE -> R.string.settings_default_home_unavailable
        }),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (state == DefaultHomeState.AVAILABLE) {
        Spacer(Modifier.height(16.dp))
        var focused by remember { mutableStateOf(false) }
        val focusColor = MaterialTheme.colorScheme.secondary
        Button(
            onClick = onSetDefaultHome,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .onFocusChanged { focused = it.isFocused }
                .drawBehind {
                    if (focused) {
                        val offset = 3.dp.toPx()
                        drawRoundRect(
                            color = focusColor,
                            topLeft = Offset(-offset, -offset),
                            size = Size(size.width + 2 * offset, size.height + 2 * offset),
                            cornerRadius = CornerRadius(size.height / 2 + offset),
                            style = Stroke(2.dp.toPx()),
                        )
                    }
                },
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
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.about_github), textAlign = TextAlign.Center)
        }
    }
}

private const val REPOSITORY_URL = "https://github.com/vladon/vesnalauncher"
