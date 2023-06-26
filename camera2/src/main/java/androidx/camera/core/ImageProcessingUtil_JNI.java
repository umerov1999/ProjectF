package androidx.camera.core;

import android.graphics.Bitmap;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.ImageOutputConfig;

import java.nio.ByteBuffer;

public interface ImageProcessingUtil_JNI {
    int nativeConvertAndroid420ToBitmap(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideY,
            int srcPixelStrideUV,
            @NonNull Bitmap bitmap,
            int bitmapStride,
            int width,
            int height);

    int nativeCopyBetweenByteBufferAndBitmap(@NonNull Bitmap bitmap,
                                             @NonNull ByteBuffer byteBuffer,
                                             int sourceStride, int destinationStride, int width, int height,
                                             boolean isCopyBufferToBitmap);

    int nativeWriteJpegToSurface(@NonNull byte[] jpegArray,
                                 @NonNull Surface surface);

    int nativeConvertAndroid420ToABGR(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideY,
            int srcPixelStrideUV,
            @NonNull Surface surface,
            @Nullable ByteBuffer convertedByteBufferRGB,
            int width,
            int height,
            int startOffsetY,
            int startOffsetU,
            int startOffsetV,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees);

    int nativeShiftPixel(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideY,
            int srcPixelStrideUV,
            int width,
            int height,
            int startOffsetY,
            int startOffsetU,
            int startOffsetV);

    int nativeRotateYUV(
            @NonNull ByteBuffer srcByteBufferY,
            int srcStrideY,
            @NonNull ByteBuffer srcByteBufferU,
            int srcStrideU,
            @NonNull ByteBuffer srcByteBufferV,
            int srcStrideV,
            int srcPixelStrideUV,
            @NonNull ByteBuffer dstByteBufferY,
            int dstStrideY,
            int dstPixelStrideY,
            @NonNull ByteBuffer dstByteBufferU,
            int dstStrideU,
            int dstPixelStrideU,
            @NonNull ByteBuffer dstByteBufferV,
            int dstStrideV,
            int dstPixelStrideV,
            @NonNull ByteBuffer rotatedByteBufferY,
            @NonNull ByteBuffer rotatedByteBufferU,
            @NonNull ByteBuffer rotatedByteBufferV,
            int width,
            int height,
            @ImageOutputConfig.RotationDegreesValue int rotationDegrees);
}