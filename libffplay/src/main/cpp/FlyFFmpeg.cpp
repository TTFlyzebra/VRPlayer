//
// Created by flyzebra on 2018/11/9.
//

#include <unistd.h>
#include <sys/system_properties.h>
#include <regex.h>
#include <dirent.h>

#include "stdatomic.h"
#include "FlyFFmpeg.h"
#include "FfPlayer.h"
#include <cstdio>
#include <cstdlib>
#include <ctime>

FlyFFmpeg::FlyFFmpeg(JavaVM *jvm, JNIEnv *env, jobject thiz) {
    LOGI("%s()", __func__);
    pthread_mutex_init(&mutex, nullptr);
    this->callBack = new CallBack(jvm, env, thiz);
    out_sampleRateInHz = 48000;
    out_channelConfig = AV_CH_LAYOUT_STEREO;
    out_audioFormat = AV_SAMPLE_FMT_S16;
    isRun = false;
    isStop = true;
}

FlyFFmpeg::~FlyFFmpeg() {
    pthread_mutex_destroy(&mutex);
    delete this->callBack;
    LOGI("%s()", __func__);
}

int FlyFFmpeg::interrupt_cb(void *ctx) {
    if (isStop) {
        LOGE("FlyFFmpeg interrupt_cb, will exit! \n");
        return 1;
    }
    return 0;
}

int FlyFFmpeg::open(const char *url, int64_t pos) {
    LOGI("%s()", __func__);
    isStop = false;
    start_pos = pos;
    sprintf(playUrl, "%s", url);
    int ret = pthread_create(&open_tid, nullptr, _topen, (void *) this);
    if (ret != 0) {
        return -1;
    } else {
        return 0;
    }

}

void *FlyFFmpeg::_topen(void *arg) {
    LOGI("%s()", __func__);
    auto *p = (FlyFFmpeg *) arg;
    p->_runopen((void *) arg);
    return nullptr;
}

int FlyFFmpeg::_runopen(void *arg) {
    LOGI("%s()", __func__);
    auto *p = (FlyFFmpeg *) arg;
    int v_format = 0;
    int v_width = 0;
    int v_height = 0;
    int videoStream = -1;
    int audioStream = -1;
    int frame_rate = 30000;

    LOGI("play url %s", p->playUrl);
    isRun = true;
    av_register_all();
    avformat_network_init();
    pFormatCtx = avformat_alloc_context();
    pFormatCtx->interrupt_callback.callback = interrupt_cb;
    pFormatCtx->interrupt_callback.opaque = p;

    AVDictionary *avdic = nullptr;
    av_dict_set(&avdic, "stimeout", "3000000", 0);//设置超时3秒
    av_dict_set(&avdic, "rtsp_transport", "tcp", 0);
    av_dict_set(&avdic, "probesize", "100*1024", 0);
    av_dict_set(&avdic, "max_analyze_duration", "5 * AV_TIME_BASE", 0);
    int ret = avformat_open_input(&pFormatCtx, p->playUrl, nullptr, &avdic);
    av_dict_free(&avdic);
    if (ret != 0) {
        LOGE("Couldn't open file %s: (ret:%d)", p->playUrl, ret);
        isRun = false;
        callBack->javaOnError(-1);
        return -1;
    }

    if (avformat_find_stream_info(pFormatCtx, nullptr) < 0) {
        LOGE("Could't find stream infomation.");
        isRun = false;
        callBack->javaOnError(-1);
        return -1;
    }

    for (int i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            LOGD("codec_id=%d, extradata_size=%d", pFormatCtx->streams[i]->codecpar->codec_id,
                 pFormatCtx->streams[i]->codecpar->extradata_size);

            uint8_t *pextradata = pFormatCtx->streams[i]->codecpar->extradata;
            int extradata_size = pFormatCtx->streams[i]->codecpar->extradata_size;
            //sps, pps
            if (pFormatCtx->streams[i]->codecpar->codec_id != 28
                || extradata_size <= 0) {
                LOGE("video format[%d] not supported", pFormatCtx->streams[i]->codecpar->codec_id);
                callBack->javaOnError(1);
                break;
            }

            //fps
            frame_rate = (int) ((double) (pFormatCtx->streams[i]->avg_frame_rate.num) /
                                (double) (pFormatCtx->streams[i]->avg_frame_rate.den) * 1000);
            if (frame_rate <= 0) {
                frame_rate = 30000;
            }
            LOGD("frame_rate:%d", frame_rate);
            //width，height
            v_format = pFormatCtx->streams[i]->codecpar->codec_id;
            v_width = pFormatCtx->streams[i]->codecpar->width;
            v_height = pFormatCtx->streams[i]->codecpar->height;
            LOGD("format=%d, width=%d, heigh=%d", v_format, v_width, v_height);

            //sps pps
            char temp[1024] = {0};
            for (int j = 0; j < extradata_size; j++) {
                sprintf(temp, "%s0x%02x,", temp, pextradata[j]);
            }
            LOGD("extradata:%s", temp);
            int spsLen = 0;
            int ppsLen = 0;
            is_avcc = pextradata[0] == 0x01;
            if (is_avcc) {
                spsLen = pextradata[7] + sizeof(h264Start);
                ppsLen = pextradata[10 + spsLen - sizeof(h264Start)] + sizeof(h264Start);
                sps = (u_char *) malloc((spsLen) * sizeof(u_char));
                pps = (u_char *) malloc((ppsLen) * sizeof(u_char));
                memcpy(sps, h264Start, sizeof(h264Start));
                memcpy(pps, h264Start, sizeof(h264Start));
                memcpy(sps + sizeof(h264Start), pextradata + 8, spsLen - sizeof(h264Start));
                memcpy(pps + sizeof(h264Start), pextradata + 11 + spsLen - sizeof(h264Start),
                       ppsLen - sizeof(h264Start));
            } else {
                int sps_start = 0;
                int sps_end = 0;
                int pps_start = 0;
                int pps_end = 0;
                for (int j = 0; j < extradata_size; j++) {
                    if (pextradata[j]==0x67) {
                        if (j > 2 && pextradata[j-1] == 0x01 && pextradata[j -2] == 0x00 && pextradata[j - 3] == 0x00) {
                            sps_start = j;
                        }
                    }else if (pextradata[j]==0x68) {
                        if (j > 3 && pextradata[j-1] == 0x01 && pextradata[j -2] == 0x00 && pextradata[j - 3] == 0x00) {
                            pps_start = j;
                            sps_end = j - (((pextradata[j -4]==0x00))?5:4);
                        }
                    }
                    if (pps_start != 0) {
                        if (pextradata[j] == 0x00) {
                            pps_end = j - 1;
                            break;
                        }
                    }
                    if (j == extradata_size - 1) {
                        pps_end = j;
                    }
                }
                if (sps_start == 0 || pps_start == 0) {
                    LOGE("find sps pps error!");
                    callBack->javaOnError(1);
                    break;
                }
                spsLen = sps_end - sps_start + 1 + sizeof(h264Start);
                ppsLen = extradata_size - pps_start + sizeof(h264Start);
                sps = (u_char *) malloc((spsLen) * sizeof(u_char));
                pps = (u_char *) malloc((ppsLen) * sizeof(u_char));
                memcpy(sps, h264Start, sizeof(h264Start));
                memcpy(pps, h264Start, sizeof(h264Start));
                memcpy(sps + sizeof(h264Start), pextradata + sps_start, spsLen - sizeof(h264Start));
                memcpy(pps + sizeof(h264Start), pextradata + pps_start, ppsLen - sizeof(h264Start));
            }
            memset(temp, 0, 1024);
            for (int j = 0; j < spsLen; j++) {
                sprintf(temp, "%s0x%02x,", temp, sps[j]);
            }
            LOGD("sps:%s", temp);
            memset(temp, 0, 1024);
            for (int j = 0; j < ppsLen; j++) {
                sprintf(temp, "%s0x%02x,", temp, pps[j]);
            }
            LOGD("pps:%s", temp);
            callBack->javaOnVideoStart(v_format, v_width, v_height, frame_rate,
                                       (const u_char *) (sps + sizeof(h264Start)), spsLen - sizeof(h264Start),
                                       (const u_char *) (pps + sizeof(h264Start)), ppsLen - sizeof(h264Start));
            callBack->javaOnVideoEncode(sps, v_width, v_height, spsLen, 0, 0);
            callBack->javaOnVideoEncode(pps, v_width, v_height, ppsLen, 0, 0);

            AVCodecParameters *pCodecPar_video = pFormatCtx->streams[i]->codecpar;
            AVCodec *pCodec_video = avcodec_find_decoder(pCodecPar_video->codec_id);

            if (pCodec_video != nullptr) {
                pCodecCtx_video = avcodec_alloc_context3(pCodec_video);
                ret = avcodec_parameters_to_context(pCodecCtx_video, pCodecPar_video);
                if (ret >= 0) {
                    if (avcodec_open2(pCodecCtx_video, pCodec_video, nullptr) >= 0) {
                        videoStream = i;
                        break;
                    } else {
                        LOGE("Could not open video decodec.");
                    }
                } else {
                    LOGE("avcodec_parameters_to_context() failed %d", ret);
                }
            } else {
                LOGE(" not found video decodec.");
            }
        }
    }

    for (int i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            AVCodecParameters *pCodecPar_audio = pFormatCtx->streams[i]->codecpar;
            AVCodec *pCodec_audio = avcodec_find_decoder(pCodecPar_audio->codec_id);
            if (pCodec_audio != nullptr) {
                pCodecCtx_audio = avcodec_alloc_context3(pCodec_audio);
                ret = avcodec_parameters_to_context(pCodecCtx_audio, pCodecPar_audio);
                if (ret >= 0) {
                    if (avcodec_open2(pCodecCtx_audio, pCodec_audio, nullptr) >= 0) {
                        LOGD("find audioStream = %d, sampleRateInHz = %d, channelConfig=%d, audioFormat=%d",
                             i, pCodecCtx_audio->sample_rate, pCodecCtx_audio->channel_layout,
                             pCodecCtx_audio->sample_fmt);
                        int in_ch_layout = pCodecCtx_audio->channel_layout;
                        if (pCodecCtx_audio->sample_fmt == 1) {
                            if (pCodecCtx_audio->channels > 1) {
                                in_ch_layout = AV_CH_LAYOUT_STEREO;
                            } else {
                                in_ch_layout = AV_CH_LAYOUT_MONO;
                            }
                        }
                        swr_cxt = swr_alloc();
                        swr_alloc_set_opts(
                                swr_cxt,
                                out_channelConfig,
                                (AVSampleFormat) out_audioFormat,
                                out_sampleRateInHz,
                                in_ch_layout,
                                pCodecCtx_audio->sample_fmt,
                                pCodecCtx_audio->sample_rate,
                                0,
                                nullptr);
                        swr_init(swr_cxt);
                        audio_buf = (uint8_t *) av_malloc(out_sampleRateInHz * 16);
                        callBack->javaOnAudioStart(out_sampleRateInHz, out_channelConfig, out_audioFormat);
                        audioStream = i;
                    } else {
                        avcodec_close(pCodecCtx_audio);
                        LOGE("init audio codec failed 3!");
                    }
                } else {
                    LOGE("init audio codec failed 2!");
                }
            } else {
                LOGE("init audio codec failed 1!");
            }
            break;
        }
    }

    if (videoStream == -1 && audioStream == -1) {
        LOGE("not find vedio stream or audio stream.");
        isRun = false;
        callBack->javaOnError(-1);
        return -1;
    }

    //首次随机取帧播放
    int totalSec = static_cast<int>(pFormatCtx->duration / AV_TIME_BASE);
    LOGD("play time [%d][%dmin%dsec].", pFormatCtx->duration, totalSec / 60, totalSec % 60);
    if (start_pos > 0 && totalSec > 0) {
        int64_t playRandTime = totalSec * start_pos / 100;
        LOGD("first play will rand seek to %ds.", playRandTime);
        av_seek_frame(pFormatCtx, -1, playRandTime * AV_TIME_BASE, AVSEEK_FLAG_BACKWARD);
    }

    frame = av_frame_alloc();
    packet = (AVPacket *) av_malloc(sizeof(AVPacket)); //分配一个packet
    while (!isStop && av_read_frame(pFormatCtx, packet) >= 0) {
        bool exit = false;
        if (packet->stream_index == videoStream) {
            if (is_avcc) {
                int ptr = 0;
                while (!isStop && (ptr + 4) < packet->size) {
                    int dataLen = (uint32_t) (packet->data[0 + ptr] << 24 & 0xff000000) +
                                  (uint32_t) (packet->data[1 + ptr] << 16 & 0x00ff0000) +
                                  (uint32_t) (packet->data[2 + ptr] << 8 & 0x0000ff00) +
                                  (uint32_t) (packet->data[3 + ptr] & 0x000000ff);
                    if (dataLen > (packet->size - ptr - 4)) {
                        int lognum = 16;
                        if (packet->size - (ptr + 1) < lognum) {
                            lognum = packet->size - (ptr + 1);
                        }
                        char temp[1024] = {0};
                        for (int j = 0; j < lognum; j++) {
                            sprintf(temp, "%s0x%02x,", temp, packet->data[j + ptr]);
                        }
                        LOGE("read error data=%s[%d][%d][%d]", temp, dataLen, packet->size, ptr);
                        exit = true;
                        break;
                    }
                    auto *video = (uint8_t *) av_malloc((dataLen + sizeof(h264Start)) * sizeof(uint8_t));
                    memcpy(video, h264Start, sizeof(h264Start));
                    memcpy(video + sizeof(h264Start), packet->data + ptr + 4, dataLen);
                    if (!isStop) {
                        callBack->javaOnVideoEncode(video, v_width, v_height, dataLen + sizeof(h264Start),
                                                    (1000000 / frame_rate), (1000000 / frame_rate));
                    }
                    av_free(video);
                    ptr = ptr + 4 + dataLen;
                }
            } else {
                auto *video = (uint8_t *) av_malloc((packet->size) * sizeof(uint8_t));
                memcpy(video, packet->data, packet->size);
                if (!isStop) {
                    callBack->javaOnVideoEncode(video, v_width, v_height, packet->size + sizeof(h264Start),
                                                packet->dts, packet->pts);
                }
                av_free(video);
            }
        } else if (packet->stream_index == audioStream) {
            ret = avcodec_send_packet(pCodecCtx_audio, packet);
            while (ret >= 0) {
                ret = avcodec_receive_frame(pCodecCtx_audio, frame);
                if (ret >= 0) {
                    int64_t delay = swr_get_delay(swr_cxt, frame->sample_rate);
                    int64_t out_count = av_rescale_rnd(
                            frame->nb_samples + delay,
                            out_sampleRateInHz,
                            frame->sample_rate,
                            AV_ROUND_UP);
                    int retLen = swr_convert(
                            swr_cxt,
                            &audio_buf,
                            out_count,
                            (const uint8_t **) frame->data,
                            frame->nb_samples);
                    if (retLen > 0) {
                        callBack->javaOnAudioDecode(audio_buf, retLen * 4);
                        //LOGE("frame->linesize[0]=%d, frame->nb_samples=%d,retLen=%d, delay=%lld,
                        //out_count=%lld",frame->linesize[0],frame->nb_samples,retLen,delay,out_count);
                    } else {
                        LOGE("frame->linesize[0]=%d, frame->nb_samples=%d,retLen=%d, delay=%lld,out_count=%lld",
                             frame->linesize[0], frame->nb_samples, retLen, delay, out_count);
                        callBack->javaOnAudioDecode(frame->data[0], frame->linesize[0]);
                    }

                }
            }
        }
        av_packet_unref(packet);
        if (exit) {
            break;
        }
    }
    if (sps != nullptr) {
        LOGD("free sps.");
        free(sps);
    }
    if (pps) {
        LOGD("free pps.");
        free(pps);
    }
    if (swr_cxt) {
        LOGD("swr_free swr_cxt.");
        swr_free(&swr_cxt);
    }
    if (audio_buf) {
        LOGD("av_free audio_buf.");
        av_free(audio_buf);
    }
    if (packet) {
        LOGD("av_free packet.");
        av_free(packet);
    }
    if (frame) {
        LOGD("av_frame_free frame.");
        av_frame_free(&frame);
    }
    if (pCodecCtx_video) {
        LOGD("avcodec_close pCodecCtx_video.");
        avcodec_close(pCodecCtx_video);
    }
    if (pCodecCtx_audio) {
        LOGD("avcodec_close pCodecCtx_audio.");
        avcodec_close(pCodecCtx_audio);
    }
    if (pFormatCtx) {
        LOGD("avformat_close_input pFormatCtx.");
        avformat_close_input(&pFormatCtx);
    }
    isRun = false;
    if (!isStop) {
        callBack->javaOnComplete();
    }
    return 0;
}

int FlyFFmpeg::close(FlyFFmpeg *p) {
    isStop = true;
    while (isRun) {
        usleep(20000);
    }
    return 0;
}


