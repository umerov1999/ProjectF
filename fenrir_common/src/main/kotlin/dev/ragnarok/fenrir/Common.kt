package dev.ragnarok.fenrir

import android.content.Context
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import dev.ragnarok.fenrir_common.R

object Common {
    private const val RANDOM_PAGAN_SYMBOL_NUMBER = 22
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
            2 -> PaganSymbolWall(R.drawable.ic_igdr)
            3 -> PaganSymbolWall(R.drawable.valknut)
            4 -> PaganSymbolWall(R.drawable.ic_mjolnir)
            5 -> PaganSymbolWall(R.drawable.ic_vegvisir)
            6 -> PaganSymbolWall(R.drawable.ic_vegvisir2)
            7 -> PaganSymbolWall(R.drawable.ic_celtic_knot)
            8 -> PaganSymbolWall(R.drawable.ic_celtic_flower)
            9 -> PaganSymbolWall(R.drawable.ic_slepnir)
            10 -> PaganSymbolWall(
                R.raw.fenrir, 140f, intArrayOf(
                    0x333333,
                    getColorPrimary(context),
                    0x777777,
                    getColorSecondary(context)
                )
            )
            11 -> PaganSymbolWall(R.drawable.ic_triskel)
            12 -> PaganSymbolWall(R.drawable.ic_hell)
            13 -> PaganSymbolWall(R.drawable.ic_odin)
            14 -> PaganSymbolWall(R.drawable.ic_odin2)
            15 -> PaganSymbolWall(R.drawable.ic_freya)
            16 -> PaganSymbolWall(R.drawable.ic_viking)
            17 -> PaganSymbolWall(R.drawable.ic_raven)
            18 -> PaganSymbolWall(R.drawable.ic_pennywise)
            19 -> PaganSymbolWall(R.drawable.ic_chur)
            20 -> PaganSymbolWall(R.drawable.ic_fire)
            21 -> PaganSymbolWall(
                R.raw.flame, 140f, intArrayOf(
                    0xFF812E,
                    getColorPrimary(context)
                ), true
            )
            else -> PaganSymbolWall(R.drawable.ic_cat)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @RawRes
    fun getPlayerNullArtAnimation(paganSymbol: Int): Int {
        return R.raw.auidio_no_cover
    }

    @DrawableRes
    fun getSnowRes(paganSymbol: Int): Int {
        if (paganSymbol == 16) {
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
        ).fallBack(R.drawable.ic_cat)
    }

    class PaganSymbolWall {
        constructor(@DrawableRes icon: Int) {
            isAnimation = false
            iconRes = icon
            lottieRes = R.raw.fenrir
            this.replacement = null
            widthHeight = 0f
            useMoveColor = false
        }

        constructor(
            @RawRes animation: Int,
            widthHeight: Float,
            replacement: IntArray? = null,
            useMoveColor: Boolean = false
        ) {
            isAnimation = true
            iconRes = R.drawable.ic_cat
            lottieRes = animation
            this.replacement = replacement
            this.widthHeight = widthHeight
            this.useMoveColor = useMoveColor
        }

        fun fallBack(@DrawableRes icon: Int): PaganSymbolWall {
            iconRes = icon
            return this
        }

        val isAnimation: Boolean

        @DrawableRes
        var iconRes: Int
            private set

        @RawRes
        val lottieRes: Int
        val replacement: IntArray?
        val widthHeight: Float
        val useMoveColor: Boolean
    }
}