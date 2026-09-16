package tv.megarohas.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Перехват кнопки HOME в духе Projectivy: Google TV жёстко резолвит HOME
 * в свой лаунчер (priority=2), пока тот включён. Вместо отключения стока
 * (опасно: чёрный экран при загрузке, если кастомный лаунчер не поднялся)
 * следим через accessibility за стоковым лаунчером и поднимаем наш экран
 * поверх. Сток остаётся включённым и служит страховкой.
 *
 * Слушаем два сигнала: появление окна стока (обычное нажатие HOME) и
 * изменение списка окон с активным стоком — так ловится выход из
 * заставки, под которой сток уже лежал и нового state-события не даёт.
 */
class HomeWatchService : AccessibilityService() {

    private var lastLaunch = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (event.packageName?.toString() == STOCK_HOME) launchOurHome("state")
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val active = try {
                    windows.firstOrNull { it.isActive }?.root?.packageName?.toString()
                } catch (_: Exception) {
                    null
                }
                if (active == STOCK_HOME) launchOurHome("windows")
            }
        }
    }

    private fun launchOurHome(reason: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunch < 800) return
        lastLaunch = now
        Log.i(TAG, "stock home active ($reason), bringing our launcher forward")
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "HomeWatch"
        const val STOCK_HOME = "com.google.android.apps.tv.launcherx"
    }
}
