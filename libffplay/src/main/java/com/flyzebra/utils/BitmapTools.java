package com.flyzebra.utils;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public class BitmapTools {

    /**
     * @方法描述 Bitmap转RGB
     */
    public static byte[] bitmap2RGB24(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();  //返回可用于储存此位图像素的最小字节数

        ByteBuffer buffer = ByteBuffer.allocate(bytes); //  使用allocate()静态方法创建字节缓冲区
        bitmap.copyPixelsToBuffer(buffer); // 将位图的像素复制到指定的缓冲区

        byte[] rgba = buffer.array();
        byte[] pixels = new byte[rgba.length * 3 / 4];

        int count = rgba.length / 4;

        //Bitmap像素点的色彩通道排列顺序是RGBA
        for (int i = 0; i < count; i++) {

            pixels[i * 3] = rgba[i * 4];        //R
            pixels[i * 3 + 1] = rgba[i * 4 + 1];    //G
            pixels[i * 3 + 2] = rgba[i * 4 + 2];       //B

        }

        return pixels;
    }

    /**
     * @方法描述 将RGB字节数组转换成Bitmap，
     */
    static public Bitmap rgb2Bitmap(byte[] data, int width, int height) {
        int[] argb = new int[width * height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                argb[i * width + j] = 255;
                argb[i * width + j] = (argb[i * width + j] << 8) + data[(i * width + j) * 3 + 2]; //+r
                argb[i * width + j] = (argb[i * width + j] << 8) + data[(i * width + j) * 3 + 1]; //+g
                argb[i * width + j] = (argb[i * width + j] << 8) + data[(i * width + j) * 3 + 0]; //+b
            }
        }
        Bitmap bmp = Bitmap.createBitmap(argb, 0, width, width, height,
                Bitmap.Config.ARGB_8888);
        return bmp;
    }

    // 将一个byte数转成int
    // 实现这个函数的目的是为了将byte数当成无符号的变量去转化成int
    public static int convertByteToInt(byte data) {
        int heightBit = (int) ((data >> 4) & 0x0F);
        int lowBit = (int) (0x0F & data);
        return heightBit * 16 + lowBit;
    }

    // 将纯RGB数据数组转化成int像素数组
    public static int[] convertByteToColor(byte[] data) {
        int size = data.length;
        if (size == 0) {
            return null;
        }

        int arg = 0;
        if (size % 3 != 0) {
            arg = 1;
        }

        // 一般RGB字节数组的长度应该是3的倍数，
        // 不排除有特殊情况，多余的RGB数据用黑色0XFF000000填充
        int[] color = new int[size / 3 + arg];
        int red, green, blue;
        int colorLen = color.length;
        if (arg == 0) {
            for (int i = 0; i < colorLen; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);

                // 获取RGB分量值通过按位或生成int的像素值
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }
        } else {
            for (int i = 0; i < colorLen - 1; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }

            color[colorLen - 1] = 0xFF000000;
        }

        return color;
    }
}
