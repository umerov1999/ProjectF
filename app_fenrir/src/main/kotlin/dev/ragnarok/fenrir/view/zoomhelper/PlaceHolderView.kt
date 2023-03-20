package dev.ragnarok.fenrir.view.zoomhelper

import android.content.Context
import android.graphics.Canvas
import android.view.View

internal class PlaceHolderView(context: Context?) : View(context) {
    private var view: View? = null

    constructor(view: View) : this(view.context) {
        this.view = view
    }

    override fun onDraw(canvas: Canvas) {
        if (view != null && isEnabled) {
            (view ?: return).draw(canvas)
            return
        }
        super.onDraw(canvas)
    }
}
