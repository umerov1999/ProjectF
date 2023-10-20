package me.minetsh.imaging.core.anim

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import me.minetsh.imaging.core.homing.IMGHoming
import me.minetsh.imaging.core.homing.IMGHomingEvaluator

/**
 * Created by felix on 2017/11/28 下午12:54.
 */
class IMGHomingAnimator : ValueAnimator() {
    var isRotate = false
        private set
    private var mEvaluator: IMGHomingEvaluator? = null

    init {
        interpolator = AccelerateDecelerateInterpolator()
    }

    override fun setObjectValues(vararg values: Any) {
        super.setObjectValues(*values)
        if (mEvaluator == null) {
            mEvaluator = IMGHomingEvaluator()
        }
        setEvaluator(mEvaluator)
    }

    fun setHomingValues(sHoming: IMGHoming, eHoming: IMGHoming) {
        setObjectValues(sHoming, eHoming)
        isRotate = IMGHoming.isRotate(sHoming, eHoming)
    }
}
