package dev.ragnarok.filegallery.activity.slidr.util

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AbsListView
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.view.ScrollingView
import androidx.viewpager2.widget.ViewPager2
import dev.ragnarok.filegallery.activity.slidr.model.SlidrPosition
import dev.ragnarok.filegallery.view.TouchImageView
import java.util.*

object ViewHelper {
    fun hasScrollableChildUnderPoint(
        mView: View,
        direction: SlidrPosition,
        x: Int,
        y: Int
    ): Boolean {
        val scrollableView = findScrollableViewContains(mView, direction, x, y)
        return scrollableView != null
    }

    private fun findScrollableViewContains(
        mView: View,
        direction: SlidrPosition,
        x: Int,
        y: Int
    ): View? {
        if (isScrollableView(mView) && (canScroll(
                mView,
                direction
            ) || mView is TouchImageView && mView.isZoomed)
        ) {
            return mView
        }
        if (mView !is ViewGroup) return null
        val relativeX = x - mView.left + mView.scrollX
        val relativeY = y - mView.top + mView.scrollY
        for (i in 0 until mView.childCount) {
            val childView = mView.getChildAt(i)
            if (childView.visibility != View.VISIBLE || !isViewUnder(
                    childView,
                    relativeX,
                    relativeY
                )
            ) continue
            val scrollableView =
                findScrollableViewContains(childView, direction, relativeX, relativeY)
            if (scrollableView != null) {
                return scrollableView
            }
        }
        return null
    }

    private fun canScroll(mView: View, direction: SlidrPosition): Boolean {
        return when (direction) {
            SlidrPosition.LEFT -> mView.canScrollHorizontally(-1)
            SlidrPosition.RIGHT -> mView.canScrollHorizontally(1)
            SlidrPosition.TOP -> mView.canScrollVertically(-1)
            SlidrPosition.BOTTOM -> mView.canScrollVertically(1)
            SlidrPosition.VERTICAL -> mView.canScrollVertically(-1) || mView.canScrollVertically(1)
            SlidrPosition.HORIZONTAL -> mView.canScrollHorizontally(-1) || mView.canScrollHorizontally(
                1
            )
        }
    }

    private fun isScrollableView(mView: View): Boolean {
        return (mView is ScrollView
                || mView is HorizontalScrollView
                || mView is AbsListView
                || mView is ScrollingView
                || mView is TouchImageView
                || mView is ViewPager2
                || mView is WebView)
    }

    private fun isViewUnder(view: View?, x: Int, y: Int): Boolean {
        return if (view == null) {
            false
        } else x >= view.left && x < view.right && y >= view.top && y < view.bottom
    }

    private fun findScrollableInIterativeWay(
        parent: View,
        direction: SlidrPosition,
        x: Int,
        y: Int
    ): View? {
        val viewStack = Stack<ViewInfo>()
        var viewInfo: ViewInfo? = ViewInfo(parent, x, y)
        while (viewInfo != null) {
            val mView = viewInfo.view
            if (isScrollableView(mView) && (canScroll(
                    mView,
                    direction
                ) || mView is TouchImageView && mView.isZoomed)
            ) {
                return mView
            }
            if (mView is ViewGroup) {
                val relativeX = viewInfo.x - mView.left + mView.scrollX
                val relativeY = viewInfo.y - mView.top + mView.scrollY
                for (i in mView.childCount - 1 downTo 0) {
                    val childView = mView.getChildAt(i)
                    if (childView.visibility != View.VISIBLE || !isViewUnder(
                            childView,
                            relativeX,
                            relativeY
                        )
                    ) continue
                    viewStack.push(ViewInfo(childView, relativeX, relativeY))
                }
            }
            viewInfo = if (viewStack.isEmpty()) null else viewStack.pop()
        }
        return null
    }

    internal class ViewInfo(val view: View, val x: Int, val y: Int)
}