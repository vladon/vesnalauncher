package dev.vladon.vesna.launcher.apps

import android.graphics.drawable.Drawable

/**
 * A launchable application entry shown in the drawer, workspace, and dock.
 *
 * [icon] is a framework [android.graphics.drawable.Drawable]; views that render it
 * must obtain a fresh drawable via `constantState.newDrawable()` because a single
 * Drawable instance must not be attached to multiple views.
 *
 * [badgedLabel] is Android's localized profile-aware name for accessibility.
 * Visible labels, sorting, and search continue to use [label].
 */
data class LauncherApp(
    val key: AppKey,
    val label: String,
    val icon: Drawable? = null,
    private val badgedLabel: String? = null,
) {
    val packageName: String get() = key.packageName
    internal val accessibilityLabel: String get() = badgedLabel ?: label
}
