package dev.ragnarok.fenrir.settings

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.squareup.picasso3.Transformation
import de.maxr1998.modernpreferences.PreferenceScreen.Companion.getPreferences
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.picasso.transforms.EllipseTransformation
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.picasso.transforms.stroke.EllipseStrokeTransformation
import dev.ragnarok.fenrir.picasso.transforms.stroke.RoundStrokeTransformation
import dev.ragnarok.fenrir.util.Utils.setColorFilter
import dev.ragnarok.fenrir.view.media.PathAnimator
import java.io.File

object CurrentTheme {
    private const val KEY_CHAT_BACKGROUND = "chat_background"
    val playPauseAnimator = createPathAnimator()
    private fun createPathAnimator(): PathAnimator {
        val animator = PathAnimator(0.293f, -26f, -28f, 1.0f)
        animator.addSvgKeyFrame(
            "M 34.141 16.042 C 37.384 17.921 40.886 20.001 44.211 21.965 C 46.139 23.104 49.285 24.729 49.586 25.917 C 50.289 28.687 48.484 30 46.274 30 L 6 30.021 C 3.79 30.021 2.075 30.023 2 26.021 L 2.009 3.417 C 2.009 0.417 5.326 -0.58 7.068 0.417 C 10.545 2.406 25.024 10.761 34.141 16.042 Z",
            166f
        )
        animator.addSvgKeyFrame(
            "M 37.843 17.769 C 41.143 19.508 44.131 21.164 47.429 23.117 C 48.542 23.775 49.623 24.561 49.761 25.993 C 50.074 28.708 48.557 30 46.347 30 L 6 30.012 C 3.79 30.012 2 28.222 2 26.012 L 2.009 4.609 C 2.009 1.626 5.276 0.664 7.074 1.541 C 10.608 3.309 28.488 12.842 37.843 17.769 Z",
            200f
        )
        animator.addSvgKeyFrame(
            "M 40.644 18.756 C 43.986 20.389 49.867 23.108 49.884 25.534 C 49.897 27.154 49.88 24.441 49.894 26.059 C 49.911 28.733 48.6 30 46.39 30 L 6 30.013 C 3.79 30.013 2 28.223 2 26.013 L 2.008 5.52 C 2.008 2.55 5.237 1.614 7.079 2.401 C 10.656 4 31.106 14.097 40.644 18.756 Z",
            217f
        )
        animator.addSvgKeyFrame(
            "M 43.782 19.218 C 47.117 20.675 50.075 21.538 50.041 24.796 C 50.022 26.606 50.038 24.309 50.039 26.104 C 50.038 28.736 48.663 30 46.453 30 L 6 29.986 C 3.79 29.986 2 28.196 2 25.986 L 2.008 6.491 C 2.008 3.535 5.196 2.627 7.085 3.316 C 10.708 4.731 33.992 14.944 43.782 19.218 Z",
            234f
        )
        animator.addSvgKeyFrame(
            "M 47.421 16.941 C 50.544 18.191 50.783 19.91 50.769 22.706 C 50.761 24.484 50.76 23.953 50.79 26.073 C 50.814 27.835 49.334 30 47.124 30 L 5 30.01 C 2.79 30.01 1 28.22 1 26.01 L 1.001 10.823 C 1.001 8.218 3.532 6.895 5.572 7.26 C 7.493 8.01 47.421 16.941 47.421 16.941 Z",
            267f
        )
        animator.addSvgKeyFrame(
            "M 47.641 17.125 C 50.641 18.207 51.09 19.935 51.078 22.653 C 51.07 24.191 51.062 21.23 51.088 23.063 C 51.109 24.886 49.587 27 47.377 27 L 5 27.009 C 2.79 27.009 1 25.219 1 23.009 L 0.983 11.459 C 0.983 8.908 3.414 7.522 5.476 7.838 C 7.138 8.486 47.641 17.125 47.641 17.125 Z",
            300f
        )
        animator.addSvgKeyFrame(
            "M 48 7 C 50.21 7 52 8.79 52 11 C 52 19 52 19 52 19 C 52 21.21 50.21 23 48 23 L 4 23 C 1.79 23 0 21.21 0 19 L 0 11 C 0 8.79 1.79 7 4 7 C 48 7 48 7 48 7 Z",
            383f
        )
        return animator
    }

    private fun getDrawerBackgroundFile(context: Context, light: Boolean): File {
        return File(context.filesDir, if (light) "chat_light.jpg" else "chat_dark.jpg")
    }

    private fun getStatic(activity: Activity): Drawable {
        if (Settings.get().other().isCustom_chat_color) {
            return GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Settings.get().other().colorChat, Settings.get().other().secondColorChat)
            )
        }
        val color = getColorFromAttrs(R.attr.messages_background_color, activity, Color.WHITE)
        return ColorDrawable(color)
    }

    fun getChatBackground(activity: Activity): Drawable? {
        val dark = Settings.get().ui().isDarkModeEnabled(activity)
        val preferences = getPreferences(activity)
        val ret: Drawable? = when (preferences.getString(KEY_CHAT_BACKGROUND, "0")) {
            "1" -> getDrawableFromAttribute(activity, R.attr.chat_background_cookies)
            "2" -> getDrawableFromAttribute(activity, R.attr.chat_background_lines)
            "3" -> getDrawableFromAttribute(activity, R.attr.chat_background_runes)
            "4" -> {
                val bitmap = getDrawerBackgroundFile(activity, !dark)
                if (bitmap.exists()) {
                    val d = Drawable.createFromPath(bitmap.absolutePath)
                        ?: return getStatic(activity)
                    if (Settings.get()
                            .other().isCustom_chat_color
                    ) setColorFilter(
                        d,
                        Settings.get().other().colorChat
                    )
                    return d
                }
                return getStatic(activity)
            }

            else -> return getStatic(activity)
        }
        if (Settings.get().other().isCustom_chat_color) {
            val r1 = ret?.mutate()
            setColorFilter(r1, Settings.get().other().colorChat)
            return r1
        }
        return ret
    }

    fun createTransformationForAvatar(): Transformation {
        val style = Settings.get()
            .ui()
            .avatarStyle
        return when (style) {
            AvatarStyle.OVAL -> EllipseTransformation()
            AvatarStyle.CIRCLE -> RoundTransformation()
            else -> RoundTransformation()
        }
    }

    fun createTransformationStrokeForAvatar(): Transformation {
        val style = Settings.get()
            .ui()
            .avatarStyle
        return when (style) {
            AvatarStyle.OVAL -> EllipseStrokeTransformation(Color.parseColor("#447bba"))
            AvatarStyle.CIRCLE -> RoundStrokeTransformation(Color.parseColor("#447bba"))
            else -> RoundStrokeTransformation(Color.parseColor("#447bba"))
        }
    }

    @DrawableRes
    fun createFavoritesAvatar(): Int {
        val style = Settings.get()
            .ui()
            .avatarStyle
        return when (style) {
            AvatarStyle.OVAL -> R.drawable.ic_favorites_ellipse
            AvatarStyle.CIRCLE -> R.drawable.ic_favorites_round
            else -> R.drawable.ic_favorites_round
        }
    }

    fun getColorPrimary(context: Context?): Int {
        return context?.let {
            getColorFromAttrs(
                androidx.appcompat.R.attr.colorPrimary,
                it,
                "#000000"
            )
        } ?: Color.TRANSPARENT
    }

    fun getColorControlNormal(context: Context?): Int {
        return context?.let {
            getColorFromAttrs(
                androidx.appcompat.R.attr.colorControlNormal,
                it, "#000000"
            )
        } ?: Color.TRANSPARENT
    }

    fun getColorToast(context: Context): Int {
        return getColorFromAttrs(R.attr.toast_background, context, "#FFFFFF")
    }

    fun getColorWhiteContrastFix(context: Context): Int {
        return getColorFromAttrs(
            dev.ragnarok.fenrir_common.R.attr.white_color_contrast_fix,
            context,
            "#FFFFFF"
        )
    }

    fun getColorBlackContrastFix(context: Context): Int {
        return getColorFromAttrs(
            dev.ragnarok.fenrir_common.R.attr.black_color_contrast_fix,
            context,
            "#000000"
        )
    }

    private fun getColorHex(@ColorInt color: Int): String {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        if (a == 255) {
            return String.format("#%02x%02x%02x", r, g, b).uppercase()
        }
        return String.format("#%02x%02x%02x%02x", a, r, g, b).uppercase()
    }
    /*
        fun dumpDynamicColors(context: Context) {
            if (!Constants.IS_DEBUG || "dynamic" != Settings.get().ui().mainThemeKey) {
                return
            }
            Log.d(
                "colorPrimary",
                getColorHex(
                    getColorFromAttrs(
                        androidx.appcompat.R.attr.colorPrimary,
                        context,
                        "#000000"
                    )
                )
            )
            Log.d(
                "colorControlNormal",
                getColorHex(
                    getColorFromAttrs(
                        androidx.appcompat.R.attr.colorControlNormal,
                        context,
                        "#000000"
                    )
                )
            )
            Log.d(
                "colorSecondary",
                getColorHex(
                    getColorFromAttrs(
                        com.google.android.material.R.attr.colorSecondary,
                        context,
                        "#000000"
                    )
                )
            )
            Log.d(
                "colorOnPrimary",
                getColorHex(
                    getColorFromAttrs(
                        com.google.android.material.R.attr.colorOnPrimary,
                        context,
                        "#000000"
                    )
                )
            )
            Log.d(
                "colorOnPrimaryContainer",
                getColorHex(
                    getColorFromAttrs(
                        com.google.android.material.R.attr.colorOnPrimaryContainer,
                        context,
                        "#000000"
                    )
                )
            )
            Log.d(
                "colorPrimaryContainer",
                getColorHex(
                    getColorFromAttrs(
                        com.google.android.material.R.attr.colorPrimaryContainer,
                        context,
                        "#000000"
                    )
                )
            )
            Log.d(
                "colorPrimaryInverse",
                getColorHex(
                    getColorFromAttrs(
                        com.google.android.material.R.attr.colorPrimaryInverse,
                        context,
                        "#000000"
                    )
                )
            )
            Log.d(
                "colorOnSecondary",
                getColorHex(
                    getColorFromAttrs(
                        com.google.android.material.R.attr.colorOnSecondary,
                        context,
                        "#000000"
                    )
                )
            )
            Log.d(
                "colorOnSecondaryContainer",
                getColorHex(
                    getColorFromAttrs(
                        com.google.android.material.R.attr.colorOnSecondaryContainer,
                        context,
                        "#000000"
                    )
                )
            )
            Log.d(
                "colorSecondaryContainer",
                getColorHex(
                    getColorFromAttrs(
                        com.google.android.material.R.attr.colorSecondaryContainer,
                        context,
                        "#000000"
                    )
                )
            )
        }
     */

    fun getColorOnPrimary(context: Context): Int {
        return getColorFromAttrs(
            com.google.android.material.R.attr.colorOnPrimary,
            context,
            "#000000"
        )
    }

    fun getColorSurface(context: Context): Int {
        return getColorFromAttrs(
            com.google.android.material.R.attr.colorSurface,
            context,
            "#000000"
        )
    }

    fun getColorOnSurface(context: Context): Int {
        return getColorFromAttrs(
            com.google.android.material.R.attr.colorOnSurface,
            context,
            "#000000"
        )
    }

    fun getColorBackground(context: Context): Int {
        return getColorFromAttrs(android.R.attr.colorBackground, context, "#000000")
    }

    fun getColorOnBackground(context: Context): Int {
        return getColorFromAttrs(
            com.google.android.material.R.attr.colorOnBackground,
            context,
            "#000000"
        )
    }

    fun getStatusBarColor(context: Context): Int {
        return getColorFromAttrs(android.R.attr.statusBarColor, context, "#000000")
    }

    fun getNavigationBarColor(context: Context): Int {
        return getColorFromAttrs(android.R.attr.navigationBarColor, context, "#000000")
    }

    fun getColorSecondary(context: Context?): Int {
        return context?.let {
            getColorFromAttrs(
                com.google.android.material.R.attr.colorSecondary,
                it,
                "#000000"
            )
        } ?: Color.TRANSPARENT
    }

    fun getColorActive(context: Context?): Int {
        return context?.let {
            getColorFromAttrs(
                R.attr.icon_color_active,
                it,
                "#000000"
            )
        } ?: Color.TRANSPARENT
    }

    fun getColorInActive(context: Context?): Int {
        return context?.let {
            getColorFromAttrs(
                R.attr.icon_color_inactive,
                it,
                "#000000"
            )
        } ?: Color.TRANSPARENT
    }

    fun getStatusBarNonColored(context: Context): Int {
        return getColorFromAttrs(R.attr.statusbarNonColoredColor, context, "#000000")
    }

    fun getMessageUnreadColor(context: Context): Int {
        return getColorFromAttrs(R.attr.message_unread_color, context, "#ffffff")
    }

    fun getMessageBackgroundSquare(context: Context): Int {
        return getColorFromAttrs(R.attr.message_bubble_color, context, "#000000")
    }

    fun getPrimaryTextColorCode(context: Context): Int {
        return getColorFromAttrs(android.R.attr.textColorPrimary, context, "#000000")
    }

    fun getSecondaryTextColorCode(context: Context): Int {
        return getColorFromAttrs(android.R.attr.textColorSecondary, context, "#000000")
    }

    fun getDialogsUnreadColor(context: Context): Int {
        return getColorFromAttrs(R.attr.dialogs_unread_color, context, "#20b0b0b0")
    }

    fun getMy_messages_bubble_color(context: Context): Int {
        return getColorFromAttrs(R.attr.my_messages_bubble_color, context, "#20b0b0b0")
    }

    fun getColorFromAttrs(resId: Int, context: Context, defaultColor: String): Int {
        val attribute = intArrayOf(resId)
        val array = context.theme.obtainStyledAttributes(attribute)
        val color = array.getColor(0, Color.parseColor(defaultColor))
        array.recycle()
        return color
    }

    private fun getColorFromAttrs(resId: Int, context: Context, defaultColor: Int): Int {
        val attribute = intArrayOf(resId)
        val array = context.theme.obtainStyledAttributes(attribute)
        val color = array.getColor(0, defaultColor)
        array.recycle()
        return color
    }

    private fun getDrawableFromAttribute(context: Context, attr: Int): Drawable? {
        val attribute = intArrayOf(attr)
        val array = context.theme.obtainStyledAttributes(attribute)
        val ret = array.getDrawable(0)
        array.recycle()
        return ret
    }
}