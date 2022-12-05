#include <jni.h>
#include <zstd.h>
#include <zstd_errors.h>
#include <stdlib.h>
#include <stdint.h>

/* field IDs can't change in the same VM */
static jfieldID src_pos_id;
static jfieldID dst_pos_id;

/*
 * Class:     com_github_luben_zstd_ZstdInputStreamNoFinalizer
 * Method:    recommendedDInSize
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_github_luben_zstd_ZstdInputStreamNoFinalizer_recommendedDInSize
  (JNIEnv *env, jclass obj) {
    return (jlong) ZSTD_DStreamInSize();
}

/*
 * Class:     com_github_luben_zstd_ZstdInputStreamNoFinalizer
 * Method:    recommendedDOutSize
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_github_luben_zstd_ZstdInputStreamNoFinalizer_recommendedDOutSize
  (JNIEnv *env, jclass obj) {
    return (jlong) ZSTD_DStreamOutSize();
}

/*
 * Class:     com_github_luben_zstd_ZstdInputStreamNoFinalizer
 * Method:    createDStream
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_github_luben_zstd_ZstdInputStreamNoFinalizer_createDStream
  (JNIEnv *env, jclass obj) {
    return (jlong)(intptr_t) ZSTD_createDStream();
}

/*
 * Class:     com_github_luben_zstd_ZstdInputStreamNoFinalizer
 * Method:    freeDStream
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_github_luben_zstd_ZstdInputStreamNoFinalizer_freeDStream
  (JNIEnv *env, jclass obj, jlong stream) {
    return ZSTD_freeDCtx((ZSTD_DCtx *)(intptr_t) stream);
}

/*
 * Class:     com_github_luben_zstd_ZstdInputStreamNoFinalizer
 * Method:    initDStream
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_github_luben_zstd_ZstdInputStreamNoFinalizer_initDStream
  (JNIEnv *env, jclass obj, jlong stream) {
    jclass clazz = (*env)->GetObjectClass(env, obj);
    // Initialize the fields ids only once - they can't change
    src_pos_id = (*env)->GetFieldID(env, clazz, "srcPos", "J");
    dst_pos_id = (*env)->GetFieldID(env, clazz, "dstPos", "J");
    return 0;
}

/*
 * Class:     com_github_luben_zstd_ZstdInputStreamNoFinalizer
 * Method:    decompressStream
 * Signature: (J[BI[BI)I
 */
JNIEXPORT jint JNICALL Java_com_github_luben_zstd_ZstdInputStreamNoFinalizer_decompressStream
  (JNIEnv *env, jclass obj, jlong stream, jbyteArray dst, jint dst_size, jbyteArray src, jint src_size) {

    size_t size = -ZSTD_error_memory_allocation;

    size_t src_pos = (size_t) (*env)->GetLongField(env, obj, src_pos_id);
    size_t dst_pos = (size_t) (*env)->GetLongField(env, obj, dst_pos_id);

    void *dst_buff = (*env)->GetPrimitiveArrayCritical(env, dst, NULL);
    if (dst_buff == NULL) goto E1;
    void *src_buff = (*env)->GetPrimitiveArrayCritical(env, src, NULL);
    if (src_buff == NULL) goto E2;

    ZSTD_outBuffer output = { dst_buff, dst_size, dst_pos };
    ZSTD_inBuffer input = { src_buff, src_size, src_pos };

    size = ZSTD_decompressStream((ZSTD_DCtx *)(intptr_t) stream, &output, &input);

    (*env)->ReleasePrimitiveArrayCritical(env, src, src_buff, JNI_ABORT);
E2: (*env)->ReleasePrimitiveArrayCritical(env, dst, dst_buff, 0);
    (*env)->SetLongField(env, obj, dst_pos_id, output.pos);
    (*env)->SetLongField(env, obj, src_pos_id, input.pos);
E1: return (jint) size;
}
