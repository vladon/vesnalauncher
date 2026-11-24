package dev.vladon.vesna.launcher.ui

import android.content.Context
import androidx.annotation.AttrRes

/** Reads a View-layer palette role from the context theme (see Theme.Vesna.Chrome overlays). */
internal fun Context.vesnaPaletteColor(@AttrRes attr: Int): Int = obtainStyledAttributes(intArrayOf(attr)).run {
    try {
        getColor(0, 0)
    } finally {
        recycle()
    }
}
