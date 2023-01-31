package dev.ragnarok.fenrir.util

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.util.Property
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.squareup.picasso3.RequestCreator
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.MonochromeTransformation
import dev.ragnarok.fenrir.settings.CurrentTheme

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


    fun getOnlineIcon(online: Boolean, onlineMobile: Boolean, platform: Int, app: Int): Int? {
        if (!online) {
            return null
        }
        when (app) {
            6079611 -> {
                return R.drawable.ic_xvii
            }

            4705861 -> {
                return R.drawable.ic_boom
            }

            2685278 -> {
                return R.drawable.ic_kate_mobile
            }

            else -> return when (platform) {
                VKApiUser.Platform.WEB -> R.drawable.web
                VKApiUser.Platform.MOBILE -> R.drawable.cellphone
                VKApiUser.Platform.IPHONE, VKApiUser.Platform.IPAD -> R.drawable.apple
                VKApiUser.Platform.WINDOWS, VKApiUser.Platform.WPHONE -> R.drawable.windows
                VKApiUser.Platform.ANDROID -> R.drawable.android
                else -> if (onlineMobile) {
                    R.drawable.cellphone
                } else {
                    R.drawable.ic_online_web
                }
            }
        }
    }


    @JvmOverloads
    fun setupSwipeRefreshLayoutWithCurrentTheme(
        activity: Activity,
        swipeRefreshLayout: SwipeRefreshLayout?,
        needToolbarOffset: Boolean = false
    ) {
        swipeRefreshLayout?.setProgressBackgroundColorSchemeColor(
            CurrentTheme.getColorSurface(
                activity
            )
        )
        val primaryColor = CurrentTheme.getColorPrimary(activity)
        val accentColor = CurrentTheme.getColorSecondary(activity)
        swipeRefreshLayout?.setColorSchemeColors(primaryColor, accentColor)
        if (needToolbarOffset) {
            swipeRefreshLayout?.setProgressViewOffset(
                false,
                activity.resources.getDimensionPixelSize(R.dimen.refresher_offset_start),
                activity.resources.getDimensionPixelSize(R.dimen.refresher_offset_end)
            )
        }
    }


    @JvmOverloads
    fun displayAvatar(
        dest: ImageView?,
        transformation: Transformation?,
        url: String?,
        tag: String?,
        @DrawableRes ifEmpty: Int = R.drawable.ic_avatar_unknown,
        monochrome: Boolean = false
    ) {
        dest ?: return
        val picasso = with()
        val requestCreator: RequestCreator = if (url.nonNullNoEmpty()) {
            picasso.load(url)
        } else {
            picasso.load(ifEmpty)
        }
        if (transformation != null) {
            requestCreator.transform(transformation)
        }
        if (monochrome) {
            requestCreator.transform(MonochromeTransformation())
        }
        if (tag != null) {
            requestCreator.tag(tag)
        }
        requestCreator.into(dest)
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
