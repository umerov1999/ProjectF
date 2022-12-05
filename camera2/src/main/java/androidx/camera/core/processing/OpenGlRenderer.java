/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.core.processing;

import static java.util.Objects.requireNonNull;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.camera.core.Logger;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenGLRenderer renders texture image to the output surface.
 *
 * <p>OpenGLRenderer's methods must run on the same thread, so called GL thread. The GL thread is
 * locked as the thread running the {@link #init(ShaderProvider)} method, otherwise an
 * {@link IllegalStateException} will be thrown when other methods are called.
 */
@WorkerThread
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class OpenGlRenderer {
    private static final String TAG = "OpenGlRenderer";

    private static final String VAR_TEXTURE_COORD = "vTextureCoord";
    private static final String VAR_TEXTURE = "sTexture";

    private static final String DEFAULT_VERTEX_SHADER = String.format(Locale.US,
            "uniform mat4 uTexMatrix;\n"
                    + "attribute vec4 aPosition;\n"
                    + "attribute vec4 aTextureCoord;\n"
                    + "varying vec2 %s;\n"
                    + "void main() {\n"
                    + "    gl_Position = aPosition;\n"
                    + "    %s = (uTexMatrix * aTextureCoord).xy;\n"
                    + "}\n", VAR_TEXTURE_COORD, VAR_TEXTURE_COORD);

    private static final String DEFAULT_FRAGMENT_SHADER = String.format(Locale.US,
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "varying vec2 %s;\n"
                    + "uniform samplerExternalOES %s;\n"
                    + "void main() {\n"
                    + "    gl_FragColor = texture2D(%s, %s);\n"
                    + "}\n", VAR_TEXTURE_COORD, VAR_TEXTURE, VAR_TEXTURE, VAR_TEXTURE_COORD);

    private static final float[] VERTEX_COORDS = {
            -1.0f, -1.0f,   // 0 bottom left
            1.0f, -1.0f,    // 1 bottom right
            -1.0f,  1.0f,   // 2 top left
            1.0f,  1.0f,    // 3 top right
    };
    private static final FloatBuffer VERTEX_BUF = createFloatBuffer(VERTEX_COORDS);

    private static final float[] TEX_COORDS = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };
    private static final FloatBuffer TEX_BUF = createFloatBuffer(TEX_COORDS);

    private static final int SIZEOF_FLOAT = 4;
    private static final int TEX_TARGET = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

    private final AtomicBoolean mInitialized = new AtomicBoolean(false);
    private final Map<Surface, OutputSurface> mOutputSurfaceMap = new HashMap<>();
    @Nullable
    private Thread mGlThread;
    @NonNull
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    @NonNull
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    @Nullable
    private EGLConfig mEglConfig;
    @NonNull
    private EGLSurface mTempSurface = EGL14.EGL_NO_SURFACE;
    @Nullable
    private OutputSurface mCurrentOutputSurface;
    private int mTexId = -1;
    private int mProgramHandle = -1;
    private int mTexMatrixLoc = -1;
    private int mPositionLoc = -1;
    private int mTexCoordLoc = -1;

    /**
     * Initializes the OpenGLRenderer
     *
     * <p>Initialization must be done before calling other methods, otherwise an
     * {@link IllegalStateException} will be thrown. Following methods must run on the same
     * thread as this method, so called GL thread, otherwise an {@link IllegalStateException}
     * will be thrown.
     *
     * @throws IllegalStateException if the renderer is already initialized or failed to be
     * initialized.
     * @throws IllegalArgumentException if the ShaderProvider fails to create shader or provides
     * invalid shader string.
     */
    public void init(@NonNull ShaderProvider shaderProvider) {
        checkInitializedOrThrow(false);
        try {
            createEglContext();
            createTempSurface();
            makeCurrent(mTempSurface);
            createProgram(shaderProvider);
            loadLocations();
            createTexture();
        } catch (IllegalStateException | IllegalArgumentException e) {
            releaseInternal();
            throw e;
        }
        mGlThread = Thread.currentThread();
        mInitialized.set(true);
    }

    /**
     * Releases the OpenGLRenderer
     *
     * @throws IllegalStateException if the caller doesn't run on the GL thread.
     */
    public void release() {
        if (!mInitialized.getAndSet(false)) {
            return;
        }
        checkGlThreadOrThrow();
        releaseInternal();
    }

    /**
     * Set the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     * on the GL thread.
     */
    public void setOutputSurface(@NonNull Surface surface) {
        checkInitializedOrThrow(true);
        checkGlThreadOrThrow();

        if (!mOutputSurfaceMap.containsKey(surface)) {
            EGLSurface eglSurface = createWindowSurface(mEglDisplay, requireNonNull(mEglConfig),
                    surface);
            Size size = getSurfaceSize(eglSurface);
            mOutputSurfaceMap.put(surface, OutputSurface.of(eglSurface, size.getWidth(),
                    size.getHeight()));
        }
        mCurrentOutputSurface = requireNonNull(mOutputSurfaceMap.get(surface));
        makeCurrent(mCurrentOutputSurface.getEglSurface());

        GLES20.glViewport(0, 0, mCurrentOutputSurface.getWidth(),
                mCurrentOutputSurface.getHeight());
        GLES20.glScissor(0, 0, mCurrentOutputSurface.getWidth(), mCurrentOutputSurface.getHeight());
    }

    /**
     * Gets the texture name.
     *
     * @return the texture name
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     * on the GL thread.
     */
    public int getTextureName() {
        checkInitializedOrThrow(true);
        checkGlThreadOrThrow();

        return mTexId;
    }

    /**
     * Renders the texture image to the output surface.
     *
     * @throws IllegalStateException if the renderer is not initialized or the caller doesn't run
     * on the GL thread.
     */
    public void render(long timestampNs, @NonNull float[] textureTransform) {
        checkInitializedOrThrow(true);
        checkGlThreadOrThrow();

        if (mCurrentOutputSurface == null) {
            return;
        }

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        checkGlErrorOrThrow("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(TEX_TARGET, mTexId);

        // TODO(b/245855601): Upload the matrix to GPU when textureTransform is changed.
        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(mTexMatrixLoc, /*count=*/1, /*transpose=*/false, textureTransform,
                /*offset=*/0);
        checkGlErrorOrThrow("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(mPositionLoc);
        checkGlErrorOrThrow("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        int coordsPerVertex = 2;
        int vertexStride = 0;
        GLES20.glVertexAttribPointer(mPositionLoc, coordsPerVertex, GLES20.GL_FLOAT,
                /*normalized=*/false, vertexStride, VERTEX_BUF);
        checkGlErrorOrThrow("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);
        checkGlErrorOrThrow("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        int coordsPerTex = 2;
        int texStride = 0;
        GLES20.glVertexAttribPointer(mTexCoordLoc, coordsPerTex, GLES20.GL_FLOAT,
                /*normalized=*/false, texStride, TEX_BUF);
        checkGlErrorOrThrow("glVertexAttribPointer");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /*firstVertex=*/0, /*vertexCount=*/4);
        checkGlErrorOrThrow("glDrawArrays");

        // TODO(b/245855601): Figure out if these calls are necessary.
        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(mPositionLoc);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
        GLES20.glUseProgram(0);
        GLES20.glBindTexture(TEX_TARGET, 0);

        // Set timestamp
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, mCurrentOutputSurface.getEglSurface(),
                timestampNs);

        // Swap buffer
        if (!EGL14.eglSwapBuffers(mEglDisplay, mCurrentOutputSurface.getEglSurface())) {
            Logger.w(TAG, "Failed to swap buffers with EGL error: 0x" + Integer.toHexString(
                    EGL14.eglGetError()));
        }
    }

    private void createEglContext() {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY)) {
            throw new IllegalStateException("Unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = EGL14.EGL_NO_DISPLAY;
            throw new IllegalStateException("Unable to initialize EGL14");
        }
        int[] attribToChooseConfig = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGLExt.EGL_RECORDABLE_ANDROID, EGL14.EGL_TRUE,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT | EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, attribToChooseConfig, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            throw new IllegalStateException("Unable to find a suitable EGLConfig");
        }
        EGLConfig config = configs[0];
        int[] attribToCreateContext = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        EGLContext context = EGL14.eglCreateContext(mEglDisplay, config, EGL14.EGL_NO_CONTEXT,
                attribToCreateContext, 0);
        checkEglErrorOrThrow("eglCreateContext");
        mEglConfig = config;
        mEglContext = context;

        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEglDisplay, mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values,
                0);
        Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    private void createTempSurface() {
        mTempSurface = createPBufferSurface(mEglDisplay, requireNonNull(mEglConfig), /*width=*/1,
                /*height=*/1);
    }

    private void makeCurrent(@NonNull EGLSurface eglSurface) {
        Preconditions.checkNotNull(mEglDisplay);
        Preconditions.checkNotNull(mEglContext);
        if (!EGL14.eglMakeCurrent(mEglDisplay, eglSurface, eglSurface, mEglContext)) {
            throw new IllegalStateException("eglMakeCurrent failed");
        }
    }

    private void createProgram(@NonNull ShaderProvider shaderProvider) {
        int vertexShader = -1;
        int fragmentShader = -1;
        int program = -1;
        try {
            vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, DEFAULT_VERTEX_SHADER);
            fragmentShader = loadFragmentShader(shaderProvider);
            program = GLES20.glCreateProgram();
            checkGlErrorOrThrow("glCreateProgram");
            GLES20.glAttachShader(program, vertexShader);
            checkGlErrorOrThrow("glAttachShader");
            GLES20.glAttachShader(program, fragmentShader);
            checkGlErrorOrThrow("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, /*offset=*/0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                throw new IllegalStateException(
                        "Could not link program: " + GLES20.glGetProgramInfoLog(program));
            }
            mProgramHandle = program;
        } catch (IllegalStateException | IllegalArgumentException e) {
            if (vertexShader != -1) {
                GLES20.glDeleteShader(vertexShader);
            }
            if (fragmentShader != -1) {
                GLES20.glDeleteShader(fragmentShader);
            }
            if (program != -1) {
                GLES20.glDeleteProgram(program);
            }
            throw e;
        }
    }

    private void loadLocations() {
        mPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        checkLocationOrThrow(mPositionLoc, "aPosition");
        mTexCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        checkLocationOrThrow(mTexCoordLoc, "aTextureCoord");
        mTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        checkLocationOrThrow(mTexMatrixLoc, "uTexMatrix");
    }

    private void createTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlErrorOrThrow("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(TEX_TARGET, texId);
        checkGlErrorOrThrow("glBindTexture " + texId);

        GLES20.glTexParameterf(TEX_TARGET, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(TEX_TARGET, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(TEX_TARGET, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(TEX_TARGET, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkGlErrorOrThrow("glTexParameter");

        mTexId = texId;
    }

    private int loadFragmentShader(@NonNull ShaderProvider shaderProvider) {
        if (shaderProvider == ShaderProvider.DEFAULT) {
            return loadShader(GLES20.GL_FRAGMENT_SHADER, DEFAULT_FRAGMENT_SHADER);
        } else {
            // Throw IllegalArgumentException if the shader provider can not provide a valid
            // fragment shader.
            String source;
            try {
                source = shaderProvider.createFragmentShader(VAR_TEXTURE, VAR_TEXTURE_COORD);
                // A simple check to workaround custom shader doesn't contain required variable.
                // See b/241193761.
                if (source == null || !source.contains(VAR_TEXTURE_COORD) || !source.contains(
                        VAR_TEXTURE)) {
                    throw new IllegalArgumentException("Invalid fragment shader");
                }
                return loadShader(GLES20.GL_FRAGMENT_SHADER, source);
            } catch (Throwable t) {
                if (t instanceof IllegalArgumentException) {
                    throw t;
                }
                throw new IllegalArgumentException("Unable to compile fragment shader", t);
            }
        }
    }

    @NonNull
    private Size getSurfaceSize(@NonNull EGLSurface eglSurface) {
        int width = querySurface(mEglDisplay, eglSurface, EGL14.EGL_WIDTH);
        int height = querySurface(mEglDisplay, eglSurface, EGL14.EGL_HEIGHT);
        return new Size(width, height);
    }

    private void releaseInternal() {
        // Delete program
        if (mProgramHandle != -1) {
            GLES20.glDeleteProgram(mProgramHandle);
            mProgramHandle = -1;
        }

        // Destroy EGLSurfaces
        for (OutputSurface outputSurface : mOutputSurfaceMap.values()) {
            EGL14.eglDestroySurface(mEglDisplay, outputSurface.getEglSurface());
        }
        mOutputSurfaceMap.clear();

        // Destroy temp surface
        if (!Objects.equals(mTempSurface, EGL14.EGL_NO_SURFACE)) {
            EGL14.eglDestroySurface(mEglDisplay, mTempSurface);
            mTempSurface = EGL14.EGL_NO_SURFACE;
        }

        // Destroy EGLContext and terminate display
        if (!Objects.equals(mEglDisplay, EGL14.EGL_NO_DISPLAY)) {
            if (!Objects.equals(mEglContext, EGL14.EGL_NO_CONTEXT)) {
                // Ignore the result of eglMakeCurrent with EGL_NO_SURFACE because it returns false
                // on some devices.
                EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        mEglContext);
                EGL14.eglDestroyContext(mEglDisplay, mEglContext);
                mEglContext = EGL14.EGL_NO_CONTEXT;
            }
            EGL14.eglTerminate(mEglDisplay);
            mEglDisplay = EGL14.EGL_NO_DISPLAY;
        }

        // Reset other members
        mEglConfig = null;
        mProgramHandle = -1;
        mTexMatrixLoc = -1;
        mPositionLoc = -1;
        mTexCoordLoc = -1;
        mTexId = -1;
        mCurrentOutputSurface = null;
        mGlThread = null;
    }

    private void checkInitializedOrThrow(boolean shouldInitialized) {
        boolean result = shouldInitialized == mInitialized.get();
        String message = shouldInitialized ? "OpenGlRenderer is not initialized"
                : "OpenGlRenderer is already initialized";
        Preconditions.checkState(result, message);
    }

    private void checkGlThreadOrThrow() {
        Preconditions.checkState(mGlThread == Thread.currentThread(),
                "Method call must be called on the GL thread.");
    }

    @SuppressWarnings("SameParameterValue") // currently hard code width/height with 1/1
    @NonNull
    private static EGLSurface createPBufferSurface(@NonNull EGLDisplay eglDisplay,
            @NonNull EGLConfig eglConfig, int width, int height) {
        int[] surfaceAttrib = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttrib,
                /*offset=*/0);
        checkEglErrorOrThrow("eglCreatePbufferSurface");
        if (eglSurface == null) {
            throw new IllegalStateException("surface was null");
        }
        return eglSurface;
    }

    @NonNull
    private static EGLSurface createWindowSurface(@NonNull EGLDisplay eglDisplay,
            @NonNull EGLConfig eglConfig, @NonNull Surface surface) {
        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttrib = {
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface,
                surfaceAttrib, /*offset=*/0);
        checkEglErrorOrThrow("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new IllegalStateException("surface was null");
        }
        return eglSurface;
    }

    private static int loadShader(int shaderType, @NonNull String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlErrorOrThrow("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, /*offset=*/0);
        if (compiled[0] == 0) {
            Logger.w(TAG, "Could not compile shader: " + source);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException(
                    "Could not compile shader type " + shaderType + ":" + GLES20.glGetShaderInfoLog(
                            shader));
        }
        return shader;
    }

    private static int querySurface(@NonNull EGLDisplay eglDisplay, @NonNull EGLSurface eglSurface,
             int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(eglDisplay, eglSurface, what, value, /*offset=*/0);
        return value[0];
    }

    @NonNull
    public static FloatBuffer createFloatBuffer(@NonNull float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    private static void checkLocationOrThrow(int location, @NonNull String label) {
        if (location < 0) {
            throw new IllegalStateException("Unable to locate '" + label + "' in program");
        }
    }

    private static void checkEglErrorOrThrow(@NonNull String op) {
        int error = EGL14.eglGetError();
        if (error != EGL14.EGL_SUCCESS) {
            throw new IllegalStateException(op + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    private static void checkGlErrorOrThrow(@NonNull String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            throw new IllegalStateException(op + ": GL error 0x" + Integer.toHexString(error));
        }
    }

    @AutoValue
    abstract static class OutputSurface {

        @NonNull
        static OutputSurface of(@NonNull EGLSurface eglSurface, int width, int height) {
            return new AutoValue_OpenGlRenderer_OutputSurface(eglSurface, width, height);
        }

        @NonNull
        abstract EGLSurface getEglSurface();

        abstract int getWidth();

        abstract int getHeight();
    }
}
