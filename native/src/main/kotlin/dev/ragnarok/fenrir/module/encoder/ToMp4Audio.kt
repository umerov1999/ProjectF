package dev.ragnarok.fenrir.module.encoder

import dev.ragnarok.fenrir.module.FenrirNative.isNativeLoaded

object ToMp4Audio {
    private external fun encodeToMp4(input: String, output: String): Boolean
    fun encodeToMp4Audio(input: String, output: String): Boolean {
        return if (!isNativeLoaded) {
            false
        } else encodeToMp4(input, output)
    }
}