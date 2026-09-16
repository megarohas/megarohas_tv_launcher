package tv.megarohas.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * Перехват кнопки HOME в духе Projectivy: Google TV жёстко резолвит HOME
 * в свой лаунчер (priority=2), пока тот включён. Вместо отключения стока
 * (опасно: чёрный экран при загрузке, если кастомный лаунчер не поднялся)
 * следим через accessibility за появлением окна стокового лаунчера и сразу
 * поднимаем наш экран поверх. Сток остаётся включённым и служит страховкой.
 */
class HomeWatchService : AccessibilityService() {

    private var lastLaunch = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != STOCK_HOME) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunch < 800) return
        lastLaunch = now
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    override fun onInterrupt() {}

    companion object {
        const val STOCK_HOME = "com.google.android.apps.tv.launcherx"
    }
}
