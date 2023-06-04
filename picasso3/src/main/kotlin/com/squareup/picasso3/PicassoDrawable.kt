/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.picasso3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.widget.ImageView
import com.squareup.picasso3.Picasso.LoadedFrom
import com.squareup.picasso3.Picasso.LoadedFrom.MEMORY
import com.squareup.picasso3.RequestHandler.Result
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal class PicassoDrawable(
    context: Context,
    bitmap: Bitmap,
    placeholder: Drawable?,
    private val loadedFrom: LoadedFrom,
    noFade: Boolean,
    private val debugging: Boolean
) : BitmapDrawable(context.resources, bitmap), Rotatable {
    private val density: Float = context.resources.displayMetrics.density
    var placeholder: Drawable? = null
    private var startTimeMillis: Long = 0
    private var animating = false
    private var _alpha = 0xFF
    private var rotate = 0f

    init {
        val fade = loadedFrom != MEMORY && !noFade
        if (fade) {
            this.placeholder = placeholder
            animating = true
            startTimeMillis = SystemClock.uptimeMillis()
        }
    }

    private fun evaluate(fraction: Float, startValue: Float, endValue: Float): Float {
        // convert to linear
        val startValueL = startValue.toDouble().pow(2.2).toFloat()
        val endValueL = endValue.toDouble().pow(2.2).toFloat()

        // compute the interpolated in linear space
        var r = startValueL + fraction * (endValueL - startValueL)
        r = r.toDouble().pow(1.0 / 2.2).toFloat()
        return r
    }

    private fun applyRotationAndDraw(canvas: Canvas) {
        if (rotate > 0f && intrinsicWidth > 0 && intrinsicHeight > 0) {
            canvas.save()
            canvas.rotate(rotate, intrinsicWidth / 2f, intrinsicHeight / 2f)

            val cosR = cos(Math.toRadians(rotate.toDouble())).toFloat()
            val sinR = sin(Math.toRadians(rotate.toDouble())).toFloat()

            val ssx = evaluate(
                abs(cosR),
                intrinsicHeight.toFloat(), intrinsicWidth.toFloat()
            ) / intrinsicWidth.toFloat()

            val ssy = evaluate(
                abs(sinR),
                intrinsicHeight.toFloat(), intrinsicWidth.toFloat()
            ) / intrinsicHeight.toFloat()

            canvas.scale(
                if (intrinsicWidth > intrinsicHeight) ssx else ssy,
                if (intrinsicHeight > intrinsicWidth) ssy else ssx,
                intrinsicWidth / 2f, intrinsicHeight / 2f
            )

            super.draw(canvas)
            canvas.restore()
        } else {
            super.draw(canvas)
        }
    }

    override fun draw(canvas: Canvas) {
        if (!animating) {
            try {
                applyRotationAndDraw(canvas)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
            }
        } else {
            val normalized = (SystemClock.uptimeMillis() - startTimeMillis) / FADE_DURATION
            if (normalized >= 1f) {
                animating = false
                placeholder = null
                try {
                    applyRotationAndDraw(canvas)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                    }
                }
            } else {
                if (placeholder != null) {
                    try {
                        placeholder!!.draw(canvas)
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace()
                        }
                    }
                }

                // setAlpha will call invalidateSelf and drive the animation.
                val partialAlpha = (_alpha * normalized).toInt()
                super.setAlpha(partialAlpha)
                try {
                    applyRotationAndDraw(canvas)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                    }
                }
                super.setAlpha(_alpha)
            }
        }

        if (debugging) {
            drawDebugIndicator(canvas)
        }
    }

    override fun setAlpha(alpha: Int) {
        this._alpha = alpha
        if (placeholder != null) {
            placeholder!!.alpha = alpha
        }
        super.setAlpha(alpha)
    }

    override fun setColorFilter(cf: ColorFilter?) {
        if (placeholder != null) {
            placeholder!!.colorFilter = cf
        }
        super.setColorFilter(cf)
    }

    override fun onBoundsChange(bounds: Rect) {
        if (placeholder != null) {
            placeholder!!.bounds = bounds
        }
        super.onBoundsChange(bounds)
    }

    private fun drawDebugIndicator(canvas: Canvas) {
        DEBUG_PAINT.color = Color.WHITE
        var path = getTrianglePath(0, 0, (16 * density).toInt())
        canvas.drawPath(path, DEBUG_PAINT)

        DEBUG_PAINT.color = loadedFrom.debugColor
        path = getTrianglePath(0, 0, (15 * density).toInt())
        canvas.drawPath(path, DEBUG_PAINT)
    }

    companion object {
        // Only accessed from main thread.
        private val DEBUG_PAINT = Paint()
        private const val FADE_DURATION = 200f // ms

        /**
         * Create or update the drawable on the target [ImageView] to display the supplied bitmap
         * image.
         */
        fun setResult(
            target: ImageView,
            context: Context,
            result: Result,
            noFade: Boolean,
            debugging: Boolean
        ) {
            val placeholder = target.drawable
            if (placeholder is Animatable) {
                (placeholder as Animatable).stop()
            }

            if (result is Result.Bitmap) {
                val bitmap = result.bitmap
                val loadedFrom = result.loadedFrom
                val drawable =
                    PicassoDrawable(context, bitmap, placeholder, loadedFrom, noFade, debugging)
                target.setImageDrawable(drawable)
            } else if (result is Result.Drawable) {
                val drawable = result.drawable
                target.setImageDrawable(drawable)
                if (drawable is Animatable) {
                    (drawable as Animatable).start()
                }
            }
        }

        /**
         * Create or update the drawable on the target [ImageView] to display the supplied
         * placeholder image.
         */
        fun setPlaceholder(target: ImageView, placeholderDrawable: Drawable?) {
            target.setImageDrawable(placeholderDrawable)
            if (target.drawable is Animatable) {
                (target.drawable as Animatable).start()
            }
        }

        fun getTrianglePath(x1: Int, y1: Int, width: Int): Path {
            return Path().apply {
                moveTo(x1.toFloat(), y1.toFloat())
                lineTo((x1 + width).toFloat(), y1.toFloat())
                lineTo(x1.toFloat(), (y1 + width).toFloat())
            }
        }
    }

    override fun rotate(degrees: Float) {
        rotate = degrees
        invalidateSelf()
    }

    override fun getRotation(): Float {
        return rotate
    }
}
