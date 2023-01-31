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
package com.google.android.exoplayer2.ext.ffmpeg

import android.os.Handler
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.C.PcmEncoding
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.RendererCapabilities.AdaptiveSupport
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.decoder.CryptoConfig
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.TraceUtil
import com.google.android.exoplayer2.util.Util
import dev.ragnarok.fenrir.module.FenrirNative

/**
 * Decodes and renders audio using FFmpeg.
 */
@Suppress("UNUSED")
class FfmpegAudioRenderer
/**
 * Creates a new instance.
 *
 * @param eventHandler  A handler to use when delivering events to `eventListener`. May be
 * null if delivery of events is not required.
 * @param eventListener A listener of events. May be null if delivery of events is not required.
 * @param audioSink     The sink to which audio will be output.
 */
    (
    eventHandler: Handler?,
    eventListener: AudioRendererEventListener?,
    audioSink: AudioSink
) : DecoderAudioRenderer<FfmpegAudioDecoder>(eventHandler, eventListener, audioSink) {
    constructor() : this( /* eventHandler= */null,  /* eventListener= */null)

    /**
     * Creates a new instance.
     *
     * @param eventHandler    A handler to use when delivering events to `eventListener`. May be
     * null if delivery of events is not required.
     * @param eventListener   A listener of events. May be null if delivery of events is not required.
     * @param audioProcessors Optional [AudioProcessor]s that will process audio before output.
     */
    constructor(
        eventHandler: Handler?,
        eventListener: AudioRendererEventListener?,
        vararg audioProcessors: AudioProcessor
    ) : this(
        eventHandler,
        eventListener,
        DefaultAudioSink.Builder().setAudioProcessors(audioProcessors).build()
    )

    override fun getName(): String {
        return TAG
    }

    override fun supportsFormatInternal(format: Format): @C.FormatSupport Int {
        val mimeType = Assertions.checkNotNull(format.sampleMimeType)
        return if (!FenrirNative.isNativeLoaded || !MimeTypes.isAudio(
                mimeType
            )
        ) {
            C.FORMAT_UNSUPPORTED_TYPE
        } else if (!FfmpegLibrary.supportsFormat(mimeType)
            || (!sinkSupportsFormat(format, C.ENCODING_PCM_16BIT)
                    && !sinkSupportsFormat(format, C.ENCODING_PCM_FLOAT))
        ) {
            C.FORMAT_UNSUPPORTED_SUBTYPE
        } else if (format.cryptoType != C.CRYPTO_TYPE_NONE) {
            C.FORMAT_UNSUPPORTED_DRM
        } else {
            C.FORMAT_HANDLED
        }
    }

    override fun supportsMixedMimeTypeAdaptation(): @AdaptiveSupport Int {
        return ADAPTIVE_NOT_SEAMLESS
    }

    /** {@inheritDoc} */
    @Throws(FfmpegDecoderException::class)
    override fun createDecoder(format: Format, cryptoConfig: CryptoConfig?): FfmpegAudioDecoder {
        TraceUtil.beginSection("createFfmpegAudioDecoder")
        val initialInputBufferSize =
            if (format.maxInputSize != Format.NO_VALUE) format.maxInputSize else DEFAULT_INPUT_BUFFER_SIZE
        val decoder = FfmpegAudioDecoder(
            format, NUM_BUFFERS, NUM_BUFFERS, initialInputBufferSize, shouldOutputFloat(format)
        )
        TraceUtil.endSection()
        return decoder
    }

    /** {@inheritDoc} */
    override fun getOutputFormat(decoder: FfmpegAudioDecoder): Format {
        Assertions.checkNotNull(decoder)
        return Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_RAW)
            .setChannelCount(decoder.channelCount)
            .setSampleRate(decoder.sampleRate)
            .setPcmEncoding(decoder.encoding)
            .build()
    }

    /**
     * Returns whether the renderer's [AudioSink] supports the PCM format that will be output
     * from the decoder for the given input format and requested output encoding.
     */
    private fun sinkSupportsFormat(inputFormat: Format, pcmEncoding: @PcmEncoding Int): Boolean {
        return sinkSupportsFormat(
            Util.getPcmFormat(pcmEncoding, inputFormat.channelCount, inputFormat.sampleRate)
        )
    }

    private fun shouldOutputFloat(inputFormat: Format): Boolean {
        if (!sinkSupportsFormat(inputFormat, C.ENCODING_PCM_16BIT)) {
            // We have no choice because the sink doesn't support 16-bit integer PCM.
            return true
        }
        val formatSupport = getSinkFormatSupport(
            Util.getPcmFormat(
                C.ENCODING_PCM_FLOAT, inputFormat.channelCount, inputFormat.sampleRate
            )
        )
        return when (formatSupport) {
            AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY ->                 // AC-3 is always 16-bit, so there's no point using floating point. Assume that it's worth
                // using for all other formats.
                MimeTypes.AUDIO_AC3 != inputFormat.sampleMimeType

            AudioSink.SINK_FORMAT_UNSUPPORTED, AudioSink.SINK_FORMAT_SUPPORTED_WITH_TRANSCODING ->                 // Always prefer 16-bit PCM if the sink does not provide direct support for floating point.
                false

            else -> false
        }
    }

    companion object {
        private const val TAG = "FfmpegAudioRenderer"

        /**
         * The number of input and output buffers.
         */
        private const val NUM_BUFFERS = 16

        /**
         * The default input buffer size.
         */
        private const val DEFAULT_INPUT_BUFFER_SIZE = 960 * 6
    }
}