#include <jni.h>
#include <string>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <cinttypes>
#include <cstdlib>

#include <android/bitmap.h>

#include "libyuv/convert_argb.h"
#include "libyuv/rotate_argb.h"
#include "libyuv/convert.h"
#include "libyuv/planar_functions.h"
#include "fenrir_native.h"

#define align_buffer_64(var, size)                                           \
  auto* var##_mem = new uint8_t[(size) + 63];         /* NOLINT */ \
  auto* var = reinterpret_cast<uint8_t *>(((intptr_t)(var##_mem) + 63) & ~63) /* NOLINT */

#define free_aligned_buffer_64(var) \
  delete[] var##_mem;                  \
  var##_mem = nullptr;                 \
  var = nullptr

static libyuv::RotationMode get_rotation_mode(int rotation) {
    libyuv::RotationMode mode = libyuv::kRotate0;
    switch (rotation) {
        case 0:
            mode = libyuv::kRotate0;
            break;
        case 90:
            mode = libyuv::kRotate90;
            break;
        case 180:
            mode = libyuv::kRotate180;
            break;
        case 270:
            mode = libyuv::kRotate270;
            break;
        default:
            break;
    }
    return mode;
}

// Helper function to convert Android420 to ABGR with options to choose full swing or studio swing.
static int Android420ToABGR(const uint8_t* src_y,
                            int src_stride_y,
                            const uint8_t* src_u,
                            int src_stride_u,
                            const uint8_t* src_v,
                            int src_stride_v,
                            int src_pixel_stride_uv,
                            uint8_t* dst_abgr,
                            int dst_stride_abgr,
                            bool is_full_swing,
                            int width,
                            int height) {
    return Android420ToARGBMatrix(src_y,
                                  src_stride_y,
                                  src_v,
                                  src_stride_v,
                                  src_u,
                                  src_stride_u,
                                  src_pixel_stride_uv,
                                  dst_abgr,
                                  dst_stride_abgr,
                                  is_full_swing
                                      ? &libyuv::kYvuJPEGConstants : &libyuv::kYvuI601Constants,
                                  width,
                                  height);
}

extern "C" JNIEXPORT jint
Java_dev_ragnarok_fenrir_module_camerax_ImageProcessingUtilNative_nativeCopyBetweenByteBufferAndBitmap(
        JNIEnv* env,
        jobject,
        jobject bitmap,
        jobject converted_buffer,
        int src_stride_argb,
        int dst_stride_argb,
        int width,
        int height,
        jboolean isCopyBufferToBitmap
) {
    void* bitmapAddress = nullptr;
    int copyResult;


    // get bitmap address
    int lockResult =  AndroidBitmap_lockPixels(env, bitmap, &bitmapAddress);
    if (lockResult != 0) {
        return -1;
    }

    // get buffer address
    auto* bufferAddress = static_cast<uint8_t*>(
            env->GetDirectBufferAddress(converted_buffer));

    // copy from buffer to bitmap
    if (isCopyBufferToBitmap) {
        copyResult = libyuv::ARGBCopy(bufferAddress, src_stride_argb,
                                      reinterpret_cast<uint8_t *> (bitmapAddress), dst_stride_argb,
                                      width, height);
    }

    // copy from bitmap to buffer
    else {
        copyResult = libyuv::ARGBCopy(reinterpret_cast<uint8_t*> (bitmapAddress), src_stride_argb,
                         bufferAddress, dst_stride_argb, width, height);
    }

    // check value of copy
    if (copyResult != 0) {
        return -1;
    }

    // balance call to AndroidBitmap_lockPixels
    int unlockResult = AndroidBitmap_unlockPixels(env,bitmap);
    if (unlockResult != 0) {
        return -1;
    }

    return 0;
}

extern "C" JNIEXPORT jint
Java_dev_ragnarok_fenrir_module_camerax_ImageProcessingUtilNative_nativeShiftPixel(
        JNIEnv* env,
        jobject,
        jobject src_y,
        jint src_stride_y,
        jobject src_u,
        jint src_stride_u,
        jobject src_v,
        jint src_stride_v,
        jint src_pixel_stride_y,
        jint src_pixel_stride_uv,
        jint width,
        jint height,
        jint start_offset_y,
        jint start_offset_u,
        jint start_offset_v) {
    auto* src_y_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_y));
    auto* src_u_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_u));
    auto* src_v_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_v));

    // TODO(b/195990691): extend the pixel shift to handle multiple corrupted pixels.
    // We don't support multiple pixel shift now.
    // Y
    for (int i = 0; i < height; i++) {
        memmove(&src_y_ptr[0 + i * src_stride_y],
                &src_y_ptr[start_offset_y + i * src_stride_y],
                width - 1);

        src_y_ptr[width - start_offset_y + i * src_stride_y] =
                src_y_ptr[src_stride_y - start_offset_y + i * src_stride_y];
    }

    const ptrdiff_t vu_off = src_v_ptr - src_u_ptr;

    // Note that if the data format is not I420, NV12 or NV21 cases, the data copy result might be
    // incorrect. That should be a very special format. If the data-shift issue also on that
    // device, the correct copy logic needs to be added here.
    if (src_pixel_stride_uv == 2 && (vu_off == 1 || vu_off == -1)) {
        // NV12 or NV21 cases
        // The U and V data are interleaved in a continuous array. Determines the start pointer by
        // the vu_off value.
        uint8_t *src_uv_ptr = vu_off == 1 ? src_u_ptr : src_v_ptr;

        // Because U/V data are interleaved and continuous, the data-copy process only need to be
        // done once. Both U/V data will be shifted together.
        for (int i = 0; i < height / 2; i++) {
            memmove(&src_uv_ptr[0 + i * src_stride_u],
                    &src_uv_ptr[src_pixel_stride_uv + i * src_stride_u],
                    width / 2 - src_pixel_stride_uv);

            src_uv_ptr[width / 2 - src_pixel_stride_uv + i * src_stride_u] =
                    src_uv_ptr[src_stride_u - src_pixel_stride_uv + i * src_stride_u];
            src_uv_ptr[width / 2 - src_pixel_stride_uv + i * src_stride_u + 1] =
                    src_uv_ptr[src_stride_u - src_pixel_stride_uv + i * src_stride_u + 1];
        }
    } else {
        // I420
        // U
        for (int i = 0; i < height / 2; i++) {
            memmove(&src_u_ptr[0 + i * src_stride_u],
                    &src_u_ptr[start_offset_u + i * src_stride_u],
                    width / 2 - 1);

            src_u_ptr[width / 2 - start_offset_u + i * src_stride_u] =
                    src_u_ptr[src_stride_u - start_offset_u + i * src_stride_u];
        }

        // V
        for (int i = 0; i < height / 2; i++) {
            memmove(&src_v_ptr[0 + i * src_stride_v],
                    &src_v_ptr[start_offset_v + i * src_stride_v],
                    width / 2 - 1);

            src_v_ptr[width / 2 - start_offset_v + i * src_stride_v] =
                    src_v_ptr[src_stride_v - start_offset_v + i * src_stride_v];
        }
    }

    return 0;
}

#define PADDING_BYTES_FOR_CAMERA3_JPEG_BLOB 8
/**
 * Writes the content JPEG array to the Surface.
 *
 * <p>This is for wrapping JPEG bytes with a media.Image object.
 */
extern "C" JNIEXPORT jint
Java_dev_ragnarok_fenrir_module_camerax_ImageProcessingUtilNative_nativeWriteJpegToSurface(
        JNIEnv *env,
        jobject,
        jbyteArray jpeg_array,
        jobject surface) {
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    if (window == nullptr) {
        LOGE("IMAGE_PROCESSING_UTIL: Failed to get ANativeWindow");
        return -1;
    }

    // Updates the size of ANativeWindow_Buffer with the JPEG bytes size. PLEASE NOTE that native
    // layer expects jpeg bytes to contain the camera3_jpeg_blob struct at the end of the buffer.
    // If jpeg bytes are supplied without the camera3_jpeg_blob, it is possible that the content
    // byte matches the CAMERA3_JPEG_BLOB_ID by chance and cause the wrong jpeg size to be reported.
    // To workaround the problem, here it adds the padding 0s to the end of the buffer so that
    // CAMERA3_JPEG_BLOB_ID won't be matched by any chance and the total bytes size is reported
    // as the jpeg size accordingly. The side effect of this approach is that there will be 8 zero
    // bytes at the end of the jpeg bytes apps received.
    jsize array_size = env->GetArrayLength(jpeg_array);
    ANativeWindow_setBuffersGeometry(window,
                                     array_size + PADDING_BYTES_FOR_CAMERA3_JPEG_BLOB,
                                     1, AHARDWAREBUFFER_FORMAT_BLOB);

    ANativeWindow_Buffer buffer;
    int lockResult = ANativeWindow_lock(window, &buffer, nullptr);
    if (lockResult != 0) {
        ANativeWindow_release(window);
        LOGE("IMAGE_PROCESSING_UTIL: Failed to lock window.");
        return -1;
    }

    // Copy from source to destination.
    jbyte *jpeg_ptr = env->GetByteArrayElements(jpeg_array, nullptr);
    if (jpeg_ptr == nullptr) {
        ANativeWindow_release(window);
        LOGE("IMAGE_PROCESSING_UTIL: Failed to get JPEG bytes array pointer.");
        return -1;
    }
    auto *buffer_ptr = reinterpret_cast<uint8_t *>(buffer.bits);
    memcpy(buffer_ptr, jpeg_ptr, array_size);
    // Set 0 for the padding bytes.
    memset(buffer_ptr + array_size, 0, PADDING_BYTES_FOR_CAMERA3_JPEG_BLOB);

    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);

    env->ReleaseByteArrayElements(jpeg_array, jpeg_ptr, 0);
    return 0;
}

extern "C" JNIEXPORT jint
Java_dev_ragnarok_fenrir_module_camerax_ImageProcessingUtilNative_nativeConvertAndroid420ToABGR(
        JNIEnv* env,
        jobject,
        jobject src_y,
        jint src_stride_y,
        jobject src_u,
        jint src_stride_u,
        jobject src_v,
        jint src_stride_v,
        jint src_pixel_stride_y,
        jint src_pixel_stride_uv,
        jobject surface,
        jobject converted_buffer,
        jint width,
        jint height,
        jint start_offset_y,
        jint start_offset_u,
        jint start_offset_v,
        int rotation) {

    auto* src_y_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_y));
    auto* src_u_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_u));
    auto* src_v_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_v));

    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    if (window == nullptr) {
        return -1;
    }
    ANativeWindow_Buffer buffer;
    int lockResult = ANativeWindow_lock(window, &buffer, nullptr);
    if(lockResult != 0 || buffer.format != WINDOW_FORMAT_RGBA_8888) {
        ANativeWindow_release(window);
        return -1;
    }

    libyuv::RotationMode mode = get_rotation_mode(rotation);
    bool has_rotation = rotation != 0;

    auto* buffer_ptr = reinterpret_cast<uint8_t*>(buffer.bits);
    auto* converted_buffer_ptr = (has_rotation && converted_buffer != nullptr)
            ? static_cast<uint8_t*>(env->GetDirectBufferAddress(converted_buffer)) : nullptr;

    auto* dst_ptr = has_rotation ? converted_buffer_ptr : buffer_ptr;
    int dst_stride_y = has_rotation ? (width * 4) : (buffer.stride * 4);

    int result = 0;
    // Apply workaround for one pixel shift issue by checking offset.
    if (start_offset_y > 0 || start_offset_u > 0 || start_offset_v > 0) {

        // TODO(b/195990691): extend the pixel shift to handle multiple corrupted pixels.
        // We don't support multiple pixel shift now.
        if (start_offset_y != src_pixel_stride_y
            || start_offset_u != src_pixel_stride_uv
            || start_offset_v != src_pixel_stride_uv) {
            ANativeWindow_unlockAndPost(window);
            ANativeWindow_release(window);
            return -1;
        }

        // Convert yuv to rgb except the last line.
        result = Android420ToABGR(src_y_ptr + start_offset_y,
                                  src_stride_y,
                                  src_u_ptr + start_offset_u,
                                  src_stride_u,
                                  src_v_ptr + start_offset_v,
                                  src_stride_v,
                                  src_pixel_stride_uv,
                                  dst_ptr,
                                  dst_stride_y,
                                  /* is_full_swing = */true,
                                  width,
                                  height - 1);
        if (result == 0) {
            // Convert the last row with (width - 1) pixels
            // since the last pixel's yuv data is missing.
            result = Android420ToABGR(
                    src_y_ptr + start_offset_y + src_stride_y * (height - 1),
                    src_stride_y - 1,
                    src_u_ptr + start_offset_u + src_stride_u * (height - 2) / 2,
                    src_stride_u - 1,
                    src_v_ptr + start_offset_v + src_stride_v * (height - 2) / 2,
                    src_stride_v - 1,
                    src_pixel_stride_uv,
                    dst_ptr + dst_stride_y * (height - 1),
                    dst_stride_y,
                    /* is_full_swing = */true,
                    width - 1,
                    1);
        }

        if (result == 0) {
            // Set the 2x2 pixels on the right bottom by duplicating the 3rd pixel
            // from the right to left in each row.
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    int r_ind = dst_stride_y * (height - 1 - i) + width * 4 - (j * 4 + 1);
                    int g_ind = dst_stride_y * (height - 1 - i) + width * 4 - (j * 4 + 2);
                    int b_ind = dst_stride_y * (height - 1 - i) + width * 4 - (j * 4 + 3);
                    int a_ind = dst_stride_y * (height - 1 - i) + width * 4 - (j * 4 + 4);
                    dst_ptr[r_ind] = dst_ptr[r_ind - 8];
                    dst_ptr[g_ind] = dst_ptr[g_ind - 8];
                    dst_ptr[b_ind] = dst_ptr[b_ind - 8];
                    dst_ptr[a_ind] = dst_ptr[a_ind - 8];
                }
            }
        }
    } else {
        result = Android420ToABGR(src_y_ptr + start_offset_y,
                                          src_stride_y,
                                          src_u_ptr + start_offset_u,
                                          src_stride_u,
                                          src_v_ptr + start_offset_v,
                                          src_stride_v,
                                          src_pixel_stride_uv,
                                          dst_ptr,
                                          dst_stride_y,
                                          /* is_full_swing = */true,
                                          width,
                                          height);
    }

    // TODO(b/203141655): avoid unnecessary memory copy by merging libyuv API for rotation.
    if (result == 0 && has_rotation) {
        result = libyuv::ARGBRotate(dst_ptr,
                                    dst_stride_y,
                                    buffer_ptr,
                                    buffer.stride * 4,
                                    width,
                                    height,
                                    mode);
    }

    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);
    return result;
}

extern "C" JNIEXPORT jint
Java_dev_ragnarok_fenrir_module_camerax_ImageProcessingUtilNative_nativeConvertAndroid420ToBitmap(
        JNIEnv* env,
        jobject,
        jobject src_y,
        jint src_stride_y,
        jobject src_u,
        jint src_stride_u,
        jobject src_v,
        jint src_stride_v,
        jint src_pixel_stride_y,
        jint src_pixel_stride_uv,
        jobject bitmap,
        jint bitmap_stride,
        jint width,
        jint height) {

    void* bitmapAddress = nullptr;

    // get bitmap address
    int lockResult =  AndroidBitmap_lockPixels(env, bitmap, &bitmapAddress);
    if (lockResult != 0) {
        return -1;
    }

    auto* src_y_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_y));
    auto* src_u_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_u));
    auto* src_v_ptr =
            static_cast<uint8_t*>(env->GetDirectBufferAddress(src_v));

    int dst_stride_y = bitmap_stride;

    int result = Android420ToABGR(
            src_y_ptr ,
            src_stride_y,
            src_u_ptr,
            src_stride_u,
            src_v_ptr,
            src_stride_v,
            src_pixel_stride_uv,
            reinterpret_cast<uint8_t *> (bitmapAddress),
            dst_stride_y,
            /* is_full_swing = */true,
            width,
            height);

    if (result != 0) {
        AndroidBitmap_unlockPixels(env,bitmap);
        return -1;
    }

    // balance call to AndroidBitmap_lockPixels
    int unlockResult = AndroidBitmap_unlockPixels(env,bitmap);
    if (unlockResult != 0) {
        return -1;
    }

    return 0;
}

extern "C" JNIEXPORT jint Java_dev_ragnarok_fenrir_module_camerax_ImageProcessingUtilNative_nativeRotateYUV(
        JNIEnv* env,
        jobject,
        jobject src_y,
        jint src_stride_y,
        jobject src_u,
        jint src_stride_u,
        jobject src_v,
        jint src_stride_v,
        jint src_pixel_stride_uv,
        jobject dst_y,
        jint dst_stride_y,
        jint dst_pixel_stride_y,
        jobject dst_u,
        jint dst_stride_u,
        jint dst_pixel_stride_u,
        jobject dst_v,
        jint dst_stride_v,
        jint dst_pixel_stride_v,
        jobject rotated_buffer_y,
        jobject rotated_buffer_u,
        jobject rotated_buffer_v,
        jint width,
        jint height,
        jint rotation) {

    auto *src_y_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(src_y));
    auto *src_u_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(src_u));
    auto *src_v_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(src_v));

    auto *dst_y_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(dst_y));
    auto *dst_u_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(dst_u));
    auto *dst_v_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(dst_v));

    int halfwidth = (width + 1) >> 1;
    int halfheight = (height + 1) >> 1;

    auto *rotated_y_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(rotated_buffer_y));
    auto *rotated_u_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(rotated_buffer_u));
    auto *rotated_v_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(rotated_buffer_v));

    libyuv::RotationMode mode = get_rotation_mode(rotation);
    bool flip_wh = (mode == libyuv::kRotate90 || mode == libyuv::kRotate270);

    int rotated_stride_y = flip_wh ? height : width;
    int rotated_stride_u = flip_wh ? halfheight : halfwidth;
    int rotated_stride_v = flip_wh ? halfheight : halfwidth;

    int result = 0;

    // Converts Android420 to I420 format with rotation applied
    result = libyuv::Android420ToI420Rotate(
            src_y_ptr,
            src_stride_y,
            src_u_ptr,
            src_stride_u,
            src_v_ptr,
            src_stride_v,
            src_pixel_stride_uv,
            rotated_y_ptr,
            rotated_stride_y,
            rotated_u_ptr,
            rotated_stride_u,
            rotated_v_ptr,
            rotated_stride_v,
            width,
            height,
            mode);

    if (result != 0) {
        return result;
    }

    // Convert to the required output format
    int rotated_width = flip_wh ? height : width;
    int rotated_height = flip_wh ? width : height;
    int rotated_halfwidth = flip_wh ? halfheight : halfwidth;
    int rotated_halfheight = flip_wh ? halfwidth : halfheight;
    const ptrdiff_t vu_off = dst_v_ptr - dst_u_ptr;

    if (dst_pixel_stride_u == 2 && vu_off == -1) {
        // NV21
        result = libyuv::I420ToNV21(
                /* src_y= */ rotated_y_ptr,
                /* src_stride_y= */ rotated_width,
                /* src_u= */ rotated_u_ptr,
                /* src_stride_u= */ rotated_halfwidth,
                /* src_v= */ rotated_v_ptr,
                /* src_stride_v= */ rotated_halfwidth,
                /* dst_y= */ dst_y_ptr,
                /* dst_stride_y= */ dst_stride_y,
                /* dst_uv= */ dst_v_ptr,
                /* dst_stride_uv= */ dst_stride_v,
                /* width= */ rotated_width,
                /* height= */ rotated_height
        );
    } else if (dst_pixel_stride_u == 2 && vu_off == 1) {
        // NV12
        result = libyuv::I420ToNV12(
                /* src_y= */ rotated_y_ptr,
                /* src_stride_y= */ rotated_width,
                /* src_u= */ rotated_u_ptr,
                /* src_stride_u= */ rotated_halfwidth,
                /* src_v= */ rotated_v_ptr,
                /* src_stride_v= */ rotated_halfwidth,
                /* dst_y= */ dst_y_ptr,
                /* dst_stride_y= */ dst_stride_y,
                /* dst_uv= */ dst_u_ptr,
                /* dst_stride_uv= */ dst_stride_u,
                /* width= */ rotated_width,
                /* height= */ rotated_height
        );
    } else if (dst_pixel_stride_u == 1 && dst_pixel_stride_v == 1) {
        // I420
        // Copies Y plane
        libyuv::CopyPlane(
                /* src_y= */ rotated_y_ptr,
                /* src_stride_y= */ rotated_width,
                /* dst_y= */ dst_y_ptr,
                /* dst_stride_y= */ dst_stride_y,
                /* width= */ rotated_width,
                /* height= */ rotated_height);
        // Copies U plane
        libyuv::CopyPlane(
                /* src_y= */ rotated_u_ptr,
                /* src_stride_y= */ rotated_halfwidth,
                /* dst_y= */ dst_u_ptr,
                /* dst_stride_y= */ dst_stride_u,
                /* width= */ rotated_halfwidth,
                /* height= */ rotated_halfheight);
        // Copies V plane
        libyuv::CopyPlane(
                /* src_y= */ rotated_v_ptr,
                /* src_stride_y= */ rotated_halfwidth,
                /* dst_y= */ dst_v_ptr,
                /* dst_stride_y= */ dst_stride_v,
                /* width= */ rotated_halfwidth,
                /* height= */ rotated_halfheight);
    } else {
        // Fall backs to directly copy according to the dst stride and pixel_stride values if
        // it is none of the above cases.

        // Y
        int rotated_pixel_stride_y = 1;
        for (int i = 0; i < rotated_height; i++) {
            for (int j = 0; j < rotated_width; j++) {
                dst_y_ptr[i * dst_stride_y + j * dst_pixel_stride_y] =
                        rotated_y_ptr[i * rotated_stride_y + j * rotated_pixel_stride_y];
            }
        }
        // U
        int rotated_pixel_stride_u = 1;
        for (int i = 0; i < rotated_halfheight; i++) {
            for (int j = 0; j < rotated_halfwidth; j++) {
                dst_u_ptr[i * dst_stride_u + j * dst_pixel_stride_u] =
                        rotated_u_ptr[i * rotated_stride_u + j * rotated_pixel_stride_u];
            }
        }
        // V
        int rotated_pixel_stride_v = 1;
        for (int i = 0; i < rotated_halfheight; i++) {
            for (int j = 0; j < rotated_halfwidth; j++) {
                dst_v_ptr[i * dst_stride_v + j * dst_pixel_stride_v] =
                        rotated_v_ptr[i * rotated_stride_v + j * rotated_pixel_stride_v];
            }
        }
    }

    return result;
}

extern "C" JNIEXPORT jint Java_dev_ragnarok_fenrir_module_camerax_ImageProcessingUtilNative_nativeGetYUVImageVUOff(
        JNIEnv* env,
        jobject,
        jobject byte_buffer_v,
        jobject byte_buffer_u) {
    auto *byte_buffer_v_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(byte_buffer_v));
    auto *byte_buffer_u_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(byte_buffer_u));
    return byte_buffer_v_ptr - byte_buffer_u_ptr;
}

/**
 * Creates ByteBuffer from the offset of the input byte buffer.
 */
extern "C" JNIEXPORT jobject
Java_dev_ragnarok_fenrir_module_camerax_ImageProcessingUtilNative_nativeNewDirectByteBuffer(
        JNIEnv *env,
        jobject,
        jobject byte_buffer,
        jint offset,
        jint capacity) {

    auto *byte_buffer_ptr =
            static_cast<uint8_t *>(env->GetDirectBufferAddress(byte_buffer));

    // Create the ByteBuffers
    return env->NewDirectByteBuffer(byte_buffer_ptr + offset, capacity);
}
