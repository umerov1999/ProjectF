package com.yalantis.ucrop.util

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.util.Log

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 9/8/16.
 */
object EglUtils {
    private const val TAG = "EglUtils"

    val maxTextureSize: Int
        get() = try {
            maxTextureEgl14
        } catch (e: Exception) {
            Log.d(TAG, "getMaxTextureSize: ", e)
            0
        }

    private val maxTextureEgl14: Int
        get() {
            val dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val vers = IntArray(2)
            EGL14.eglInitialize(dpy, vers, 0, vers, 1)
            val configAttr = intArrayOf(
                EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
                EGL14.EGL_LEVEL, 0,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfig = IntArray(1)
            EGL14.eglChooseConfig(
                dpy, configAttr, 0,
                configs, 0, 1, numConfig, 0
            )
            if (numConfig[0] == 0) {
                return 0
            }
            val config = configs[0]
            val surfAttr = intArrayOf(
                EGL14.EGL_WIDTH, 64,
                EGL14.EGL_HEIGHT, 64,
                EGL14.EGL_NONE
            )
            val surf = EGL14.eglCreatePbufferSurface(dpy, config, surfAttr, 0)
            val ctxAttrib = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            val ctx = EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0)
            EGL14.eglMakeCurrent(dpy, surf, surf, ctx)
            val maxSize = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0)
            EGL14.eglMakeCurrent(
                dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(dpy, surf)
            EGL14.eglDestroyContext(dpy, ctx)
            EGL14.eglTerminate(dpy)
            return maxSize[0]
        }
}
