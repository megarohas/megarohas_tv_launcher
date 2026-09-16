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
                if (!Apps.launch(this, app)) {
                    Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show()
                }
            },
            onLongClick = { app -> showAppMenu(app) }
        )
        grid.layoutManager = GridLayoutManager(this, 5)
        grid.adapter = adapter

        findViewById<ImageButton>(R.id.btn_settings).apply {
            applyFocusScale()
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, R.string.launch_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
        findViewById<ImageButton>(R.id.btn_hidden).apply {
            applyFocusScale()
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

    // Из лаунчера не выходят: Back возвращает наверх списка.
    override fun onBackPressed() {
        grid.scrollToPosition(0)
        grid.post { grid.getChildAt(0)?.requestFocus() }
    }

    private fun refresh() {
        Thread {
            val list = Apps.visible(this)
            runOnUiThread {
                adapter.submit(list)
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
            getString(R.string.menu_hide),
            getString(R.string.menu_info),
            getString(R.string.menu_uninstall)
        )
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        Apps.setHidden(this, app.packageName, true)
                        refresh()
                    }
                    1 -> startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + app.packageName)
                        )
                    )
                    2 -> startActivity(
                        Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName))
                    )
                }
            }
            .show()
    }
}
