package dev.ragnarok.filegallery.activity

import android.app.Activity
import android.content.Context
import dev.ragnarok.filegallery.activity.ActivityFeatures.StatusbarColorFeature.Companion.STATUSBAR_COLOR_COLORED
import dev.ragnarok.filegallery.listener.AppStyleable
import dev.ragnarok.filegallery.settings.Settings.get

class ActivityFeatures(builder: Builder) {
    private val statusBarColorOption: Int
    private val statusBarInvertIconsOption: Boolean
    fun apply(activity: Activity) {
        if (activity !is AppStyleable) return
        val styleable = activity as AppStyleable
        styleable.setStatusbarColored(
            statusBarColorOption == STATUSBAR_COLOR_COLORED,
            statusBarInvertIconsOption
        )
    }

    class Builder {
        var statusbarColorFeature: StatusbarColorFeature? = null
        fun begin(): StatusbarColorFeature {
            return StatusbarColorFeature(this)
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
            statusBarIconInvertedOption = !get().main().isDarkModeEnabled(
                context
            )
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

    init {
        statusBarColorOption =
            builder.statusbarColorFeature?.statusBarColorOption ?: STATUSBAR_COLOR_COLORED
        statusBarInvertIconsOption =
            builder.statusbarColorFeature?.statusBarIconInvertedOption ?: false
    }
}