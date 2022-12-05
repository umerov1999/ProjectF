package dev.ragnarok.fenrir.util.spots

import android.view.animation.Interpolator
import kotlin.math.pow

internal class HesitateInterpolator : Interpolator {
    override fun getInterpolation(input: Float): Float {
        return if (input < 0.5) (input * 2).toDouble().pow(POW)
            .toFloat() * 0.5f else ((1 - input) * 2).toDouble().pow(POW).toFloat() * -0.5f + 1
    }

    companion object {
        private const val POW = 1.0 / 2.0
    }
}