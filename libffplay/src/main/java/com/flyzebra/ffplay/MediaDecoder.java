package com.flyzebra.ffplay;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Surface;

import com.flyzebra.config.Prop;
import com.flyzebra.utils.FlyLog;
import com.flyzebra.utils.SystemPropTools;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by FlyZebra on 2018/5/11.
 * Descrip:
 */

public class MediaDecoder {
    private final boolean isLocalFile;
    private MediaCodec mediaCodec;
    private ICallBack iCallBack;

    private final Surface surface;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo info;
    private MediaFormat mediaFormat;
    private long frame_num = 0;
    private long first_time = 0;
    private long start_time = 0;
    private final int fps;
    private ByteBuffer byteBuffer;
    private byte[] glData;
    private boolean is_qsv = false;

    public MediaDecoder(ICallBack iCallBack, Surface surface, int widht, int height, int fps, byte[] sps, byte[] pps) {
        this.iCallBack = iCallBack;
        this.surface = surface;
        this.fps = fps;
        is_qsv = SystemPropTools.get(Prop.WEBCAM_H264QSV, "false").equals("true");
        String url = SystemPropTools.get(Prop.WEBCAM_URL, "");
        this.isLocalFile = TextUtils.isEmpty(url) || url.startsWith("/") || url.startsWith("http");
        initMediaCodec(widht, height, fps, sps, pps);
        start();
    }

    private void initMediaCodec(int width, int height, int fps, byte[] sps, byte[] pps) {
        FlyLog.d("initMediaCodec() width=%d, height=%d", width, height);
        try {
            if (byteBuffer == null) {
                byteBuffer = ByteBuffer.wrap(new byte[width * height * 3 / 2]);
            }
            if (glData == null) {
                glData = new byte[(width / 64 + 2) * 64 * (height / 64 + 2) * 64 * 3 / 2];
            }
            info = new MediaCodec.BufferInfo();
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
            mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps / 1000);
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
            mediaCodec.configure(mediaFormat, surface, null, 0);
        } catch (IOException e) {
            FlyLog.e("create MediaCodec error e=%s", e.toString());
        }
    }

    public void start() {
        mediaCodec.start();
        inputBuffers = mediaCodec.getInputBuffers();
        outputBuffers = mediaCodec.getOutputBuffers();
    }

    public void input(byte[] bytes, long dts, long pts) {
        try {
            if (start_time == 0) {
                start_time = SystemClock.uptimeMillis();
            }
            int inIndex = mediaCodec.dequeueInputBuffer(1000000 / fps * 5000);
            if (inIndex >= 0) {
                ByteBuffer buffer = inputBuffers[inIndex];
                buffer.clear();
                buffer.put(bytes);
                mediaCodec.queueInputBuffer(inIndex, 0, bytes.length, (is_qsv ? pts : (1000000 / fps)) * 1000, 0);
            }
            int outIndex = mediaCodec.dequeueOutputBuffer(info, (is_qsv ? pts : (1000000 / fps)) * 1000);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    inputBuffers = mediaCodec.getInputBuffers();
                    FlyLog.d("INFO_OUTPUT_FORMAT_CHANGED");
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    outputBuffers = mediaCodec.getOutputBuffers();
                    FlyLog.d("INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    break;
                default:
                    MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                    //FlyLog.v("MediaFormat:%s", mediaFormat.toString());
                    if (surface == null) {
                        int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                        if (mediaFormat.containsKey("crop-left") && mediaFormat.containsKey("crop-right")) {
                            width = mediaFormat.getInteger("crop-right") + 1 - mediaFormat.getInteger("crop-left");
                        }
                        int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        if (mediaFormat.containsKey("crop-top") && mediaFormat.containsKey("crop-bottom")) {
                            height = mediaFormat.getInteger("crop-bottom") + 1 - mediaFormat.getInteger("crop-top");
                        }
                        int glWidth = Math.max(mediaFormat.getInteger(MediaFormat.KEY_STRIDE), width);
                        int glheight = Math.max(mediaFormat.getInteger(MediaFormat.KEY_SLICE_HEIGHT), height);
                        int glDataSize = glWidth * glheight * 3 / 2;
                        int sendDataSize = width * height * 3 / 2;
                        outputBuffers[outIndex].position(info.offset);
                        outputBuffers[outIndex].limit(info.offset + glDataSize);
                        outputBuffers[outIndex].get(glData, 0, glDataSize);

                        byteBuffer.clear();
                        if (glWidth == width && glheight == height) {
                            byteBuffer.put(glData, 0, sendDataSize);
                        } else {
                            //cut yuv from mediacodec
                            for (int j = 0; j < height; j++) {
                                byteBuffer.put(glData, j * glWidth, width);
                            }
                            for (int j = 0; j < height / 2; j++) {
                                byteBuffer.put(glData, (j + glheight) * glWidth, width);
                            }
                        }
                        byteBuffer.flip();
                        iCallBack.onVideoDecode(byteBuffer.array(), width, height, sendDataSize);
                    }
                    mediaCodec.releaseOutputBuffer(outIndex, surface != null);
                    long currentTime = SystemClock.uptimeMillis();
                    frame_num++;
                    long willUseTime = (frame_num * 1000000 / fps);
                    if (isLocalFile) {
                        if (first_time == 0) {
                            first_time = currentTime;
                        }
                    } else {
                        first_time = currentTime - willUseTime;
                    }
                    long playUseTime = currentTime - first_time;
                    if (isLocalFile) {
                        if (willUseTime > playUseTime) {
                            long sleep_t = willUseTime - playUseTime;
                            if (sleep_t > 0) {
//                                FlyLog.e("frame %d sleep 1 time %d, playTime=%d", frame_num, sleep_t, willUseTime);
                                try {
                                    Thread.sleep(willUseTime - currentTime + first_time);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else if (frame_num > (fps / 1000 * 5)) {
                        if (willUseTime > playUseTime) {
                            long sleep_t = willUseTime - playUseTime;
                            if (sleep_t > 0) {
//                                FlyLog.e("frame %d sleep 1 time %d, playTime=%d", frame_num, sleep_t, willUseTime);
                                try {
                                    Thread.sleep(willUseTime - currentTime + first_time);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            long sleep_t = 1000000 / fps - (SystemClock.uptimeMillis() - start_time) - 1000000 / fps / 2;
                            if (sleep_t > 0) {
//                                FlyLog.e("frame %d sleep 2 time %d, playTime=%d", frame_num, sleep_t, willUseTime);
                                try {
                                    Thread.sleep(sleep_t);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    start_time = SystemClock.uptimeMillis();
                    break;
            }
        } catch (Exception e) {
            FlyLog.e(e.toString());
        }
    }

    public void output(/*out*/byte[] data,/* out */int[] len,/* out */long[] ts) {
        int outIndex = mediaCodec.dequeueOutputBuffer(info, -1);
        if (outIndex >= 0) {
            outputBuffers[outIndex].position(info.offset);
            outputBuffers[outIndex].limit(info.offset + info.size);
            outputBuffers[outIndex].get(data, 0, info.size);
            len[0] = info.size;
            ts[0] = info.presentationTimeUs;
            mediaCodec.releaseOutputBuffer(outIndex, false);
        }
    }

    public void close() {
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        inputBuffers = null;
        outputBuffers = null;
        byteBuffer = null;
        glData = null;
    }
}
