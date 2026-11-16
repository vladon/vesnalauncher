package dev.vladon.vesna.launcher.apps

import android.graphics.drawable.Drawable

/**
 * A launchable application entry shown in the drawer, workspace, and dock.
 *
 * [icon] is a framework [android.graphics.drawable.Drawable]; views that render it
 * must obtain a fresh drawable via `constantState.newDrawable()` because a single
 * Drawable instance must not be attached to multiple views.
 */
data class LauncherApp(
    val key: AppKey,
    val label: String,
    val icon: Drawable? = null,
) {
    val packageName: String get() = key.packageName
}
