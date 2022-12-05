package dev.ragnarok.fenrir.activity

import android.app.Activity
import android.content.Context
import dev.ragnarok.fenrir.activity.ActivityFeatures.StatusbarColorFeature.Companion.STATUSBAR_COLOR_COLORED
import dev.ragnarok.fenrir.listener.AppStyleable
import dev.ragnarok.fenrir.settings.Settings

class ActivityFeatures(builder: Builder) {
    private val hideMenu: Boolean = builder.blockNavigationFeature?.blockNavigationDrawer == true
    private val statusBarColorOption: Int
    private val statusBarInvertIconsOption: Boolean
    fun apply(activity: Activity) {
        if (activity !is AppStyleable) return
        val styleable = activity as AppStyleable
        styleable.hideMenu(hideMenu)
        styleable.setStatusbarColored(
            statusBarColorOption == STATUSBAR_COLOR_COLORED,
            statusBarInvertIconsOption
        )
    }

    class Builder {
        var blockNavigationFeature: BlockNavigationFeature? = null
        var statusbarColorFeature: StatusbarColorFeature? = null
        fun begin(): BlockNavigationFeature {
            return BlockNavigationFeature(this)
        }

        fun build(): ActivityFeatures {
            return ActivityFeatures(this)
        }
    }

    open class Feature internal constructor(val builder: Builder)
    class StatusbarColorFeature(b: Builder) : Feature(b) {
        var statusBarColorOption = 0
        var statusBarIconInvertedOption = false
        fun setBarsColored(context: Context, colored: Boolean): Builder {
            statusBarColorOption =
                if (colored) STATUSBAR_COLOR_COLORED else STATUSBAR_COLOR_NON_COLORED
            statusBarIconInvertedOption = !Settings.get().ui().isDarkModeEnabled(context)
            return builder
        }

        fun setBarsColored(colored: Boolean, invertIcons: Boolean): Builder {
            statusBarColorOption =
                if (colored) STATUSBAR_COLOR_COLORED else STATUSBAR_COLOR_NON_COLORED
            statusBarIconInvertedOption = invertIcons
            return builder
        }

        companion object {
            const val STATUSBAR_COLOR_COLORED = 1
            const val STATUSBAR_COLOR_NON_COLORED = 2
        }

        init {
            b.statusbarColorFeature = this
        }
    }

    class BlockNavigationFeature(b: Builder) : Feature(b) {
        var blockNavigationDrawer = false
        fun setHideNavigationMenu(blockNavigationDrawer: Boolean): StatusbarColorFeature {
            this.blockNavigationDrawer = blockNavigationDrawer
            return StatusbarColorFeature(builder)
        }

        init {
            b.blockNavigationFeature = this
        }
    }

    init {
        statusBarColorOption =
            builder.statusbarColorFeature?.statusBarColorOption ?: STATUSBAR_COLOR_COLORED
        statusBarInvertIconsOption =
            builder.statusbarColorFeature?.statusBarIconInvertedOption ?: false
    }
}