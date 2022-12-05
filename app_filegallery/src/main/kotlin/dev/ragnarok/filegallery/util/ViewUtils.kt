package dev.ragnarok.filegallery.util

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.util.Property
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorPrimary
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorSecondary
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorSurface

object ViewUtils {
    private val DEFAULT_COUNT_FORMATTER: ICountFormatter =
        object : ICountFormatter {
            override fun format(count: Int): String {
                return count.toString()
            }
        }

    fun setCountText(view: TextView?, count: Int, animate: Boolean): ObjectAnimator? {
        if (view != null) {
            if (animate) {
                val animator = ObjectAnimator.ofInt(
                    view, createAmountAnimatorProperty(
                        DEFAULT_COUNT_FORMATTER
                    ), count
                )
                animator.duration = 250
                animator.start()
                return animator
            } else {
                view.tag = count
                view.text = DEFAULT_COUNT_FORMATTER.format(count)
            }
        }
        return null
    }

    private fun createAmountAnimatorProperty(formatter: ICountFormatter): Property<TextView, Int> {
        return object : Property<TextView, Int>(Int::class.java, "counter_text") {
            override fun get(view: TextView): Int {
                return try {
                    view.tag as Int
                } catch (e: Exception) {
                    0
                }
            }

            override fun set(view: TextView, value: Int) {
                view.text = formatter.format(value)
                view.tag = value
            }
        }
    }

    @JvmOverloads
    fun setupSwipeRefreshLayoutWithCurrentTheme(
        activity: Activity,
        swipeRefreshLayout: SwipeRefreshLayout?,
        needToolbarOffset: Boolean = false
    ) {
        swipeRefreshLayout?.setProgressBackgroundColorSchemeColor(getColorSurface(activity))
        val primaryColor = getColorPrimary(activity)
        val accentColor = getColorSecondary(activity)
        swipeRefreshLayout?.setColorSchemeColors(primaryColor, accentColor)
        if (needToolbarOffset) {
            swipeRefreshLayout?.setProgressViewOffset(
                false,
                activity.resources.getDimensionPixelSize(R.dimen.refresher_offset_start),
                activity.resources.getDimensionPixelSize(R.dimen.refresher_offset_end)
            )
        }
    }

    fun showProgress(fragment: Fragment, swipeRefreshLayout: SwipeRefreshLayout?, show: Boolean) {
        if (!fragment.isAdded || swipeRefreshLayout == null) return
        if (!show) {
            swipeRefreshLayout.isRefreshing = false
            return
        }
        if (fragment.isResumed) {
            swipeRefreshLayout.isRefreshing = true
        } else {
            swipeRefreshLayout.post(Runnable { swipeRefreshLayout.isRefreshing = true })
        }
    }

    fun keyboardHide(context: Context) {
        try {
            val inputManager =
                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inputManager?.hideSoftInputFromWindow(
                (context as Activity).window.decorView.rootView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        } catch (ignored: Exception) {
        }
    }

    interface ICountFormatter {
        fun format(count: Int): String
    }
}