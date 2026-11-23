package dev.vladon.vesna.launcher.ui

import android.content.res.Configuration
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import dev.vladon.vesna.launcher.R
import dev.vladon.vesna.launcher.apps.AppKey
import dev.vladon.vesna.launcher.apps.LauncherApp

/**
 * The stock Material picker behind the unlocked Home folder menu's Add apps
 * entry: it lists the installed apps that are not yet folder members and lets
 * the user check the ones to add.
 *
 * The dialog owns nothing. The owner keeps [selected] across recreations,
 * applies [onToggle] to its retained draft, reopens with the current theme,
 * and restores focus to the folder after [onDismiss] or [onSave]. Saving adds
 * the checked apps once, preserving the folder's position, order, and other
 * rows; dismissing discards the draft.
 *
 * Rows always show the app name even when the drawer hides labels, and their
 * accessible name is Android's profile-aware badged label. With no
 * installable apps left the dialog explains that and keeps Add disabled.
 */
@Composable
internal fun FolderAddAppsDialog(
    apps: List<LauncherApp>,
    selected: Set<AppKey>,
    onToggle: (AppKey) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    AlertDialog(
        onDismissRequest = onDismiss,
        // A wider landscape dialog keeps a wrapped heading and footer from
        // consuming the whole list viewport at enlarged font scales.
        modifier = if (landscape) Modifier.widthIn(max = 480.dp).padding(horizontal = 16.dp) else Modifier,
        properties = DialogProperties(usePlatformDefaultWidth = !landscape),
        title = {
            Text(
                stringResource(R.string.folder_add_apps),
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            if (apps.isEmpty()) {
                Text(
                    stringResource(R.string.folder_add_apps_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = VesnaFocusRingOutset)) {
                    items(apps) { app ->
                        AppChoiceRow(
                            app = app,
                            checked = app.key in selected,
                            onToggle = { onToggle(app.key) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = selected.isNotEmpty(), modifier = Modifier.vesnaFocusRing()) {
                Text(stringResource(R.string.folder_add_apps_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.vesnaFocusRing()) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

/**
 * One installable app: artwork, visible name, and the checked state. The whole
 * row toggles; the Checkbox only renders state, so each row is a single
 * accessibility node named by the profile-aware badged label.
 */
@Composable
private fun AppChoiceRow(app: LauncherApp, checked: Boolean, onToggle: () -> Unit) {
    val resources = LocalContext.current.resources
    val icon = remember(app.icon, resources) { app.icon?.constantState?.newDrawable(resources)?.mutate() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .vesnaFocusRing(radius = 12.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .clearAndSetSemantics { contentDescription = app.accessibilityLabel }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                },
                modifier = Modifier.size(48.dp),
                update = { it.setImageDrawable(icon) },
            )
        }
        Text(
            app.label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        Checkbox(checked = checked, onCheckedChange = null)
    }
}
