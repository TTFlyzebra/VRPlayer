//
// Created by FlyZebra on 2020/9/15 0015.
//

#ifndef FLYZEBRAPLAYER_CALLBACK_H
#define FLYZEBRAPLAYER_CALLBACK_H
#include "jni.h"


class CallBack {
public:
    CallBack(JavaVM* jvm, JNIEnv *env, jobject thiz);
    ~CallBack();
    void javaOnVideoEncode(u_char *videoBytes, int width, int height, int size, long dts, long pts);
    void javaOnVideoDecode(u_char *videoBytes, int width, int height, int size);
    void javaOnAudioEncode(const u_char *audioBytes, int size);
    void javaOnAudioDecode(const u_char *audioBytes, int size);
    void javaOnVideoStart(int format, int width, int height,int fps, const u_char *sps, int len1, const u_char *pps, int len2);
    void javaOnAudioStart(int sampleRateInHz, int channelConfig, int audioFormat);
    void javaOnError(int error);
    void javaOnComplete();

private:
    JavaVM* javeVM ;
    JNIEnv *jniEnv ;
    jobject jObject;
    jmethodID onVideoEncode;
    jmethodID onVideoDecode;
    jmethodID onAudioEncode;
    jmethodID onAudioDecode;
    jmethodID onVideoStart;
    jmethodID onAudioStart;
    jmethodID onError;
    jmethodID onComplete;
};


#endif //FLYZEBRAPLAYER_CALLBACK_H
