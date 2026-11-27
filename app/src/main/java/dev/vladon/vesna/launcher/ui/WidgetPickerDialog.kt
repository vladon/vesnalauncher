package dev.vladon.vesna.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.vladon.vesna.launcher.R

/** One browsable catalog row (#149): the provider's widget with its size. */
data class WidgetPickerEntry(
    val info: AppWidgetProviderInfo,
    val providerLabel: String,
    val widgetLabel: String,
    val icon: Drawable?,
    val preview: Drawable?,
    val spanText: String,
)

/**
 * Case-insensitive label containment over [query] trimmed; the empty query
 * keeps everything. [labels] returns the widget and provider labels (#149).
 */
internal fun <T> filterByLabels(
    items: List<T>,
    query: String,
    labels: (T) -> Pair<String, String>,
): List<T> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return items
    return items.filter {
        val (widgetLabel, providerLabel) = labels(it)
        widgetLabel.contains(trimmed, ignoreCase = true) ||
            providerLabel.contains(trimmed, ignoreCase = true)
    }
}

internal fun filterWidgetEntries(
    entries: List<WidgetPickerEntry>,
    query: String,
): List<WidgetPickerEntry> = filterByLabels(entries, query) { it.widgetLabel to it.providerLabel }

/**
 * The searchable widget catalog (#149): the installed widgets grouped by
 * provider, each row showing a preview, a name, and the grid size. A query
 * filters the list; a non-matching query shows an explicit empty state.
 * Browsing binds nothing — placement is the caller's [onPick].
 */
@Composable
fun WidgetPickerDialog(
    entries: List<WidgetPickerEntry>,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visible = remember(query, entries) { filterWidgetEntries(entries, query) }
    val grouped = remember(visible) {
        visible.groupBy { it.providerLabel }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_picker_close)) }
        },
        title = { Text(stringResource(R.string.menu_add_widget)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.widget_picker_search_hint)) },
                )
                if (visible.isEmpty()) {
                    Text(
                        stringResource(R.string.widget_picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        grouped.forEach { (providerLabel, rows) ->
                            item(key = "header-$providerLabel") {
                                Text(
                                    providerLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                )
                            }
                            items(rows, key = { it.info.provider.flattenToString() }) { entry ->
                                // The whole row places the widget: no squeezed
                                // per-row button (it collapsed to a vertical
                                // letter column on narrow widths).
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { onPick(entry.info) }
                                        .padding(vertical = 10.dp),
                                ) {
                                    AndroidView(
                                        factory = { context ->
                                            ImageView(context).apply {
                                                val drawable = entry.preview ?: entry.icon
                                                drawable?.setBounds(0, 0, 96, 96)
                                                setImageDrawable(drawable)
                                                scaleType = ImageView.ScaleType.FIT_CENTER
                                            }
                                        },
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Column(Modifier.padding(start = 12.dp)) {
                                        Text(
                                            entry.widgetLabel,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            entry.spanText,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}
