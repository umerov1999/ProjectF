package dev.ragnarok.fenrir

import android.content.Context
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import dev.ragnarok.fenrir_common.R

object Common {
    private const val RANDOM_PAGAN_SYMBOL_NUMBER = 25
    private var randomPaganSymbol = -1

    fun randomizePaganSymbol() {
        randomPaganSymbol = randomRange(1, RANDOM_PAGAN_SYMBOL_NUMBER - 1)
    }

    private fun randomRange(min: Int, max: Int): Int {
        var pMax = max
        pMax -= min
        return (Math.random() * ++pMax).toInt() + min
    }

    private fun getColorFromAttrs(resId: Int, context: Context, defaultColor: String): Int {
        val attribute = intArrayOf(resId)
        val array = context.theme.obtainStyledAttributes(attribute)
        val color = array.getColor(0, Color.parseColor(defaultColor))
        array.recycle()
        return color
    }

    private fun getColorSecondary(context: Context): Int {
        return getColorFromAttrs(
            com.google.android.material.R.attr.colorSecondary,
            context,
            "#000000"
        )
    }

    private fun getColorPrimary(context: Context): Int {
        return getColorFromAttrs(androidx.appcompat.R.attr.colorPrimary, context, "#000000")
    }

    fun requirePaganSymbol(paganSymbol: Int, context: Context): PaganSymbolWall {
        var number = paganSymbol
        if (number >= RANDOM_PAGAN_SYMBOL_NUMBER) {
            if (randomPaganSymbol <= 0) {
                randomizePaganSymbol()
            }
            number = randomPaganSymbol
        }
        return when (number) {
            2 -> PaganSymbolWall(R.raw.svg_pagan_igdr, 120f, 120f)
            3 -> PaganSymbolWall(R.raw.svg_pagan_valknut, 160f, 160f)
            4 -> PaganSymbolWall(R.raw.svg_pagan_mjolnir, 108f, 108f)
            5 -> PaganSymbolWall(R.raw.svg_pagan_vegvisir, 140f, 140f)
            6 -> PaganSymbolWall(R.raw.svg_pagan_vegvisir2, 160f, 160f)
            7 -> PaganSymbolWall(R.raw.svg_pagan_celtic_knot, 108f, 108f)
            8 -> PaganSymbolWall(R.raw.svg_pagan_celtic_flower, 180f, 180f)
            9 -> PaganSymbolWall(R.raw.svg_pagan_slepnir, 108f, 108f)
            10 -> PaganSymbolWall(
                R.raw.fenrir, 140f, intArrayOf(
                    0x333333,
                    getColorPrimary(context),
                    0x777777,
                    getColorSecondary(context)
                )
            )

            11 -> PaganSymbolWall(R.raw.svg_pagan_triskel, 108f, 108f)
            12 -> PaganSymbolWall(R.raw.svg_pagan_hell, 140f, 140f)
            13 -> PaganSymbolWall(R.raw.svg_pagan_odin, 178f, 120f)
            14 -> PaganSymbolWall(R.raw.svg_pagan_odin2, 160f, 160f)
            15 -> PaganSymbolWall(R.raw.svg_pagan_freya, 160f, 160f)
            16 -> PaganSymbolWall(R.raw.svg_pagan_viking, 140f, 140f)
            17 -> PaganSymbolWall(R.raw.svg_pagan_raven, 160f, 160f)
            18 -> PaganSymbolWall(R.raw.svg_pagan_pennywise, 140f, 140f)
            19 -> PaganSymbolWall(R.raw.svg_pagan_chur, 150f, 150f)
            20 -> PaganSymbolWall(R.raw.svg_pagan_fire, 180f, 180f)
            21 -> PaganSymbolWall(
                R.raw.flame, 140f, intArrayOf(
                    0xFF812E,
                    getColorPrimary(context)
                ), true
            )

            22 -> PaganSymbolWall(R.raw.svg_pagan_valkyrie_1, 170f, 170f)
            23 -> PaganSymbolWall(R.raw.svg_pagan_valkyrie_2, 180f, 180f)
            24 -> PaganSymbolWall(R.raw.svg_pagan_fenrir, 160f, 160f)
            else -> PaganSymbolWall(R.raw.svg_pagan_cat, 160f, 160f)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @RawRes
    fun getPlayerNullArtAnimation(paganSymbol: Int): Int {
        return R.raw.auidio_no_cover
    }

    @DrawableRes
    fun getSnowRes(paganSymbol: Int): Int {
        if (paganSymbol == 15) {
            return R.drawable.ic_maple_leaf
        }
        return R.drawable.ic_snowflake
    }

    @Suppress("UNUSED_PARAMETER")
    @StringRes
    fun getAboutUsHeader(paganSymbol: Int): Int {
        return R.string.first_pagan
    }

    @Suppress("UNUSED_PARAMETER")
    fun getAboutUsAnimation(paganSymbol: Int, context: Context): PaganSymbolWall {
        return PaganSymbolWall(
            R.raw.fenrir,
            140f,
            intArrayOf(
                0x333333,
                getColorPrimary(context),
                0x777777,
                getColorSecondary(context)
            )
        ).fallBack(R.raw.svg_pagan_cat, 160f, 160f)
    }

    class PaganSymbolWall {
        constructor(@RawRes icon: Int, width: Float, height: Float) {
            isAnimation = false
            iconRes = icon
            lottieRes = R.raw.fenrir
            this.lottie_replacement = null
            lottie_widthHeight = 140f
            icon_width = width
            icon_height = height
            lottie_useMoveColor = false
        }

        constructor(
            @RawRes animation: Int,
            widthHeight: Float,
            replacement: IntArray? = null,
            useMoveColor: Boolean = false
        ) {
            isAnimation = true
            iconRes = R.raw.svg_pagan_cat
            lottieRes = animation
            this.lottie_replacement = replacement
            this.lottie_widthHeight = widthHeight
            this.lottie_useMoveColor = useMoveColor
            icon_width = 160f
            icon_height = 160f
        }

        fun fallBack(@RawRes icon: Int, width: Float, height: Float): PaganSymbolWall {
            iconRes = icon
            icon_width = width
            icon_height = height
            return this
        }

        val isAnimation: Boolean

        @RawRes
        var iconRes: Int
            private set

        @RawRes
        val lottieRes: Int
        val lottie_replacement: IntArray?
        val lottie_widthHeight: Float
        val lottie_useMoveColor: Boolean

        var icon_width: Float
        var icon_height: Float
    }
}
