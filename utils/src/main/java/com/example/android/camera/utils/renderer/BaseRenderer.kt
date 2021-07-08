package com.example.android.camera.utils.renderer

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Size
import java.nio.ByteBuffer

abstract class BaseRenderer: GLSurfaceView.Renderer {

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
}