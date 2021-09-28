package com.flyzebra.ffplay;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.flyzebra.utils.FlyLog;

import java.util.ArrayList;
import java.util.List;


/**
 * Author: FlyZebra
 * Time: 18-5-14 下午9:00.
 * Discription: This is GlVideoView
 */
public class GlVideoView extends GLSurfaceView implements SurfaceHolder.Callback, ICallBack {
    private GlRender glRender;
    private FfPlayer ffplayer;
    private MediaDecoder mediaDecoder;
    private AudioPlayer audioPlayer;
    private long startPlayTime = 0;
    private long palyLength = 0;
    private String playUrl;

    public GlVideoView(Context context) {
        this(context, null);
    }

    public GlVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        //int width = MeasureSpec.getSize(widthMeasureSpec);
        //float scale = 9f / 16f;
        //int height = (int) (width * scale);
        //setMeasuredDimension(width, height);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        glRender = new GlRender(context);
        setRenderer(glRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        super.surfaceCreated(surfaceHolder);
        ffplayer = new FfPlayer();
        ffplayer.open(this);
        if(!TextUtils.isEmpty(playUrl)){
            ffplayer.play(playUrl);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        super.surfaceChanged(surfaceHolder, i, i1, i2);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        super.surfaceDestroyed(surfaceHolder);
        if (ffplayer != null) {
            ffplayer.close();
            ffplayer = null;
        }
        if (mediaDecoder != null) {
            mediaDecoder.close();
            mediaDecoder = null;
        }
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer = null;
        }
    }

    @Override
    public void onVideoEncode(byte[] videoBytes, int width, int height, int size, long dts,  long pts) {
        try {
            mediaDecoder.input(videoBytes, dts, pts);
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    @Override
    public void onVideoDecode(byte[] videoBytes, int widht, int height, int size) {
        glRender.update(videoBytes, widht, height, size);
        requestRender();
    }

    @Override
    public void onAudioEncode(byte[] audioBytes, int size) {

    }

    @Override
    public void onAudioDecode(byte[] audioBytes, int size) {
        try {
            if (startPlayTime != 0) {
                long playUseTime = palyLength * 1000 / (48000 * 16 / 8 * 2);
                long sleepTime = playUseTime - (SystemClock.uptimeMillis() - startPlayTime);
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(Math.min(sleepTime, 100));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                startPlayTime = SystemClock.uptimeMillis();
            }
            if (audioPlayer != null) {
                audioPlayer.write(audioBytes, size);
            }
            palyLength += size;
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    @Override
    public void onVideoStart(int width, int height, int fps, byte[] sps, byte[] pps) {
        glRender.setSize(width, height);
        mediaDecoder = new MediaDecoder(this, null, width, height, fps, sps, pps);
    }

    @Override
    public void onAudioStart(int sampleRateInHz, int channelConfig, int audioFormat) {
        palyLength = 0;
        startPlayTime = 0;
        audioPlayer = new AudioPlayer(sampleRateInHz, channelConfig, audioFormat);
    }

    @Override
    public void onError(int error) {

    }

    @Override
    public void onComplete() {
        synchronized (mLock) {
            for (IPlayListener listener : listeners) {
                listener.onComplete();
            }
        }
        if (mediaDecoder != null) {
            mediaDecoder.close();
            mediaDecoder = null;
        }
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer = null;
        }
        if(!TextUtils.isEmpty(playUrl)){
            ffplayer.play(playUrl);
        }
    }

    public void playUrl(String url) {
        playUrl = url;
        if (ffplayer != null) {
            ffplayer.close();
        }
        if (mediaDecoder != null) {
            mediaDecoder.close();
            mediaDecoder = null;
        }
        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer = null;
        }
        if (ffplayer != null) {
            ffplayer.open(this);
            ffplayer.play(playUrl);
        }
    }

    private List<IPlayListener> listeners = new ArrayList<>();
    private final Object mLock = new Object();

    public void addListener(IPlayListener listener){
        synchronized (mLock){
            listeners.add(listener);
        }
    }

    public void removeListener(IPlayListener listener){
        synchronized (mLock){
            listeners.remove(listener);
        }
    }

}
