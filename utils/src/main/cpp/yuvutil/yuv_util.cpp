//
// Created by 孙凯 on 2018/6/6.
//

#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <android/bitmap.h>
#include <malloc.h>

/*NV21（YYYYVUVU） 转YV12 (YYYYVVUU)*/
extern "C"
JNIEXPORT void JNICALL
Java_com_example_android_camera_utils_YuvUtil_convertNv21torealyuv
        (JNIEnv *env, jobject instance, jbyteArray Nv21Buf, jint width,
         jint height, jint stride, jboolean isBurst) {
    __android_log_print(ANDROID_LOG_DEBUG, "yuvUtil", "convertNV21torealNV21 start");
    unsigned char *pNv21 = (unsigned char *) env->GetByteArrayElements(Nv21Buf, NULL);
    long srcDataSize = env->GetArrayLength(Nv21Buf);
    __android_log_print(ANDROID_LOG_DEBUG, "yuvUtil", "nv21 length: %ld", srcDataSize);
    //unsigned char *pRelYuv = (unsigned char *) env->GetByteArrayElements(realNv21Buf, NULL);
    unsigned char *pRelYuv = (unsigned char *) malloc(srcDataSize + 1);
    int frame_size = width * height;
    // copy y channel
    if (stride == width) {
        memcpy(pRelYuv, pNv21, frame_size);
    } else {
        unsigned char *p_pass1_y = pRelYuv;
        unsigned char *p_gy = pNv21;
        for (int i = 0; i < height; ++i) {
            memcpy(p_pass1_y, p_gy, width);
            p_pass1_y += width;
            p_gy += stride;
        }
    }
    // copy uv channel
    unsigned char *p_gvu = NULL;
    unsigned char *p_pass1_vu = pRelYuv + frame_size;
    int strideH = height / 64 + ((height % 64 > 0) ? 1 : 0);
    int height_stride = strideH * 64; //for height stride
    __android_log_print(ANDROID_LOG_DEBUG, "yuvUtil", "stride: %d, height_stride3: %d", stride,
                        height_stride);
    if (stride == width) {
        if (isBurst) {
            p_gvu = pNv21 + stride * height;
        } else {
            p_gvu = pNv21 + stride * (height - 1) +
                    width;  //frame_size; //note: if height stride change maybe bug
        }
        memcpy(p_pass1_vu, p_gvu, (frame_size >> 2));
    } else {
        if (isBurst) {
            p_gvu = pNv21 + stride * height;
        } else {
            p_gvu = pNv21 + stride * (height - 1) +
                    width;  //frame_size; //note: if height stride change maybe bug
        }
        const int vu_h = height >> 1;
        const int vu_w = width;
        for (int i = 0; i < vu_h; ++i) {
            memcpy(p_pass1_vu, p_gvu, vu_w);
            p_gvu += stride;
            p_pass1_vu += vu_w;
        }
    }
    int yv12length = (frame_size >> 1) + frame_size;
    memcpy(pNv21, pRelYuv, yv12length);

    free(pRelYuv);
    if (NULL != pNv21) {
        env->ReleaseByteArrayElements(Nv21Buf, (jbyte *) pNv21, 0);
    }
/*    if (NULL != pRelYuv) {
        env->ReleaseByteArrayElements(realNv21Buf, (jbyte *) pRelYuv, 0);
    }*/
    __android_log_print(ANDROID_LOG_DEBUG, "yuvUtil", "convertNV21torealNV21 end");
}

