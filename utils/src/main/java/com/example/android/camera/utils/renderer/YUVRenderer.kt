package com.example.android.camera.utils.renderer

import android.graphics.ImageFormat
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import android.util.Size
import com.example.android.camera.utils.R
import com.gain.longexposure.EGLUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class YUVRenderer : BaseRenderer() {

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
    private val yuvTransformMatrix = FloatArray(16)

    /**
     * YUV转rgba
     */
    private var yuvProgram = 0

    //接收yuv三通道的纹理
    private val yuvTextureIds = IntArray(3)

    private var yData: ByteBuffer? = null
    private var uData: ByteBuffer? = null
    private var vData: ByteBuffer? = null

    private var uvData: ByteBuffer? = null

    private var format: Int = ImageFormat.NV21

    init {
        initVertexAttrib()

        Matrix.setIdentityM(mMVPMatrix, 0)
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        initProgram()

        initYUVTexture()

        Log.i("GLSL", "onSurfaceCreated")
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        viewWidth = width.toFloat()
        viewHeight = height.toFloat()
        calculateMatrix()
    }

    private fun initVertexAttrib() {
        yuvTextureCoordBuffer = EGLUtil.getFloatBuffer(yuvTextureCoord)
        textureCoordBuffer = EGLUtil.getFloatBuffer(textureCoord)
        vertexBuffer = EGLUtil.getFloatBuffer(vertex)
    }

    fun renderYUV(y: ByteBuffer?, u: ByteBuffer?, v: ByteBuffer?, uv:ByteBuffer?, f:Int) {
        yData = y
        uData = u
        vData = v
        uvData = uv
        format = f

//        initProgram()
    }

    public override fun setDataSize(size: Size) {
        dataWidth = size.width
        dataHeight = size.height
    }

    private fun initYUVTexture() {
        GLES20.glGenTextures(yuvTextureIds.size, yuvTextureIds, 0)

        for (i in yuvTextureIds.indices) {
            //绑定纹理
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, yuvTextureIds[i])
            //设置环绕和过滤方式
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        }
    }

    override fun onDrawFrame(p0: GL10?) {
        if(format==ImageFormat.YUV_420_888) {
            if(yData!=null&&uData!=null&&vData!=null) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

                //yuv->in
                bindI420Program()

                render(1080, 810)
            }
        } else {
            if(yData!=null&&uvData!=null) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

                //yuv->in
                bindNV21Program()

                render(1080, 810)
            }
        }

    }

    private fun render(viewWidth: Int, viewHeight: Int) {
        GLES20.glViewport(0, 0, viewWidth, viewHeight)

        // 调用draw方法用TRIANGLES的方式执行渲染，顶点数量为3个
        // Call the draw method with GL_TRIANGLES to render 3 vertices
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertex.size / 3)

    }

    private fun bindI420Program() {
        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
        // 应用GL程序
        // Use the GL program
        GLES20.glUseProgram(yuvProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, dataWidth, dataHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yData)
        val yTextureLocation = GLES20.glGetUniformLocation(yuvProgram, "sampler_y")
        GLES20.glUniform1i(yTextureLocation, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[1])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, dataWidth / 2, dataHeight / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, uData)
        val uTextureLocation = GLES20.glGetUniformLocation(yuvProgram, "sampler_u")
        GLES20.glUniform1i(uTextureLocation, 1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[2])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, dataWidth / 2, dataHeight / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vData)
        val vTextureLocation = GLES20.glGetUniformLocation(yuvProgram, "sampler_v")
        GLES20.glUniform1i(vTextureLocation, 2)

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

        val matrixHandler = GLES20.glGetUniformLocation(yuvProgram, "vMatrix")
        GLES20.glUniformMatrix4fv(matrixHandler, 1, false, yuvTransformMatrix, 0)

        val coordMatrixHandler = GLES20.glGetUniformLocation(yuvProgram, "vCoordMatrix")
        GLES20.glUniformMatrix4fv(coordMatrixHandler, 1, false, mYUVCoordMatrix, 0)
    }

    private fun bindNV21Program() {
        // 应用GL程序
        // Use the GL program
        GLES20.glUseProgram(yuvProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, dataWidth, dataHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yData)
        val yTextureLocation = GLES20.glGetUniformLocation(yuvProgram, "sampler_y")
        GLES20.glUniform1i(yTextureLocation, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[1])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, dataWidth/2, dataHeight / 2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uvData)
        val uvTextureLocation = GLES20.glGetUniformLocation(yuvProgram, "sampler_uv")
        GLES20.glUniform1i(uvTextureLocation, 1)

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

        val matrixHandler = GLES20.glGetUniformLocation(yuvProgram, "vMatrix")
        GLES20.glUniformMatrix4fv(matrixHandler, 1, false, yuvTransformMatrix, 0)

        val coordMatrixHandler = GLES20.glGetUniformLocation(yuvProgram, "vCoordMatrix")
        GLES20.glUniformMatrix4fv(coordMatrixHandler, 1, false, mYUVCoordMatrix, 0)
    }

    fun initProgram() {
        if(format==ImageFormat.YUV_420_888) {
            yuvProgram = EGLUtil.createAndLinkProgram(
                    R.raw.oes_texture_vertex_shader,
                    R.raw.i420_to_rgb_fragtment_shader
            )
        } else {
            yuvProgram = EGLUtil.createAndLinkProgram(
                    R.raw.oes_texture_vertex_shader,
                    R.raw.nv21_to_rgb_fragtment_shader
            )
        }
    }

    private fun calculateMatrix() {
        Matrix.orthoM(mMVPMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)

        Matrix.orthoM(yuvTransformMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
        Matrix.orthoM(mYUVCoordMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)

        Matrix.rotateM(yuvTransformMatrix, 0, 180f, 1f, 0f, 0f)

    }
}