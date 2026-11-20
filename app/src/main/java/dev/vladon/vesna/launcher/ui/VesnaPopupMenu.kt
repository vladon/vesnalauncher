package dev.vladon.vesna.launcher.ui

import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import dev.vladon.vesna.launcher.R

internal fun vesnaPopupMenu(anchor: View): PopupMenu = PopupMenu(
    ContextThemeWrapper(anchor.context, R.style.ThemeOverlay_Vesna_PopupMenu),
    anchor,
    Gravity.START,
)
