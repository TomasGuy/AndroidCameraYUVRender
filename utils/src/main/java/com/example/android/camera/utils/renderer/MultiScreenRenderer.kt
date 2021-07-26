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


class MultiScreenRenderer : BaseRenderer() {

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

    private var vertexBuffer: FloatBuffer? = null
    private var textureCoordBuffer: FloatBuffer? = null

    private val mCoordMatrix: FloatArray = FloatArray(16)

    private val mMVPMatrix = FloatArray(16)

    /**
     * OES预览
     */
    private var program0 = 0

    init {
        initVertexAttrib()

        Matrix.setIdentityM(mMVPMatrix, 0)
    }

    private fun initVertexAttrib() {
        textureCoordBuffer = EGLUtil.getFloatBuffer(textureCoord)
        vertexBuffer = EGLUtil.getFloatBuffer(vertex)
    }

    public override fun setDataSize(size: Size) {
        dataWidth = size.width
        dataHeight = size.height
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        initProgram()

        Log.i("GLSL", "onSurfaceCreated")
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        previewWidth = width.toFloat()
        previewHeight = height.toFloat()
        calculateMatrix()
    }

    override fun onDrawFrame(p0: GL10?) {
        if(OesRenderer.OES_TEXTURE_ID!=0) {
            // Bind GL program 0
            bindGLProgram0(OesRenderer.OES_TEXTURE_ID)

            render(previewWidth.toInt(), previewHeight.toInt())

            Log.i("GLView", "OesRenderer.OES_TEXTURE_ID:${OesRenderer.OES_TEXTURE_ID}")
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
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val coordMatrixHandler = GLES20.glGetUniformLocation(program0, "vCoordMatrix")
        val matrixHandler = GLES20.glGetUniformLocation(program0, "vMatrix")

        GLES20.glUniformMatrix4fv(matrixHandler, 1, false, mMVPMatrix, 0)
        GLES20.glUniformMatrix4fv(coordMatrixHandler, 1, false, mCoordMatrix, 0)

        val uTextureLocation = GLES20.glGetUniformLocation(program0, "s_texture")
        GLES20.glUniform1i(uTextureLocation, 1)
    }

    fun initProgram() {
        //创建并连接程序
        program0 = EGLUtil.createAndLinkProgram(
                R.raw.oes_texture_vertex_shader,
                R.raw.multi_screen_fragtment_shader
        )
    }

    private fun calculateMatrix() {
        Matrix.orthoM(mMVPMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
    }
}