/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.graphics.ImageFormat;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * An {@link ImageReaderProxy} which takes one or more {@link android.media.Image}, processes it,
 * then output the final result {@link ImageProxy} to
 * {@link ImageReaderProxy.OnImageAvailableListener}.
 *
 * <p>ProcessingImageReader takes {@link CaptureBundle} as the expected set of
 * {@link CaptureStage}. Once all the ImageProxy from the captures are ready. It invokes
 * the {@link CaptureProcessor} set, then returns a single output ImageProxy to
 * OnImageAvailableListener.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class ProcessingImageReader implements ImageReaderProxy {
    private static final String TAG = "ProcessingImageReader";

    // Exif metadata are restricted in size to 64 kB in JPEG images because according to
    // the specification this information must be contained within a single JPEG APP1
    // segment. (See: https://en.wikipedia.org/wiki/Exif)
    private static final int EXIF_MAX_SIZE_BYTES = 64000;

    final Object mLock = new Object();

    // Callback when Image is ready from InputImageReader.
    private ImageReaderProxy.OnImageAvailableListener mTransformedListener =
            new ImageReaderProxy.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(@NonNull ImageReaderProxy reader) {
                    imageIncoming(reader);
                }
            };

    // Callback when Image is ready from OutputImageReader.
    private ImageReaderProxy.OnImageAvailableListener mImageProcessedListener =
            new ImageReaderProxy.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(@NonNull ImageReaderProxy reader) {
                    // Callback the output OnImageAvailableListener.
                    ImageReaderProxy.OnImageAvailableListener listener;
                    Executor executor;
                    synchronized (mLock) {
                        listener = mListener;
                        executor = mExecutor;

                        // Resets SettableImageProxyBundle after the processor finishes processing.
                        mSettableImageProxyBundle.reset();
                        setupSettableImageProxyBundleCallbacks();
                    }
                    if (listener != null) {
                        if (executor != null) {
                            executor.execute(
                                    () -> listener.onImageAvailable(ProcessingImageReader.this));
                        } else {
                            listener.onImageAvailable(ProcessingImageReader.this);
                        }
                    }
                }
            };

    // Callback when all the ImageProxies in SettableImageProxyBundle are ready.
    private FutureCallback<List<ImageProxy>> mCaptureStageReadyCallback =
            new FutureCallback<List<ImageProxy>>() {
                @Override
                public void onSuccess(@Nullable List<ImageProxy> imageProxyList) {
                    SettableImageProxyBundle settableImageProxyBundle;
                    OnProcessingErrorCallback errorCallback;
                    Executor errorCallbackExecutor;
                    synchronized (mLock) {
                        if (mClosed) {
                            return;
                        }
                        mProcessing = true;
                        settableImageProxyBundle = mSettableImageProxyBundle;
                        errorCallback = mOnProcessingErrorCallback;
                        errorCallbackExecutor = mErrorCallbackExecutor;
                    }
                    try {
                        mCaptureProcessor.process(settableImageProxyBundle);
                    } catch (Exception e) {
                        synchronized (mLock) {
                            // Resets mSettableImageProxyBundle to close the held images.
                            mSettableImageProxyBundle.reset();

                            if (errorCallback != null && errorCallbackExecutor != null) {
                                errorCallbackExecutor.execute(
                                        () -> errorCallback.notifyProcessingError(
                                                e.getMessage(), e.getCause()));
                            }
                        }
                    }

                    synchronized (mLock) {
                        mProcessing = false;
                    }

                    closeAndCompleteFutureIfNecessary();
                }

                @Override
                public void onFailure(@NonNull Throwable throwable) {

                }
            };

    @GuardedBy("mLock")
    boolean mClosed = false;

    @GuardedBy("mLock")
    boolean mProcessing = false;

    @GuardedBy("mLock")
    final ImageReaderProxy mInputImageReader;

    @GuardedBy("mLock")
    final ImageReaderProxy mOutputImageReader;

    @GuardedBy("mLock")
    @Nullable
    ImageReaderProxy.OnImageAvailableListener mListener;

    @GuardedBy("mLock")
    @Nullable
    Executor mExecutor;

    @GuardedBy("mLock")
    CallbackToFutureAdapter.Completer<Void> mCloseCompleter;
    @GuardedBy("mLock")
    private ListenableFuture<Void> mCloseFuture;

    /** The Executor to execute the image post processing task. */
    @NonNull
    final Executor mPostProcessExecutor;

    @NonNull
    final CaptureProcessor mCaptureProcessor;

    @NonNull
    private final ListenableFuture<Void> mUnderlyingCaptureProcessorCloseFuture;

    private String mTagBundleKey = new String();

    @GuardedBy("mLock")
    @NonNull
    SettableImageProxyBundle mSettableImageProxyBundle =
            new SettableImageProxyBundle(Collections.emptyList(), mTagBundleKey);

    private final List<Integer> mCaptureIdList = new ArrayList<>();

    private ListenableFuture<List<ImageProxy>> mSettableImageProxyFutureList =
            Futures.immediateFuture(new ArrayList<>());

    @GuardedBy("mLock")
    OnProcessingErrorCallback mOnProcessingErrorCallback;

    @GuardedBy("mLock")
    Executor mErrorCallbackExecutor;

    ProcessingImageReader(@NonNull Builder builder) {
        if (builder.mInputImageReader.getMaxImages()
                < builder.mCaptureBundle.getCaptureStages().size()) {
            throw new IllegalArgumentException(
                    "MetadataImageReader is smaller than CaptureBundle.");
        }

        mInputImageReader = builder.mInputImageReader;

        // For JPEG ImageReaders, the Surface that is created will have format BLOB which can
        // only be allocated with a height of 1. The output Image from the image reader will read
        // its dimensions from the JPEG data's EXIF in order to set the final dimensions.
        int outputWidth = mInputImageReader.getWidth();
        int outputHeight = mInputImageReader.getHeight();

        if (builder.mOutputFormat == ImageFormat.JPEG) {
            // The output JPEG compression quality is 100 when taking a picture in MAX_QUALITY
            // mode. It might cause the compressed data size exceeds image's width * height.
            // YUV_420_888 should be 1.5 times of image's width * height. The compressed data
            // size shouldn't exceed it. Therefore, scales the output image reader byte buffer to
            // 1.5 times when the JPEG compression quality setting is 100.
            outputWidth = (int) (outputWidth * outputHeight * 1.5f) + EXIF_MAX_SIZE_BYTES;
            outputHeight = 1;
        }
        mOutputImageReader = new AndroidImageReaderProxy(
                ImageReader.newInstance(outputWidth, outputHeight, builder.mOutputFormat,
                        mInputImageReader.getMaxImages()));

        mPostProcessExecutor = builder.mPostProcessExecutor;
        mCaptureProcessor = builder.mCaptureProcessor;
        mCaptureProcessor.onOutputSurface(mOutputImageReader.getSurface(), builder.mOutputFormat);
        mCaptureProcessor.onResolutionUpdate(
                new Size(mInputImageReader.getWidth(), mInputImageReader.getHeight()));

        mUnderlyingCaptureProcessorCloseFuture = mCaptureProcessor.getCloseFuture();

        setCaptureBundle(builder.mCaptureBundle);
    }

    @Override
    @Nullable
    public ImageProxy acquireLatestImage() {
        synchronized (mLock) {
            return mOutputImageReader.acquireLatestImage();
        }
    }

    @Override
    @Nullable
    public ImageProxy acquireNextImage() {
        synchronized (mLock) {
            return mOutputImageReader.acquireNextImage();
        }
    }

    @Override
    public void close() {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            // Prevent the ImageAvailableListener from being triggered after the close function
            // is called.
            mInputImageReader.clearOnImageAvailableListener();
            mOutputImageReader.clearOnImageAvailableListener();

            mClosed = true;
        }

        mCaptureProcessor.close();
        closeAndCompleteFutureIfNecessary();
    }

    void closeAndCompleteFutureIfNecessary() {
        boolean closed;
        boolean processing;
        CallbackToFutureAdapter.Completer<Void> closeCompleter;

        synchronized (mLock) {
            closed = mClosed;
            processing = mProcessing;
            closeCompleter = mCloseCompleter;

            // If the CaptureProcessor is in the middle of processing then don't close the
            // ImageReaderProxys and associated ImageProxy. Let the processing complete before
            // closing them.
            if (closed && !processing) {
                mInputImageReader.close();
                mSettableImageProxyBundle.close();
                mOutputImageReader.close();
            }
        }

        if (closed && !processing) {
            // Complete the capture process pipeline's close future after the underlying capture
            // processor is closed.
            mUnderlyingCaptureProcessorCloseFuture.addListener(() -> {
                cancelSettableImageProxyBundleFutureList();
                if (closeCompleter != null) {
                    closeCompleter.set(null);
                }
            }, CameraXExecutors.directExecutor());
        }
    }

    /**
     * Returns a future that will complete when the ProcessingImageReader is actually closed.
     *
     * @return A future that signals when the ProcessingImageReader is actually closed
     * (after all processing). Cancelling this future has no effect.
     */
    @NonNull
    ListenableFuture<Void> getCloseFuture() {
        ListenableFuture<Void> closeFuture;
        synchronized (mLock) {
            if (mClosed && !mProcessing) {
                // Everything should be closed but still need to wait for underlying capture
                // processors being closed.
                closeFuture = Futures.transform(mUnderlyingCaptureProcessorCloseFuture,
                        nullVoid -> null, CameraXExecutors.directExecutor());
            } else {
                if (mCloseFuture == null) {
                    mCloseFuture = CallbackToFutureAdapter.getFuture(completer -> {
                        // Should already be locked, but lock again to satisfy linter.
                        synchronized (mLock) {
                            mCloseCompleter = completer;
                        }
                        return "ProcessingImageReader-close";
                    });
                }
                closeFuture = Futures.nonCancellationPropagating(mCloseFuture);
            }
        }
        return closeFuture;
    }

    @Override
    public int getHeight() {
        synchronized (mLock) {
            return mInputImageReader.getHeight();
        }
    }

    @Override
    public int getWidth() {
        synchronized (mLock) {
            return mInputImageReader.getWidth();
        }
    }

    @Override
    public int getImageFormat() {
        synchronized (mLock) {
            return mOutputImageReader.getImageFormat();
        }
    }

    @Override
    public int getMaxImages() {
        synchronized (mLock) {
            return mInputImageReader.getMaxImages();
        }
    }

    @Nullable
    @Override
    public Surface getSurface() {
        synchronized (mLock) {
            return mInputImageReader.getSurface();
        }
    }

    @Override
    public void setOnImageAvailableListener(@NonNull OnImageAvailableListener listener,
            @NonNull Executor executor) {
        synchronized (mLock) {
            mListener = Preconditions.checkNotNull(listener);
            mExecutor = Preconditions.checkNotNull(executor);
            mInputImageReader.setOnImageAvailableListener(mTransformedListener, executor);
            mOutputImageReader.setOnImageAvailableListener(mImageProcessedListener, executor);
        }
    }

    @Override
    public void clearOnImageAvailableListener() {
        synchronized (mLock) {
            mListener = null;
            mExecutor = null;
            mInputImageReader.clearOnImageAvailableListener();
            mOutputImageReader.clearOnImageAvailableListener();

            if (!mProcessing) {
                mSettableImageProxyBundle.close();
            }
        }
    }

    /** Sets a CaptureBundle */
    public void setCaptureBundle(@NonNull CaptureBundle captureBundle) {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            cancelSettableImageProxyBundleFutureList();

            if (captureBundle.getCaptureStages() != null) {
                if (mInputImageReader.getMaxImages() < captureBundle.getCaptureStages().size()) {
                    throw new IllegalArgumentException(
                            "CaptureBundle is larger than InputImageReader.");
                }

                mCaptureIdList.clear();

                for (CaptureStage captureStage : captureBundle.getCaptureStages()) {
                    if (captureStage != null) {
                        mCaptureIdList.add(captureStage.getId());
                    }
                }
            }

            // Use the mCaptureBundle as the key for TagBundle
            mTagBundleKey = Integer.toString(captureBundle.hashCode());
            mSettableImageProxyBundle = new SettableImageProxyBundle(mCaptureIdList, mTagBundleKey);
            setupSettableImageProxyBundleCallbacks();
        }
    }

    private void cancelSettableImageProxyBundleFutureList() {
        synchronized (mLock) {
            if (!mSettableImageProxyFutureList.isDone()) {
                mSettableImageProxyFutureList.cancel(true);
            }

            mSettableImageProxyBundle.reset();
        }
    }

    /** Returns a TagBundleKey which is used in this processing image reader.*/
    @NonNull
    public String getTagBundleKey() {
        return mTagBundleKey;
    }

    /** Returns necessary camera callbacks to retrieve metadata from camera result. */
    @Nullable
    CameraCaptureCallback getCameraCaptureCallback() {
        synchronized (mLock) {
            if (mInputImageReader instanceof MetadataImageReader) {
                return ((MetadataImageReader) mInputImageReader).getCameraCaptureCallback();
            } else {
                return new CameraCaptureCallback() {};
            }
        }
    }

    /**
     * Sets {@link OnProcessingErrorCallback} to receive error notifications.
     *
     * @param executor The executor in which the callback methods will be run.
     * @param callback Callback to be invoked if an error occurs when processing the images.
     */
    public void setOnProcessingErrorCallback(@NonNull Executor executor,
            @NonNull OnProcessingErrorCallback callback) {
        synchronized (mLock) {
            mErrorCallbackExecutor = executor;
            mOnProcessingErrorCallback = callback;
        }
    }

    @GuardedBy("mLock")
    void setupSettableImageProxyBundleCallbacks() {
        List<ListenableFuture<ImageProxy>> futureList = new ArrayList<>();
        for (Integer id : mCaptureIdList) {
            futureList.add(mSettableImageProxyBundle.getImageProxy(id));
        }

        mSettableImageProxyFutureList = Futures.allAsList(futureList);

        Futures.addCallback(Futures.allAsList(futureList), mCaptureStageReadyCallback,
                mPostProcessExecutor);
    }

    // Incoming Image from InputImageReader. Acquires it and add to SettableImageProxyBundle.
    void imageIncoming(ImageReaderProxy imageReader) {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            ImageProxy image = null;
            try {
                image = imageReader.acquireNextImage();
            } catch (IllegalStateException e) {
                Logger.e(TAG, "Failed to acquire latest image.", e);
            } finally {
                if (image != null) {
                    // Currently use the same key which intends to get a captureStage id value.
                    Integer tagValue =
                            (Integer) image.getImageInfo().getTagBundle().getTag(mTagBundleKey);

                    if (!mCaptureIdList.contains(tagValue)) {
                        Logger.w(TAG, "ImageProxyBundle does not contain this id: " + tagValue);
                        image.close();
                    } else {
                        mSettableImageProxyBundle.addImageProxy(image);
                    }
                }
            }
        }
    }

    /**
     * The builder to create a {@link ProcessingImageReader} object.
     */
    static final class Builder {
        @NonNull
        protected final ImageReaderProxy mInputImageReader;
        @NonNull
        protected final CaptureBundle mCaptureBundle;
        @NonNull
        protected final CaptureProcessor mCaptureProcessor;

        protected int mOutputFormat;

        @NonNull
        protected Executor mPostProcessExecutor = Executors.newSingleThreadExecutor();

        /**
         * Create a {@link Builder} with specific configurations.
         *
         * @param imageReader      The input image reader.
         * @param captureBundle    The {@link CaptureBundle} includes the processing information
         * @param captureProcessor The {@link CaptureProcessor} to be invoked when the Images are
         *                         ready
         */
        Builder(@NonNull ImageReaderProxy imageReader, @NonNull CaptureBundle captureBundle,
                @NonNull CaptureProcessor captureProcessor) {
            mInputImageReader = imageReader;
            mCaptureBundle = captureBundle;
            mCaptureProcessor = captureProcessor;
            mOutputFormat = imageReader.getImageFormat();
        }

        /**
         * Create a {@link Builder} with specific configurations.
         *
         * @param width            Width of the ImageReader
         * @param height           Height of the ImageReader
         * @param inputFormat      Input image format
         * @param maxImages        Maximum Image number the ImageReader can hold. The capacity
         *                         should be greater than the captureBundle size in order to hold
         *                         all the Images needed with this processing.
         * @param captureBundle    The {@link CaptureBundle} includes the processing information
         * @param captureProcessor The {@link CaptureProcessor} to be invoked when the Images are
         *                         ready
         */
        Builder(int width, int height, int inputFormat, int maxImages,
                @NonNull CaptureBundle captureBundle, @NonNull CaptureProcessor captureProcessor) {
            this(new MetadataImageReader(width, height, inputFormat, maxImages), captureBundle,
                    captureProcessor);
        }

        /**
         * Sets an Executor to execute the post-process of the image result.
         */
        @NonNull
        Builder setPostProcessExecutor(@NonNull Executor postProcessExecutor) {
            mPostProcessExecutor = postProcessExecutor;
            return this;
        }

        /**
         * Sets the output image format.
         */
        @NonNull
        Builder setOutputFormat(int outputFormat) {
            mOutputFormat = outputFormat;
            return this;
        }

        /**
         * Builds an {@link ProcessingImageReader} from current configurations.
         */
        ProcessingImageReader build() {
            return new ProcessingImageReader(this);
        }
    }

    /**
     * Callback for notifying processing errors.
     */
    interface OnProcessingErrorCallback {
        void notifyProcessingError(@Nullable String message, @Nullable Throwable cause);
    }
}
