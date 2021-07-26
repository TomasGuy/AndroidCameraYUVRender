package com.gain.longexposure.renderer

import android.graphics.*
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import android.util.Size
import com.example.android.camera.utils.R
import com.example.android.camera.utils.renderer.BaseRenderer
import com.gain.longexposure.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class OesRenderer : BaseRenderer() {

    private val VERTEX_ATTRIB_POSITION_SIZE = 3
    private val VERTEX_ATTRIB_TEXTURE_POSITION_SIZE = 2;

    private val vertex = floatArrayOf(
            -1f, 1f, 0.0f,  //左上
            -1f, -1f, 0.0f,  //左下
            1f, 1f, 0.0f,  //右下
            1f, -1f, 0.0f //右上
    )

    //纹理坐标，（s,t），t坐标方向和顶点y坐标反着
    var textureCoord = floatArrayOf(
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    )

    var yuvTextureCoord = floatArrayOf(
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    )

    private var vertexBuffer: FloatBuffer? = null
    private var textureCoordBuffer: FloatBuffer? = null
    private var yuvTextureCoordBuffer: FloatBuffer? = null

    private val mCoordMatrix: FloatArray = FloatArray(16)
    private val mYUVCoordMatrix: FloatArray = FloatArray(16)

    private val mMVPMatrix = FloatArray(16)
    private val frameBufferTransformMatrix = FloatArray(16)
    private val screenTransformMatrix = FloatArray(16)
    private val yuvTransformMatrix = FloatArray(16)

    /**
     * YUV转rgba
     */
    private var yuvProgram = 0

    /**
     * OES预览
     */
    private var program0 = 0

    /**
     * 算法处理
     */
    private var program1 = 0

    /**
     * screen预览
     */
    private var program2 = 0

    //接收相机数据的纹理
    private val oesTextureIds = IntArray(1)

    //接收yuv三通道的纹理
    private val yuvTextureIds = IntArray(2)

    private val frameBuffers = IntArray(3)
    private val frameTexture = IntArray(3)

    private lateinit var yData: ByteBuffer
    private lateinit var uvData: ByteBuffer

    var isCapture: Boolean = false

    lateinit var mYuvData: ByteArray

    companion object {
        var OES_TEXTURE_ID = 0
    }

    init {
        initVertexAttrib()

        Matrix.setIdentityM(mMVPMatrix, 0)
    }

    private fun initVertexAttrib() {
        yuvTextureCoordBuffer = EGLUtil.getFloatBuffer(yuvTextureCoord)
        textureCoordBuffer = EGLUtil.getFloatBuffer(textureCoord)
        vertexBuffer = EGLUtil.getFloatBuffer(vertex)
    }

    fun renderYUV(y: ByteBuffer, uv: ByteBuffer) {
        yData = y
        uvData = uv
    }

    public override fun setDataSize(size: Size) {
        dataWidth = size.width
        dataHeight = size.height

        delFrameBufferAndTexture()
        initFrameBuffer(dataWidth, dataHeight)
        delTexture(yuvTextureIds)
        initYUVTexture()
    }

    private fun initYUVTexture() {
        GLES20.glGenTextures(yuvTextureIds.size, yuvTextureIds, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[1])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun delTexture(textures:IntArray) {
        GLES20.glDeleteTextures(textures.size, textures, 0)
    }

    private fun initFrameBuffer(width: Int, height: Int) {
        Log.d("test", "width:$width  height:$height")
        // 创建frame buffer绑定的纹理
        // Create texture which binds to frame buffer
        GLES20.glGenTextures(frameTexture.size, frameTexture, 0)

        // 创建frame buffer
        // Create frame buffer
        GLES20.glGenFramebuffers(frameBuffers.size, frameBuffers, 0)

        // 将frame buffer与texture绑定
        // Bind the texture to frame buffer

        //Bind framebuffer0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTexture[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameTexture[0], 0)

        //Bind framebuffer1
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTexture[1])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[1])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameTexture[1], 0)

        //Bind framebuffer2
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTexture[2])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[2])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameTexture[2], 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

    }

    fun delFrameBufferAndTexture() {
        GLES20.glDeleteFramebuffers(frameBuffers.size, frameBuffers, 0)
        GLES20.glDeleteTextures(frameTexture.size, frameTexture, 0)
    }

    private fun bindFrameBuffer(frameBuffer: Int) {

        // 绑定frame buffer
        // Bind the frame buffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer)
    }

    var resultBitmap: Bitmap? = null
    fun saveImg(index: Int): Bitmap {
        var bitmapBuff = ByteBuffer.allocate((dataWidth * dataHeight * 4).toInt())

        var startReadTime = System.currentTimeMillis()

        GLES20.glReadPixels(
                0,
                0,
                dataWidth,
                dataHeight,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                bitmapBuff
        )
        var endReadTime = System.currentTimeMillis()

        Log.i("GLView", "read cost:" +(endReadTime -startReadTime))

        var bitmap =
                Bitmap.createBitmap(dataWidth, dataHeight, Bitmap.Config.ARGB_8888)

        bitmap.copyPixelsFromBuffer(bitmapBuff)

        val matrix = android.graphics.Matrix()
        when {
            rotation%360==90 -> {
                matrix.setScale(bitmap.height/bitmap.width.toFloat(), bitmap.width/bitmap.height.toFloat())
            }
            rotation%360==180 -> {
                matrix.setScale(-1f, -1f)
            }
            rotation%360==270 -> {
                matrix.setScale(bitmap.height/bitmap.width.toFloat(), bitmap.width/bitmap.height.toFloat())
                matrix.setScale(-1f, -1f)
            }

//        val b = ByteBuffer.wrap(bitmapBuff.array())
        }

        var reversePic =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

//        val b = ByteBuffer.wrap(bitmapBuff.array())


        resultBitmap = reversePic

        return reversePic
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        initProgram()

        //创建纹理对象
        GLES20.glGenTextures(oesTextureIds.size, oesTextureIds, 0)

        OES_TEXTURE_ID = oesTextureIds[0]

        //将纹理对象textureId[0]绑定到surfaceTexture
        surfaceTexture = SurfaceTexture(oesTextureIds[0])

        mListener.mOnSurfaceCreatedCb?.invoke()
        Log.i("GLSL", "onSurfaceCreated")
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        viewWidth = width.toFloat()
        viewHeight = height.toFloat()
        calculateMatrix()

        delFrameBufferAndTexture()
        initFrameBuffer(dataWidth.toInt(), dataHeight.toInt())
        initYUVTexture()
    }

    var takePhotoFlag = false
    fun takePhoto() {
        takePhotoFlag = true
    }

    var size = 0
    override fun onDrawFrame(p0: GL10?) {
        surfaceTexture!!.updateTexImage()

        surfaceTexture!!.getTransformMatrix(mCoordMatrix)

        if (isCapture.not()) {
            //preview
            bindFrameBuffer(0)

            // 绑定第0个GL Program
            // Bind GL program 0
            bindGLProgram0(oesTextureIds[0])

            render(previewWidth.toInt(), previewHeight.toInt())
        } else {
            if(this::yData.isInitialized&&this::uvData.isInitialized) {
                //capture
                size++

                //yuv->in
                bindFrameBuffer(frameBuffers[2])

                bindYUVGLProgram(yData, uvData)

                render(dataWidth, dataHeight)

                //in(oes),avg->avg
                bindFrameBuffer(frameBuffers[(size + 1) % 2])

                bindGLProgram1(frameTexture[2], frameTexture[size % 2], size)

                render(dataWidth, dataHeight)

                if (takePhotoFlag) {
                    mListener.mOnPicTokenCb?.invoke(saveImg((size + 1) % 2))
                    takePhotoFlag = false
                }

                //avg->screen
                bindFrameBuffer(0)

                bindGLProgram2(frameTexture[(size + 1) % 2])

                render(viewWidth.toInt(), viewHeight.toInt())

                /*if (takePhotoFlag) {
                    mListener.mOnPicTokenCb?.invoke(saveImg())
                    takePhotoFlag = false
                }*/
            }
        }

        if (!isCapture) {
//            mListener.mOnFrameCb?.invoke()
        } else {
            mListener.mOnRenderDoneCb?.invoke()
        }
    }

    private fun render(viewWidth: Int, viewHeight: Int) {
        GLES20.glViewport(0, 0, viewWidth, viewHeight)

        // 清屏
        // Clear the screen
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        // 调用draw方法用TRIANGLES的方式执行渲染，顶点数量为3个
        // Call the draw method with GL_TRIANGLES to render 3 vertices
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertex.size / 3)

    }

    private fun bindYUVGLProgram(yData: ByteBuffer, uvData: ByteBuffer) {
        //设置清除渲染时的颜色
        GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f)

        // 开启纹理透明混合，这样才能绘制透明图片
        GLES20.glEnable(GL10.GL_BLEND)
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)

        // 应用GL程序
        // Use the GL program
        GLES20.glUseProgram(yuvProgram)

//        var vMatrixHandler = GLES20.glGetUniformLocation(program0, "vMatrix")

        // 获取字段a_position在shader中的位置
        // Get the location of a_position in the shader
        val aPositionLocation = GLES20.glGetAttribLocation(yuvProgram, "a_Position")

        // 启动对应位置的参数
        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 指定a_position所使用的顶点数据
        // Specify the data of a_position
        GLES20.glVertexAttribPointer(aPositionLocation, VERTEX_ATTRIB_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        // 获取字段a_textureCoordinate在shader中的位置
        // Get the location of a_textureCoordinate in the shader
        val aTextureCoordinateLocation = GLES20.glGetAttribLocation(yuvProgram, "a_texCoord")

        // 启动对应位置的参数
        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aTextureCoordinateLocation)

        // 指定a_textureCoordinate所使用的顶点数据
        // Specify the data of a_textureCoordinate
        GLES20.glVertexAttribPointer(aTextureCoordinateLocation, VERTEX_ATTRIB_TEXTURE_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, yuvTextureCoordBuffer)

        //绑定yuv三通道纹理数据到3、4、5纹理单元（0、1、2被oes和framebuffer交换纹理使用了）
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, dataWidth.toInt(), dataHeight.toInt(), 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, dataWidth, dataHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yData)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[1])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, dataWidth/2, dataHeight/2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, dataWidth / 2, dataHeight / 2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uvData)

        val matrixHandler = GLES20.glGetUniformLocation(yuvProgram, "vMatrix")
        GLES20.glUniformMatrix4fv(matrixHandler, 1, false, yuvTransformMatrix, 0)

        val coordMatrixHandler = GLES20.glGetUniformLocation(yuvProgram, "vCoordMatrix")
        GLES20.glUniformMatrix4fv(coordMatrixHandler, 1, false, mYUVCoordMatrix, 0)

        val yTextureLocation = GLES20.glGetUniformLocation(yuvProgram, "sampler_y")
        GLES20.glUniform1i(yTextureLocation, 3)

        val uvTextureLocation = GLES20.glGetUniformLocation(yuvProgram, "sampler_uv")
        GLES20.glUniform1i(uvTextureLocation, 4)
    }

    private fun bindGLProgram0(texture: Int) {

        //设置清除渲染时的颜色
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)

        // 开启纹理透明混合，这样才能绘制透明图片
        GLES20.glEnable(GL10.GL_BLEND)
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)

        // 应用GL程序
        // Use the GL program
        GLES20.glUseProgram(program0)

//        var vMatrixHandler = GLES20.glGetUniformLocation(program0, "vMatrix")


        // 获取字段a_position在shader中的位置
        // Get the location of a_position in the shader
        val aPositionLocation = GLES20.glGetAttribLocation(program0, "a_Position")

        // 启动对应位置的参数
        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 指定a_position所使用的顶点数据
        // Specify the data of a_position
        GLES20.glVertexAttribPointer(aPositionLocation, VERTEX_ATTRIB_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        // 获取字段a_textureCoordinate在shader中的位置
        // Get the location of a_textureCoordinate in the shader
        val aTextureCoordinateLocation = GLES20.glGetAttribLocation(program0, "a_texCoord")

        // 启动对应位置的参数
        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aTextureCoordinateLocation)

        // 指定a_textureCoordinate所使用的顶点数据
        // Specify the data of a_textureCoordinate
        GLES20.glVertexAttribPointer(aTextureCoordinateLocation, VERTEX_ATTRIB_TEXTURE_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, textureCoordBuffer)

        // 绑定纹理并设置u_texture参数
        // Bind the texture and set the u_texture parameter
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val coordMatrixHandler = GLES20.glGetUniformLocation(program0, "vCoordMatrix")
        val matrixHandler = GLES20.glGetUniformLocation(program0, "vMatrix")

        GLES20.glUniformMatrix4fv(matrixHandler, 1, false, mMVPMatrix, 0)
        GLES20.glUniformMatrix4fv(coordMatrixHandler, 1, false, mCoordMatrix, 0)

        val uTextureLocation = GLES20.glGetUniformLocation(program0, "s_texture")
        GLES20.glUniform1i(uTextureLocation, 0)
    }

    private fun bindGLProgram1(inTexture: Int, avgTexture: Int, size: Int) {

        //设置清除渲染时的颜色
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f)

        // 开启纹理透明混合，这样才能绘制透明图片
        GLES20.glEnable(GL10.GL_BLEND)
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)

        // 应用GL程序
        // Use the GL program
        GLES20.glUseProgram(program1)

        // 获取字段a_position在shader中的位置
        // Get the location of a_position in the shader
        val aPositionLocation = GLES20.glGetAttribLocation(program1, "a_Position")

        // 启动对应位置的参数
        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 指定a_position所使用的顶点数据
        // Specify the data of a_position
        GLES20.glVertexAttribPointer(aPositionLocation, VERTEX_ATTRIB_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        // 获取字段a_textureCoordinate在shader中的位置
        // Get the location of a_textureCoordinate in the shader
        val aTextureCoordinateLocation = GLES20.glGetAttribLocation(program1, "a_texCoord")

        // 启动对应位置的参数
        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aTextureCoordinateLocation)

        // 指定a_textureCoordinate所使用的顶点数据
        // Specify the data of a_textureCoordinate
        GLES20.glVertexAttribPointer(aTextureCoordinateLocation, VERTEX_ATTRIB_TEXTURE_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, textureCoordBuffer)

        var vMatrixHandler = GLES20.glGetUniformLocation(program1, "vMatrix")
        GLES20.glUniformMatrix4fv(vMatrixHandler, 1, false, frameBufferTransformMatrix, 0)

        val sizeHandler = GLES20.glGetUniformLocation(program1, "size")
        GLES20.glUniform1i(sizeHandler, size)

        // 绑定纹理并设置u_texture参数
        // Bind the texture and set the u_texture parameter
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inTexture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val uTextureLocation = GLES20.glGetUniformLocation(program1, "s_texture")
        GLES20.glUniform1i(uTextureLocation, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, avgTexture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val avgTextureLocation = GLES20.glGetUniformLocation(program1, "avg_texture")
        GLES20.glUniform1i(avgTextureLocation, 1)
    }

    private fun bindGLProgram2(texture: Int) {

        //设置清除渲染时的颜色
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)

        // 开启纹理透明混合，这样才能绘制透明图片
        GLES20.glEnable(GL10.GL_BLEND)
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)

        // 应用GL程序
        // Use the GL program
        GLES20.glUseProgram(program2)

        // 获取字段a_position在shader中的位置
        // Get the location of a_position in the shader
        val aPositionLocation = GLES20.glGetAttribLocation(program2, "a_Position")

        // 启动对应位置的参数
        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // 指定a_position所使用的顶点数据
        // Specify the data of a_position
        GLES20.glVertexAttribPointer(aPositionLocation, VERTEX_ATTRIB_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        // 获取字段a_textureCoordinate在shader中的位置
        // Get the location of a_textureCoordinate in the shader
        val aTextureCoordinateLocation = GLES20.glGetAttribLocation(program2, "a_texCoord")

        // 启动对应位置的参数
        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aTextureCoordinateLocation)

        // 指定a_textureCoordinate所使用的顶点数据
        // Specify the data of a_textureCoordinate
        GLES20.glVertexAttribPointer(aTextureCoordinateLocation, VERTEX_ATTRIB_TEXTURE_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, textureCoordBuffer)

        // 绑定纹理并设置u_texture参数
        // Bind the texture and set the u_texture parameter
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val matrixHandler = GLES20.glGetUniformLocation(program2, "vMatrix")
        GLES20.glUniformMatrix4fv(matrixHandler, 1, false, screenTransformMatrix, 0)

        val uTextureLocation = GLES20.glGetUniformLocation(program2, "s_texture")
        GLES20.glUniform1i(uTextureLocation, 0)

    }

    fun initProgram() {
        //创建并连接程序
        program0 = EGLUtil.createAndLinkProgram(
                R.raw.oes_texture_vertex_shader,
                R.raw.oes_fragtment_shader
        )

        program1 = EGLUtil.createAndLinkProgram(
                R.raw.texture_vertex_shader,
                R.raw.longexposure_fragtment_shader
        )

        program2 = EGLUtil.createAndLinkProgram(
                R.raw.texture_vertex_shader,
                R.raw.screen_fragtment_shader
        )

        yuvProgram = EGLUtil.createAndLinkProgram(
                R.raw.oes_texture_vertex_shader,
                R.raw.i420_to_rgb_fragtment_shader
        )
    }

    private fun calculateMatrix() {
        val dataRatio = previewWidth / previewHeight

        if (dataRatio > 1) {
        }
        val y: Float =
                viewHeight * dataWidth / (viewWidth * dataHeight)

        val xScale = viewWidth / dataWidth

        Matrix.orthoM(mMVPMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
//        Matrix.orthoM(frameBufferTransformMatrix, 0, -1f, 1f, -y, y, -1f, 1f)
        Matrix.orthoM(screenTransformMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)

//        Matrix.orthoM(yuvTransformMatrix, 0, -xScale, xScale, -xScale, xScale, -1f, 1f)
        Matrix.orthoM(yuvTransformMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
        Matrix.orthoM(mYUVCoordMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
//        Matrix.setIdentityM(yuvTransformMatrix, 0)

        Matrix.rotateM(yuvTransformMatrix, 0, (rotation % 360).toFloat(), 0f, 0f, 1f)

        //消除framebuffer的镜像
        Matrix.rotateM(screenTransformMatrix, 0, 180f, 1f, 0f, 0f)

        Matrix.setIdentityM(frameBufferTransformMatrix, 0)
//        Matrix.rotateM(frameBufferTransformMatrix, 0, -90f, 0f, 0f, 1f)
//        Matrix.rotateM(frameBufferTransformMatrix, 0, 180f, 1f, 0f, 0f)
    }
}