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

package androidx.camera.core.imagecapture;

import static androidx.camera.core.CaptureBundles.singleDefaultCaptureBundle;
import static androidx.camera.core.impl.ImageCaptureConfig.OPTION_BUFFER_FORMAT;
import static androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_FORMAT;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.TransformUtils.hasCropping;
import static androidx.camera.core.internal.utils.ImageUtil.isJpegFormats;
import static androidx.camera.core.internal.utils.ImageUtil.isRawFormats;

import static java.util.Objects.requireNonNull;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.media.ImageReader;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.ForwardingImageProxy;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.MetadataImageReader;
import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability;
import androidx.camera.core.processing.InternalImageProcessor;
import androidx.core.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The class that builds and maintains the {@link ImageCapture} pipeline.
 *
 * <p>This class is responsible for building the entire pipeline, from creating camera request to
 * post-processing the output.
 */
public class ImagePipeline {

    static final byte JPEG_QUALITY_MAX_QUALITY = 100;
    static final byte JPEG_QUALITY_MIN_LATENCY = 95;

    private static int sNextRequestId = 0;

    static final ExifRotationAvailability EXIF_ROTATION_AVAILABILITY =
            new ExifRotationAvailability();
    // Use case configs.
    private final @NonNull ImageCaptureConfig mUseCaseConfig;
    private final @NonNull CaptureConfig mCaptureConfig;

    // Post-processing pipeline.
    private final @NonNull CaptureNode mCaptureNode;
    private final @NonNull ProcessingNode mProcessingNode;
    private final CaptureNode.@NonNull In mPipelineIn;

    // ===== public methods =====

    @MainThread
    @VisibleForTesting
    public ImagePipeline(
            @NonNull ImageCaptureConfig useCaseConfig,
            @NonNull Size cameraSurfaceSize,
            @NonNull CameraCharacteristics cameraCharacteristics) {
        this(useCaseConfig, cameraSurfaceSize, cameraCharacteristics, /*cameraEffect=*/ null,
                /*isVirtualCamera=*/ false, /* postviewSettings */ null);
    }

    @MainThread
    public ImagePipeline(
            @NonNull ImageCaptureConfig useCaseConfig,
            @NonNull Size cameraSurfaceSize,
            @NonNull CameraCharacteristics cameraCharacteristics,
            @Nullable CameraEffect cameraEffect,
            boolean isVirtualCamera) {
        this(useCaseConfig, cameraSurfaceSize, cameraCharacteristics, cameraEffect, isVirtualCamera,
                null);
    }

    @MainThread
    public ImagePipeline(
            @NonNull ImageCaptureConfig useCaseConfig,
            @NonNull Size cameraSurfaceSize,
            @Nullable CameraCharacteristics cameraCharacteristics,
            @Nullable CameraEffect cameraEffect,
            boolean isVirtualCamera,
            @Nullable PostviewSettings postviewSettings) {
        checkMainThread();
        mUseCaseConfig = useCaseConfig;
        mCaptureConfig = CaptureConfig.Builder.createFrom(useCaseConfig).build();

        // Create nodes
        mCaptureNode = new CaptureNode();
        mProcessingNode = new ProcessingNode(
                requireNonNull(mUseCaseConfig.getIoExecutor(CameraXExecutors.ioExecutor())),
                cameraCharacteristics,
                cameraEffect != null ? new InternalImageProcessor(cameraEffect) : null);

        // Pass down [RAW_SENSOR, JPEG] to the pipeline if simultaneous capture is enabled.
        List<Integer> outputFormats = new ArrayList<>();
        if (mUseCaseConfig.getSecondaryInputFormat() != ImageFormat.UNKNOWN) {
            outputFormats.add(ImageFormat.RAW_SENSOR);
            outputFormats.add(ImageFormat.JPEG);
        } else {
            outputFormats.add(getOutputFormat());
        }

        // Connect nodes
        mPipelineIn = CaptureNode.In.of(
                cameraSurfaceSize,
                mUseCaseConfig.getInputFormat(),
                outputFormats,
                isVirtualCamera,
                mUseCaseConfig.getImageReaderProxyProvider(),
                postviewSettings);
        ProcessingNode.In processingIn = mCaptureNode.transform(mPipelineIn);
        mProcessingNode.transform(processingIn);
    }

    /**
     * Creates a {@link SessionConfig.Builder} for configuring camera.
     */
    public SessionConfig.@NonNull Builder createSessionConfigBuilder(@NonNull Size resolution) {
        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(mUseCaseConfig,
                resolution);
        builder.addNonRepeatingSurface(mPipelineIn.getSurface());
        if (mPipelineIn.getOutputFormats().size() > 1
                && mPipelineIn.getSecondarySurface() != null) {
            builder.addNonRepeatingSurface(mPipelineIn.getSecondarySurface());
        }

        // Postview surface is generated when initializing CaptureNode.
        if (mPipelineIn.getPostviewSurface() != null) {
            builder.setPostviewSurface(mPipelineIn.getPostviewSurface());
        }
        return builder;
    }

    /**
     * Closes the pipeline and release all resources.
     *
     * <p>Releases all the buffers and resources allocated by the pipeline. e.g. closing
     * {@link ImageReader}s.
     */
    @MainThread
    public void close() {
        checkMainThread();
        mCaptureNode.release();
        mProcessingNode.release();
    }

    /**
     * Returns the number of empty slots in the queue.
     */
    @MainThread
    public int getCapacity() {
        checkMainThread();
        return mCaptureNode.getCapacity();
    }

    /**
     * Sets a listener for close calls on this image.
     *
     * @param listener to set
     */
    @MainThread
    public void setOnImageCloseListener(
            ForwardingImageProxy.@NonNull OnImageCloseListener listener) {
        checkMainThread();
        mCaptureNode.setOnImageCloseListener(listener);
    }

    // ===== protected methods =====

    /**
     * Creates two requests from a {@link TakePictureRequest}: a request for camera and a request
     * for post-processing.
     *
     * <p>{@link ImagePipeline} creates two requests from {@link TakePictureRequest}: 1) a
     * request sent for post-processing pipeline and 2) a request for camera. The camera request
     * is returned to the caller, and the post-processing request is handled by this class.
     *
     * @param captureFuture used to monitor the events when the request is terminated due to
     *                      capture failure or abortion.
     */
    @MainThread
    @NonNull Pair<CameraRequest, ProcessingRequest> createRequests(
            @NonNull TakePictureRequest takePictureRequest,
            @NonNull TakePictureCallback takePictureCallback,
            @NonNull ListenableFuture<Void> captureFuture) {
        checkMainThread();
        CaptureBundle captureBundle = createCaptureBundle();
        // sNextRequestId is not thread-safe. Changed it to use AtomicInteger if thread safety is
        // needed in the future.
        int requestId = sNextRequestId++;
        return new Pair<>(
                createCameraRequest(
                        requestId,
                        captureBundle,
                        takePictureRequest,
                        takePictureCallback),
                createProcessingRequest(
                        requestId,
                        captureBundle,
                        takePictureRequest,
                        takePictureCallback,
                        captureFuture));
    }

    @MainThread
    void submitProcessingRequest(@NonNull ProcessingRequest request) {
        checkMainThread();
        mPipelineIn.getRequestEdge().accept(request);
    }

    @MainThread
    void notifyCaptureError(TakePictureManager.@NonNull CaptureError error) {
        checkMainThread();
        mPipelineIn.getErrorEdge().accept(error);
    }

    // ===== private methods =====

    private int getOutputFormat() {
        Integer bufferFormat = mUseCaseConfig.retrieveOption(OPTION_BUFFER_FORMAT, null);
        // Return the buffer format if it is set.
        if (bufferFormat != null) {
            return bufferFormat;
        }

        Integer inputFormat = mUseCaseConfig.retrieveOption(OPTION_INPUT_FORMAT, null);
        if (inputFormat != null && inputFormat == ImageFormat.JPEG_R) {
            return ImageFormat.JPEG_R;
        }
        if (inputFormat != null && inputFormat == ImageFormat.RAW_SENSOR) {
            return ImageFormat.RAW_SENSOR;
        }

        // By default, use JPEG format.
        return ImageFormat.JPEG;
    }

    private @NonNull CaptureBundle createCaptureBundle() {
        return requireNonNull(mUseCaseConfig.getCaptureBundle(singleDefaultCaptureBundle()));
    }

    private @NonNull ProcessingRequest createProcessingRequest(
            int requestId,
            @NonNull CaptureBundle captureBundle,
            @NonNull TakePictureRequest takePictureRequest,
            @NonNull TakePictureCallback takePictureCallback,
            @NonNull ListenableFuture<Void> captureFuture) {
        return new ProcessingRequest(
                captureBundle,
                takePictureRequest,
                takePictureCallback,
                captureFuture,
                requestId);
    }

    private boolean shouldEnablePostview() {
        return mPipelineIn.getPostviewSurface() != null;
    }

    @VisibleForTesting
    public @Nullable PostviewSettings getPostviewSettings() {
        return mPipelineIn.getPostviewSettings();
    }

    private CameraRequest createCameraRequest(
            int requestId,
            @NonNull CaptureBundle captureBundle,
            @NonNull TakePictureRequest takePictureRequest,
            @NonNull TakePictureCallback takePictureCallback) {
        List<CaptureConfig> captureConfigs = new ArrayList<>();
        String tagBundleKey = String.valueOf(captureBundle.hashCode());
        for (final CaptureStage captureStage : requireNonNull(captureBundle.getCaptureStages())) {
            final CaptureConfig.Builder builder = new CaptureConfig.Builder();
            builder.setTemplateType(mCaptureConfig.getTemplateType());

            // Add the default implementation options of ImageCapture
            builder.addImplementationOptions(mCaptureConfig.getImplementationOptions());
            builder.addAllCameraCaptureCallbacks(
                    takePictureRequest.getSessionConfigCameraCaptureCallbacks());
            builder.addSurface(mPipelineIn.getSurface());
            if (mPipelineIn.getOutputFormats().size() > 1
                    && mPipelineIn.getSecondarySurface() != null) {
                builder.addSurface(mPipelineIn.getSecondarySurface());
            }
            boolean shouldEnablePostview = shouldEnablePostview();
            if (shouldEnablePostview) {
                // According to the javadoc of CameraExtensionSession#capture, postview surface
                // should be added.
                builder.addSurface(requireNonNull(mPipelineIn.getPostviewSurface()));
            }
            builder.setPostviewEnabled(shouldEnablePostview);

            // Sets the JPEG rotation and quality for JPEG and RAW formats. Some devices do not
            // handle these configs for non-JPEG images. See b/204375890.
            if (isJpegFormats(mPipelineIn.getInputFormat())
                    || isRawFormats(mPipelineIn.getInputFormat())) {
                if (EXIF_ROTATION_AVAILABILITY.isRotationOptionSupported()) {
                    builder.addImplementationOption(CaptureConfig.OPTION_ROTATION,
                            takePictureRequest.getRotationDegrees());
                }
                builder.addImplementationOption(CaptureConfig.OPTION_JPEG_QUALITY,
                        getCameraRequestJpegQuality(takePictureRequest));
            }

            // Add the implementation options required by the CaptureStage
            builder.addImplementationOptions(
                    captureStage.getCaptureConfig().getImplementationOptions());

            // Use CaptureBundle object as the key for TagBundle
            builder.addTag(tagBundleKey, captureStage.getId());
            builder.setId(requestId);
            builder.addCameraCaptureCallback(mPipelineIn.getCameraCaptureCallback());
            if (mPipelineIn.getOutputFormats().size() > 1
                    && mPipelineIn.getSecondaryCameraCaptureCallback() != null) {
                builder.addCameraCaptureCallback(mPipelineIn.getSecondaryCameraCaptureCallback());
            }
            captureConfigs.add(builder.build());
        }

        return new CameraRequest(captureConfigs, takePictureCallback);
    }

    /**
     * Returns the JPEG quality for camera request.
     *
     * <p>If there is JPEG encoding in post-processing, use max quality for the camera request to
     * minimize quality loss.
     *
     * <p>However this results in poor performance during cropping than setting 95 (b/206348741).
     */
    int getCameraRequestJpegQuality(@NonNull TakePictureRequest request) {
        boolean isOnDisk = request.getOnDiskCallback() != null;
        boolean hasCropping = hasCropping(request.getCropRect(), mPipelineIn.getSize());
        if (isOnDisk && hasCropping) {
            // For saving to disk, the image is decoded to Bitmap, cropped and encoded to JPEG
            // again. In that case, use a high JPEG quality for the hardware compression to avoid
            // quality loss.
            if (request.getCaptureMode() == ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) {
                // The trade-off of using a high quality is poorer performance. So we only do
                // that if the capture mode is CAPTURE_MODE_MAXIMIZE_QUALITY.
                return JPEG_QUALITY_MAX_QUALITY;
            } else {
                return JPEG_QUALITY_MIN_LATENCY;
            }
        }
        return request.getJpegQuality();
    }

    @VisibleForTesting
    @NonNull CaptureNode getCaptureNode() {
        return mCaptureNode;
    }

    @VisibleForTesting
    @NonNull ProcessingNode getProcessingNode() {
        return mProcessingNode;
    }


    /**
     * Returns true if the image reader is a {@link MetadataImageReader}.
     */
    @VisibleForTesting
    public boolean expectsMetadata() {
        return mCaptureNode.getSafeCloseImageReaderProxy().getImageReaderProxy()
                instanceof MetadataImageReader;
    }
}
