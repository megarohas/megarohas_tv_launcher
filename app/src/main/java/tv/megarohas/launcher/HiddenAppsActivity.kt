package tv.megarohas.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * «Папка» скрытых приложений: обычное нажатие запускает приложение,
 * удержание OK открывает меню с возвратом на главный экран.
 */
class HiddenAppsActivity : Activity() {

    private lateinit var adapter: AppAdapter
    private lateinit var grid: RecyclerView
    private lateinit var empty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hidden)

        grid = findViewById(R.id.grid)
        empty = findViewById(R.id.empty)

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
        grid.enableEdgeFade()
        refresh()
    }

    private fun showAppMenu(app: AppEntry) {
        val actions = arrayOf(
            getString(R.string.menu_unhide),
            getString(R.string.menu_info)
        )
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        Apps.setHidden(this, app.packageName, false)
                        Toast.makeText(
                            this,
                            getString(R.string.unhidden_toast, app.label),
                            Toast.LENGTH_SHORT
                        ).show()
                        refresh()
                    }
                    1 -> startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + app.packageName)
                        )
                    )
                }
            }
            .show()
    }

    private fun refresh() {
        Thread {
            val list = Apps.hiddenEntries(this)
            runOnUiThread {
                val hadGridFocus = grid.focusedChild != null
                grid.updateKeepingFocus { adapter.submit(list) }
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                if (!hadGridFocus && list.isNotEmpty()) {
                    grid.post { grid.getChildAt(0)?.requestFocus() }
                }
            }
        }.start()
    }
}
