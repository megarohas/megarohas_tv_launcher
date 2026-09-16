package tv.megarohas.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import java.text.Collator
import java.util.Locale

data class AppEntry(
    val label: String,
    val packageName: String,
    val banner: Drawable?,
    val icon: Drawable
)

object Apps {
    private const val PREFS = "launcher"
    private const val KEY_HIDDEN = "hidden"

    fun hidden(ctx: Context): Set<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

    fun setHidden(ctx: Context, pkg: String, hide: Boolean) {
        val set = hidden(ctx).toMutableSet()
        if (hide) set.add(pkg) else set.remove(pkg)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_HIDDEN, set).apply()
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
                seen[pkg] = AppEntry(
                    label = ri.loadLabel(pm)?.toString() ?: pkg,
                    packageName = pkg,
                    banner = if (category == Intent.CATEGORY_LEANBACK_LAUNCHER) loadBanner(pm, ri) else null,
                    icon = ri.loadIcon(pm)
                )
            }
        }
        val collator = Collator.getInstance(Locale.getDefault())
        return seen.values.sortedWith(compareBy(collator) { it.label.lowercase() })
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
