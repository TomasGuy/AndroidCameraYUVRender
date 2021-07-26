/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2.basic.fragments

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.android.camera.utils.renderer.YUVRenderer
import com.example.android.camera2.basic.R
import com.example.android.camera2basic.CameraGLSurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

class YUVRenderFragment : Fragment() {

    private lateinit var mPreviewView: CameraGLSurfaceView
    var mRenderer: YUVRenderer = YUVRenderer()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_yuv, container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mPreviewView = view.findViewById(R.id.view_finder)
        mPreviewView.setAspectRatio(Size(4624, 3468))
        mPreviewView.setEGLContextClientVersion(3)
        mPreviewView.setRenderer(mRenderer)
        mPreviewView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        lifecycleScope.launch(Dispatchers.Main) {
            loadYUVImage()
        }
    }

    private suspend fun loadYUVImage()  = withContext(Dispatchers.IO) {
//        loadI420()
        loadNV21()
    }

    private fun loadI420() {
        var `is`: InputStream? = null
        try {
            `is` = requireContext().getAssets().open("YUV_Image_4624x3468.I420")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var lenght = 0
        try {
            lenght = `is`!!.available()
            val buffer = ByteArray(lenght)
            `is`.read(buffer)

            val yBuffer = ByteArray(4624*3468)
            val uBuffer = ByteArray(4624*3468/4)
            val vBuffer = ByteArray(4624*3468/4)
            System.arraycopy(buffer, 0, yBuffer, 0, yBuffer.size)
            System.arraycopy(buffer, yBuffer.size, uBuffer, 0, uBuffer.size)
            System.arraycopy(buffer, yBuffer.size+uBuffer.size, vBuffer, 0, vBuffer.size)
            mRenderer.setDataSize(Size(4624, 3468))
            mRenderer.renderYUV(ByteBuffer.wrap(yBuffer), ByteBuffer.wrap(uBuffer), ByteBuffer.wrap(vBuffer), null, ImageFormat.YUV_420_888)

            mPreviewView.requestRender()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun loadNV21() {
        var `is`: InputStream? = null
        try {
            `is` = requireContext().getAssets().open("YUV_Image_840x1074.NV21")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var lenght = 0
        try {
            lenght = `is`!!.available()
            val buffer = ByteArray(lenght)
            `is`.read(buffer)

            val yBuffer = ByteArray(840*1074)
            val uvBuffer = ByteArray(840*1074/2)
            System.arraycopy(buffer, 0, yBuffer, 0, yBuffer.size)
            System.arraycopy(buffer, yBuffer.size, uvBuffer, 0, uvBuffer.size)
            mRenderer.setDataSize(Size(840, 1074))
            mRenderer.renderYUV(ByteBuffer.wrap(yBuffer), null, null, ByteBuffer.wrap(uvBuffer), ImageFormat.NV21)

            mPreviewView.requestRender()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
