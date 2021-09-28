//
// Created by FlyZebra on 2018/11/8.
//

#include "jni.h"
#include "FfPlayer.h"
#include "FlyFFmpeg.h"
#include "CallBack.h"

JavaVM* javaVM = nullptr;

extern "C" jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    JNIEnv *env = nullptr;
    jint result = -1;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("JNI OnLoad failed\n");
        return result;
    }
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flyzebra_ffplay_FfPlayer__1open(JNIEnv *env, jobject thiz) {
    LOGD("JNI ffmpeg open");
    FlyFFmpeg *ffmpeg = new FlyFFmpeg(javaVM,env,thiz);
    return reinterpret_cast<jlong>(ffmpeg);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_flyzebra_ffplay_FfPlayer__1play(JNIEnv *env, jobject thiz, jlong ffmpegPointer, jstring jurl,jlong pos) {
    LOGD("JNI ffmpeg play");
    const char *surl = env->GetStringUTFChars(jurl, 0);
    FlyFFmpeg *ffmpeg = reinterpret_cast<FlyFFmpeg *>(ffmpegPointer);
    ffmpeg->open(surl,pos);
    env->ReleaseStringUTFChars(jurl, surl);
    return reinterpret_cast<jlong>(ffmpeg);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_flyzebra_ffplay_FfPlayer__1close(JNIEnv *env, jobject thiz, jlong ffmpegPointer) {
    LOGD("JNI ffmpeg close");
    FlyFFmpeg *ffmpeg = reinterpret_cast<FlyFFmpeg *>(ffmpegPointer);
    ffmpeg->close(ffmpeg);
    delete ffmpeg;
}