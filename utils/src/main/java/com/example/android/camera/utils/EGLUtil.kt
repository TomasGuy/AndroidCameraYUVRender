package com.gain.longexposure

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils.texImage2D
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer


class EGLUtil {
    companion object{
        private val TAG = "opengl-demos"

        private var context: Context? = null

        fun init(ctx: Context) {
            context = ctx.getApplicationContext()
        }

        /*********************** 纹理  */
        fun loadTexture(resId: Int): Int {
            //创建纹理对象
            val textureObjIds = IntArray(1)
            //生成纹理：纹理数量、保存纹理的数组，数组偏移量
            glGenTextures(1, textureObjIds, 0)
            if (textureObjIds[0] == 0) {
                throw RuntimeException("创建纹理对象失败")
            }
            //原尺寸加载位图资源（禁止缩放）
            val options = BitmapFactory.Options()
            options.inScaled = false
            val bitmap = BitmapFactory.decodeResource(context!!.getResources(), resId, options)
            if (bitmap == null) {
                //删除纹理对象
                glDeleteTextures(1, textureObjIds, 0)
                throw RuntimeException("加载位图失败")
            }
            //绑定纹理到opengl
            glBindTexture(GL_TEXTURE_2D, textureObjIds[0])
            //设置放大、缩小时的纹理过滤方式，必须设定，否则纹理全黑
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            //将位图加载到opengl中，并复制到当前绑定的纹理对象上
            texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
            //创建 mip 贴图
            glGenerateMipmap(GL_TEXTURE_2D)
            //释放bitmap资源（上面已经把bitmap的数据复制到纹理上了）
            bitmap.recycle()
            //解绑当前纹理，防止其他地方以外改变该纹理
            glBindTexture(GL_TEXTURE_2D, 0)
            //返回纹理对象
            return textureObjIds[0]
        }

        /*********************** 纹理  */
        fun loadBitmapTexture(bitmap: Bitmap): Int {
            //创建纹理对象
            val textureObjIds = IntArray(1)
            //生成纹理：纹理数量、保存纹理的数组，数组偏移量
            glGenTextures(1, textureObjIds, 0)
            if (textureObjIds[0] == 0) {
                throw RuntimeException("创建纹理对象失败")
            }
            //原尺寸加载位图资源（禁止缩放）
            if (bitmap == null) {
                //删除纹理对象
                glDeleteTextures(1, textureObjIds, 0)
                throw RuntimeException("加载位图失败")
            }
            //绑定纹理到opengl
            glBindTexture(GL_TEXTURE_2D, textureObjIds[0])
            //设置放大、缩小时的纹理过滤方式，必须设定，否则纹理全黑
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            //将位图加载到opengl中，并复制到当前绑定的纹理对象上
            texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
            //创建 mip 贴图
            glGenerateMipmap(GL_TEXTURE_2D)
            //释放bitmap资源（上面已经把bitmap的数据复制到纹理上了）
            bitmap.recycle()
            //解绑当前纹理，防止其他地方以外改变该纹理
            glBindTexture(GL_TEXTURE_2D, 0)
            //返回纹理对象
            return textureObjIds[0]
        }

        /*********************** 着色器、程序  */
        fun loadShaderSource(resId: Int): String? {
            val res = StringBuilder()
            val `is`: InputStream = context!!.getResources().openRawResource(resId)
            val isr = InputStreamReader(`is`)
            val br = BufferedReader(isr)
            var nextLine: String?
            try {
                while (br.readLine().also { nextLine = it } != null) {
                    res.append(nextLine)
                    res.append('\n')
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return res.toString()
        }

        /**
         * 加载着色器源，并编译
         *
         * @param type         顶点着色器（GL_VERTEX_SHADER）/片段着色器（GL_FRAGMENT_SHADER）
         * @param shaderSource 着色器源
         * @return 着色器
         */
        fun loadShader(type: Int, shaderSource: String?): Int {
            //创建着色器对象
            val shader = glCreateShader(type)
            if (shader == 0) return 0 //创建失败
            //加载着色器源
            glShaderSource(shader, shaderSource)
            //编译着色器
            glCompileShader(shader)
            //检查编译状态
            val compiled = IntArray(1)
            glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, glGetShaderInfoLog(shader))
                glDeleteShader(shader)
                return 0 //编译失败
            }
            return shader
        }

        fun createAndLinkProgram(vertextShaderResId: Int, fragmentShaderResId: Int): Int {
            //获取顶点着色器
            val vertexShader: Int = loadShader(
                GL_VERTEX_SHADER,
                loadShaderSource(vertextShaderResId)
            )
            if (0 == vertexShader) {
                Log.e(TAG, "failed to load vertexShader")
                return 0
            }
            //获取片段着色器
            val fragmentShader: Int = loadShader(
                GL_FRAGMENT_SHADER,
                loadShaderSource(fragmentShaderResId)
            )
            if (0 == fragmentShader) {
                Log.e(TAG, "failed to load fragmentShader")
                return 0
            }
            val program = glCreateProgram()
            if (program == 0) {
                Log.e(TAG, "failed to create program")
            }
            //绑定着色器到程序
            glAttachShader(program, vertexShader)
            glAttachShader(program, fragmentShader)
            //连接程序
            glLinkProgram(program)
            //检查连接状态
            val linked = IntArray(1)
            glGetProgramiv(program, GL_LINK_STATUS, linked, 0)
            if (linked[0] == 0) {
                glDeleteProgram(program)
                Log.e(TAG, "failed to link program")
                return 0
            }
            return program
        }

        fun createProgram(vertexSource: String, fragmentSource: String): Int {
            //加载顶点着色器
            val vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource)
            if (vertexShader == 0) {
                return 0
            }

            // 加载片元着色器
            val pixelShader = loadShader(GL_FRAGMENT_SHADER, fragmentSource)
            if (pixelShader == 0) {
                return 0
            }

            // 创建程序
            var program = glCreateProgram()
            // 若程序创建成功则向程序中加入顶点着色器与片元着色器
            if (program != 0) {
                // 向程序中加入顶点着色器
                glAttachShader(program, vertexShader)
                // 向程序中加入片元着色器
                glAttachShader(program, pixelShader)
                // 链接程序
                glLinkProgram(program)
                // 存放链接成功program数量的数组
                val linkStatus = IntArray(1)
                // 获取program的链接情况
                glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)
                // 若链接失败则报错并删除程序
                if (linkStatus[0] != GL_TRUE) {
                    Log.e("ES20_ERROR", "Could not link program: ")
                    Log.e("ES20_ERROR", glGetProgramInfoLog(program))
                    glDeleteProgram(program)
                    program = 0
                }
            }
            return program
        }

        /*********************** （暂时放这，后面统一组织） */
        fun getVertextBuffer(): FloatBuffer? {
            return getFloatBuffer(VERTEX)
        }

        fun getVertexColorBuffer(): FloatBuffer? {
            return getFloatBuffer(VERTEX_COLORS)
        }

        fun getTextureCoordBuffer(): FloatBuffer? {
            return getFloatBuffer(TEXTURE_COORD)
        }

        fun getFloatBuffer(array: FloatArray): FloatBuffer? {
            //将数据拷贝映射到 native 内存中，以便opengl能够访问
            val buffer: FloatBuffer = ByteBuffer
                    .allocateDirect(array.size * BYTES_PER_FLOAT) //直接分配 native 内存，不会被gc
                    .order(ByteOrder.nativeOrder()) //和本地平台保持一致的字节序（大/小头）
                    .asFloatBuffer() //将底层字节映射到FloatBuffer实例，方便使用
            buffer
                    .put(array) //将顶点拷贝到 native 内存中
                    .position(0) //每次 put position 都会 + 1，需要在绘制前重置为0
            return buffer
        }

        fun getShortBuffer(array: ShortArray): ShortBuffer? {
            //将数据拷贝映射到 native 内存中，以便opengl能够访问
            val buffer: ShortBuffer = ByteBuffer
                    .allocateDirect(array.size * BYTES_PER_SHORT) //直接分配 native 内存，不会被gc
                    .order(ByteOrder.nativeOrder()) //和本地平台保持一致的字节序（大/小头）
                    .asShortBuffer() //将底层字节映射到Buffer实例，方便使用
            buffer
                    .put(array) //将顶点拷贝到 native 内存中
                    .position(0) //每次 put position 都会增加，需要在绘制前重置为0
            return buffer
        }

        //各数值类型字节数
        val BYTES_PER_FLOAT = 4
        val BYTES_PER_SHORT = 2

        //顶点，按逆时针顺序排列
        val VERTEX = floatArrayOf(
                0.0f, 0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f)

        //顶点颜色
        val VERTEX_COLORS = floatArrayOf(
                0.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f
        )

        //纹理坐标，（s,t），t坐标方向和顶点y坐标反着
        val TEXTURE_COORD = floatArrayOf(
                0.5f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f
        )
    }
}