package com.example.android.camera.utils;

public class YuvUtil {
    static {
        System.loadLibrary("yuvutil");
    }

    public static native void convertNv21torealyuv(byte[] Nv21Buf, int width, int height, int stride, boolean isBurst);
}
