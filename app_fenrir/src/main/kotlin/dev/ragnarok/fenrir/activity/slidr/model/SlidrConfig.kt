package dev.ragnarok.fenrir.activity.slidr.model

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import dev.ragnarok.fenrir.settings.Settings.get

/**
 * This class contains the configuration information for all the options available in
 * this library
 */
class SlidrConfig internal constructor() {
    private var fromUnColoredToColoredStatusBar = false
    private var sensitivity = 0.5f
    private var scrimColor = -1
    private var scrimStartAlpha = 0.8f
    private var scrimEndAlpha = 0f
    private var velocityThreshold = 5f
    private var distanceThreshold = 0.25f

    /**
     * Has the user configured slidr to only catch at the edge of the screen ?
     *
     * @return true if is edge capture only
     */
    var isEdgeOnly = false
        private set
    private var edgeSize = 0.18f

    /**
     * Has the user configured slidr to ignore all scrollable children inside
     *
     * @return true if is ignore all scrollable children under touch
     */
    var isIgnoreChildScroll = false
        private set
    private var alphaForView = true

    /**
     * Get the position of the slidable mechanism for this configuration. This is the position on
     * the screen that the user can swipe the activity away from
     *
     * @return the slider position
     */
    var position = SlidrPosition.LEFT
        private set

    /**
     * Get the slidr listener set by the user to respond to certain events in the sliding
     * mechanism.
     *
     * @return the slidr listener
     */
    var listener: SlidrListener? = null
        private set

    fun isFromUnColoredToColoredStatusBar(): Boolean {
        return fromUnColoredToColoredStatusBar
    }

    /***********************************************************************************************
     *
     * Setters
     *
     */
    fun setFromUnColoredToColoredStatusBar(en: Boolean) {
        fromUnColoredToColoredStatusBar = en
    }

    /**
     * Get the color of the background scrim
     *
     * @return the scrim color integer
     */
    @ColorInt
    fun getScrimColor(): Int {
        return scrimColor
    }

    fun setScrimColor(@ColorInt scrimColor: Int) {
        this.scrimColor = scrimColor
    }

    /**
     * Get teh start alpha value for when the activity is not swiped at all
     *
     * @return the start alpha value (0.0 to 1.0)
     */
    fun getScrimStartAlpha(): Float {
        return scrimStartAlpha
    }

    fun setScrimStartAlpha(scrimStartAlpha: Float) {
        this.scrimStartAlpha = scrimStartAlpha
    }

    /**
     * Get the end alpha value for when the user almost swipes the activity off the screen
     *
     * @return the end alpha value (0.0 to 1.0)
     */
    fun getScrimEndAlpha(): Float {
        return scrimEndAlpha
    }

    fun setScrimEndAlpha(scrimEndAlpha: Float) {
        this.scrimEndAlpha = scrimEndAlpha
    }

    /**
     * Get the velocity threshold at which the slide action is completed regardless of offset
     * distance of the drag
     *
     * @return the velocity threshold
     */
    fun getVelocityThreshold(): Float {
        return velocityThreshold
    }

    fun setVelocityThreshold(velocityThreshold: Float) {
        this.velocityThreshold = velocityThreshold
    }

    /**
     * Get at what % of the screen is the minimum viable distance the activity has to be dragged
     * in-order to be slinged off the screen
     *
     * @return the distant threshold as a percentage of the screen size (width or height)
     */
    fun getDistanceThreshold(): Float {
        return distanceThreshold
    }

    fun setDistanceThreshold(distanceThreshold: Float) {
        this.distanceThreshold = distanceThreshold
    }

    fun getSensitivity(): Float {
        return sensitivity
    }

    fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity
    }

    fun isAlphaForView(): Boolean {
        return alphaForView
    }

    fun setAlphaForView(alphaForView: Boolean) {
        this.alphaForView = alphaForView
    }

    /**
     * Get the size of the edge field that is catchable
     *
     * @return the size of the edge that is grabable
     * @see .isEdgeOnly
     */
    fun getEdgeSize(size: Float): Float {
        return edgeSize * size
    }

    /**
     * The Builder for this configuration class. This is the only way to create a
     * configuration
     */
    class Builder {
        private val config: SlidrConfig = SlidrConfig()
        fun fromUnColoredToColoredStatusBar(en: Boolean): Builder {
            config.fromUnColoredToColoredStatusBar = en
            return this
        }

        fun position(position: SlidrPosition): Builder {
            config.position = position
            return this
        }

        fun scrimColor(@ColorInt color: Int): Builder {
            config.scrimColor = color
            return this
        }

        fun scrimStartAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float): Builder {
            config.scrimStartAlpha = alpha
            return this
        }

        fun scrimEndAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float): Builder {
            config.scrimEndAlpha = alpha
            return this
        }

        fun edge(flag: Boolean): Builder {
            config.isEdgeOnly = flag
            return this
        }

        fun edgeSize(@FloatRange(from = 0.0, to = 1.0) edgeSize: Float): Builder {
            config.edgeSize = edgeSize
            return this
        }

        fun ignoreChildScroll(ignoreChildScroll: Boolean): Builder {
            config.isIgnoreChildScroll = ignoreChildScroll
            return this
        }

        fun setAlphaForView(alphaForView: Boolean): Builder {
            config.alphaForView = alphaForView
            return this
        }

        fun listener(listener: SlidrListener?): Builder {
            config.listener = listener
            return this
        }

        fun build(): SlidrConfig {
            val settings = get().other().slidrSettings
            when (config.position) {
                SlidrPosition.LEFT, SlidrPosition.RIGHT, SlidrPosition.HORIZONTAL -> {
                    config.sensitivity = settings.horizontal_sensitive
                    config.velocityThreshold = settings.horizontal_velocity_threshold
                    config.distanceThreshold = settings.horizontal_distance_threshold
                }
                SlidrPosition.TOP, SlidrPosition.BOTTOM, SlidrPosition.VERTICAL -> {
                    config.sensitivity = settings.vertical_sensitive
                    config.velocityThreshold = settings.vertical_velocity_threshold
                    config.distanceThreshold = settings.vertical_distance_threshold
                }
            }
            return config
        }

    }
}