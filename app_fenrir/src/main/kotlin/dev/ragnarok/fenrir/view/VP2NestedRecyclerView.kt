package dev.ragnarok.fenrir.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

@SuppressLint("CustomViewStyleable", "PrivateResource")
class VP2NestedRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {
    private val touchSlop: Int
    private var initialX = 0f
    private var initialY = 0f
    private var scrolled = false
    private var viewPager2: ViewPager2? = null
    private var orientation = 0
    private var uid: Int = -1

    fun updateUid(uid: Int?) {
        this.uid = uid ?: -1
        if (uid == -1) {
            return
        }
        parcelables[uid]?.let {
            try {
                layoutManager?.onRestoreInstanceState(it)
            } catch (_: Exception) {
            }
        }
    }

    fun saveState() {
        if (uid == -1) {
            return
        }
        parcelables[uid] = layoutManager?.onSaveInstanceState()
    }

    override fun onDetachedFromWindow() {
        if (uid == -1) {
            super.onDetachedFromWindow()
            return
        }
        parcelables[uid] = layoutManager?.onSaveInstanceState()
        super.onDetachedFromWindow()
    }

    internal fun handleChild(ev: MotionEvent): Boolean {
        if (viewPager2 == null) {
            findViewPager2()
            if (viewPager2 == null) {
                return false
            }
        }
        val orientation = viewPager2?.orientation
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            scrolled = false
            viewPager2?.isUserInputEnabled = true
            parent.requestDisallowInterceptTouchEvent(true)
            return false
        } else if (ev.action == MotionEvent.ACTION_DOWN) {
            initialX = ev.x
            initialY = ev.y
            parent.requestDisallowInterceptTouchEvent(true)
            return false
        } else if (ev.action == MotionEvent.ACTION_MOVE) {
            if (!scrolled) {
                val dx = ev.x - initialX
                val dy = ev.y - initialY
                val isVpHorizontal = orientation == ViewPager2.ORIENTATION_HORIZONTAL
                val absDX = abs(dx)
                val absDY = abs(dy)
                // assuming ViewPager2 touch-slop is 2x touch-slop of child
                val scaledDx = absDX * if (isVpHorizontal) .7f else 1f
                val scaledDy = absDY * if (isVpHorizontal) 1f else .7f
                if (scaledDx > touchSlop || scaledDy > touchSlop) {
                    return if (isVpHorizontal == scaledDy > scaledDx) { // Gesture is perpendicular
                        scrolled = true
                        viewPager2?.isUserInputEnabled = false
                        parent.requestDisallowInterceptTouchEvent(false)
                        false
                    } else { // Gesture is parallel
                        val canScroll = canScroll(orientation, if (isVpHorizontal) dx else dy)
                        scrolled = canScroll
                        viewPager2?.isUserInputEnabled = !canScroll
                        parent.requestDisallowInterceptTouchEvent(canScroll)
                        !canScroll
                    }
                }
            }
        }
        return false
    }

    private fun canScroll(orientation: Int?, delta: Float): Boolean {
        orientation ?: return false
        val direction = if (delta > 0) -1 else 1
        when (orientation) {
            0 -> {
                return canScrollHorizontally(direction)
            }
            1 -> {
                return canScrollVertically(direction)
            }
        }
        return false
    }

    private fun findViewPager2() {
        var viewParent = parent
        while (viewParent !is ViewPager2 && viewParent != null) {
            viewParent = viewParent.parent
        }
        viewPager2 = viewParent as ViewPager2?
    }

    private fun handle(ev: MotionEvent) {
        if (viewPager2 == null) {
            findViewPager2()
            if (viewPager2 == null) {
                return
            }
        }
        val orientation = viewPager2?.orientation
        if (this.orientation == HORIZONTAL) {
            if (!canScroll(this.orientation, -1f) && !canScroll(this.orientation, 1f)) {
                return
            }
            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
                scrolled = false
                viewPager2?.isUserInputEnabled = true
                parent.requestDisallowInterceptTouchEvent(false)
            } else if (ev.action == MotionEvent.ACTION_DOWN) {
                initialX = ev.x
                initialY = ev.y
                parent.requestDisallowInterceptTouchEvent(true)
            } else if (ev.action == MotionEvent.ACTION_MOVE) {
                val dx = ev.x - initialX
                val dy = ev.y - initialY
                val isVpHorizontal = orientation == ViewPager2.ORIENTATION_HORIZONTAL
                val absDX = abs(dx)
                val absDY = abs(dy)
                // assuming ViewPager2 touch-slop is 2x touch-slop of child
                val scaledDx: Float = absDX * if (isVpHorizontal) .5f else 1f
                val scaledDy: Float = absDY * if (isVpHorizontal) 1f else .5f
                if (scaledDx > touchSlop || scaledDy > touchSlop) {
                    if (isVpHorizontal == scaledDy > scaledDx) { // Gesture is perpendicular
                        scrolled = true
                        viewPager2?.isUserInputEnabled = false
                        parent.requestDisallowInterceptTouchEvent(false)
                    } else { // Gesture is parallel
                        val canScroll = canScroll(orientation, if (isVpHorizontal) dx else dy)
                        scrolled = canScroll
                        viewPager2?.isUserInputEnabled = !canScroll
                        parent.requestDisallowInterceptTouchEvent(canScroll)
                    }
                }
            }
        } else {
            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
                scrolled = false
                parent.requestDisallowInterceptTouchEvent(false)
            } else if (ev.action == MotionEvent.ACTION_DOWN) {
                initialX = ev.x
                initialY = ev.y
                parent.requestDisallowInterceptTouchEvent(true)
            } else if (ev.action == MotionEvent.ACTION_MOVE) {
                if (!scrolled) {
                    val dx = ev.x - initialX
                    val dy = ev.y - initialY
                    val isVpHorizontal = orientation == ViewPager2.ORIENTATION_HORIZONTAL
                    val absDX = abs(dx)
                    val absDY = abs(dy)
                    // assuming ViewPager2 touch-slop is 2x touch-slop of child
                    val scaledDx = absDX * if (isVpHorizontal) .5f else 1f
                    val scaledDy = absDY * if (isVpHorizontal) 1f else .5f
                    if (scaledDx > touchSlop || scaledDy > touchSlop) {
                        if (isVpHorizontal == scaledDy > scaledDx) { // Gesture is perpendicular
                            scrolled = true
                            parent.requestDisallowInterceptTouchEvent(true)
                        } else { // Gesture is parallel
                            scrolled = false
                            parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        handle(e)
        return super.onTouchEvent(e)
    }

    init {
        setHasFixedSize(false)
        orientation = if (attrs != null) {
            val a =
                context.obtainStyledAttributes(
                    attrs,
                    androidx.viewpager2.R.styleable.RecyclerView,
                    0,
                    0
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveAttributeDataForStyleable(
                    context,
                    androidx.viewpager2.R.styleable.RecyclerView,
                    attrs,
                    a,
                    0,
                    0
                )
            }
            a.getInt(androidx.viewpager2.R.styleable.RecyclerView_android_orientation, 0)
        } else {
            VERTICAL
        }
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        if (orientation == HORIZONTAL) {
            addOnItemTouchListener(object : SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, ev: MotionEvent): Boolean {
                    return handleChild(ev)
                }
            })
        }
    }

    companion object {
        private var parcelables = HashMap<Int, Parcelable?>()
    }
}