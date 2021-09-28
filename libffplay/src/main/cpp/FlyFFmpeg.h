//
// Created by flyzebra on 2018/11/9.
//

#ifndef FLYPLAYER_FLYFFMPEG_H
#define FLYPLAYER_FLYFFMPEG_H
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
#include <pthread.h>
}
#include <vector>
#include <string>
#include <sys/system_properties.h>
#include "FlyLog.h"
#include "CallBack.h"

static volatile bool isRun;
static volatile bool isStop;
static const u_char h264Start[4] = {0x00, 0x00, 0x00, 0x01};

class FlyFFmpeg {
public:
    FlyFFmpeg(JavaVM* jvm, JNIEnv *env, jobject thiz);
    ~FlyFFmpeg();
    int open(const char* url, int64_t pos);
    static void *_topen(void *arg);
    int _runopen(void *arg);
    int close(FlyFFmpeg *flyFFmpeg);
    static int interrupt_cb(void *ctx);
private:
    char temp_prop[PROP_VALUE_MAX] = {'\0'};
    CallBack *callBack;
    bool is_avcc = false;
    u_char *sps = nullptr;
    u_char *pps = nullptr;
    AVFormatContext *pFormatCtx;
    AVCodecContext *pCodecCtx_video = nullptr;
    AVCodecContext *pCodecCtx_audio = nullptr;
    AVPacket *packet = nullptr;
    AVFrame *frame = nullptr;
    struct SwrContext* swr_cxt = nullptr;
    uint8_t *audio_buf = nullptr;
    pthread_t open_tid;
    pthread_mutex_t mutex;
    uint16_t out_sampleRateInHz;
    uint16_t out_channelConfig;
    uint16_t out_audioFormat;
    char playUrl[255] = {0};
    int64_t start_pos = 0;
};

#endif //FLYPLAYER_FLYFFMPEG_H
