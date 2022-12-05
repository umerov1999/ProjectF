package dev.ragnarok.filegallery.view.media

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import dev.ragnarok.filegallery.util.Utils

class PathAnimator(
    private val scale: Float,
    private val tx: Float,
    private val ty: Float,
    private val durationScale: Float
) {
    private val path = Path()
    private val keyFrames = ArrayList<KeyFrame>()
    fun addSvgKeyFrame(svg: String?, ms: Float) {
        if (svg == null) {
            return
        }
        try {
            val keyFrame = KeyFrame()
            keyFrame.time = ms * durationScale
            val args = svg.split(Regex(" ")).toTypedArray()
            var a = 0
            while (a < args.size) {
                when (args[a][0]) {
                    'M' -> {
                        val moveTo = MoveTo()
                        moveTo.x = (args[a + 1].toFloat() + tx) * scale
                        moveTo.y = (args[a + 2].toFloat() + ty) * scale
                        keyFrame.commands.add(moveTo)
                        a += 2
                    }
                    'C' -> {
                        val curveTo = CurveTo()
                        curveTo.x1 = (args[a + 1].toFloat() + tx) * scale
                        curveTo.y1 = (args[a + 2].toFloat() + ty) * scale
                        curveTo.x2 = (args[a + 3].toFloat() + tx) * scale
                        curveTo.y2 = (args[a + 4].toFloat() + ty) * scale
                        curveTo.x = (args[a + 5].toFloat() + tx) * scale
                        curveTo.y = (args[a + 6].toFloat() + ty) * scale
                        keyFrame.commands.add(curveTo)
                        a += 6
                    }
                    'L' -> {
                        val lineTo = LineTo()
                        lineTo.x = (args[a + 1].toFloat() + tx) * scale
                        lineTo.y = (args[a + 2].toFloat() + ty) * scale
                        keyFrame.commands.add(lineTo)
                        a += 2
                    }
                }
                a++
            }
            keyFrames.add(keyFrame)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun draw(canvas: Canvas, paint: Paint?, time: Float) {
        var startKeyFrame: KeyFrame? = null
        var endKeyFrame: KeyFrame? = null
        run {
            var a = 0
            val N = keyFrames.size
            while (a < N) {
                val keyFrame = keyFrames[a]
                if ((startKeyFrame == null || (startKeyFrame
                        ?: return@run).time < keyFrame.time) && keyFrame.time <= time
                ) {
                    startKeyFrame = keyFrame
                }
                if ((endKeyFrame == null || (endKeyFrame
                        ?: return@run).time > keyFrame.time) && keyFrame.time >= time
                ) {
                    endKeyFrame = keyFrame
                }
                a++
            }
        }
        if (endKeyFrame === startKeyFrame) {
            startKeyFrame = null
        }
        if (startKeyFrame != null && endKeyFrame == null) {
            endKeyFrame = startKeyFrame
            startKeyFrame = null
        }
        if (endKeyFrame == null || startKeyFrame != null && (startKeyFrame
                ?: return).commands.size != (endKeyFrame ?: return).commands.size
        ) {
            return
        }
        path.reset()
        var a = 0
        val N = (endKeyFrame ?: return).commands.size
        while (a < N) {
            val startCommand =
                if (startKeyFrame != null) (startKeyFrame ?: return).commands[a] else null
            val endCommand = (endKeyFrame ?: return).commands[a]
            if (startCommand != null && startCommand.javaClass != endCommand.javaClass) {
                return
            }
            val progress: Float = if (startKeyFrame != null) {
                (time - (startKeyFrame ?: return).time) / ((endKeyFrame
                    ?: return).time - (startKeyFrame ?: return).time)
            } else {
                1.0f
            }
            if (endCommand is MoveTo) {
                val start = startCommand as MoveTo?
                if (start != null) {
                    path.moveTo(
                        Utils.dp(start.x + (endCommand.x - start.x) * progress).toFloat(),
                        Utils.dp(start.y + (endCommand.y - start.y) * progress).toFloat()
                    )
                } else {
                    path.moveTo(Utils.dp(endCommand.x).toFloat(), Utils.dp(endCommand.y).toFloat())
                }
            } else if (endCommand is LineTo) {
                val start = startCommand as LineTo?
                if (start != null) {
                    path.lineTo(
                        Utils.dp(start.x + (endCommand.x - start.x) * progress).toFloat(),
                        Utils.dp(start.y + (endCommand.y - start.y) * progress).toFloat()
                    )
                } else {
                    path.lineTo(Utils.dp(endCommand.x).toFloat(), Utils.dp(endCommand.y).toFloat())
                }
            } else if (endCommand is CurveTo) {
                val start = startCommand as CurveTo?
                if (start != null) {
                    path.cubicTo(
                        Utils.dp(start.x1 + (endCommand.x1 - start.x1) * progress).toFloat(),
                        Utils.dp(start.y1 + (endCommand.y1 - start.y1) * progress).toFloat(),
                        Utils.dp(start.x2 + (endCommand.x2 - start.x2) * progress).toFloat(),
                        Utils.dp(start.y2 + (endCommand.y2 - start.y2) * progress).toFloat(),
                        Utils.dp(start.x + (endCommand.x - start.x) * progress).toFloat(),
                        Utils.dp(start.y + (endCommand.y - start.y) * progress).toFloat()
                    )
                } else {
                    path.cubicTo(
                        Utils.dp(endCommand.x1).toFloat(),
                        Utils.dp(endCommand.y1).toFloat(),
                        Utils.dp(endCommand.x2).toFloat(),
                        Utils.dp(endCommand.y2).toFloat(),
                        Utils.dp(endCommand.x).toFloat(),
                        Utils.dp(endCommand.y).toFloat()
                    )
                }
            }
            a++
        }
        path.close()
        canvas.drawPath(path, paint ?: return)
    }

    private class KeyFrame {
        val commands = ArrayList<Any>()
        var time = 0f
    }

    private class MoveTo {
        var x = 0f
        var y = 0f
    }

    private class LineTo {
        var x = 0f
        var y = 0f
    }

    private class CurveTo {
        var x = 0f
        var y = 0f
        var x1 = 0f
        var y1 = 0f
        var x2 = 0f
        var y2 = 0f
    }
}