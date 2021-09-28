package com.flyzebra.ffplay;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.flyzebra.utils.FlyLog;


/**
 * Author: FlyZebra
 * Time: 18-5-14 下午9:00.
 * Discription: This is RtspVideoView
 */
public class FlyVideoView extends SurfaceView implements SurfaceHolder.Callback, ICallBack {
    private FfPlayer ffplayer;
    private MediaDecoder mediaDecoder;
    private AudioPlayer audioPlayer;

    public FlyVideoView(Context context) {
        this(context, null);
    }

    public FlyVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlyVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        ffplayer = new FfPlayer();
        ffplayer.open(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (ffplayer != null) {
            ffplayer.open(this);
            ffplayer = null;
        }
        if (mediaDecoder != null) {
            mediaDecoder.close();
            mediaDecoder = null;
        }
    }

    @Override
    public void onVideoEncode(byte[] videoBytes, int widht, int height, int length, long dts, long pts) {
        try {
            if (mediaDecoder != null) mediaDecoder.input(videoBytes, dts, pts);
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    @Override
    public void onVideoDecode(byte[] videoBytes, int widht, int height, int length) {

    }

    @Override
    public void onAudioEncode(byte[] audioBytes, int length) {

    }

    @Override
    public void onAudioDecode(byte[] audioBytes, int length) {
        try {
            if (audioPlayer != null) audioPlayer.write(audioBytes, length);
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    @Override
    public void onVideoStart(int width, int height, int fps, byte[] sps, byte[] pps) {
        mediaDecoder = new MediaDecoder(this, getHolder().getSurface(), width, height, fps, sps, pps);
    }

    @Override
    public void onAudioStart(int sampleRateInHz, int channelConfig, int audioFormat) {
        audioPlayer = new AudioPlayer(sampleRateInHz, channelConfig, audioFormat);
    }

    @Override
    public void onError(int error) {

    }

    @Override
    public void onComplete() {
        if (mediaDecoder != null) {
            mediaDecoder.close();
            mediaDecoder = null;
        }
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer = null;
        }
    }
}
