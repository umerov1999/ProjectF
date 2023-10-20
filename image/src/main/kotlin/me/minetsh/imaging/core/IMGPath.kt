package me.minetsh.imaging.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path

/**
 * Created by felix on 2017/11/22 下午6:13.
 */
open class IMGPath @JvmOverloads constructor(
    var path: Path = Path(),
    var mode: IMGMode = IMGMode.DOODLE,
    var color: Int = Color.RED,
    var width: Float = BASE_MOSAIC_WIDTH
) {

    init {
        if (mode == IMGMode.MOSAIC) {
            path.fillType = Path.FillType.EVEN_ODD
        }
    }

    fun onDrawDoodle(canvas: Canvas, paint: Paint) {
        if (mode == IMGMode.DOODLE) {
            paint.color = color
            paint.strokeWidth = BASE_DOODLE_WIDTH
            // rewind
            canvas.drawPath(path, paint)
        }
    }

    fun onDrawMosaic(canvas: Canvas, paint: Paint) {
        if (mode == IMGMode.MOSAIC) {
            paint.strokeWidth = width
            canvas.drawPath(path, paint)
        }
    }

    fun transform(matrix: Matrix) {
        path.transform(matrix)
    }

    companion object {
        const val BASE_DOODLE_WIDTH = 20f
        const val BASE_MOSAIC_WIDTH = 72f
    }
}
