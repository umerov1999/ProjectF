package dev.ragnarok.fenrir.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.util.Utils

open class UpEditFab : FloatingActionButton {
    var isEdit = true
        private set

    constructor(context: Context) : super(context) {
        updateIcon()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    ) {
        updateIcon()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        updateIcon()
    }

    internal fun updateIcon() {
        setImageResource(if (isEdit) R.drawable.pencil else R.drawable.ic_outline_keyboard_arrow_up)
    }

    fun getRecyclerObserver(itemPosTrigger: Int): RecyclerView.OnScrollListener {
        return object : UpRecyclerOnScrollListener(0, 200, itemPosTrigger) {
            override fun onScrollElement(status: Boolean) {
                if (isEdit != status) {
                    isEdit = status
                    updateIcon()
                }
            }

        }
    }

    abstract inner class UpRecyclerOnScrollListener(
        visibilityThreshold: Int,
        minDelay: Int,
        private val triggerPosition: Int
    ) : RecyclerView.OnScrollListener() {
        private val MIN_DELAY: Int = minDelay
        private val VISIBILITY_THRESHOLD //elements to the end
                : Int = visibilityThreshold
        private var mLastInterceptTime: Long? = null
        private var visibleItemCount = 0
        private var pastVisibleItems = 0
        private var scrollMinOffset = Utils.dp(2f)

        private fun isAllowScrollIntercept(minDelay: Long): Boolean {
            return mLastInterceptTime == null || System.currentTimeMillis() - (mLastInterceptTime
                ?: 0) > minDelay
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val manager = recyclerView.layoutManager
            manager ?: return
            if (!isAllowScrollIntercept(MIN_DELAY.toLong())) {
                return
            }
            if (dy > scrollMinOffset && isShown) {
                hide()
            } else if (dy < -scrollMinOffset && !isShown) {
                show()
            }

            var isLastElementVisible = false
            when (manager) {
                is StaggeredGridLayoutManager -> {
                    isLastElementVisible = isAtLastElementOfStaggedGridLayoutManager(manager)
                }

                is LinearLayoutManager -> {
                    isLastElementVisible = isAtLastElementOfLinearLayoutManager(manager)
                }

                is GridLayoutManager -> {
                    isLastElementVisible = isAtLastElementOfGridLayoutManager(manager)
                }
            }
            mLastInterceptTime = System.currentTimeMillis()
            onScrollElement(!isLastElementVisible)
        }

        private fun isAtLastElementOfLinearLayoutManager(linearLayoutManager: LinearLayoutManager): Boolean {
            visibleItemCount = linearLayoutManager.childCount
            pastVisibleItems = linearLayoutManager.findFirstVisibleItemPosition()
            return visibleItemCount + pastVisibleItems >= triggerPosition - VISIBILITY_THRESHOLD
        }

        private fun isAtLastElementOfGridLayoutManager(gridLayoutManager: GridLayoutManager): Boolean {
            visibleItemCount = gridLayoutManager.childCount
            pastVisibleItems = gridLayoutManager.findFirstVisibleItemPosition()
            return visibleItemCount + pastVisibleItems >= triggerPosition - VISIBILITY_THRESHOLD
        }

        private fun isAtLastElementOfStaggedGridLayoutManager(staggeredGridLayoutManager: StaggeredGridLayoutManager): Boolean {
            visibleItemCount = staggeredGridLayoutManager.childCount
            var firstVisibleItems: IntArray? = IntArray(staggeredGridLayoutManager.spanCount)
            firstVisibleItems =
                staggeredGridLayoutManager.findFirstVisibleItemPositions(firstVisibleItems)
            if (firstVisibleItems?.isNotEmpty() == true) {
                pastVisibleItems = firstVisibleItems[0]
            }
            return visibleItemCount + pastVisibleItems >= triggerPosition - VISIBILITY_THRESHOLD
        }

        abstract fun onScrollElement(status: Boolean)
    }
}
