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

package androidx.camera.core.impl;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.camera2.CaptureRequest;
import android.util.Range;
import android.util.Size;

import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.UseCase;
import androidx.camera.core.featurecombination.ExperimentalFeatureCombination;
import androidx.camera.core.featurecombination.Feature;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * An interface for retrieving camera information.
 *
 * <p>Contains methods for retrieving characteristics for a specific camera.
 *
 * <p>{@link #getImplementation()} returns a {@link CameraInfoInternal} instance
 * that contains the actual implementation and can be cast to an implementation specific class.
 * If the instance itself is the implementation instance, then it should return <code>this</code>.
 */
public interface CameraInfoInternal extends CameraInfo {

    /**
     * Returns the camera id of this camera.
     *
     * @return the camera id
     */
    @NonNull String getCameraId();

    /**
     * Returns the camera characteristics of this camera. The actual type is determined by the
     * underlying camera implementation. For camera2 implementation, the actual type of the
     * returned object is {@link android.hardware.camera2.CameraCharacteristics}.
     */
    @NonNull Object getCameraCharacteristics();

    /**
     * Returns the camera characteristics of the specified physical camera id associated with
     * the current camera.
     *
     * <p>It returns {@code null} if the physical camera id does not belong to
     * the current logical camera. The actual type is determined by the underlying camera
     * implementation. For camera2 implementation, the actual type of the returned object is
     * {@link android.hardware.camera2.CameraCharacteristics}.
     */
    @Nullable Object getPhysicalCameraCharacteristics(@NonNull String physicalCameraId);

    /**
     * Adds a {@link CameraCaptureCallback} which will be invoked when session capture request is
     * completed, failed or cancelled.
     *
     * <p>The callback will be invoked on the specified {@link Executor}.
     */
    void addSessionCaptureCallback(@NonNull Executor executor,
            @NonNull CameraCaptureCallback callback);

    /**
     * Removes the {@link CameraCaptureCallback} which was added in
     * {@link #addSessionCaptureCallback(Executor, CameraCaptureCallback)}.
     */
    void removeSessionCaptureCallback(@NonNull CameraCaptureCallback callback);

    /** Returns a list of quirks related to the camera. */
    @NonNull Quirks getCameraQuirks();

    /** Returns the {@link EncoderProfilesProvider} associated with this camera. */
    @NonNull EncoderProfilesProvider getEncoderProfilesProvider();

    /** Returns the {@link Timebase} of frame output by this camera. */
    @NonNull Timebase getTimebase();

    /**
     * Returns the supported output formats of this camera.
     *
     * @return a set of supported output format, or an empty set if no output format is supported.
     */
    @NonNull Set<Integer> getSupportedOutputFormats();

    /**
     * Returns the supported resolutions of this camera based on the input image format.
     *
     * @param format an image format from {@link ImageFormat} or {@link PixelFormat}.
     * @return a list of supported resolutions, or an empty list if the format is not supported.
     */
    @NonNull List<Size> getSupportedResolutions(int format);

    /**
     * Returns the supported high resolutions of this camera based on the input image format.
     *
     * @param format an image format from {@link ImageFormat} or {@link PixelFormat}.
     * @return a list of supported resolutions, or an empty list if the format is not supported.
     */
    @NonNull List<Size> getSupportedHighResolutions(int format);

    /**
     * Returns the supported dynamic ranges of this camera.
     *
     * @return a set of supported dynamic range, or an empty set if no dynamic range is supported.
     */
    @NonNull Set<DynamicRange> getSupportedDynamicRanges();

    /** Returns if high speed capturing is supported on the device. */
    boolean isHighSpeedSupported();

    /** Returns the supported high speed frame rate ranges. */
    @NonNull
    Set<Range<Integer>> getSupportedHighSpeedFrameRateRanges();

    /**
     * Returns the supported high speed frame rate ranges for a given size.
     *
     * @param size one of the sizes returned by {@link #getSupportedHighSpeedResolutions()}.
     * @return a set of supported high speed frame rate ranges for a given size, or an empty set
     * if the size is not supported.
     */
    @NonNull
    Set<Range<Integer>> getSupportedHighSpeedFrameRateRangesFor(@NonNull Size size);

    /** Returns the supported high speed resolutions. */
    @NonNull
    List<Size> getSupportedHighSpeedResolutions();

    /**
     * Returns the supported high speed resolutions for a given frame rate range.
     *
     * @param fpsRange one of the frame rate ranges returned by
     * {@link #getSupportedHighSpeedFrameRateRanges()}.
     * @return a list of supported high speed resolutions for the given frame rate range, or an
     * empty list if the frame rate range is not supported.
     */
    @NonNull
    List<Size> getSupportedHighSpeedResolutionsFor(@NonNull Range<Integer> fpsRange);

    /**
     * Gets the full sensor rect.
     */
    @NonNull
    Rect getSensorRect();

    /**
     * Returns if preview stabilization is supported on the device.
     *
     * @return true if
     * {@link CaptureRequest#CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION} is supported,
     * otherwise false.
     *
     * @see CaptureRequest#CONTROL_VIDEO_STABILIZATION_MODE
     */
    boolean isPreviewStabilizationSupported();

    /**
     * Returns if video stabilization is supported on the device.
     *
     * @return true if {@link CaptureRequest#CONTROL_VIDEO_STABILIZATION_MODE_ON} is supported,
     * otherwise false.
     *
     * @see CaptureRequest#CONTROL_VIDEO_STABILIZATION_MODE
     */
    boolean isVideoStabilizationSupported();

    /**
     * Gets the underlying implementation instance which could be cast into an implementation
     * specific class for further use in implementation module. Returns <code>this</code> if this
     * instance is the implementation instance.
     */
    default @NonNull CameraInfoInternal getImplementation() {
        return this;
    }

    /**
     * Returns if postview is supported or not.
     */
    default boolean isPostviewSupported() {
        return false;
    }

    /**
     * Returns if capture process progress is supported or not.
     */
    default boolean isCaptureProcessProgressSupported() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    default @NonNull CameraSelector getCameraSelector() {
        return new CameraSelector.Builder()
                .addCameraFilter(cameraInfos -> {
                    final String cameraId = getCameraId();
                    for (CameraInfo cameraInfo : cameraInfos) {
                        Preconditions.checkArgument(cameraInfo instanceof CameraInfoInternal);
                        final CameraInfoInternal cameraInfoInternal =
                                (CameraInfoInternal) cameraInfo;
                        if (cameraInfoInternal.getCameraId().equals(cameraId)) {
                            return Collections.singletonList(cameraInfo);
                        }
                    }
                    throw new IllegalStateException("Unable to find camera with id " + cameraId
                            + " from list of available cameras.");
                })
                .addCameraFilter(new LensFacingCameraFilter(getLensFacing()))
                .build();
    }

    /** Checks if a use case combination is supported. */
    default boolean isUseCaseCombinationSupported(@NonNull List<@NonNull UseCase> useCases) {
        return isUseCaseCombinationSupported(useCases, CameraMode.DEFAULT);
    }

    /** Checks if a use case combination is supported for some specific camera mode. */
    default boolean isUseCaseCombinationSupported(
            @NonNull List<@NonNull UseCase> useCases,
            @CameraMode.Mode int cameraMode
    ) {
        return isUseCaseCombinationSupported(useCases, cameraMode, false);
    }

    /**
     * Checks if a use case combination is supported for some specific camera mode and the option to
     * allow feature combination resolutions.
     */
    default boolean isUseCaseCombinationSupported(@NonNull List<@NonNull UseCase> useCases,
            int cameraMode, boolean allowFeatureCombinationResolutions) {
        return isUseCaseCombinationSupported(useCases, cameraMode,
                allowFeatureCombinationResolutions, CameraConfigs.defaultConfig());
    }

    /**
     * Checks if a use case combination is supported for some specific camera mode,
     * {@link CameraConfig}, and the option to allow feature combination resolutions.
     */
    default boolean isUseCaseCombinationSupported(@NonNull List<@NonNull UseCase> useCases,
            int cameraMode, boolean allowFeatureCombinationResolutions,
            @NonNull CameraConfig cameraConfig) {
        return false;
    }

    @ExperimentalFeatureCombination
    @Override
    default boolean isFeatureCombinationSupported(@NonNull Set<@NonNull UseCase> useCases,
            @NonNull Set<@NonNull Feature> features) {
        for (UseCase useCase : useCases) {
            useCase.setFeatureCombination(features);
        }

        boolean isSupported = isUseCaseCombinationSupported(new ArrayList<>(useCases),
                CameraMode.DEFAULT, true);

        for (UseCase useCase : useCases) {
            useCase.setFeatureCombination(Collections.emptySet());
        }

        return isSupported;
    }
}
