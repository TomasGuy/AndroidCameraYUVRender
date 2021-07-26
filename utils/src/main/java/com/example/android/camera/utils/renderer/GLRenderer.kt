package com.example.android.camera.utils.renderer

import android.graphics.SurfaceTexture
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.example.android.camera.utils.R
import com.example.android.camera.utils.gles.EglCore
import com.example.android.camera.utils.gles.WindowSurface
import com.gain.longexposure.EGLUtil
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

class GLRenderer : HandlerThread("GLRenderer") {
    private var mEglCore: EglCore? = null
    private var mDisplaySurface: WindowSurface? = null
    //接收相机数据的 SurfaceTexture
    var surfaceTexture: SurfaceTexture? = null

    private var vPosition = 0
    private var uColor = 0

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

    //接收相机数据的纹理
    private val oesTextureIds = IntArray(1)

    var dataWidth = 1
    var dataHeight = 1

    /**
     * OES预览
     */
    private var program0 = 0

    companion object {
        var SHARED_EGL_CONTEXT:EGLContext?=null
        var OES_TEXTURE_ID:Int=0
        var COORD_MATRIX: FloatArray = FloatArray(16)

    }

    init {
        initVertexAttrib()

        Matrix.setIdentityM(mMVPMatrix, 0)

        calculateMatrix()
    }

    private fun initVertexAttrib() {
        textureCoordBuffer = EGLUtil.getFloatBuffer(textureCoord)
        vertexBuffer = EGLUtil.getFloatBuffer(vertex)
    }

    public fun setDataSize(size: Size) {
        dataWidth = size.width.coerceAtMost(size.height)
        dataHeight = size.width.coerceAtLeast(size.height)
    }

    /**
     * 创建OpenGL环境
     */
    public fun createGL(context: EGLContext?, surface: Surface?) {
        Handler(looper).post {
            // Set up everything that requires an EGL context.
            //
            // We had to wait until we had a surface because you can't make an EGL context current
            // without one, and creating a temporary 1x1 pbuffer is a waste of time.
            //
            // The display surface that we use for the SurfaceView, and the encoder surface we
            // use for video, use the same EGL context.
            mEglCore = EglCore(context, -1)
            mDisplaySurface = WindowSurface(mEglCore, surface, false)
            mDisplaySurface!!.makeCurrent()

            if(SHARED_EGL_CONTEXT==null) {
                SHARED_EGL_CONTEXT = EGL14.eglGetCurrentContext()

                //创建纹理对象
                GLES20.glGenTextures(oesTextureIds.size, oesTextureIds, 0)

                OES_TEXTURE_ID = oesTextureIds[0]

                //将纹理对象textureId[0]绑定到surfaceTexture
                surfaceTexture = SurfaceTexture(oesTextureIds[0])

                var renderFlag = false
                surfaceTexture!!.setOnFrameAvailableListener {
                    if(renderFlag.not()) {
                        render()
                        renderFlag = renderFlag.not()
                    }
                }
            }
        }
    }

    /**
     * 销毁OpenGL环境
     */
    private fun destroyGL() {
        mEglCore!!.release()
    }

    @Synchronized
    override fun start() {
        super.start()
    }

    fun release() {
        Handler(looper).post {
            destroyGL()
            quit()
        }
    }

    fun render() {
        Handler(looper).post {
            while (true) {
                // Latch the next frame from the camera.
                mDisplaySurface!!.makeCurrent()

                Log.i("GLRender", "render")
                surfaceTexture?.let {
                    surfaceTexture!!.updateTexImage()
                    surfaceTexture!!.getTransformMatrix(mCoordMatrix)
                    COORD_MATRIX = mCoordMatrix
                }

                // 初始化着色器
                // 基于顶点着色器与片元着色器创建程序
                initProgram()

                bindGLProgram0(oesTextureIds[0])

                GLES20.glViewport(0, 0, dataWidth, dataHeight)

                // 清屏
                // Clear the screen
                GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

                // 调用draw方法用TRIANGLES的方式执行渲染，顶点数量为3个
                // Call the draw method with GL_TRIANGLES to render 3 vertices
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertex.size / 3)

                // 交换显存(将surface显存和显示器的显存交换)
                mDisplaySurface!!.swapBuffers()
            }
        }
    }

    private fun bindGLProgram0(texture: Int) {

        //设置清除渲染时的颜色
//        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)

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