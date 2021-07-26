package com.example.android.camera.utils.renderer

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Size
import java.nio.ByteBuffer

abstract class BaseRenderer: GLSurfaceView.Renderer {

    //接收相机数据的 SurfaceTexture
    var surfaceTexture: SurfaceTexture? = null

    var previewWidth = 1F
    var previewHeight = 1F
    var dataWidth = 1
    var dataHeight = 1
    var viewWidth = 1F
    var viewHeight = 1F

    var rotation = 0

    public fun setPreviewSize(size: Size) {
        previewWidth = size.width.toFloat().coerceAtMost(size.height.toFloat())
        previewHeight = size.width.toFloat().coerceAtLeast(size.height.toFloat())
    }

    public open fun setDataSize(size: Size) {
        dataWidth = size.width.coerceAtMost(size.height)
        dataHeight = size.width.coerceAtLeast(size.height)
    }

    open fun setTextureParameters() {
        GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
    }

    inner class ListenerBuilder {
        internal var mOnSurfaceCreatedCb: (() -> Unit)? = null
        internal var mOnFrameReadCb: ((ByteBuffer, Int, Int) -> Unit)? = null
        internal var mOnFrameCb: (() -> Unit)? = null
        internal var mOnPicTokenCb: ((Bitmap) -> Unit)? = null
        internal var mOnRenderDoneCb: (() -> Unit)? = null
        fun onSurfaceCreated(action: () -> Unit) {
            mOnSurfaceCreatedCb = action
        }

        fun onFrameRead(action: (ByteBuffer, Int, Int) -> Unit) {
            mOnFrameReadCb = action
        }

        fun onFrame(action: () -> Unit) {
            mOnFrameCb = action
        }

        fun onRenderDone(action: () -> Unit) {
            mOnRenderDoneCb = action
        }

        fun onPicToken(action: (Bitmap) -> Unit) {
            mOnPicTokenCb = action
        }
    }

    protected lateinit var mListener: ListenerBuilder

    fun registerListener(listenerBuilder: ListenerBuilder.() -> Unit) {//带ListenerBuilder返回值的lamba
        mListener = ListenerBuilder().also(listenerBuilder)
    }
}