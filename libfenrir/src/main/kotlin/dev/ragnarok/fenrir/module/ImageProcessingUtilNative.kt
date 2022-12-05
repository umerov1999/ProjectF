package dev.ragnarok.fenrir.module

import android.view.Surface
import java.nio.ByteBuffer

object ImageProcessingUtilNative {
    fun writeJpegToSurface(
        jpegArray: ByteArray,
        surface: Surface
    ): Int {
        return nativeWriteJpegToSurface(jpegArray, surface)
    }

    fun convertAndroid420ToABGR(
        srcByteBufferY: ByteBuffer,
        srcStrideY: Int,
        srcByteBufferU: ByteBuffer,
        srcStrideU: Int,
        srcByteBufferV: ByteBuffer,
        srcStrideV: Int,
        srcPixelStrideY: Int,
        srcPixelStrideUV: Int,
        surface: Surface,
        convertedByteBufferRGB: ByteBuffer?,
        width: Int,
        height: Int,
        startOffsetY: Int,
        startOffsetU: Int,
        startOffsetV: Int,
        rotationDegrees: Int
    ): Int {
        return nativeConvertAndroid420ToABGR(
            srcByteBufferY,
            srcStrideY,
            srcByteBufferU,
            srcStrideU,
            srcByteBufferV,
            srcStrideV,
            srcPixelStrideY,
            srcPixelStrideUV,
            surface,
            convertedByteBufferRGB,
            width,
            height,
            startOffsetY,
            startOffsetU,
            startOffsetV,
            rotationDegrees
        )
    }

    fun shiftPixel(
        srcByteBufferY: ByteBuffer,
        srcStrideY: Int,
        srcByteBufferU: ByteBuffer,
        srcStrideU: Int,
        srcByteBufferV: ByteBuffer,
        srcStrideV: Int,
        srcPixelStrideY: Int,
        srcPixelStrideUV: Int,
        width: Int,
        height: Int,
        startOffsetY: Int,
        startOffsetU: Int,
        startOffsetV: Int
    ): Int {
        return nativeShiftPixel(
            srcByteBufferY,
            srcStrideY,
            srcByteBufferU,
            srcStrideU,
            srcByteBufferV,
            srcStrideV,
            srcPixelStrideY,
            srcPixelStrideUV,
            width,
            height,
            startOffsetY,
            startOffsetU,
            startOffsetV
        )
    }

    fun rotateYUV(
        srcByteBufferY: ByteBuffer,
        srcStrideY: Int,
        srcByteBufferU: ByteBuffer,
        srcStrideU: Int,
        srcByteBufferV: ByteBuffer,
        srcStrideV: Int,
        srcPixelStrideUV: Int,
        dstByteBufferY: ByteBuffer,
        dstStrideY: Int,
        dstPixelStrideY: Int,
        dstByteBufferU: ByteBuffer,
        dstStrideU: Int,
        dstPixelStrideU: Int,
        dstByteBufferV: ByteBuffer,
        dstStrideV: Int,
        dstPixelStrideV: Int,
        rotatedByteBufferY: ByteBuffer,
        rotatedByteBufferU: ByteBuffer,
        rotatedByteBufferV: ByteBuffer,
        width: Int,
        height: Int,
        rotationDegrees: Int
    ): Int {
        return nativeRotateYUV(
            srcByteBufferY,
            srcStrideY,
            srcByteBufferU,
            srcStrideU,
            srcByteBufferV,
            srcStrideV,
            srcPixelStrideUV,
            dstByteBufferY,
            dstStrideY,
            dstPixelStrideY,
            dstByteBufferU,
            dstStrideU,
            dstPixelStrideU,
            dstByteBufferV,
            dstStrideV,
            dstPixelStrideV,
            rotatedByteBufferY,
            rotatedByteBufferU,
            rotatedByteBufferV,
            width,
            height,
            rotationDegrees
        )
    }

    private external fun nativeWriteJpegToSurface(
        jpegArray: ByteArray,
        surface: Surface
    ): Int

    private external fun nativeConvertAndroid420ToABGR(
        srcByteBufferY: ByteBuffer,
        srcStrideY: Int,
        srcByteBufferU: ByteBuffer,
        srcStrideU: Int,
        srcByteBufferV: ByteBuffer,
        srcStrideV: Int,
        srcPixelStrideY: Int,
        srcPixelStrideUV: Int,
        surface: Surface,
        convertedByteBufferRGB: ByteBuffer?,
        width: Int,
        height: Int,
        startOffsetY: Int,
        startOffsetU: Int,
        startOffsetV: Int,
        rotationDegrees: Int
    ): Int

    private external fun nativeShiftPixel(
        srcByteBufferY: ByteBuffer,
        srcStrideY: Int,
        srcByteBufferU: ByteBuffer,
        srcStrideU: Int,
        srcByteBufferV: ByteBuffer,
        srcStrideV: Int,
        srcPixelStrideY: Int,
        srcPixelStrideUV: Int,
        width: Int,
        height: Int,
        startOffsetY: Int,
        startOffsetU: Int,
        startOffsetV: Int
    ): Int

    private external fun nativeRotateYUV(
        srcByteBufferY: ByteBuffer,
        srcStrideY: Int,
        srcByteBufferU: ByteBuffer,
        srcStrideU: Int,
        srcByteBufferV: ByteBuffer,
        srcStrideV: Int,
        srcPixelStrideUV: Int,
        dstByteBufferY: ByteBuffer,
        dstStrideY: Int,
        dstPixelStrideY: Int,
        dstByteBufferU: ByteBuffer,
        dstStrideU: Int,
        dstPixelStrideU: Int,
        dstByteBufferV: ByteBuffer,
        dstStrideV: Int,
        dstPixelStrideV: Int,
        rotatedByteBufferY: ByteBuffer,
        rotatedByteBufferU: ByteBuffer,
        rotatedByteBufferV: ByteBuffer,
        width: Int,
        height: Int,
        rotationDegrees: Int
    ): Int
}
