package tv.megarohas.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Плавное увеличение при фокусе, в духе tvOS. */
fun View.applyFocusScale(scale: Float = 1.15f) {
    setOnFocusChangeListener { v, focused ->
        val s = if (focused) scale else 1f
        v.animate().scaleX(s).scaleY(s).setDuration(140).start()
    }
}

class AppAdapter(
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    private val items = mutableListOf<AppEntry>()

    fun submit(list: List<AppEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val card: View = v.findViewById(R.id.card)
        val bannerView: ImageView = v.findViewById(R.id.banner)
        val iconView: ImageView = v.findViewById(R.id.icon)
        val labelView: TextView = v.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        val h = Holder(v)
        h.card.clipToOutline = true
        val dp = parent.resources.displayMetrics.density
        v.setOnFocusChangeListener { view, focused ->
            val s = if (focused) 1.12f else 1f
            view.animate().scaleX(s).scaleY(s).setDuration(150).start()
            view.translationZ = if (focused) 12f * dp else 0f
            h.card.animate().translationZ(if (focused) 6f * dp else 0f).setDuration(150).start()
            h.labelView.animate().alpha(if (focused) 1f else 0.55f).setDuration(150).start()
        }
        return h
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val app = items[position]
        if (app.banner != null) {
            h.bannerView.visibility = View.VISIBLE
            h.iconView.visibility = View.GONE
            h.bannerView.setImageDrawable(app.banner)
        } else {
            h.bannerView.visibility = View.GONE
            h.iconView.visibility = View.VISIBLE
            h.iconView.setImageDrawable(app.icon)
        }
        h.labelView.text = app.label
        h.labelView.alpha = if (h.itemView.isFocused) 1f else 0.55f
        h.itemView.setOnClickListener { onClick(app) }
        h.itemView.setOnLongClickListener { onLongClick(app); true }
    }

    override fun getItemCount() = items.size
}
