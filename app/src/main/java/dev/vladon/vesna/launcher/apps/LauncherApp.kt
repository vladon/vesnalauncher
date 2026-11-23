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
 *
 * [canonicalLabel] is the publisher label a rendered pin carries once a per-pin
 * rename has been projected onto it ([withVisibleLabel]): [label] then shows the
 * user's visible name while [publisherLabel] still resolves the canonical name,
 * so copying a renamed pin can never persist the override as publisher data.
 */
data class LauncherApp(
    val key: AppKey,
    val label: String,
    val icon: Drawable? = null,
    private val badgedLabel: String? = null,
    internal val canonicalLabel: String? = null,
) {
    val packageName: String get() = key.packageName

    /** The canonical (publisher) label, even when [label] shows a per-pin rename. */
    internal val publisherLabel: String get() = canonicalLabel ?: label

    /**
     * Profile-aware name for accessibility. Without a rename this is the badged
     * label (unchanged behavior). A renamed pin announces the visible name first
     * and then the badged app name, so custom names stay clear while the
     * profile badge is never dropped.
     */
    internal val accessibilityLabel: String
        get() = when {
            canonicalLabel == null || badgedLabel == null -> badgedLabel ?: label
            else -> "$label, $badgedLabel"
        }

    /**
     * Renders this catalogue entry under a per-pin label override (a workspace,
     * dock or folder rename) without mutating the catalogue itself. The copy
     * keeps the same [icon] instance, remembers the canonical label for
     * [publisherLabel], and never rebuilds a drawable.
     */
    internal fun withVisibleLabel(visibleLabel: String?): LauncherApp =
        if (visibleLabel == null || visibleLabel == label) this
        else copy(label = visibleLabel, canonicalLabel = canonicalLabel ?: label)
}
