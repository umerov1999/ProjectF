package dev.ragnarok.fenrir.module.hls

import dev.ragnarok.fenrir.module.FenrirNative.isNativeLoaded

object TSDemuxer {
    private external fun unpack(
        input: String,
        output: String,
        info: Boolean,
        print_debug: Boolean
    ): Boolean

    fun unpackTS(input: String, output: String, info: Boolean, print_debug: Boolean): Boolean {
        return if (!isNativeLoaded) {
            false
        } else unpack(input, output, info, print_debug)
    }
}