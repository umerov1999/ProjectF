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

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A default implementation of {@link SurfaceProcessor}.
 *
 * <p> This implementation simply copies the frame from the source to the destination with the
 * transformation defined in {@link SurfaceOutput#updateTransformMatrix}.
 */
@RequiresApi(21)
public class DefaultSurfaceProcessor implements SurfaceProcessorInternal,
        SurfaceTexture.OnFrameAvailableListener {
    private final OpenGlRenderer mGlRenderer;
    @VisibleForTesting
    final HandlerThread mGlThread;
    private final Executor mGlExecutor;
    @VisibleForTesting
    final Handler mGlHandler;
    private final AtomicBoolean mIsReleased = new AtomicBoolean(false);
    private final float[] mTextureMatrix = new float[16];
    private final float[] mSurfaceOutputMatrix = new float[16];
    // Map of current set of available outputs. Only access this on GL thread.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Map<SurfaceOutput, Surface> mOutputSurfaces = new LinkedHashMap<>();

    // Only access this on GL thread.
    private int mInputSurfaceCount = 0;

    /** Constructs {@link DefaultSurfaceProcessor} with default shaders. */
    public DefaultSurfaceProcessor() {
        this(ShaderProvider.DEFAULT);
    }

    /**
     * Constructs {@link DefaultSurfaceProcessor} with custom shaders.
     *
     * @param shaderProvider custom shader provider for OpenGL rendering.
     * @throws IllegalArgumentException if the shaderProvider provides invalid shader.
     */
    public DefaultSurfaceProcessor(@NonNull ShaderProvider shaderProvider) {
        mGlThread = new HandlerThread("GL Thread");
        mGlThread.start();
        mGlHandler = new Handler(mGlThread.getLooper());
        mGlExecutor = CameraXExecutors.newHandlerExecutor(mGlHandler);
        mGlRenderer = new OpenGlRenderer();
        try {
            initGlRenderer(shaderProvider);
        } catch (RuntimeException e) {
            release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInputSurface(@NonNull SurfaceRequest surfaceRequest) {
        if (mIsReleased.get()) {
            surfaceRequest.willNotProvideSurface();
            return;
        }
        mGlExecutor.execute(() -> {
            mInputSurfaceCount++;
            SurfaceTexture surfaceTexture = new SurfaceTexture(mGlRenderer.getTextureName());
            surfaceTexture.setDefaultBufferSize(surfaceRequest.getResolution().getWidth(),
                    surfaceRequest.getResolution().getHeight());
            Surface surface = new Surface(surfaceTexture);
            surfaceRequest.provideSurface(surface, mGlExecutor, result -> {
                surfaceTexture.setOnFrameAvailableListener(null);
                surfaceTexture.release();
                surface.release();
                mInputSurfaceCount--;
                checkReadyToRelease();
            });
            surfaceTexture.setOnFrameAvailableListener(this, mGlHandler);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
        if (mIsReleased.get()) {
            surfaceOutput.close();
            return;
        }
        mGlExecutor.execute(() ->
                mOutputSurfaces.put(surfaceOutput, surfaceOutput.getSurface(mGlExecutor, event -> {
                    surfaceOutput.close();
                    mOutputSurfaces.remove(surfaceOutput);
                }))
        );
    }

    /**
     * Release the {@link DefaultSurfaceProcessor}.
     */
    @Override
    public void release() {
        if (mIsReleased.getAndSet(true)) {
            return;
        }
        mGlExecutor.execute(this::checkReadyToRelease);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFrameAvailable(@NonNull SurfaceTexture surfaceTexture) {
        if (mIsReleased.get()) {
            // Ignore frame update if released.
            return;
        }

        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(mTextureMatrix);

        for (Map.Entry<SurfaceOutput, Surface> entry : mOutputSurfaces.entrySet()) {
            Surface surface = entry.getValue();
            SurfaceOutput surfaceOutput = entry.getKey();
            mGlRenderer.setOutputSurface(surface);
            surfaceOutput.updateTransformMatrix(mSurfaceOutputMatrix, mTextureMatrix);
            mGlRenderer.render(surfaceTexture.getTimestamp(), mSurfaceOutputMatrix);
        }
    }

    @WorkerThread
    private void checkReadyToRelease() {
        if (mIsReleased.get() && mInputSurfaceCount == 0) {
            // Once release is called, we can stop sending frame to output surfaces.
            for (SurfaceOutput surfaceOutput : mOutputSurfaces.keySet()) {
                surfaceOutput.close();
            }
            mOutputSurfaces.clear();
            mGlRenderer.release();
            mGlThread.quit();
        }
    }

    private void initGlRenderer(@NonNull ShaderProvider shaderProvider) {
        ListenableFuture<Void> initFuture = CallbackToFutureAdapter.getFuture(completer -> {
            mGlExecutor.execute(() -> {
                try {
                    mGlRenderer.init(shaderProvider);
                    completer.set(null);
                } catch (RuntimeException e) {
                    completer.setException(e);
                }
            });
            return "Init GlRenderer";
        });
        try {
            initFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            // If the cause is a runtime exception, throw it directly. Otherwise convert to runtime
            // exception and throw.
            Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new IllegalStateException("Failed to create DefaultSurfaceProcessor", cause);
            }
        }
    }
}
