package dev.vladon.vesna.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val VesnaFocusRingOutset = 4.dp

@Composable
internal fun Modifier.vesnaFocusRing(radius: Dp? = null): Modifier {
    var focused by remember { mutableStateOf(false) }
    val focusColor = MaterialTheme.colorScheme.secondary
    return onFocusChanged { focused = it.isFocused }.drawBehind {
        if (focused) {
            // The center of the 2 dp stroke is 3 dp outside the control.
            val offset = (VesnaFocusRingOutset - 1.dp).toPx()
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
