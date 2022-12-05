package dev.ragnarok.filegallery.activity.slidr.widget

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import dev.ragnarok.filegallery.activity.slidr.model.SlidrPosition

internal class ScrimRenderer(private val rootView: View, private val decorView: View) {
    private val dirtyRect: Rect = Rect()
    fun render(canvas: Canvas, position: SlidrPosition?, paint: Paint) {
        when (position) {
            SlidrPosition.LEFT -> renderLeft(canvas, paint)
            SlidrPosition.RIGHT -> renderRight(canvas, paint)
            SlidrPosition.TOP -> renderTop(canvas, paint)
            SlidrPosition.BOTTOM -> renderBottom(canvas, paint)
            SlidrPosition.VERTICAL -> renderVertical(canvas, paint)
            SlidrPosition.HORIZONTAL -> renderHorizontal(canvas, paint)
            else -> {}
        }
    }

    fun getDirtyRect(position: SlidrPosition?): Rect {
        when (position) {
            SlidrPosition.LEFT -> dirtyRect[0, 0, decorView.left] = rootView.measuredHeight
            SlidrPosition.RIGHT -> dirtyRect[decorView.right, 0, rootView.measuredWidth] =
                rootView.measuredHeight
            SlidrPosition.TOP -> dirtyRect[0, 0, rootView.measuredWidth] = decorView.top
            SlidrPosition.BOTTOM -> dirtyRect[0, decorView.bottom, rootView.measuredWidth] =
                rootView.measuredHeight
            SlidrPosition.VERTICAL -> if (decorView.top > 0) {
                dirtyRect[0, 0, rootView.measuredWidth] = decorView.top
            } else {
                dirtyRect[0, decorView.bottom, rootView.measuredWidth] = rootView.measuredHeight
            }
            SlidrPosition.HORIZONTAL -> if (decorView.left > 0) {
                dirtyRect[0, 0, decorView.left] = rootView.measuredHeight
            } else {
                dirtyRect[decorView.right, 0, rootView.measuredWidth] = rootView.measuredHeight
            }
            else -> {}
        }
        return dirtyRect
    }

    private fun renderLeft(canvas: Canvas, paint: Paint) {
        canvas.drawRect(0f, 0f, decorView.left.toFloat(), rootView.measuredHeight.toFloat(), paint)
    }

    private fun renderRight(canvas: Canvas, paint: Paint) {
        canvas.drawRect(
            decorView.right.toFloat(),
            0f,
            rootView.measuredWidth.toFloat(),
            rootView.measuredHeight.toFloat(),
            paint
        )
    }

    private fun renderTop(canvas: Canvas, paint: Paint) {
        canvas.drawRect(0f, 0f, rootView.measuredWidth.toFloat(), decorView.top.toFloat(), paint)
    }

    private fun renderBottom(canvas: Canvas, paint: Paint) {
        canvas.drawRect(
            0f,
            decorView.bottom.toFloat(),
            rootView.measuredWidth.toFloat(),
            rootView.measuredHeight.toFloat(),
            paint
        )
    }

    private fun renderVertical(canvas: Canvas, paint: Paint) {
        if (decorView.top > 0) {
            renderTop(canvas, paint)
        } else {
            renderBottom(canvas, paint)
        }
    }

    private fun renderHorizontal(canvas: Canvas, paint: Paint) {
        if (decorView.left > 0) {
            renderLeft(canvas, paint)
        } else {
            renderRight(canvas, paint)
        }
    }

}