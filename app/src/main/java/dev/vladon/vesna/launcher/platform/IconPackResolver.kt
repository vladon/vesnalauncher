package dev.vladon.vesna.launcher.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import dev.vladon.vesna.launcher.settings.AppearancePreferences
import java.util.Locale

/** One installed icon pack discoverable through the ADW theme action. */
internal data class IconPackInfo(val packageName: String, val label: String)

/**
 * Serves artwork from an installed icon pack in the documented ADW format, which
 * Arcticons and other CandyBar packs follow: a launchable activity declaring the
 * `org.adw.launcher.THEMES` action, plus `assets/appfilter.xml` mapping
 * `ComponentInfo{package/class}` entries to drawable resource names inside the
 * pack package. A component the pack does not cover resolves to null, so the
 * caller falls back to system artwork.
 *
 * All lookups must be off the main thread; [discover] callers already run on IO.
 */
internal class IconPackResolver(context: Context, private val appearance: AppearancePreferences) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    private val cacheLock = Any()
    private var cachedPack: String? = null
    private var cachedResources: Resources? = null
    private var componentMap: Map<String, String> = emptyMap()
    private val drawableIds = HashMap<String, Int>()

    /** Installed, visible packs sorted by label for a stable settings list. */
    fun installedPacks(): List<IconPackInfo> {
        val intent = Intent(THEME_ACTION).addCategory(Intent.CATEGORY_DEFAULT)
        val packs = try {
            packageManager.queryIntentActivities(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }
        return packs.mapNotNull { info ->
            val label = info.loadLabel(packageManager).toString()
            if (label.isBlank()) null else IconPackInfo(info.activityInfo.packageName, label)
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    /**
     * The selected pack's artwork for a launchable activity, or null when the
     * user chose system icons, the pack is missing, or the pack does not cover
     * the component. The caller badges work-profile artwork itself.
     */
    fun selectedDrawableFor(appPackage: String, activityClass: String, densityDpi: Int): Drawable? {
        val pack = appearance.iconPack
        if (pack == AppearancePreferences.SYSTEM_ICON_PACK) return null
        val packData = packResources(pack) ?: return null
        val (resources, componentMap) = packData
        val drawableName = componentMap["$appPackage/$activityClass"] ?: return null
        val resId = synchronized(cacheLock) { drawableIds.getOrPut(drawableName) {
            resources.getIdentifier(drawableName, "drawable", pack)
        } }
        if (resId <= 0) return null
        return try {
            resources.getDrawable(resId, null)
        } catch (_: Resources.NotFoundException) {
            synchronized(cacheLock) { drawableIds.remove(drawableName) }
            null
        } catch (_: Exception) {
            null
        }
    }

    /** Loads the pack's resources and component map once per selected package. */
    private fun packResources(pack: String): Pair<Resources, Map<String, String>>? {
        synchronized(cacheLock) {
            if (cachedPack == pack && cachedResources != null) {
                return cachedResources!! to componentMap
            }
        }
        val resources = try {
            packageManager.getResourcesForApplication(pack)
        } catch (_: PackageManager.NameNotFoundException) {
            markUnresolvable(pack)
            return null
        }
        val map = try {
            resources.assets.open(APPFILTER_ASSET).bufferedReader().use { reader ->
                parseAppfilter(reader.lineSequence())
            }
        } catch (_: Exception) {
            markUnresolvable(pack)
            return null
        }
        synchronized(cacheLock) {
            cachedPack = pack
            cachedResources = resources
            componentMap = map
            drawableIds.clear()
        }
        return resources to map
    }

    private fun markUnresolvable(pack: String) {
        synchronized(cacheLock) {
            cachedPack = pack
            cachedResources = null
            componentMap = emptyMap()
            drawableIds.clear()
        }
    }

    private companion object {
        const val THEME_ACTION = "org.adw.launcher.THEMES"
        const val APPFILTER_ASSET = "appfilter.xml"
    }
}

/**
 * Parses the documented appfilter mapping into component keys
 * (`package/class`) and drawable names. The first entry wins so a pack cannot
 * redefine a component twice; malformed lines are skipped. Line-scanning with
 * [indexOf] instead of a regex keeps the pass linear on Android's regex
 * engine, which backtracks badly on the multi-megabyte Arcticons tables.
 */
internal fun parseAppfilter(lines: Sequence<String>): Map<String, String> {
    val componentMarker = "component=\"ComponentInfo{"
    val drawableMarker = "drawable=\""
    val map = HashMap<String, String>()
    for (line in lines) {
        val componentStart = line.indexOf(componentMarker).takeIf { it >= 0 } ?: continue
        val subjectStart = componentStart + componentMarker.length
        val subjectEnd = line.indexOf('}', subjectStart).takeIf { it >= 0 } ?: continue
        val subject = line.substring(subjectStart, subjectEnd)
        val slash = subject.indexOf('/').takeIf { it >= 0 } ?: continue
        if (slash == 0 || slash == subject.length - 1) continue
        val drawableStart = line.indexOf(drawableMarker, subjectEnd).takeIf { it >= 0 } ?: continue
        val nameStart = drawableStart + drawableMarker.length
        val nameEnd = line.indexOf('"', nameStart).takeIf { it >= 0 } ?: continue
        if (nameEnd == nameStart) continue
        map.putIfAbsent(subject, line.substring(nameStart, nameEnd))
    }
    return map
}

/** Convenience overload for in-memory fixtures. */
internal fun parseAppfilter(xml: String): Map<String, String> = parseAppfilter(xml.lineSequence())
