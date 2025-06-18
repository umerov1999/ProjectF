package dev.ragnarok.fenrir.module.camerax

import java.nio.ByteBuffer

object ImageToolsNative {
    fun rotateBuffer(
        src: ByteBuffer,
        stride: Int,
        width: Int,
        height: Int,
        rotation: Int,
        flip: Boolean
    ): ByteArray? {
        return nativeRotateBuffer(src, stride, width, height, rotation, flip)
    }

    private external fun nativeRotateBuffer(
        src: ByteBuffer,
        stride: Int,
        width: Int,
        height: Int,
        rotation: Int,
        flip: Boolean
    ): ByteArray?
}
