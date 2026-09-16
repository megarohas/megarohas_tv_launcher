package tv.megarohas.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.KeyEvent
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var adapter: AppAdapter
    private lateinit var grid: RecyclerView
    private lateinit var timeView: TextView
    private lateinit var dateView: TextView
    private var needInitialFocus = true
    private var moveMode = false

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = updateClock()
    }
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeView = findViewById(R.id.time)
        dateView = findViewById(R.id.date)
        grid = findViewById(R.id.grid)

        adapter = AppAdapter(
            onClick = { app ->
                if (!moveMode && !Apps.launch(this, app)) {
                    Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show()
                }
            },
            onLongClick = { app -> if (!moveMode) showAppMenu(app) }
        )
        grid.layoutManager = GridLayoutManager(this, COLUMNS)
        grid.adapter = adapter
        grid.enableEdgeFade()

        findViewById<ImageButton>(R.id.btn_settings).apply {
            applyFocusScale(1.1f)
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, R.string.launch_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
        findViewById<ImageButton>(R.id.btn_hidden).apply {
            applyFocusScale(1.1f)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, HiddenAppsActivity::class.java))
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(timeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        })
        updateClock()
    }

    override fun onStop() {
        unregisterReceiver(timeReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onPause() {
        if (moveMode) finishMove()
        super.onPause()
    }

    // Из лаунчера не выходят: Back возвращает наверх списка.
    override fun onBackPressed() {
        grid.scrollToPosition(0)
        grid.post { grid.getChildAt(0)?.requestFocus() }
    }

    // Режим перемещения: стрелки двигают карточку, OK или Back завершают.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (moveMode) {
            val code = event.keyCode
            val handled = when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_BACK -> true
                else -> false
            }
            if (handled && event.action == KeyEvent.ACTION_DOWN) {
                when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> nudge(-1)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> nudge(1)
                    KeyEvent.KEYCODE_DPAD_UP -> nudge(-COLUMNS)
                    KeyEvent.KEYCODE_DPAD_DOWN -> nudge(COLUMNS)
                    else -> finishMove()
                }
            }
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun startMove(app: AppEntry) {
        val pos = adapter.indexOf(app.packageName)
        if (pos < 0) return
        moveMode = true
        adapter.movingPos = pos
        styleMovingCard(pos, true)
        grid.post { grid.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus() }
        Toast.makeText(this, R.string.move_hint, Toast.LENGTH_LONG).show()
    }

    private fun finishMove() {
        val pos = adapter.movingPos
        moveMode = false
        adapter.movingPos = -1
        styleMovingCard(pos, false)
        Apps.saveOrder(this, adapter.currentPackages())
    }

    private fun nudge(delta: Int) {
        val from = adapter.movingPos
        if (from < 0) return
        val to = (from + delta).coerceIn(0, adapter.itemCount - 1)
        if (to == from) return
        adapter.moveItem(from, to)
        // Держим ряд перемещаемой карточки в комфортной зоне: не в фейде у
        // верхней кромки и не за нижним краем. RecyclerView сам ограничит
        // скролл границами контента.
        grid.post {
            val v = grid.findViewHolderForAdapterPosition(to)?.itemView
            if (v == null) {
                grid.scrollToPosition(to)
                return@post
            }
            val topLimit = v.height / 2
            val bottomLimit = grid.height - v.height / 4
            when {
                v.top < topLimit -> grid.smoothScrollBy(0, v.top - topLimit)
                v.bottom > bottomLimit -> grid.smoothScrollBy(0, v.bottom - bottomLimit)
            }
        }
    }

    private fun styleMovingCard(pos: Int, moving: Boolean) {
        val vh = grid.findViewHolderForAdapterPosition(pos) as? AppAdapter.Holder ?: return
        vh.card.foreground = if (moving) getDrawable(R.drawable.card_stroke_moving) else null
    }

    private fun refresh() {
        Thread {
            val list = Apps.visible(this)
            runOnUiThread {
                if (moveMode) return@runOnUiThread
                val hadGridFocus = grid.focusedChild != null
                grid.updateKeepingFocus { adapter.submit(list) }
                if (hadGridFocus) needInitialFocus = false
                if (needInitialFocus && list.isNotEmpty()) {
                    needInitialFocus = false
                    grid.post { grid.getChildAt(0)?.requestFocus() }
                }
            }
        }.start()
    }

    private fun updateClock() {
        val now = Date()
        timeView.text = DateFormat.getTimeFormat(this).format(now)
        val df = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        dateView.text = df.format(now).replaceFirstChar { it.uppercase() }
    }

    private fun showAppMenu(app: AppEntry) {
        val actions = arrayOf(
            getString(R.string.menu_move),
            getString(R.string.menu_hide),
            getString(R.string.menu_info),
            getString(R.string.menu_uninstall)
        )
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> startMove(app)
                    1 -> {
                        Apps.setHidden(this, app.packageName, true)
                        refresh()
                    }
                    2 -> startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + app.packageName)
                        )
                    )
                    3 -> startActivity(
                        Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName))
                    )
                }
            }
            .show()
    }

    companion object {
        private const val COLUMNS = 5
    }
}
