#include <jni.h>
#include <android/bitmap.h>
#include <cstdint>
#include <limits>
#include <string>
#include <unistd.h>
#include <linux/stat.h>
#include <asm/fcntl.h>
#include <fcntl.h>
#include <libyuv.h>
#include <jni.h>
#include "fenrir_native.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include "libavutil/display.h"
#include <libavutil/eval.h>
#include <libswscale/swscale.h>
}

static inline std::string av_make_error_str_animation(int errnum) {
    char errbuf[AV_ERROR_MAX_STRING_SIZE];
    av_strerror(errnum, errbuf, AV_ERROR_MAX_STRING_SIZE);
    return (std::string) errbuf;
}

#undef av_err2str
#define av_err2str(errnum) av_make_error_str_animation(errnum).c_str()

/*
inline void print_ffmpeg_error(int error) {
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s\n", av_err2str(error));
}
*/

class VideoInfo {
public:
    ~VideoInfo() {
        if (video_dec_ctx) {
            avcodec_free_context(&video_dec_ctx);
            video_dec_ctx = nullptr;
        }
        if (fmt_ctx) {
            avformat_close_input(&fmt_ctx);
            fmt_ctx = nullptr;
        }
        if (frame) {
            av_frame_free(&frame);
            frame = nullptr;
        }
        if (src) {
            delete[] src;
            src = nullptr;
        }
        if (ioContext != nullptr) {
            if (ioContext->buffer) {
                av_freep(&ioContext->buffer);
            }
            avio_context_free(&ioContext);
            ioContext = nullptr;
        }
        if (sws_ctx != nullptr) {
            sws_freeContext(sws_ctx);
            sws_ctx = nullptr;
        }

        if (pkt != nullptr) {
            av_packet_free(&pkt);
            pkt = nullptr;
        }

        orig_pkt_data = nullptr;
        orig_pkt_size = 0;

        video_stream_idx = -1;
        video_stream = nullptr;
    }

    void backup_pkt_buffer() {
        if (pkt != nullptr) {
            orig_pkt_data = pkt->data;
            orig_pkt_size = pkt->size;
        }
    }

    void restore_pkt_buffer() {
        if (pkt != nullptr) {
            pkt->data = orig_pkt_data;
            pkt->size = orig_pkt_size;

            orig_pkt_data = nullptr;
            orig_pkt_size = 0;
        }
    }

    AVFormatContext *fmt_ctx = nullptr;
    char *src = nullptr;
    int video_stream_idx = -1;
    AVStream *video_stream = nullptr;
    AVCodecContext *video_dec_ctx = nullptr;
    AVFrame *frame = nullptr;
    bool has_decoded_frames = false;

    AVPacket *pkt = av_packet_alloc();
    uint8_t *orig_pkt_data = nullptr;
    int orig_pkt_size = 0;

    bool stopped = false;
    bool seeking = false;

    int32_t dst_linesize[1] = {};

    struct SwsContext *sws_ctx = nullptr;

    AVIOContext *ioContext = nullptr;
};

int
open_codec_context_animation(int *stream_idx, AVCodecContext **dec_ctx, AVFormatContext *fmt_ctx,
                             enum AVMediaType type) {
    int ret, stream_index;
    AVStream *st;
    const AVCodec *dec;
    AVDictionary *opts = nullptr;

    ret = av_find_best_stream(fmt_ctx, type, -1, -1, nullptr, 0);
    if (ret < 0) {
        LOGE("can't find %s stream in input file", av_get_media_type_string(type));
        return ret;
    } else {
        stream_index = ret;
        st = fmt_ctx->streams[stream_index];

        dec = avcodec_find_decoder(st->codecpar->codec_id);
        if (!dec) {
            LOGE("failed to find %d codec", st->codecpar->codec_id);
            return AVERROR(EINVAL);
        }

        *dec_ctx = avcodec_alloc_context3(dec);
        if (!*dec_ctx) {
            LOGE("Failed to allocate the %s codec context", av_get_media_type_string(type));
            return AVERROR(ENOMEM);
        }

        if ((ret = avcodec_parameters_to_context(*dec_ctx, st->codecpar)) < 0) {
            LOGE("Failed to copy %s codec parameters to decoder context",
                 av_get_media_type_string(type));
            return ret;
        }

        av_dict_set(&opts, "refcounted_frames", "1", 0);
        if ((ret = avcodec_open2(*dec_ctx, dec, &opts)) < 0) {
            LOGE("Failed to open %s codec", av_get_media_type_string(type));
            return ret;
        }
        *stream_idx = stream_index;
    }

    return 0;
}

int decode_packet_animation(VideoInfo *info, bool &got_frame) {
    int ret;
    int decoded = info->pkt->size;
    got_frame = false;

    if (info->pkt->stream_index == info->video_stream_idx) {
        while (decoded > 0) {
            ret = avcodec_send_packet(info->video_dec_ctx, info->pkt);
            if (ret < 0 && ret != AVERROR(EAGAIN)) {
                return ret;
            }
            if (ret >= 0) {
                ret = avcodec_receive_frame(info->video_dec_ctx, info->frame);
                if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                    return 0;
                } else if (ret < 0) {
                    return ret;
                }
                got_frame = true;
                return info->pkt->size;
            }
            decoded = info->pkt->size;
        }
    }

    return decoded;
}

jlong createDecoder_animation(JNIEnv *env, jstring src,
                              jintArray data) {
    auto *info = new VideoInfo();

    char const *srcString = SafeGetStringUTFChars(env, src, nullptr);
    size_t len = strlen(srcString);
    info->src = new char[len + 1];
    memcpy(info->src, srcString, len);
    info->src[len] = '\0';
    if (srcString != nullptr) {
        env->ReleaseStringUTFChars(src, srcString);
    }

    if (avformat_open_input(&info->fmt_ctx, info->src, nullptr, nullptr) < 0) {
        LOGE("can't open source file %s", info->src);
        delete info;
        return 0;
    }

    if (avformat_find_stream_info(info->fmt_ctx, nullptr) < 0) {
        LOGE("can't find stream information %s", info->src);
        delete info;
        return 0;
    }

    if (open_codec_context_animation(&info->video_stream_idx, &info->video_dec_ctx, info->fmt_ctx,
                                     AVMEDIA_TYPE_VIDEO) >= 0) {
        info->video_stream = info->fmt_ctx->streams[info->video_stream_idx];
    }

    if (info->video_stream == nullptr) {
        LOGE("can't find video stream in the input, aborting %s", info->src);
        delete info;
        return 0;
    }

    info->frame = av_frame_alloc();
    if (info->frame == nullptr) {
        LOGE("can't allocate frame %s", info->src);
        delete info;
        return 0;
    }

    jint *dataArr = env->GetIntArrayElements(data, nullptr);
    if (dataArr != nullptr) {
        dataArr[0] = info->video_dec_ctx->width;
        dataArr[1] = info->video_dec_ctx->height;
        //float pixelWidthHeightRatio = info->video_dec_ctx->sample_aspect_ratio.num / info->video_dec_ctx->sample_aspect_ratio.den; TODO support
        AVDictionaryEntry *rotate_tag = av_dict_get(info->video_stream->metadata, "rotate", nullptr,
                                                    0);
        if (rotate_tag && *rotate_tag->value && strcmp(rotate_tag->value, "0") != 0) {
            char *tail;
            dataArr[2] = (jint) av_strtod(rotate_tag->value, &tail);
            if (*tail) {
                dataArr[2] = 0;
            }
        } else {
            const AVPacketSideData *psd = av_packet_side_data_get(
                    info->video_stream->codecpar->coded_side_data,
                    info->video_stream->codecpar->nb_coded_side_data,
                    AV_PKT_DATA_DISPLAYMATRIX);
            if (psd && psd->data) {
                dataArr[2] = (int) -av_display_rotation_get((int32_t *) psd->data);
            } else {
                dataArr[2] = 0;
            }
        }
        dataArr[4] = (int32_t) (info->fmt_ctx->duration * 1000 / AV_TIME_BASE);
        int video_stream_index = -1;
        double fps = 30.0;
        for (int i = 0; i < info->fmt_ctx->nb_streams; i++) {
            if (info->fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
                video_stream_index = i;
                break;
            }
        }
        if (video_stream_index != -1) {
            AVStream *video_stream = info->fmt_ctx->streams[video_stream_index];
            if (video_stream->avg_frame_rate.den && video_stream->avg_frame_rate.num) {
                fps = av_q2d(video_stream->avg_frame_rate);
            } else if (video_stream->r_frame_rate.den && video_stream->r_frame_rate.num) {
                fps = av_q2d(video_stream->r_frame_rate);
            } else {
                fps = 1.0 / (av_q2d(video_stream->time_base));
            }
        }
        dataArr[5] = (int32_t) fps;
        //(int32_t) (1000 * info->video_stream->duration * av_q2d(info->video_stream->time_base));
        env->ReleaseIntArrayElements(data, dataArr, 0);
    }

    //LOGD("successfully opened file %s", info->src);

    return reinterpret_cast<jlong>(info);
}

extern "C" JNIEXPORT jlong JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileDrawable_createDecoder(JNIEnv *env, jobject,
                                                                             jstring src,
                                                                             jintArray data) {
    return createDecoder_animation(env, src, data);
}

extern "C" JNIEXPORT jlong JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileFrame_createDecoder(JNIEnv *env, jobject,
                                                                          jstring src,
                                                                          jintArray data) {
    return createDecoder_animation(env, src, data);
}

void destroyDecoder_animation(jlong ptr) {
    if (ptr == 0) {
        return;
    }
    delete reinterpret_cast<VideoInfo *>(ptr);
}

extern "C" JNIEXPORT void JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileDrawable_destroyDecoder(JNIEnv *, jobject,
                                                                              jlong ptr) {
    destroyDecoder_animation(ptr);
}

extern "C" JNIEXPORT void JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileFrame_destroyDecoder(JNIEnv *, jobject,
                                                                           jlong ptr) {
    destroyDecoder_animation(ptr);
}

extern "C" JNIEXPORT void JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileDrawable_stopDecoder(JNIEnv *, jobject,
                                                                           jlong ptr) {
    if (ptr == 0) {
        return;
    }
    auto *info = reinterpret_cast<VideoInfo *>(ptr);
    info->stopped = true;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileDrawable_prepareToSeek(JNIEnv *, jobject,
                                                                             jlong ptr) {
    if (ptr == 0) {
        return;
    }
    auto *info = reinterpret_cast<VideoInfo *>(ptr);
    info->seeking = true;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileDrawable_seekToMs(JNIEnv *, jobject,
                                                                        jlong ptr,
                                                                        jlong ms,
                                                                        jboolean precise) {
    if (ptr == 0) {
        return;
    }
    auto *info = reinterpret_cast<VideoInfo *>(ptr);
    info->seeking = false;
    auto pts = (int64_t) ((double) ms / av_q2d(info->video_stream->time_base) / 1000);
    int ret;
    if ((ret = av_seek_frame(info->fmt_ctx, info->video_stream_idx, pts,
                             AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME)) < 0) {
        LOGE("can't seek file %s, %s", info->src, av_err2str(ret));
        return;
    } else {
        avcodec_flush_buffers(info->video_dec_ctx);
        if (!precise) {
            return;
        }
        bool got_frame = false;
        int32_t tries = 100;
        while (tries > 0) {
            if (info->pkt->size == 0) {
                ret = av_read_frame(info->fmt_ctx, info->pkt);
                if (ret >= 0) {
                    info->backup_pkt_buffer();
                }
            }

            if (info->pkt->size > 0) {
                ret = decode_packet_animation(info, got_frame);
                if (ret < 0) {
                    if (info->has_decoded_frames) {
                        ret = 0;
                    }
                    info->pkt->size = 0;
                } else {
                    info->pkt->data += ret;
                    info->pkt->size -= ret;
                }
                if (info->pkt->size == 0) {
                    info->restore_pkt_buffer();
                    av_packet_unref(info->pkt);
                }
            } else {
                info->pkt->data = nullptr;
                info->pkt->size = 0;
                ret = decode_packet_animation(info, got_frame);
                if (ret < 0) {
                    return;
                }
                if (!got_frame) {
                    av_seek_frame(info->fmt_ctx, info->video_stream_idx, 0,
                                  AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
                    return;
                }
            }
            if (ret < 0) {
                return;
            }
            if (got_frame) {
                info->has_decoded_frames = true;
                bool finished = false;
                if (info->frame->format == AV_PIX_FMT_YUV444P ||
                    info->frame->format == AV_PIX_FMT_YUV420P ||
                    info->frame->format == AV_PIX_FMT_BGRA ||
                    info->frame->format == AV_PIX_FMT_YUVJ420P) {
                    int64_t pkt_pts = info->frame->best_effort_timestamp;
                    if (pkt_pts >= pts) {
                        finished = true;
                    }
                }
                av_frame_unref(info->frame);
                if (finished) {
                    return;
                }
            }
            tries--;
        }
    }
}

void writeFrameToBitmap_animation(JNIEnv *env, VideoInfo *info, jintArray data, jobject bitmap,
                                  jint stride) {
    jint *dataArr = env->GetIntArrayElements(data, nullptr);
    int32_t wantedWidth;
    int32_t wantedHeight;

    AndroidBitmapInfo bitmapInfo;
    AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
    auto bitmapWidth = (int32_t) bitmapInfo.width;
    auto bitmapHeight = (int32_t) bitmapInfo.height;
    if (dataArr != nullptr) {
        wantedWidth = dataArr[0];
        wantedHeight = dataArr[1];
        dataArr[3] = (jint) (1000 * (double) info->frame->best_effort_timestamp *
                             av_q2d(info->video_stream->time_base));
        env->ReleaseIntArrayElements(data, dataArr, 0);
    } else {
        wantedWidth = bitmapWidth;
        wantedHeight = bitmapHeight;
    }

    if ((wantedWidth == info->frame->width && wantedHeight == info->frame->height) ||
        (wantedWidth == info->frame->height && wantedHeight == info->frame->width)) {
        void *pixels;
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
            if (info->sws_ctx == nullptr) {
                if (info->frame->format > AV_PIX_FMT_NONE && info->frame->format < AV_PIX_FMT_NB &&
                    info->frame->format != AV_PIX_FMT_YUVA420P) {
                    info->sws_ctx = sws_getContext(info->frame->width, info->frame->height,
                                                   (AVPixelFormat) info->frame->format, bitmapWidth,
                                                   bitmapHeight, AV_PIX_FMT_RGBA, SWS_BILINEAR,
                                                   nullptr, nullptr, nullptr);
                } else if (info->video_dec_ctx->pix_fmt > AV_PIX_FMT_NONE &&
                           info->video_dec_ctx->pix_fmt < AV_PIX_FMT_NB &&
                           info->frame->format != AV_PIX_FMT_YUVA420P) {
                    info->sws_ctx = sws_getContext(info->video_dec_ctx->width,
                                                   info->video_dec_ctx->height,
                                                   info->video_dec_ctx->pix_fmt, bitmapWidth,
                                                   bitmapHeight, AV_PIX_FMT_RGBA, SWS_BILINEAR,
                                                   nullptr, nullptr, nullptr);
                }
            }
            if (info->sws_ctx == nullptr || ((intptr_t) pixels) % 16 != 0) {
                if (info->frame->format == AV_PIX_FMT_YUVA420P) {
                    libyuv::I420AlphaToARGBMatrix(info->frame->data[0], info->frame->linesize[0],
                                                  info->frame->data[2], info->frame->linesize[2],
                                                  info->frame->data[1], info->frame->linesize[1],
                                                  info->frame->data[3], info->frame->linesize[3],
                                                  (uint8_t *) pixels, bitmapWidth * 4,
                                                  &libyuv::kYvuI601Constants, bitmapWidth,
                                                  bitmapHeight, 1);
                } else if (info->frame->format == AV_PIX_FMT_YUV444P) {
                    libyuv::H444ToARGB(info->frame->data[0], info->frame->linesize[0],
                                       info->frame->data[2], info->frame->linesize[2],
                                       info->frame->data[1], info->frame->linesize[1],
                                       (uint8_t *) pixels, bitmapWidth * 4, bitmapWidth,
                                       bitmapHeight);
                } else if (info->frame->format == AV_PIX_FMT_YUV420P ||
                           info->frame->format == AV_PIX_FMT_YUVJ420P) {
                    if (info->frame->colorspace == AVColorSpace::AVCOL_SPC_BT709) {
                        libyuv::H420ToARGB(info->frame->data[0], info->frame->linesize[0],
                                           info->frame->data[2], info->frame->linesize[2],
                                           info->frame->data[1], info->frame->linesize[1],
                                           (uint8_t *) pixels, bitmapWidth * 4, bitmapWidth,
                                           bitmapHeight);
                    } else {
                        libyuv::I420ToARGB(info->frame->data[0], info->frame->linesize[0],
                                           info->frame->data[2], info->frame->linesize[2],
                                           info->frame->data[1], info->frame->linesize[1],
                                           (uint8_t *) pixels, bitmapWidth * 4, bitmapWidth,
                                           bitmapHeight);
                    }
                } else if (info->frame->format == AV_PIX_FMT_BGRA) {
                    libyuv::ABGRToARGB(info->frame->data[0], info->frame->linesize[0],
                                       (uint8_t *) pixels, info->frame->width * 4,
                                       info->frame->width, info->frame->height);
                }
            } else {
                uint8_t __attribute__ ((aligned (16))) *dst_data[1];
                dst_data[0] = (uint8_t *) pixels;
                info->dst_linesize[0] = stride;
                sws_scale(info->sws_ctx, info->frame->data, info->frame->linesize, 0,
                          info->frame->height, dst_data, info->dst_linesize);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }
}

jint getFrameAtTime_animation(JNIEnv *env, jlong ptr, jlong ms,
                              jobject bitmap,
                              jintArray data,
                              jint stride) {
    if (ptr == 0 || bitmap == nullptr || data == nullptr) {
        return 0;
    }
    auto *info = reinterpret_cast<VideoInfo *>(ptr);
    info->seeking = false;
    auto pts = (int64_t) ((double) ms / av_q2d(info->video_stream->time_base) / 1000);
    int ret;
    if ((ret = av_seek_frame(info->fmt_ctx, info->video_stream_idx, pts,
                             AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME)) < 0) {
        LOGE("can't seek file %s, %s", info->src, av_err2str(ret));
        return 0;
    } else {
        avcodec_flush_buffers(info->video_dec_ctx);
        bool got_frame = false;
        int32_t tries = 1000;
        bool readNextPacket = true;
        while (tries > 0) {
            if (info->pkt->size == 0 && readNextPacket) {
                ret = av_read_frame(info->fmt_ctx, info->pkt);
                if (ret >= 0) {
                    info->backup_pkt_buffer();
                }
            }

            if (info->pkt->size > 0) {
                ret = decode_packet_animation(info, got_frame);
                if (ret < 0) {
                    if (info->has_decoded_frames) {
                        ret = 0;
                    }
                    info->pkt->size = 0;
                } else {
                    info->pkt->data += ret;
                    info->pkt->size -= ret;
                }
                if (info->pkt->size == 0) {
                    info->restore_pkt_buffer();
                    av_packet_unref(info->pkt);
                }
            } else {
                info->pkt->data = nullptr;
                info->pkt->size = 0;
                ret = decode_packet_animation(info, got_frame);
                if (ret < 0) {
                    return 0;
                }
                if (!got_frame) {
                    av_seek_frame(info->fmt_ctx, info->video_stream_idx, 0,
                                  AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
                    return 0;
                }
            }
            if (ret < 0) {
                return 0;
            }
            if (got_frame) {
                bool finished = false;
                if (info->frame->format == AV_PIX_FMT_YUV444P ||
                    info->frame->format == AV_PIX_FMT_YUV420P ||
                    info->frame->format == AV_PIX_FMT_BGRA ||
                    info->frame->format == AV_PIX_FMT_YUVJ420P) {
                    int64_t pkt_pts = info->frame->best_effort_timestamp;
                    bool isLastPacket = false;
                    if (info->pkt->size == 0) {
                        readNextPacket = false;
                        isLastPacket = av_read_frame(info->fmt_ctx, info->pkt) < 0;
                    }
                    if (pkt_pts >= pts || isLastPacket) {
                        writeFrameToBitmap_animation(env, info, data, bitmap, stride);
                        finished = true;
                    }
                }
                av_frame_unref(info->frame);
                if (finished) {
                    return 1;
                }
            } else {
                readNextPacket = true;
            }
            tries--;
        }
        return 0;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileDrawable_getFrameAtTime(JNIEnv *env, jobject,
                                                                              jlong ptr, jlong ms,
                                                                              jobject bitmap,
                                                                              jintArray data,
                                                                              jint stride) {
    return getFrameAtTime_animation(env, ptr, ms, bitmap, data, stride);
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileFrame_getFrameAtTime(JNIEnv *env, jobject,
                                                                           jlong ptr, jlong ms,
                                                                           jobject bitmap,
                                                                           jintArray data,
                                                                           jint stride) {
    return getFrameAtTime_animation(env, ptr, ms, bitmap, data, stride);
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_ragnarok_fenrir_module_animation_AnimatedFileDrawable_getVideoFrame(JNIEnv *env, jobject,
                                                                             jlong ptr,
                                                                             jobject bitmap,
                                                                             jintArray data,
                                                                             jint stride,
                                                                             jboolean preview,
                                                                             jfloat start_time,
                                                                             jfloat end_time) {
    if (ptr == 0) {
        return 0;
    }
    //int64_t time = ConnectionsManager::getInstance(0).getCurrentTimeMonotonicMillis();
    auto *info = reinterpret_cast<VideoInfo *>(ptr);
    int ret;
    bool got_frame = false;
    int32_t triesCount = preview ? 50 : 6;
    //info->has_decoded_frames = false;
    while (!info->stopped && triesCount != 0) {
        if (info->pkt->size == 0) {
            ret = av_read_frame(info->fmt_ctx, info->pkt);
            if (ret >= 0) {
                double pts = (double) info->pkt->pts * av_q2d(info->video_stream->time_base);
                if (end_time > 0 && info->pkt->stream_index == info->video_stream_idx &&
                    pts > end_time) {
                    av_packet_unref(info->pkt);
                    info->pkt->data = nullptr;
                    info->pkt->size = 0;
                } else {
                    info->backup_pkt_buffer();
                }
            }
        }

        if (info->pkt->size > 0) {
            ret = decode_packet_animation(info, got_frame);
            if (ret < 0) {
                if (info->has_decoded_frames) {
                    ret = 0;
                }
                info->pkt->size = 0;
            } else {
                //LOGD("read size %d from packet", ret);
                info->pkt->data += ret;
                info->pkt->size -= ret;
            }

            if (info->pkt->size == 0) {
                info->restore_pkt_buffer();
                av_packet_unref(info->pkt);
            }
        } else {
            info->pkt->data = nullptr;
            info->pkt->size = 0;
            ret = decode_packet_animation(info, got_frame);
            if (ret < 0) {
                LOGE("can't decode packet flushed %d %s %s", ret, av_err2str(ret),
                     info->src);
                return 0;
            }
            if (!preview && !got_frame) {
                if (info->has_decoded_frames) {
                    int64_t start_from = 0;
                    if (start_time > 0) {
                        start_from = (int64_t) (start_time / av_q2d(info->video_stream->time_base));
                    }
                    if ((ret = av_seek_frame(info->fmt_ctx, info->video_stream_idx, start_from,
                                             AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME)) < 0) {
                        LOGE("can't seek to begin of file %s, %s", info->src, av_err2str(ret));
                        return 0;
                    } else {
                        avcodec_flush_buffers(info->video_dec_ctx);
                    }
                }
            }
        }
        if (ret < 0 || info->seeking) {
            return 0;
        }
        if (got_frame) {
            //LOGD("decoded frame with w = %d, h = %d, format = %d", info->frame->width, info->frame->height, info->frame->format);
            if (bitmap != nullptr && (info->frame->format == AV_PIX_FMT_YUV420P ||
                                      info->frame->format == AV_PIX_FMT_BGRA ||
                                      info->frame->format == AV_PIX_FMT_YUVJ420P ||
                                      info->frame->format == AV_PIX_FMT_YUV444P ||
                                      info->frame->format == AV_PIX_FMT_YUVA420P)) {
                writeFrameToBitmap_animation(env, info, data, bitmap, stride);
            }
            info->has_decoded_frames = true;
            av_frame_unref(info->frame);
            return 1;
        }
        if (!info->has_decoded_frames) {
            triesCount--;
        }
    }
    return 0;
}
