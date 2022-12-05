package dev.ragnarok.filegallery.listener

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

abstract class EndlessRecyclerOnScrollListener : RecyclerView.OnScrollListener {
    private val MIN_DELAY: Int
    private val VISIBILITY_THRESHOLD //elements to the end
            : Int
    private var mLastInterceptTime: Long? = null
    private var visibleItemCount = 0
    private var totalItemCount = 0
    private var pastVisibleItems = 0

    constructor() {
        MIN_DELAY = 200
        VISIBILITY_THRESHOLD = 0
    }

    constructor(visibilityThreshold: Int, minDelay: Int) {
        MIN_DELAY = minDelay
        VISIBILITY_THRESHOLD = visibilityThreshold
    }

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
        var isLastElementVisible = false
        var isFirstElementVisible = false
        when (manager) {
            is StaggeredGridLayoutManager -> {
                isLastElementVisible = isAtLastElementOfStaggedGridLayoutManager(manager)
                var firstVisibleItems = IntArray(manager.spanCount)
                firstVisibleItems =
                    manager.findFirstVisibleItemPositions(firstVisibleItems)
                if (firstVisibleItems.isNotEmpty()) {
                    for (i in firstVisibleItems) {
                        if (i == 0) {
                            isFirstElementVisible = true
                            break
                        }
                    }
                }
            }
            is LinearLayoutManager -> {
                isLastElementVisible = isAtLastElementOfLinearLayoutManager(manager)
                isFirstElementVisible = manager.findFirstVisibleItemPosition() == 0
            }
            is GridLayoutManager -> {
                isLastElementVisible = isAtLastElementOfGridLayoutManager(manager)
                isFirstElementVisible = manager.findFirstVisibleItemPosition() == 0
            }
        }
        if (isLastElementVisible) {
            mLastInterceptTime = System.currentTimeMillis()
            onScrollToLastElement()
            return
        } else if (isFirstElementVisible) {
            mLastInterceptTime = System.currentTimeMillis()
            onScrollToFirstElement()
        }
    }

    private fun isAtLastElementOfLinearLayoutManager(linearLayoutManager: LinearLayoutManager): Boolean {
        visibleItemCount = linearLayoutManager.childCount
        totalItemCount = linearLayoutManager.itemCount
        pastVisibleItems = linearLayoutManager.findFirstVisibleItemPosition()
        return visibleItemCount + pastVisibleItems >= totalItemCount - VISIBILITY_THRESHOLD
    }

    private fun isAtLastElementOfGridLayoutManager(gridLayoutManager: GridLayoutManager): Boolean {
        visibleItemCount = gridLayoutManager.childCount
        totalItemCount = gridLayoutManager.itemCount
        pastVisibleItems = gridLayoutManager.findFirstVisibleItemPosition()
        return visibleItemCount + pastVisibleItems >= totalItemCount - VISIBILITY_THRESHOLD
    }

    private fun isAtLastElementOfStaggedGridLayoutManager(staggeredGridLayoutManager: StaggeredGridLayoutManager): Boolean {
        visibleItemCount = staggeredGridLayoutManager.childCount
        totalItemCount = staggeredGridLayoutManager.itemCount
        var firstVisibleItems: IntArray? = IntArray(staggeredGridLayoutManager.spanCount)
        firstVisibleItems =
            staggeredGridLayoutManager.findFirstVisibleItemPositions(firstVisibleItems)
        if (firstVisibleItems?.isNotEmpty() == true) {
            pastVisibleItems = firstVisibleItems[0]
        }
        return visibleItemCount + pastVisibleItems >= totalItemCount - VISIBILITY_THRESHOLD
    }

    abstract fun onScrollToLastElement()
    open fun onScrollToFirstElement() {}
}
