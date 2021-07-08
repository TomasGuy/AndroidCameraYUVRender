package com.example.android.camera.utils.renderer

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Size
import com.example.android.camera.utils.R
import com.gain.longexposure.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class OesRenderer : BaseRenderer() {

    private val VERTEX_ATTRIB_POSITION_SIZE = 3
    private val VERTEX_ATTRIB_TEXTURE_POSITION_SIZE = 2;

    private val vertex = floatArrayOf(
            -1f, 1f, 0.0f,  //left top
            -1f, -1f, 0.0f,  //left bottom
            1f, 1f, 0.0f,  //right bottom
            1f, -1f, 0.0f //right top
    )

    //（s,t）
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

    private val mYUVCoordMatrix: FloatArray = FloatArray(16)

    private val mMVPMatrix = FloatArray(16)
    private val yuvTransformMatrix = FloatArray(16)

    /**
     * YUV to rgba
     */
    private var yuvProgram = 0

    private val yuvTextureIds = IntArray(2)

    private lateinit var yData: ByteBuffer
    private lateinit var uvData: ByteBuffer

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


    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        initProgram()
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        viewWidth = width.toFloat()
        viewHeight = height.toFloat()
        calculateMatrix()

        initYUVTexture()
    }

    var size = 0
    override fun onDrawFrame(p0: GL10?) {
        if(this::yData.isInitialized&&this::uvData.isInitialized) {

            bindYUVGLProgram(yData, uvData)

            render(viewWidth.toInt(), viewHeight.toInt())
        }
    }

    private fun render(viewWidth: Int, viewHeight: Int) {
        GLES20.glViewport(0, 0, viewWidth, viewHeight)

        // Clear the screen
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        // Call the draw method with GL_TRIANGLES to render 3 vertices
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertex.size / 3)

    }

    private fun bindYUVGLProgram(yData: ByteBuffer, uvData: ByteBuffer) {
        GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f)

        GLES20.glEnable(GL10.GL_BLEND)
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA)

        // Use the GL program
        GLES20.glUseProgram(yuvProgram)

        // Get the location of a_position in the shader
        val aPositionLocation = GLES20.glGetAttribLocation(yuvProgram, "a_Position")

        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aPositionLocation)

        // Specify the data of a_position
        GLES20.glVertexAttribPointer(aPositionLocation, VERTEX_ATTRIB_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        // Get the location of a_textureCoordinate in the shader
        val aTextureCoordinateLocation = GLES20.glGetAttribLocation(yuvProgram, "a_texCoord")

        // Enable the parameter of the location
        GLES20.glEnableVertexAttribArray(aTextureCoordinateLocation)

        // Specify the data of a_textureCoordinate
        GLES20.glVertexAttribPointer(aTextureCoordinateLocation, VERTEX_ATTRIB_TEXTURE_POSITION_SIZE, GLES20.GL_FLOAT, false, 0, yuvTextureCoordBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, dataWidth, dataHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yData)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[1])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE_ALPHA, dataWidth / 2, dataHeight / 2, 0, GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, uvData)

        val matrixHandler = GLES20.glGetUniformLocation(yuvProgram, "vMatrix")
        GLES20.glUniformMatrix4fv(matrixHandler, 1, false, yuvTransformMatrix, 0)

        val coordMatrixHandler = GLES20.glGetUniformLocation(yuvProgram, "vCoordMatrix")
        GLES20.glUniformMatrix4fv(coordMatrixHandler, 1, false, mYUVCoordMatrix, 0)

        val yTextureLocation = GLES20.glGetUniformLocation(yuvProgram, "sampler_y")
        GLES20.glUniform1i(yTextureLocation, 0)

        val uvTextureLocation = GLES20.glGetUniformLocation(yuvProgram, "sampler_uv")
        GLES20.glUniform1i(uvTextureLocation, 1)
    }

    fun initProgram() {
        yuvProgram = EGLUtil.createAndLinkProgram(
                R.raw.oes_texture_vertex_shader,
                R.raw.yuv_to_rgb_fragtment_shader
        )
    }

    private fun calculateMatrix() {
        Matrix.orthoM(mMVPMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)

        Matrix.orthoM(mYUVCoordMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)

        Matrix.orthoM(yuvTransformMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
        Matrix.rotateM(yuvTransformMatrix, 0, -(rotation % 360).toFloat(), 0f, 0f, 1f)
        Matrix.rotateM(yuvTransformMatrix, 0, 180f, 1f, 0f, 0f)

    }
}