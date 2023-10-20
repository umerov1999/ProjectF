package com.yalantis.ucrop.util

object CubicEasing {
    fun easeOut(time: Float, start: Float, end: Float, duration: Float): Float {
        var timeS = time
        return end * ((timeS / duration - 1.0f.also { timeS = it }) * timeS * timeS + 1.0f) + start
    }

    fun easeIn(time: Float, start: Float, end: Float, duration: Float): Float {
        var timeS = time
        return end * duration.let { timeS /= it; timeS } * timeS * timeS + start
    }

    fun easeInOut(time: Float, start: Float, end: Float, duration: Float): Float {
        var timeS = time
        return if (duration / 2.0f.let { timeS /= it; timeS } < 1.0f) end / 2.0f * timeS * timeS * timeS + start else end / 2.0f * (2.0f.let { timeS -= it; timeS } * timeS * timeS + 2.0f) + start
    }
}