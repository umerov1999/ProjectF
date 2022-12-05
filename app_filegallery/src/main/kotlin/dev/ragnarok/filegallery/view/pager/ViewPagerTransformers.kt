package dev.ragnarok.filegallery.view.pager

import android.view.View
import androidx.core.view.ViewCompat
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class DepthTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }
                position <= 0 -> { // [-1,0]
                    // Use the default slide transition when moving to the left page
                    alpha = 1f
                    translationX = 0f
                    translationZ = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
                position <= 1 -> { // (0,1]
                    // Fade the page out.
                    alpha = 1 - position

                    // Counteract the default slide transition
                    translationX = pageWidth * -position
                    // Move it behind the left page
                    translationZ = -1f

                    // Scale the page down (between MIN_SCALE and 1)
                    val scaleFactor =
                        (MIN_SCALE + (1 - MIN_SCALE) * (1 - abs(position)))
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }

    companion object {
        private const val MIN_SCALE = 0.75f
    }
}

class ZoomOutTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            val pageHeight = height
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }
                position <= 1 -> { // [-1,1]
                    // Modify the default slide transition to shrink the page as well
                    val scaleFactor = (1 - abs(position)).coerceAtLeast(MIN_SCALE)
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horizontalMargin = pageWidth * (1 - scaleFactor) / 2
                    translationX = if (position < 0) {
                        horizontalMargin - vertMargin / 2
                    } else {
                        horizontalMargin + vertMargin / 2
                    }

                    // Scale the page down (between MIN_SCALE and 1)
                    scaleX = scaleFactor
                    scaleY = scaleFactor

                    // Fade the page relative to its size.
                    alpha = (MIN_ALPHA +
                            (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }

    companion object {
        private const val MIN_SCALE = 0.85f
        private const val MIN_ALPHA = 0.5f
    }
}

class ClockSpinTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.translationX = -position * page.width
        if (abs(position) <= 0.5) {
            page.visibility = View.VISIBLE
            page.scaleX = 1 - abs(position)
            page.scaleY = 1 - abs(position)
        } else if (abs(position) > 0.5) {
            page.visibility = View.GONE
        }
        when {
            position < -1 -> {  // [-Infinity,-1)
                // This page is way off-screen to the left.
                page.alpha = 0f
            }
            position <= 0 -> {   // [-1,0]
                page.alpha = 1f
                page.rotation = 360 * abs(position)
            }
            position <= 1 -> {   // (0,1]
                page.alpha = 1f
                page.rotation = -360 * abs(position)
            }
            else -> {  // (1,+Infinity]
                // This page is way off-screen to the right.
                page.alpha = 0f
            }
        }
    }
}

class BackgroundToForegroundTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val height = page.height.toFloat()
        val width = page.width.toFloat()
        val scale = min(if (position < 0.0f) 1.0f else abs(1.0f - position), 0.5f)
        page.scaleX = scale
        page.scaleY = scale
        page.pivotX = width * 0.5f
        page.pivotY = height * 0.5f
        page.translationX = if (position < 0.0f) width * position else -width * position * 0.25f
    }

    private fun min(value: Float, min: Float): Float {
        return value.coerceAtLeast(min)
    }
}

class CubeInDepthTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.cameraDistance = 20000f
        when {
            position < -1 -> {
                page.alpha = 0f
            }
            position <= 0 -> {
                page.alpha = 1f
                page.pivotX = page.width.toFloat()
                page.rotationY = 90 * abs(position)
            }
            position <= 1 -> {
                page.alpha = 1f
                page.pivotX = 0f
                page.rotationY = -90 * abs(position)
            }
            else -> {
                page.alpha = 0f
            }
        }
        if (abs(position) <= 0.5) {
            page.scaleY = .4f.coerceAtLeast(1 - abs(position))
        } else if (abs(position) <= 1) {
            page.scaleY = .4f.coerceAtLeast(1 - abs(position))
        }
    }
}

class FanTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.translationX = -position * page.width
        page.pivotX = 0f
        page.pivotY = (page.height / 2).toFloat()
        page.cameraDistance = 20000f
        when {
            position < -1 -> {     // [-Infinity,-1)
                // This page is way off-screen to the left.
                page.alpha = 0f
            }
            position <= 0 -> {    // [-1,0]
                page.alpha = 1f
                page.rotationY = -120 * abs(position)
            }
            position <= 1 -> {    // (0,1]
                page.alpha = 1f
                page.rotationY = 120 * abs(position)
            }
            else -> {    // (1,+Infinity]
                // This page is way off-screen to the right.
                page.alpha = 0f
            }
        }
    }
}

class GateTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.translationX = -position * page.width
        when {
            position < -1 -> {    // [-Infinity,-1)
                // This page is way off-screen to the left.
                page.alpha = 0f
            }
            position <= 0 -> {    // [-1,0]
                page.alpha = 1f
                page.pivotX = 0f
                page.rotationY = 90 * abs(position)
            }
            position <= 1 -> {    // (0,1]
                page.alpha = 1f
                page.pivotX = page.width.toFloat()
                page.rotationY = -90 * abs(position)
            }
            else -> {    // (1,+Infinity]
                // This page is way off-screen to the right.
                page.alpha = 0f
            }
        }
    }
}

class SliderTransformer(private val offscreenPageLimit: Int) : ViewPager2.PageTransformer {
    companion object {
        private const val DEFAULT_TRANSLATION_X = .0f
        private const val DEFAULT_TRANSLATION_FACTOR = 1.2f
        private const val SCALE_FACTOR = .14f
        private const val DEFAULT_SCALE = 1f
        private const val ALPHA_FACTOR = .3f
        private const val DEFAULT_ALPHA = 1f

    }

    override fun transformPage(page: View, position: Float) {
        page.apply {
            ViewCompat.setElevation(page, -abs(position))
            val scaleFactor = -SCALE_FACTOR * position + DEFAULT_SCALE
            val alphaFactor = -ALPHA_FACTOR * position + DEFAULT_ALPHA
            when {
                position <= 0f -> {
                    translationX = DEFAULT_TRANSLATION_X
                    scaleX = DEFAULT_SCALE
                    scaleY = DEFAULT_SCALE
                    alpha = DEFAULT_ALPHA + position
                }
                position <= offscreenPageLimit - 1 -> {
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                    translationX = -(width / DEFAULT_TRANSLATION_FACTOR) * position
                    alpha = alphaFactor
                }
                else -> {
                    translationX = DEFAULT_TRANSLATION_X
                    scaleX = DEFAULT_SCALE
                    scaleY = DEFAULT_SCALE
                    alpha = DEFAULT_ALPHA
                }
            }
        }
    }
}
