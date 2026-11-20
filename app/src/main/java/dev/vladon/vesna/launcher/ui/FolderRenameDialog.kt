package dev.vladon.vesna.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import dev.vladon.vesna.launcher.R

@Composable
internal fun FolderRenameDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(TextFieldValue(initialTitle, TextRange(0, initialTitle.length))) }
    var showNameError by remember { mutableStateOf(false) }
    val errorMessage = stringResource(R.string.folder_name_required)
    val focusRequester = remember { FocusRequester() }
    val submit: () -> Unit = {
        val title = draft.text.trim()
        if (title.isEmpty()) {
            showNameError = true
            focusRequester.requestFocus()
        } else {
            onSave(title)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_rename)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        if (showNameError && it.text.isNotBlank()) showNameError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics { if (showNameError) error(errorMessage) },
                    label = { Text(stringResource(R.string.folder_name)) },
                    isError = showNameError,
                    supportingText = if (showNameError) {
                        { Text(errorMessage) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(focusRequester) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = submit) { Text(stringResource(R.string.folder_rename_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
