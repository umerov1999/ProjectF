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

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.C.PcmEncoding
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.decoder.DecoderInputBuffer
import com.google.android.exoplayer2.decoder.SimpleDecoder
import com.google.android.exoplayer2.decoder.SimpleDecoderOutputBuffer
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.ParsableByteArray
import com.google.android.exoplayer2.util.Util
import dev.ragnarok.fenrir.module.FenrirNative
import java.nio.ByteBuffer

/**
 * FFmpeg audio decoder.
 */
/* package */
@Suppress("UNCHECKED_CAST")
class FfmpegAudioDecoder(
    format: Format,
    numInputBuffers: Int,
    numOutputBuffers: Int,
    initialInputBufferSize: Int,
    outputFloat: Boolean
) : SimpleDecoder<DecoderInputBuffer, SimpleDecoderOutputBuffer, FfmpegDecoderException>(
    arrayOfNulls<DecoderInputBuffer>(numInputBuffers) as Array<out DecoderInputBuffer>,
    arrayOfNulls<SimpleDecoderOutputBuffer>(numOutputBuffers) as Array<out SimpleDecoderOutputBuffer>
) {
    private val codecName: String
    private val extraData: ByteArray?

    /**
     * Returns the encoding of output audio.
     */
    val encoding: @PcmEncoding Int
    private val outputBufferSize: Int
    private var nativeContext // May be reassigned on resetting the codec.
            : Long
    private var hasOutputFormat = false

    /**
     * Returns the channel count of output audio.
     */
    @Volatile
    var channelCount = 0
        private set

    /**
     * Returns the sample rate of output audio.
     */
    @Volatile
    var sampleRate = 0
        private set

    override fun getName(): String {
        return "ffmpeg" + (FfmpegLibrary.version ?: "not_loaded") + "-" + codecName
    }

    override fun createInputBuffer(): DecoderInputBuffer {
        return DecoderInputBuffer(
            DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT,
            FfmpegLibrary.inputBufferPaddingSize
        )
    }

    override fun createOutputBuffer(): SimpleDecoderOutputBuffer {
        return SimpleDecoderOutputBuffer {
            releaseOutputBuffer(
                it
            )
        }
    }

    override fun createUnexpectedDecodeException(error: Throwable): FfmpegDecoderException {
        return FfmpegDecoderException("Unexpected decode error", error)
    }

    override fun decode(
        inputBuffer: DecoderInputBuffer, outputBuffer: SimpleDecoderOutputBuffer, reset: Boolean
    ): FfmpegDecoderException? {
        if (reset) {
            nativeContext = ffmpegReset(nativeContext, extraData)
            if (nativeContext == 0L) {
                return FfmpegDecoderException("Error resetting (see logcat).")
            }
        }
        val inputData = Util.castNonNull(inputBuffer.data)
        val inputSize = inputData.limit()
        val outputData = outputBuffer.init(inputBuffer.timeUs, outputBufferSize)
        val result = ffmpegDecode(nativeContext, inputData, inputSize, outputData, outputBufferSize)
        when {
            result == AUDIO_DECODER_ERROR_OTHER -> {
                return FfmpegDecoderException("Error decoding (see logcat).")
            }
            result == AUDIO_DECODER_ERROR_INVALID_DATA -> {
                // Treat invalid data errors as non-fatal to match the behavior of MediaCodec. No output will
                // be produced for this buffer, so mark it as decode-only to ensure that the audio sink's
                // position is reset when more audio is produced.
                outputBuffer.setFlags(C.BUFFER_FLAG_DECODE_ONLY)
                return null
            }
            result == 0 -> {
                // There's no need to output empty buffers.
                outputBuffer.setFlags(C.BUFFER_FLAG_DECODE_ONLY)
                return null
            }
            !hasOutputFormat -> {
                channelCount = ffmpegGetChannelCount(nativeContext)
                sampleRate = ffmpegGetSampleRate(nativeContext)
                if (sampleRate == 0 && "alac" == codecName) {
                    Assertions.checkNotNull(extraData)
                    // ALAC decoder did not set the sample rate in earlier versions of FFmpeg. See
                    // https://trac.ffmpeg.org/ticket/6096.
                    val parsableExtraData = ParsableByteArray(extraData ?: return null)
                    parsableExtraData.position = extraData.size - 4
                    sampleRate = parsableExtraData.readUnsignedIntToInt()
                }
                hasOutputFormat = true
            }
        }
        outputData.position(0)
        outputData.limit(result)
        return null
    }

    override fun release() {
        super.release()
        ffmpegRelease(nativeContext)
        nativeContext = 0
    }

    private external fun ffmpegInitialize(
        codecName: String,
        extraData: ByteArray?,
        outputFloat: Boolean,
        rawSampleRate: Int,
        rawChannelCount: Int
    ): Long

    private external fun ffmpegDecode(
        context: Long,
        inputData: ByteBuffer,
        inputSize: Int,
        outputData: ByteBuffer,
        outputSize: Int
    ): Int

    private external fun ffmpegGetChannelCount(context: Long): Int
    private external fun ffmpegGetSampleRate(context: Long): Int
    private external fun ffmpegReset(context: Long, extraData: ByteArray?): Long
    private external fun ffmpegRelease(context: Long)

    companion object {
        // Output buffer sizes when decoding PCM mu-law streams, which is the maximum FFmpeg outputs.
        private const val OUTPUT_BUFFER_SIZE_16BIT = 65536
        private const val OUTPUT_BUFFER_SIZE_32BIT = OUTPUT_BUFFER_SIZE_16BIT * 2
        private const val AUDIO_DECODER_ERROR_INVALID_DATA = -1
        private const val AUDIO_DECODER_ERROR_OTHER = -2

        /**
         * Returns FFmpeg-compatible codec-specific initialization data ("extra data"), or `null` if
         * not required.
         */
        internal fun getExtraData(
            mimeType: String?,
            initializationData: List<ByteArray>
        ): ByteArray? {
            return when (mimeType) {
                MimeTypes.AUDIO_AAC, MimeTypes.AUDIO_OPUS -> initializationData[0]
                MimeTypes.AUDIO_ALAC -> getAlacExtraData(
                    initializationData
                )
                MimeTypes.AUDIO_VORBIS -> getVorbisExtraData(
                    initializationData
                )
                else ->                 // Other codecs do not require extra data.
                    null
            }
        }

        private fun getAlacExtraData(initializationData: List<ByteArray>): ByteArray {
            // FFmpeg's ALAC decoder expects an ALAC atom, which contains the ALAC "magic cookie", as extra
            // data. initializationData[0] contains only the magic cookie, and so we need to package it into
            // an ALAC atom. See:
            // https://ffmpeg.org/doxygen/0.6/alac_8c.html
            // https://github.com/macosforge/alac/blob/master/ALACMagicCookieDescription.txt
            val magicCookie = initializationData[0]
            val alacAtomLength = 12 + magicCookie.size
            val alacAtom = ByteBuffer.allocate(alacAtomLength)
            alacAtom.putInt(alacAtomLength)
            alacAtom.putInt(0x616c6163) // type=alac
            alacAtom.putInt(0) // version=0, flags=0
            alacAtom.put(magicCookie,  /* offset = */0, magicCookie.size)
            return alacAtom.array()
        }

        private fun getVorbisExtraData(initializationData: List<ByteArray>): ByteArray {
            val header0 = initializationData[0]
            val header1 = initializationData[1]
            val extraData = ByteArray(header0.size + header1.size + 6)
            extraData[0] = (header0.size shr 8).toByte()
            extraData[1] = (header0.size and 0xFF).toByte()
            System.arraycopy(header0, 0, extraData, 2, header0.size)
            extraData[header0.size + 2] = 0
            extraData[header0.size + 3] = 0
            extraData[header0.size + 4] = (header1.size shr 8).toByte()
            extraData[header0.size + 5] = (header1.size and 0xFF).toByte()
            System.arraycopy(header1, 0, extraData, header0.size + 6, header1.size)
            return extraData
        }
    }

    init {
        if (!FenrirNative.isNativeLoaded) {
            throw FfmpegDecoderException("Failed to load decoder native libraries.")
        }
        Assertions.checkNotNull(format.sampleMimeType)
        codecName = Assertions.checkNotNull(FfmpegLibrary.getCodecName(format.sampleMimeType))
        extraData = getExtraData(format.sampleMimeType, format.initializationData)
        encoding = if (outputFloat) C.ENCODING_PCM_FLOAT else C.ENCODING_PCM_16BIT
        outputBufferSize = if (outputFloat) OUTPUT_BUFFER_SIZE_32BIT else OUTPUT_BUFFER_SIZE_16BIT
        nativeContext = ffmpegInitialize(
            codecName,
            extraData,
            outputFloat,
            format.sampleRate,
            format.channelCount
        )
        if (nativeContext == 0L) {
            throw FfmpegDecoderException("Initialization failed.")
        }
        setInitialInputBufferSize(initialInputBufferSize)
    }
}