package tv.megarohas.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import java.text.Collator
import java.util.Locale

data class AppEntry(
    val label: String,
    val packageName: String,
    val banner: Drawable?,
    val icon: Drawable,
    val accent: Int
)

object Apps {
    private const val PREFS = "launcher"
    private const val KEY_HIDDEN = "hidden"
    private const val KEY_ORDER = "order"
    private const val DEFAULT_ACCENT = 0xFF4A6E9C.toInt()

    fun hidden(ctx: Context): Set<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

    fun setHidden(ctx: Context, pkg: String, hide: Boolean) {
        val set = hidden(ctx).toMutableSet()
        if (hide) set.add(pkg) else set.remove(pkg)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_HIDDEN, set).apply()
    }

    fun savedOrder(ctx: Context): List<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ORDER, null)?.split("\n")?.filter { it.isNotEmpty() } ?: emptyList()

    fun saveOrder(ctx: Context, packages: List<String>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ORDER, packages.joinToString("\n")).apply()
    }

    fun all(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val seen = LinkedHashMap<String, AppEntry>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val ai = ri.activityInfo ?: continue
                val pkg = ai.packageName
                if (pkg == ctx.packageName || seen.containsKey(pkg)) continue
                val banner = if (category == Intent.CATEGORY_LEANBACK_LAUNCHER) loadBanner(pm, ri) else null
                val icon = ri.loadIcon(pm)
                seen[pkg] = AppEntry(
                    label = ri.loadLabel(pm)?.toString() ?: pkg,
                    packageName = pkg,
                    banner = banner,
                    icon = icon,
                    accent = accentOf(banner ?: icon)
                )
            }
        }
        val collator = Collator.getInstance(Locale.getDefault())
        val posMap = HashMap<String, Int>()
        savedOrder(ctx).forEachIndexed { i, p -> posMap[p] = i }
        return seen.values.sortedWith(Comparator { a, b ->
            val ia = posMap[a.packageName] ?: Int.MAX_VALUE
            val ib = posMap[b.packageName] ?: Int.MAX_VALUE
            if (ia != ib) Integer.compare(ia, ib)
            else collator.compare(a.label.lowercase(), b.label.lowercase())
        })
    }

    /** Доминантный цвет артворка: среднее с весом по насыщенности, слегка усиленное. */
    private fun accentOf(d: Drawable): Int = try {
        val w = 24
        val h = 14
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, w, h)
        d.draw(Canvas(bmp))
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        bmp.recycle()
        val hsv = FloatArray(3)
        var sr = 0.0
        var sg = 0.0
        var sb = 0.0
        var sw = 0.0
        for (p in px) {
            if (p ushr 24 < 0x80) continue
            Color.colorToHSV(p, hsv)
            val weight = hsv[1] * hsv[2] + 0.03f
            sr += Color.red(p) * weight
            sg += Color.green(p) * weight
            sb += Color.blue(p) * weight
            sw += weight
        }
        if (sw <= 0) DEFAULT_ACCENT else {
            Color.colorToHSV(
                Color.rgb((sr / sw).toInt(), (sg / sw).toInt(), (sb / sw).toInt()), hsv
            )
            hsv[1] = (hsv[1] * 1.35f).coerceAtMost(1f)
            hsv[2] = hsv[2].coerceAtLeast(0.55f)
            Color.HSVToColor(hsv)
        }
    } catch (_: Exception) {
        DEFAULT_ACCENT
    }

    private fun loadBanner(pm: PackageManager, ri: ResolveInfo): Drawable? = try {
        ri.activityInfo.loadBanner(pm) ?: ri.activityInfo.applicationInfo.loadBanner(pm)
    } catch (_: Exception) {
        null
    }

    fun visible(ctx: Context): List<AppEntry> {
        val hidden = hidden(ctx)
        return all(ctx).filter { it.packageName !in hidden }
    }

    fun hiddenEntries(ctx: Context): List<AppEntry> {
        val hidden = hidden(ctx)
        return all(ctx).filter { it.packageName in hidden }
    }

    fun launch(ctx: Context, entry: AppEntry): Boolean {
        val pm = ctx.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(entry.packageName)
            ?: pm.getLaunchIntentForPackage(entry.packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
