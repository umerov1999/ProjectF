package dev.ragnarok.fenrir.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.drawerlayout.widget.DrawerLayout

class ErrorIgnoreDrawerLayout : DrawerLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    )

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (ignored: IllegalArgumentException) {
        } catch (ignored: ArrayIndexOutOfBoundsException) {
        }
        return false
    }
}