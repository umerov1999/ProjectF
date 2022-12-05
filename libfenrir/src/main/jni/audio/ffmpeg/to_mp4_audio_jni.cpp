extern "C" {
#include "libswresample/swresample.h"
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/opt.h"
}

#include "log.h"

#define ERRRET(str) {ret = false; LOGE("%s\n", str); goto out;}

static int
decode_packet(AVCodecContext *context, AVFrame *frame, AVPacket *packet, bool &got_frame) {
    int ret;
    int decoded = packet->size;
    got_frame = false;

    ret = avcodec_send_packet(context, packet);
    if (ret == AVERROR_EOF)
        return decoded;
    else if (ret == AVERROR(EAGAIN)) {
        return AVERROR_BUG;
    }
    ret = avcodec_receive_frame(context, frame);
    if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
        return decoded;
    } else if (ret) {
        return ret;
    }
    got_frame = true;

    return decoded;
}

static int
encode_frame(AVCodecContext *context, AVFrame *frame, AVPacket *packet, bool &got_packet) {
    int ret;
    int decoded = packet->size;
    got_packet = false;

    ret = avcodec_send_frame(context, frame);
    if (ret == AVERROR_EOF)
        return decoded;
    else if (ret == AVERROR(EAGAIN)) {
        return AVERROR_BUG;
    }
    ret = avcodec_receive_packet(context, packet);
    if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
        return decoded;
    } else if (ret) {
        return ret;
    }
    got_packet = true;

    return decoded;
}

static bool encode_to_mp4a(const char *src, const char *dst) {
    bool ret = true;
    // Allocate and init re-usable frames
    AVCodecContext *fileCodecContext = nullptr, *audioCodecContext = nullptr;
    AVFormatContext *formatContext = nullptr, *outContext = nullptr;
    AVStream *audioStream;
    SwrContext *swrContext = nullptr;
    AVFrame *audioFrameDecoded = nullptr;
    AVFrame *audioFrameConverted = nullptr;
    AVPacket *inPacket = nullptr;
    AVDictionary *dict = nullptr;
    const AVCodec *encoder;
    const AVOutputFormat *fmt;
    int streamId;
    bool frameFinished = false;
    enum AVCodecID cdc = AV_CODEC_ID_AAC;

    int res = avformat_open_input(&formatContext, src, nullptr, nullptr);
    if (res != 0) ERRRET("avformat_open_input")
    res = avformat_find_stream_info(formatContext, nullptr);
    if (res < 0) ERRRET("avformat_find_stream_info")
    const AVCodec *codec;
    res = av_find_best_stream(formatContext, AVMEDIA_TYPE_AUDIO, -1, -1, &codec, 0);
    if (res < 0) ERRRET("av_find_best_stream")
    streamId = res;
    fileCodecContext = avcodec_alloc_context3(codec);
    avcodec_parameters_to_context(fileCodecContext, formatContext->streams[streamId]->codecpar);
    res = avcodec_open2(fileCodecContext, codec, nullptr);
    if (res < 0) ERRRET("avcodec_open2")

    fmt = av_guess_format("mp4", nullptr, nullptr);
    if (!fmt) ERRRET("av_guess_format")
    outContext = avformat_alloc_context();
    outContext->oformat = fmt;

    encoder = avcodec_find_encoder(cdc);
    if (!encoder) ERRRET("encoder")

    audioCodecContext = avcodec_alloc_context3(encoder);
    /* put sample parameters */
    audioCodecContext->bit_rate = 96000;
    audioCodecContext->sample_rate = 44100;
    audioCodecContext->channels = 1;
    audioCodecContext->sample_fmt = encoder->sample_fmts[0];
    audioCodecContext->channel_layout = AV_CH_LAYOUT_MONO;

    // some formats want stream headers to be separate
    if (outContext->oformat->flags & AVFMT_GLOBALHEADER)
        audioCodecContext->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

    av_dict_set(&dict, "strict", "+experimental", 0);
    av_dict_set(&dict, "brand", "mp42", 0);

    audioStream = avformat_new_stream(outContext, encoder);
    if (!audioStream) ERRRET("av_new_stream")

    if (avcodec_open2(audioCodecContext, encoder, &dict) < 0) ERRRET("avcodec_open")

    if (avcodec_parameters_from_context(audioStream->codecpar, audioCodecContext) < 0) {
        ERRRET("Failed to copy context from input to output stream codec context\n")
    }

    res = avio_open2(&outContext->pb, dst, AVIO_FLAG_WRITE, nullptr, nullptr);
    if (res < 0) ERRRET("url_fopen")

    if (avformat_write_header(outContext, &dict) < 0) ERRRET("avformat_write_header")

    // resampling
    swrContext = swr_alloc();
    av_opt_set_channel_layout(swrContext, "in_channel_layout", fileCodecContext->channel_layout,
                              0);
    av_opt_set_channel_layout(swrContext, "out_channel_layout",
                              audioCodecContext->channel_layout, 0);
    av_opt_set_int(swrContext, "in_sample_rate", fileCodecContext->sample_rate, 0);
    av_opt_set_int(swrContext, "out_sample_rate", audioCodecContext->sample_rate, 0);
    av_opt_set_sample_fmt(swrContext, "in_sample_fmt", fileCodecContext->sample_fmt, 0);
    av_opt_set_sample_fmt(swrContext, "out_sample_fmt", audioCodecContext->sample_fmt, 0);
    res = swr_init(swrContext);
    if (res < 0) ERRRET("swr_init")

    audioFrameDecoded = av_frame_alloc();
    if (!audioFrameDecoded) ERRRET("Could not allocate audio frame")

    audioFrameDecoded->format = fileCodecContext->sample_fmt;
    audioFrameDecoded->channel_layout = fileCodecContext->channel_layout;
    audioFrameDecoded->channels = fileCodecContext->channels;
    audioFrameDecoded->sample_rate = fileCodecContext->sample_rate;

    audioFrameConverted = av_frame_alloc();
    if (!audioFrameConverted) ERRRET("Could not allocate audio frame")

    audioFrameConverted->nb_samples = audioCodecContext->frame_size;
    audioFrameConverted->format = audioCodecContext->sample_fmt;
    audioFrameConverted->channel_layout = audioCodecContext->channel_layout;
    audioFrameConverted->channels = audioCodecContext->channels;
    audioFrameConverted->sample_rate = audioCodecContext->sample_rate;

    inPacket = av_packet_alloc();
    if (inPacket == nullptr) {
        ERRRET("Invalid packet")
    }
    inPacket->data = nullptr;
    inPacket->size = 0;

    while (av_read_frame(formatContext, inPacket) >= 0) {
        if (inPacket->stream_index == streamId) {
            if (decode_packet(fileCodecContext, audioFrameDecoded,
                              inPacket, frameFinished) < 0) {
                ERRRET("Could not decode_packet")
            }

            if (frameFinished) {

                // Convert

                uint8_t *convertedData = nullptr;

                if (av_samples_alloc(&convertedData,
                                     nullptr,
                                     audioCodecContext->channels,
                                     audioFrameConverted->nb_samples,
                                     audioCodecContext->sample_fmt, 0) < 0) ERRRET(
                        "Could not allocate samples")

                int outSamples = swr_convert(swrContext, nullptr, 0,
                        //&convertedData,
                        //audioFrameConverted->nb_samples,
                                             (const uint8_t **) audioFrameDecoded->data,
                                             audioFrameDecoded->nb_samples);
                if (outSamples < 0) ERRRET("Could not convert")

                for (;;) {
                    outSamples = swr_get_out_samples(swrContext, 0);
                    if (outSamples <
                        audioCodecContext->frame_size * audioCodecContext->channels)
                        break; // see comments, thanks to @dajuric for fixing this

                    swr_convert(swrContext,
                                &convertedData,
                                audioFrameConverted->nb_samples, nullptr, 0);

                    size_t buffer_size = av_samples_get_buffer_size(nullptr,
                                                                    audioCodecContext->channels,
                                                                    audioFrameConverted->nb_samples,
                                                                    audioCodecContext->sample_fmt,
                                                                    0);
                    if (buffer_size < 0) ERRRET("Invalid buffer size")

                    if (avcodec_fill_audio_frame(audioFrameConverted,
                                                 audioCodecContext->channels,
                                                 audioCodecContext->sample_fmt,
                                                 convertedData,
                                                 buffer_size,
                                                 0) < 0) ERRRET("Could not fill frame")

                    AVPacket *outPacket = av_packet_alloc();
                    if (outPacket == nullptr) {
                        ERRRET("Invalid packet")
                    }
                    outPacket->data = nullptr;
                    outPacket->size = 0;

                    if (encode_frame(audioCodecContext, audioFrameConverted, outPacket,
                                     frameFinished) < 0) {
                        av_packet_free(&outPacket);
                        ERRRET("Error encoding audio frame")
                    }

                    if (frameFinished) {
                        outPacket->stream_index = audioStream->index;

                        if (av_interleaved_write_frame(outContext, outPacket) != 0) {
                            av_packet_free(&outPacket);
                            ERRRET("Error while writing audio frame")
                        }
                    }
                    av_packet_free(&outPacket);
                }
            }
        }
    }
    out:
    if (outContext != nullptr) {
        if (ret) {
            av_write_trailer(outContext);
        }
        avformat_close_input(&outContext);
        avformat_free_context(outContext);
    }
    if (swrContext != nullptr) {
        swr_close(swrContext);
        swr_free(&swrContext);
    }
    av_frame_free(&audioFrameConverted);
    av_frame_free(&audioFrameDecoded);
    av_packet_free(&inPacket);
    avcodec_free_context(&fileCodecContext);
    avformat_close_input(&formatContext);
    avformat_free_context(formatContext);
    avcodec_free_context(&audioCodecContext);
    return ret;
}

/*
static void av_log(void*, int level, const char *fmt, va_list vl)
{
    int android_level = ANDROID_LOG_DEFAULT;
    switch (level) {
        case AV_LOG_QUIET:
            android_level = ANDROID_LOG_DEFAULT;
        case AV_LOG_DEBUG:
            android_level = ANDROID_LOG_DEBUG;
        case AV_LOG_INFO:
        case AV_LOG_TRACE:
            android_level = ANDROID_LOG_INFO;
        case AV_LOG_VERBOSE:
            android_level = ANDROID_LOG_VERBOSE;
        case AV_LOG_WARNING:
            android_level = ANDROID_LOG_WARN;
        case AV_LOG_ERROR:
            android_level = ANDROID_LOG_ERROR;
        case AV_LOG_FATAL:
        case AV_LOG_PANIC:
            android_level = ANDROID_LOG_FATAL;
        default:
            android_level = ANDROID_LOG_DEFAULT;
    }
    __android_log_print(android_level, LOG_TAG, fmt, vl);
}
*/

extern "C" {
JNIEXPORT jboolean
Java_dev_ragnarok_fenrir_module_encoder_ToMp4Audio_encodeToMp4(JNIEnv *env, jobject, jstring input,
                                                               jstring output) {
    //av_log_set_callback(av_log);
    char const *inputString = SafeGetStringUTFChars(env, input, nullptr);
    char const *outputString = SafeGetStringUTFChars(env, output, nullptr);
    bool ret = encode_to_mp4a(inputString, outputString);
    if (inputString != nullptr) {
        env->ReleaseStringUTFChars(input, inputString);
    }
    if (outputString != nullptr) {
        env->ReleaseStringUTFChars(output, outputString);
    }
    return ret;
}
}
