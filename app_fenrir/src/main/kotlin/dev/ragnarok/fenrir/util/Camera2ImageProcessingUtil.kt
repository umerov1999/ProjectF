package dev.ragnarok.fenrir.util

import android.graphics.Bitmap
import android.view.Surface
import androidx.camera.core.ImageProcessingUtil_JNI
import dev.ragnarok.fenrir.module.ImageProcessingUtilNative
import java.nio.ByteBuffer

object Camera2ImageProcessingUtil : ImageProcessingUtil_JNI {
    override fun nativeConvertAndroid420ToBitmap(
        srcByteBufferY: ByteBuffer,
        srcStrideY: Int,
        srcByteBufferU: ByteBuffer,
        srcStrideU: Int,
        srcByteBufferV: ByteBuffer,
        srcStrideV: Int,
        srcPixelStrideY: Int,
        srcPixelStrideUV: Int,
        bitmap: Bitmap,
        bitmapStride: Int,
        width: Int,
        height: Int
    ): Int {
        return ImageProcessingUtilNative.convertAndroid420ToBitmap(
            srcByteBufferY,
            srcStrideY,
            srcByteBufferU,
            srcStrideU,
            srcByteBufferV,
            srcStrideV,
            srcPixelStrideY,
            srcPixelStrideUV,
            bitmap,
            bitmapStride,
            width,
            height
        )
    }

    override fun nativeCopyBetweenByteBufferAndBitmap(
        bitmap: Bitmap,
        byteBuffer: ByteBuffer,
        sourceStride: Int,
        destinationStride: Int,
        width: Int,
        height: Int,
        isCopyBufferToBitmap: Boolean
    ): Int {
        return ImageProcessingUtilNative.copyBetweenByteBufferAndBitmap(
            bitmap,
            byteBuffer,
            sourceStride,
            destinationStride,
            width,
            height,
            isCopyBufferToBitmap
        )
    }

    override fun nativeWriteJpegToSurface(jpegArray: ByteArray, surface: Surface): Int {
        return ImageProcessingUtilNative.writeJpegToSurface(
            jpegArray,
            surface
        )
    }

    override fun nativeConvertAndroid420ToABGR(
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
        return ImageProcessingUtilNative.convertAndroid420ToABGR(
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

    override fun nativeShiftPixel(
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
        return ImageProcessingUtilNative.shiftPixel(
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

    override fun nativeRotateYUV(
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
        return ImageProcessingUtilNative.rotateYUV(
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
}