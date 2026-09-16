package tv.megarohas.launcher

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
                Apps.setHidden(this, app.packageName, false)
                Toast.makeText(
                    this,
                    getString(R.string.unhidden_toast, app.label),
                    Toast.LENGTH_SHORT
                ).show()
                refresh()
            },
            onLongClick = { }
        )
        grid.layoutManager = GridLayoutManager(this, 5)
        grid.adapter = adapter
        refresh()
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
