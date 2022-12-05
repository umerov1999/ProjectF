package dev.ragnarok.fenrir.util.spots

import android.content.Context
import android.view.View
import androidx.annotation.Keep

internal class AnimatedView(context: Context?) : View(context) {
    var target = 0

    @set:Keep
    var xFactor: Float
        get() = x / target
        set(xFactor) {
            x = target * xFactor
        }
}