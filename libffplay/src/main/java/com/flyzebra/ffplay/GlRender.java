package com.flyzebra.ffplay;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.flyzebra.utils.FlyLog;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Author: FlyZebra
 * Time: 18-5-14 下午9:00.
 * Discription: This is GlRender
 */
public class GlRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private Context context;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    //顶点坐标
    static float vertexData[] = {   // in counterclockwise order:
            -1f, -1f, 0f,// bottom left
            +1f, -1f, 0f,// bottom right
            -1f, +1f, 0f,// top left
            +1f, +1f, 0f,// top right

            -1f, -1f, 0f,// bottom left
            +1f, -1f, 0f,// bottom right
            -1f, +1f, 0f,// top left
            +1f, +1f, 0f,// top right
    };
    //纹理坐标
    static float textureData[] = {   // in counterclockwise order:
            0f, 1f, // bottom left
            1f, 1f, // bottom right
            0f, 0f, // top left
            1f, 0f, // top right

            1f, 1f, // bottom left
            1f, 0f, // bottom right
            0f, 1f, // top left
            0f, 0f, // top right
    };

    private int programId_yuv;
    private int avPosition_yuv;
    private int afPosition_yuv;
    private int sampler_y;
    private int sampler_u;
    private int sampler_v;
    private int[] textureid_yuv;
    int width;
    int height;
    Buffer y;
    Buffer u;
    Buffer v;

    private final Object objectLock = new Object();

    public GlRender(Context context) {
        this.context = context;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        y = ByteBuffer.wrap(new byte[this.width * this.height]);
        u = ByteBuffer.wrap(new byte[this.width * this.height / 4]);
        v = ByteBuffer.wrap(new byte[this.width * this.height / 4]);

    }

    public void update(byte[] yuv, int w, int h, int size) {
        //NV12
        if (w != width || h != height) {
            FlyLog.e("yuv size error.");
            return;
        }
        synchronized (objectLock) {
            ((ByteBuffer) y).put(yuv, 0, w * h);
            for (int i = 0; i < w * h / 4; i++) {
                ((ByteBuffer) u).put(yuv[w * h + i * 2]);
                ((ByteBuffer) v).put(yuv[w * h + i * 2 + 1]);
            }
            y.flip();
            u.flip();
            v.flip();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        FlyLog.d("onSurfaceCreated");
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
        //创建一个渲染程序
        String vertexShader = GlShaderUtils.readRawTextFile(context, R.raw.vertex_shader);
        String fragmentShader = GlShaderUtils.readRawTextFile(context, R.raw.fragment_yuv);
        programId_yuv = GlShaderUtils.createProgram(vertexShader, fragmentShader);

        //得到着色器中的属性
        avPosition_yuv = GLES20.glGetAttribLocation(programId_yuv, "av_Position");
        afPosition_yuv = GLES20.glGetAttribLocation(programId_yuv, "af_Position");
        sampler_y = GLES20.glGetUniformLocation(programId_yuv, "sampler_y");
        sampler_u = GLES20.glGetUniformLocation(programId_yuv, "sampler_u");
        sampler_v = GLES20.glGetUniformLocation(programId_yuv, "sampler_v");

        GLES20.glUseProgram(programId_yuv);
        GLES20.glEnableVertexAttribArray(avPosition_yuv);
        GLES20.glVertexAttribPointer(avPosition_yuv, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);//为顶点属性赋值
        GLES20.glEnableVertexAttribArray(afPosition_yuv);
        GLES20.glVertexAttribPointer(afPosition_yuv, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);

        textureid_yuv = new int[3];
        GLES20.glGenTextures(3, textureid_yuv, 0);
        for (int i = 0; i < 3; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid_yuv[i]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }

        GLES20.glClearColor(0f, 0f, 0f, 1f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        FlyLog.d("onSurfaceChanged, width:" + width + ",height :" + height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (objectLock) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glClearColor(0f, 0f, 0f, 1f);
            if (width > 0 && height > 0 && y != null && u != null && v != null) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid_yuv[0]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y);//
                GLES20.glUniform1i(sampler_y, 0);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid_yuv[1]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width / 2, height / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, u);
                GLES20.glUniform1i(sampler_u, 1);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid_yuv[2]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width / 2, height / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, v);
                GLES20.glUniform1i(sampler_v, 2);
                y.clear();
                u.clear();
                v.clear();
            }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, width > height ? 4 : 0, 4);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        FlyLog.d("updateSurface");
    }

}
