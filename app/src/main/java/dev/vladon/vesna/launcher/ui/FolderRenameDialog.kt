package dev.vladon.vesna.launcher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.vladon.vesna.launcher.R
import kotlinx.coroutines.flow.first

@Composable
internal fun FolderRenameDialog(
    draft: TextFieldState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var showNameError by remember { mutableStateOf(false) }
    val errorMessage = stringResource(R.string.folder_name_required)
    val nameLabel = stringResource(R.string.folder_name)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(showNameError) {
        if (showNameError) {
            snapshotFlow { draft.text.isNotBlank() }.first { it }
            showNameError = false
        }
    }

    val dialogTitle = stringResource(R.string.folder_rename)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            windowTitle = dialogTitle,
        ),
    ) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        val keyboard = LocalSoftwareKeyboardController.current
        SideEffect { window.setDimAmount(0.32f) }
        Box(Modifier.fillMaxSize()) {
            // The window covers the display to receive IME insets. Keep outside taps
            // dismissible without making the backdrop an accessibility focus stop.
            Box(Modifier.matchParentSize().pointerInput(onDismiss) {
                detectTapGestures { onDismiss() }
            })
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentAlignment = Alignment.Center,
            ) {
                val availableHeight = maxHeight
                val standardFieldHeight = with(LocalDensity.current) {
                    MaterialTheme.typography.bodyLarge.lineHeight.toDp() +
                        MaterialTheme.typography.bodySmall.lineHeight.toDp() + 32.dp
                }
                val compactField = availableHeight < standardFieldHeight
                val submit: () -> Unit = {
                    val title = draft.text.toString().trim()
                    if (title.isEmpty()) {
                        showNameError = true
                        focusRequester.requestFocus()
                        // In this space the keyboard leaves room for the field, not its error.
                        if (compactField) keyboard?.hide()
                    } else {
                        onSave(title)
                    }
                }
                Surface(
                    modifier = Modifier.width((maxWidth - 48.dp).coerceIn(280.dp, 560.dp)),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    // The title and actions must scroll too: with the IME open they
                    // can be taller than the entire space available for the editor.
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                        Text(
                            text = dialogTitle,
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            state = draft,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .then(if (compactField) Modifier.heightIn(max = availableHeight) else Modifier)
                                .semantics {
                                    if (compactField) contentDescription = nameLabel
                                    if (showNameError) error(errorMessage)
                                },
                            label = if (compactField) null else ({ Text(nameLabel) }),
                            isError = showNameError,
                            contentPadding = if (compactField) {
                                PaddingValues(horizontal = 16.dp)
                            } else {
                                OutlinedTextFieldDefaults.contentPadding()
                            },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done,
                            ),
                            onKeyboardAction = { submit() },
                        )
                        if (showNameError) {
                            Text(
                                text = errorMessage,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(android.R.string.cancel))
                            }
                            TextButton(onClick = submit) {
                                Text(stringResource(R.string.folder_rename_save))
                            }
                        }
                    }
                }
            }
        }
        val windowInfo = LocalWindowInfo.current
        LaunchedEffect(focusRequester) {
            snapshotFlow { windowInfo.isWindowFocused }.first { it }
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
}
