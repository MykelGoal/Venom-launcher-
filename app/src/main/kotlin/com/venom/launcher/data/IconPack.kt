package com.venom.launcher.data

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import androidx.core.content.res.ResourcesCompat
import android.graphics.drawable.Drawable
import java.util.Calendar
import org.xmlpull.v1.XmlPullParser

/**
 * Support for third-party icon packs (Nova / Apex / ADW / GO / Atom formats).
 *
 * We read `res/xml/appfilter.xml` out of the pack's APK, build a map of
 * `component -> drawable resource name`, then resolve drawables lazily against
 * the pack's own [Resources].
 */
class IconPack internal constructor(
    val packageName: String,
    val label: String,
    private val res: Resources,
) {
    private val byComponent = HashMap<String, String>()
    private val byPackage = HashMap<String, String>()
    private val calendarPrefixes = HashMap<String, String>()

    fun getDrawable(componentKey: String, packageName: String): Drawable? {
        val name = byComponent[componentKey]
            ?: byPackage[packageName]
            ?: calendarPrefixes[componentKey]?.let { prefix ->
                prefix + Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            }
            ?: return null

        val id = res.getIdentifier(name, "drawable", this.packageName)
        if (id == 0) return null
        return runCatching { ResourcesCompat.getDrawable(res, id, null) }.getOrNull()
    }

    val coverage: Int get() = byComponent.size

    internal fun parseAppFilter(id: Int) {
        val parser: XmlResourceParser = runCatching { res.getXml(id) }.getOrNull() ?: return
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "item" -> {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (component != null && drawable != null) {
                            componentKey(component)?.let { byComponent[it] = drawable }
                        }
                    }

                    "calendar" -> {
                        val component = parser.getAttributeValue(null, "component")
                        val prefix = parser.getAttributeValue(null, "prefix")
                        if (component != null && prefix != null) {
                            componentKey(component)?.let { calendarPrefixes[it] = prefix }
                        }
                    }
                }
            }
            event = try {
                parser.next()
            } catch (t: Throwable) {
                XmlPullParser.END_DOCUMENT
            }
        }
    }

    internal fun parseDrawableXml(id: Int) {
        val parser: XmlResourceParser = runCatching { res.getXml(id) }.getOrNull() ?: return
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val pkg = parser.getAttributeValue(null, "pkg")
                    ?: parser.getAttributeValue(null, "package")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (pkg != null && drawable != null) byPackage[pkg] = drawable
            }
            event = try {
                parser.next()
            } catch (t: Throwable) {
                XmlPullParser.END_DOCUMENT
            }
        }
    }

    /** "ComponentInfo{com.foo/com.foo.Main}" -> "com.foo/com.foo.Main" */
    private fun componentKey(raw: String): String? {
        var s = raw.trim()
        val start = s.indexOf('{')
        if (start >= 0) {
            val end = s.indexOf('}', start)
            if (end > start) s = s.substring(start + 1, end)
        }
        return s.takeIf { it.contains('/') }
    }

    companion object {
        fun load(packageName: String, pm: PackageManager): IconPack? {
            val res = runCatching { pm.getResourcesForApplication(packageName) }.getOrNull()
                ?: return null
            val appFilterId = res.getIdentifier("appfilter", "xml", packageName)
            if (appFilterId == 0) return null

            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)

            val pack = IconPack(packageName, label, res)
            val drawableXmlId = res.getIdentifier("drawable", "xml", packageName)
            if (drawableXmlId != 0) runCatching { pack.parseDrawableXml(drawableXmlId) }
            runCatching { pack.parseAppFilter(appFilterId) }
            return pack
        }
    }
}

data class IconPackRef(val packageName: String, val label: String)

object IconPackManager {

    private val THEME_ACTIONS = listOf(
        "com.novalauncher.THEME",
        "org.adw.launcher.THEME",
        "com.fede.launcher.THEME",
        "com.gau.go.launcherex.theme",
        "com.dlto.atomlauncher.THEME",
        "org.adw.launcher.icons.ACTION_PICK_ICON",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
    )

    /** Installed icon packs, discovered via the standard theme broadcast actions. */
    suspend fun listPacks(context: Context): List<IconPackRef> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val pm = context.packageManager
            val found = LinkedHashMap<String, String>()
            for (action in THEME_ACTIONS) {
                val infos = runCatching {
                    pm.queryIntentActivities(android.content.Intent(action), 0)
                }.getOrNull() ?: continue
                for (ri in infos) {
                    val pkg = ri.activityInfo?.packageName ?: continue
                    if (pkg == context.packageName) continue
                    if (found.containsKey(pkg)) continue
                    val label = runCatching { ri.loadLabel(pm).toString() }.getOrNull() ?: pkg
                    found[pkg] = label
                }
            }
            // Some packs don't advertise an intent but do ship appfilter.xml
            val extra = runCatching {
                pm.getInstalledApplications(0)
                    .filter { ai ->
                        val n = ai.packageName
                        !found.containsKey(n) &&
                            (n.contains("iconpack", true) || n.contains("icon.pack", true) ||
                                n.contains("icons", true) || n.contains(".icons.", true))
                    }
            }.getOrDefault(emptyList())
            for (ai in extra) {
                val hasFilter = runCatching {
                    val r = pm.getResourcesForApplication(ai.packageName)
                    r.getIdentifier("appfilter", "xml", ai.packageName) != 0
                }.getOrDefault(false)
                if (hasFilter) found[ai.packageName] = runCatching {
                    pm.getApplicationLabel(ai).toString()
                }.getOrDefault(ai.packageName)
            }

            found.map { (pkg, label) -> IconPackRef(pkg, label) }
                .sortedBy { it.label.lowercase() }
        }

    suspend fun load(context: Context, packageName: String?): IconPack? {
        if (packageName.isNullOrBlank()) return null
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            IconPack.load(packageName, context.packageManager)
        }
    }
}
