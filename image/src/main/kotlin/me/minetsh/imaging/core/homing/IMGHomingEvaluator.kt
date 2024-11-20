package me.minetsh.imaging.core.homing

import android.animation.TypeEvaluator

/**
 * Created by felix on 2017/11/28 下午4:13.
 */
class IMGHomingEvaluator : TypeEvaluator<IMGHoming> {
    private var homing: IMGHoming? = null

    constructor()
    constructor(homing: IMGHoming?) {
        this.homing = homing
    }

    override fun evaluate(fraction: Float, startValue: IMGHoming, endValue: IMGHoming): IMGHoming {
        val x = startValue.x + fraction * (endValue.x - startValue.x)
        val y = startValue.y + fraction * (endValue.y - startValue.y)
        val scale = startValue.scale + fraction * (endValue.scale - startValue.scale)
        val rotate = startValue.rotate + fraction * (endValue.rotate - startValue.rotate)
        homing?.let {
            it.set(x, y, scale, rotate)
            return it
        } ?: run {
            IMGHoming(x, y, scale, rotate).let {
                homing = it
                return it
            }
        }
    }
}
