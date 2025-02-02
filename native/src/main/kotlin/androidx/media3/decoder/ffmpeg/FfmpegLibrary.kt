/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.decoder.ffmpeg

import androidx.media3.common.C
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import dev.ragnarok.fenrir.module.FenrirNative

/**
 * Configures and queries the underlying native library.
 */
@UnstableApi
object FfmpegLibrary {
    private const val TAG = "FfmpegLibrary"

    /**
     * Returns the version of the underlying library if available, or null otherwise.
     */
    var version: String? = null
        get() {
            if (!FenrirNative.isNativeLoaded) {
                return null
            }
            if (field == null) {
                field = ffmpegGetVersion()
            }
            return field
        }
        private set

    /**
     * Returns the required amount of padding for input buffers in bytes, or [C.LENGTH_UNSET] if
     * the underlying library is not available.
     */
    var inputBufferPaddingSize = C.LENGTH_UNSET
        get() {
            if (!FenrirNative.isNativeLoaded) {
                return C.LENGTH_UNSET
            }
            if (field == C.LENGTH_UNSET) {
                field = ffmpegGetInputBufferPaddingSize()
            }
            return field
        }
        private set

    /**
     * Returns whether the underlying library supports the specified MIME type.
     *
     * @param mimeType The MIME type to check.
     */
    fun supportsFormat(mimeType: String?): Boolean {
        if (!FenrirNative.isNativeLoaded) {
            return false
        }
        val codecName = getCodecName(mimeType)
            ?: return false
        if (!ffmpegHasDecoder(codecName)) {
            Log.w(TAG, "No $codecName decoder available. Check the FFmpeg build configuration.")
            return false
        }
        return true
    }

    /**
     * Returns the name of the FFmpeg decoder that could be used to decode the format, or `null`
     * if it's unsupported.
     */
    fun getCodecName(mimeType: String?): String? {
        return when (mimeType) {
            MimeTypes.AUDIO_AAC -> "aac"
            MimeTypes.AUDIO_MPEG, MimeTypes.AUDIO_MPEG_L1, MimeTypes.AUDIO_MPEG_L2 -> "mp3"
            MimeTypes.AUDIO_AC3 -> "ac3"
            MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "eac3"
            MimeTypes.AUDIO_VORBIS -> "vorbis"
            MimeTypes.AUDIO_OPUS -> "opus"
            MimeTypes.AUDIO_FLAC -> "flac"
            MimeTypes.AUDIO_ALAC -> "alac"
            else -> null
        }
    }

    private external fun ffmpegGetVersion(): String?
    private external fun ffmpegGetInputBufferPaddingSize(): Int
    private external fun ffmpegHasDecoder(codecName: String): Boolean

    init {
        MediaLibraryInfo.registerModule("media3.decoder.ffmpeg")
    }
}