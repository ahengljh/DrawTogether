package edu.utexas.jinheng.drawtogether.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class PencilView : AppCompatImageView {
    private var selected: Boolean = false
    private var density: Float = 0f

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        density = context.resources.displayMetrics.density
    }

    override fun setSelected(selected: Boolean) {
        this.selected = selected
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!selected) {
            return
        }
        val paint = Paint()
        paint.color = 0xccffffff.toInt()
        val top = height - (12 * density)
        val rad = 3 * density
        canvas.drawCircle(width / 2f, top, rad, paint)
    }
}