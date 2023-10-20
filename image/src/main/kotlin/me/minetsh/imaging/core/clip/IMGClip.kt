package me.minetsh.imaging.core.clip

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * Created by felix on 2017/11/28 下午6:15.
 */
interface IMGClip {
    enum class Anchor(val v: Int) {
        LEFT(1),
        RIGHT(2),
        TOP(4),
        BOTTOM(8),
        LEFT_TOP(5),
        RIGHT_TOP(6),
        LEFT_BOTTOM(9),
        RIGHT_BOTTOM(10);

        fun move(win: RectF, frame: RectF, dx: Float, dy: Float) {
            val maxFrame = cohesion(win, CLIP_MARGIN)
            val minFrame = cohesion(frame, CLIP_FRAME_MIN)
            val theFrame = cohesion(frame, 0f)
            val dxy = floatArrayOf(dx, 0f, dy)
            for (i in 0..3) {
                if (1 shl i and v != 0) {
                    val pn = PN[i and 1]
                    theFrame[i] = pn * revise(
                        pn * (theFrame[i] + dxy[i and 2]),
                        pn * maxFrame[i], pn * minFrame[i + PN[i and 1]]
                    )
                }
            }
            frame[theFrame[0], theFrame[2], theFrame[1]] = theFrame[3]
        }

        companion object {
            /**
             * LEFT: 0
             * TOP: 2
             * RIGHT: 1
             * BOTTOM: 3
             */
            const val P = 0
            const val N = 1
            const val H = 0
            const val V = 2
            val PN = intArrayOf(1, -1)
            fun revise(v: Float, min: Float, max: Float): Float {
                return min(max(v, min), max)
            }

            fun cohesion(win: RectF, v: Float): FloatArray {
                return floatArrayOf(
                    win.left + v, win.right - v,
                    win.top + v, win.bottom - v
                )
            }

            fun isCohesionContains(frame: RectF, v: Float, x: Float, y: Float): Boolean {
                return frame.left + v < x && frame.right - v > x && frame.top + v < y && frame.bottom - v > y
            }

            fun valueOf(v: Int): Anchor? {
                val values = entries.toTypedArray()
                for (anchor in values) {
                    if (anchor.v == v) {
                        return anchor
                    }
                }
                return null
            }
        }
    }

    companion object {
        /**
         * 裁剪区域的边距
         */
        const val CLIP_MARGIN = 60f

        /**
         * 角尺寸
         */
        const val CLIP_CORNER_SIZE = 48f

        /**
         * 裁剪区域最小尺寸
         */
        const val CLIP_FRAME_MIN = CLIP_CORNER_SIZE * 3.14f

        /**
         * 内边厚度
         */
        const val CLIP_THICKNESS_CELL = 3f

        /**
         * 外边框厚度
         */
        const val CLIP_THICKNESS_FRAME = 8f

        /**
         * 角边厚度
         */
        const val CLIP_THICKNESS_SEWING = 14f

        /**
         * 比例尺，用于计算出 {0, width, 1/3 width, 2/3 width} & {0, height, 1/3 height, 2/3 height}
         */
        val CLIP_SIZE_RATIO = floatArrayOf(0f, 1f, 0.33f, 0.66f)
        const val CLIP_CELL_STRIDES = 0x7362DC98
        const val CLIP_CORNER_STRIDES = 0x0AAFF550
        val CLIP_CORNER_STEPS = floatArrayOf(0f, 3f, -3f)
        val CLIP_CORNER_SIZES = floatArrayOf(0f, CLIP_CORNER_SIZE, -CLIP_CORNER_SIZE)
        val CLIP_CORNERS = byteArrayOf(
            0x8, 0x8, 0x9, 0x8,
            0x6, 0x8, 0x4, 0x8,
            0x4, 0x8, 0x4, 0x1,
            0x4, 0xA, 0x4, 0x8,
            0x4, 0x4, 0x6, 0x4,
            0x9, 0x4, 0x8, 0x4,
            0x8, 0x4, 0x8, 0x6,
            0x8, 0x9, 0x8, 0x8
        )
    }
}