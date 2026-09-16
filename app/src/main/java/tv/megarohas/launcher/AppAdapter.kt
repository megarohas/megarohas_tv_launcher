package tv.megarohas.launcher

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

/**
 * Пофейдовое растворение рядов у кромок: элемент гаснет ровно в процессе
 * пересечения верха списка и проявляется, выплывая из-за нижнего края
 * (высота элемента = зона фейда), вместо жёсткой обрезки.
 */
fun RecyclerView.enableEdgeFade() {
    val apply = {
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.height > 0) {
                val top = c.bottom.toFloat() / c.height.toFloat()
                val bottom = (height - c.top).toFloat() / c.height.toFloat()
                c.alpha = minOf(top, bottom).coerceIn(0f, 1f)
            }
        }
    }
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) = apply()
    })
    viewTreeObserver.addOnGlobalLayoutListener { apply() }
}

/**
 * Выполняет обновление адаптера, сохраняя фокус на той же позиции сетки
 * (или ближайшей существующей), если фокус был внутри неё.
 */
fun RecyclerView.updateKeepingFocus(update: () -> Unit) {
    val pos = focusedChild?.let { getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
    update()
    val count = adapter?.itemCount ?: 0
    if (pos != RecyclerView.NO_POSITION && count > 0) {
        val p = pos.coerceAtMost(count - 1)
        post {
            scrollToPosition(p)
            post { findViewHolderForAdapterPosition(p)?.itemView?.requestFocus() }
        }
    }
}

class AppAdapter(
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    private val items = mutableListOf<AppEntry>()
    private val glowCache = HashMap<Int, Bitmap>()

    /** Позиция карточки, которую сейчас перетаскивают; -1 если нет. */
    var movingPos: Int = -1

    fun submit(list: List<AppEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val card: FrameLayout = v.findViewById(R.id.card)
        val glow: ImageView = v.findViewById(R.id.glow)
        val bannerView: ImageView = v.findViewById(R.id.banner)
        val iconView: ImageView = v.findViewById(R.id.icon)
        val labelView: TextView = v.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        val h = Holder(v)
        h.card.clipToOutline = true
        h.glow.scaleX = 1.35f
        h.glow.scaleY = 1.8f
        val dp = parent.resources.displayMetrics.density
        v.setOnFocusChangeListener { view, focused ->
            val s = if (focused) 1.12f else 1f
            view.animate().scaleX(s).scaleY(s).setDuration(150).start()
            view.translationZ = if (focused) 12f * dp else 0f
            h.card.animate().translationZ(if (focused) 6f * dp else 0f).setDuration(150).start()
            h.glow.animate().alpha(if (focused) 0.85f else 0f).setDuration(if (focused) 220L else 150L).start()
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
        h.card.foreground = if (position == movingPos)
            h.card.context.getDrawable(R.drawable.card_stroke_moving) else null
        h.glow.setImageBitmap(glowBitmap(app.accent))
        h.glow.alpha = if (h.itemView.isFocused) 0.85f else 0f
        h.labelView.text = app.label
        h.labelView.alpha = if (h.itemView.isFocused) 1f else 0.55f
        h.itemView.setOnClickListener { onClick(app) }
        h.itemView.setOnLongClickListener { onLongClick(app); true }
    }

    override fun getItemCount() = items.size

    fun indexOf(pkg: String): Int = items.indexOfFirst { it.packageName == pkg }

    fun currentPackages(): List<String> = items.map { it.packageName }

    fun moveItem(from: Int, to: Int) {
        if (from == to || from !in items.indices || to !in items.indices) return
        val item = items.removeAt(from)
        items.add(to, item)
        if (movingPos == from) movingPos = to
        notifyItemMoved(from, to)
    }

    /** Мягкое размытое гало в акцентном цвете; рисуется один раз на цвет. */
    private fun glowBitmap(color: Int): Bitmap = glowCache.getOrPut(color) {
        val w = 120
        val h = 72
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
        }
        Canvas(bmp).drawRoundRect(RectF(24f, 20f, 96f, 52f), 10f, 10f, paint)
        bmp
    }
}
