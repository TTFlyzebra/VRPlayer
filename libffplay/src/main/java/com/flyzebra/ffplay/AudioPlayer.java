package com.flyzebra.ffplay;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

import com.flyzebra.utils.FlyLog;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioPlayer implements Runnable {
    private AudioTrack audioTrack;
    private int channel;
    private int format;
    private Thread mThread;
    private static final String THREAD_TAG = "AudioPlayer";
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean isStop = new AtomicBoolean(true);
    private ByteBuffer audioBuffer;
    private byte[] sendAudio;
    private final Object mDataLock = new Object();

    public AudioPlayer(int sampleRateInHz, int channelConfig, int audioFormat) {
        FlyLog.d("sampleRateInHz=%d,channelConfig=%d,audioFormat=%d", sampleRateInHz, channelConfig, audioFormat);
        int cacheSize = sampleRateInHz;
        switch (channelConfig) {
            case 4:
                channel = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case 3:
            default:
                channel = AudioFormat.CHANNEL_OUT_STEREO;
                cacheSize = cacheSize * 2;
                break;
        }

        switch (audioFormat) {
            case 0:
                format = AudioFormat.ENCODING_PCM_8BIT;
                break;
            case 1:
                format = AudioFormat.ENCODING_PCM_16BIT;
                cacheSize = cacheSize * 2;
                break;
            case 2:
                format = AudioFormat.ENCODING_PCM_FLOAT;
                cacheSize = cacheSize * 4;
                break;
            default:
                format = AudioFormat.ENCODING_PCM_16BIT;
                cacheSize = cacheSize * 2;
                break;
        }
        audioBuffer = ByteBuffer.allocate(cacheSize);
        sendAudio = new byte[cacheSize / 20];
        int bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channel, format) * 4;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, (int) (sampleRateInHz), channel, format, bufferSize, AudioTrack.MODE_STREAM);
        isStop.set(false);
        audioTrack.play();
        mThread = new Thread(this, THREAD_TAG);
        mThread.start();
        FlyLog.d("cacheSize=%d, bufferSize = %d", cacheSize, bufferSize);
    }

    public void stop() {
        isStop.set(true);
        synchronized (mDataLock) {
            mDataLock.notify();
        }
        try {
            if (audioTrack != null) {
                if (audioTrack.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioTrack.stop();
                }
                audioTrack.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        isRunning.set(true);
        while (!isStop.get()) {
            synchronized (mDataLock) {
                if (audioBuffer.position() >= sendAudio.length) {
                    audioBuffer.flip();
                    audioBuffer.get(sendAudio);
                    audioBuffer.compact();
                    mDataLock.notify();
                } else {
                    try {
                        mDataLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
            }
            audioTrack.write(sendAudio, 0, sendAudio.length);
        }
        isRunning.set(false);
    }

    public void write(final byte[] buffer, int size) {
        synchronized (mDataLock) {
            while (!isStop.get() && audioBuffer.remaining() < size) {
                try {
                    mDataLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            audioBuffer.put(buffer, 0, size);
            if (audioBuffer.position() >= sendAudio.length) {
                mDataLock.notify();
            }
        }
    }
}
