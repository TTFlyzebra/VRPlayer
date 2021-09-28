package com.flyzebra.ffplay;

/**
 * Author: FlyZebra
 * Time: 18-5-13 下午7:09.
 * Discription: This is IRtspCallBack
 */
public interface ICallBack {

    void onVideoEncode(byte[] videoBytes,int widht, int height, int size, long dts, long pts);

    void onVideoDecode(byte[] videoBytes,int width, int height, int size);

    void onAudioEncode(byte[] audioBytes, int size);

    void onAudioDecode(byte[] audioBytes, int size);

    void onVideoStart(int width, int height, int fps, byte[] sps, byte[] pps);

    void onAudioStart(int sampleRateInHz, int channelConfig, int audioFormat);

    void onError(int error);

    void onComplete();
}
