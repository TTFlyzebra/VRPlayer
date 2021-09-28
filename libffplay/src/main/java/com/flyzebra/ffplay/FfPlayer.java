package com.flyzebra.ffplay;

import com.flyzebra.utils.FlyLog;

/**
 * Author: FlyZebra
 * Time: 18-5-13 下午6:55.
 * Discription: This is FlyClient
 */
public class FfPlayer {
    private ICallBack iCallBack;
    private long ffmpegPointer = -1;

    static {
        System.loadLibrary("ffplay");
    }

    public FfPlayer(){
        FlyLog.d("new FfPlayer.");
        ffmpegPointer = -1;
    }

    public void open(ICallBack iCallBack) {
        this.iCallBack = iCallBack;
        ffmpegPointer = _open();
    }

    public void play(String url) {
        play(url,0);
    }

    public void play(String url,long pos) {
        if (ffmpegPointer == -1) {
            ffmpegPointer = _open();
        }
        if (ffmpegPointer != -1) {
            _play(ffmpegPointer, url, pos);
        } else {
            FlyLog.e("You must run the open method first.");
        }
    }

    public void stop() {
        if (ffmpegPointer != -1) {
            _close(ffmpegPointer);
        }
        ffmpegPointer = -1;
    }

    public void close() {
        if (ffmpegPointer != -1) {
            _close(ffmpegPointer);
        }
        ffmpegPointer = -1;
        this.iCallBack = null;
    }

    public void onVideoEncode(byte[] videoBytes, int width, int height, int size, long dts,  long pts) {
        if (iCallBack != null) iCallBack.onVideoEncode(videoBytes, width, height, size, dts, pts);
    }

    public void onVideoDecode(byte[] videoBytes, int width, int height, int lenght) {
        if (iCallBack != null) iCallBack.onVideoDecode(videoBytes, width, height, lenght);
    }

    public void onAudioEncode(byte[] audioBytes, int size) {
        if (iCallBack != null) iCallBack.onAudioEncode(audioBytes, size);
    }

    public void onAudioDecode(byte[] audioBytes, int size) {
        if (iCallBack != null) iCallBack.onAudioDecode(audioBytes, size);
    }

    public void onVideoStart(int width, int height, int fps, byte[] sps, byte[] pps) {
        if (iCallBack != null) iCallBack.onVideoStart(width, height, fps, sps, pps);
    }

    public void onAudioStart(int sampleRateInHz, int channelConfig, int audioFormat) {
        if (iCallBack != null) iCallBack.onAudioStart(sampleRateInHz, channelConfig, audioFormat);
    }

    public void onError(int error) {
        if (iCallBack != null) iCallBack.onError(error);
    }


    public void onComplete() {
        if (iCallBack != null) iCallBack.onComplete();
    }

    private native long _open();

    private native void _play(long objPointer, String url, long pos);

    private native void _close(long objPointer);


}
