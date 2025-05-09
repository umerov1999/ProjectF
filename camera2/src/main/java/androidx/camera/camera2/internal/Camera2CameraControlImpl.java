/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY;

import static androidx.camera.core.ImageCapture.FLASH_MODE_AUTO;
import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.core.ImageCapture.FLASH_MODE_ON;

import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Rational;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.annotation.CameraExecutor;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.workaround.AutoFlashAEModeDisabler;
import androidx.camera.camera2.interop.Camera2CameraControl;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.ScreenFlash;
import androidx.camera.core.Logger;
import androidx.camera.core.imagecapture.CameraCapturePipeline;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.annotation.ExecutedBy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A Camera2 implementation for CameraControlInternal interface
 *
 * <p>There are 2 states in the control, use count and active boolean. Use count controls
 * whether the user can submit new requests, it can be increased or decreased via
 * {@link #incrementUseCount()} and {@link #decrementUseCount()}. Before sending the request to
 * the control, it must increase use count, otherwise the request will be dropped. Active state
 * controls whether the requests are sent to the camera. It can be set via
 * {@link #setActive(boolean)}. The transition of active boolean from {@code true} to {@code
 * false} may also reset state.
 *
 * <p>There are 4 possible state combinations when processing a request.
 *
 * <ul>
 * <li>Use count >= 1 but active boolean == false: the control can accept new requests for
 * changing parameters, but won't attempt to send them to the camera device yet. New requests can
 * be either cached and replace old requests, or may end with {@code ImmediateFailedFuture}
 * directly, depending on whether the type of request needs to be cached reasonably.</li>
 * <li>Use count >= 1 and active boolean is true: the control now sends cached requests to the
 * camera. Any new requests are also sent directly to the camera.</li>
 * <li>Use count == 0 and active boolean is true: This state may not be possible or may be very
 * short lived depending on how we want to use it. the control does not accept new requests;
 * all requests end in {@code ImmediateFailedFuture}. Previously cached requests may continue
 * processing.</li>
 * <li>Use count == 0 and active boolean is false: the control does not accept new requests; all
 * requests end in {@code ImmediateFailedFuture}. Any cached requests are dropped.</li>
 * </ul>
 */
@OptIn(markerClass = ExperimentalCamera2Interop.class)
public class Camera2CameraControlImpl implements CameraControlInternal {
    private static final String TAG = "Camera2CameraControlImp";
    private static final int DEFAULT_TEMPLATE = CameraDevice.TEMPLATE_PREVIEW;
    @VisibleForTesting
    final CameraControlSessionCallback mSessionCallback;
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @CameraExecutor
    final Executor mExecutor;
    private final Object mLock = new Object();
    private final CameraCharacteristicsCompat mCameraCharacteristics;
    private final ControlUpdateCallback mControlUpdateCallback;

    private final SessionConfig.Builder mSessionConfigBuilder = new SessionConfig.Builder();
    private final FocusMeteringControl mFocusMeteringControl;
    private final ZoomControl mZoomControl;
    private final TorchControl mTorchControl;
    private final LowLightBoostControl mLowLightBoostControl;
    private final ExposureControl mExposureControl;
    @VisibleForTesting
    ZslControl mZslControl;
    private final Camera2CameraControl mCamera2CameraControl;
    private final Camera2CapturePipeline mCamera2CapturePipeline;
    private final VideoUsageControl mVideoUsageControl;
    @GuardedBy("mLock")
    private int mUseCount = 0;

    private ImageCapture.ScreenFlash mScreenFlash;

    // use volatile modifier to make these variables in sync in all threads.
    @TorchControl.TorchStateInternal
    private volatile int mTorchState = TorchControl.OFF;
    @IntRange(from = 1)
    private volatile int mTorchStrength;
    private volatile boolean mIsLowLightBoostOn = false;
    @ImageCapture.FlashMode
    private volatile int mFlashMode = FLASH_MODE_OFF;

    // Workarounds
    private final AutoFlashAEModeDisabler mAutoFlashAEModeDisabler;

    static final String TAG_SESSION_UPDATE_ID = "CameraControlSessionUpdateId";
    private final AtomicLong mNextSessionUpdateId = new AtomicLong(0);
    private volatile @NonNull ListenableFuture<Void> mFlashModeChangeSessionUpdateFuture =
            Futures.immediateFuture(null);

    //******************** Should only be accessed by executor *****************************//
    private int mTemplate = DEFAULT_TEMPLATE;
    // SessionUpdateId will auto-increment every time session updates.
    private long mCurrentSessionUpdateId = 0;
    private final CameraCaptureCallbackSet mCameraCaptureCallbackSet =
            new CameraCaptureCallbackSet();
    //**************************************************************************************//

    @VisibleForTesting
    Camera2CameraControlImpl(@NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @NonNull ScheduledExecutorService scheduler,
            @CameraExecutor @NonNull Executor executor,
            @NonNull ControlUpdateCallback controlUpdateCallback) {
        this(cameraCharacteristics, scheduler, executor, controlUpdateCallback,
                new Quirks(new ArrayList<>()));
    }

    /**
     * Constructor for a Camera2CameraControlImpl.
     *
     * <p>All {@code controlUpdateListener} invocations will be on the provided {@code executor}.
     *
     * <p>All tasks scheduled by {@code scheduler} will be immediately executed by {@code executor}.
     *
     * @param cameraCharacteristics Characteristics for the camera being controlled.
     * @param scheduler             Scheduler used for scheduling tasks in the future.
     * @param executor              Camera executor for synchronizing and offloading all commands.
     * @param controlUpdateCallback Listener which will be notified of control changes.
     * @param cameraQuirks          Camera-related quirks of the camera being controlled
     */
    Camera2CameraControlImpl(@NonNull CameraCharacteristicsCompat cameraCharacteristics,
            @NonNull ScheduledExecutorService scheduler,
            @CameraExecutor @NonNull Executor executor,
            @NonNull ControlUpdateCallback controlUpdateCallback,
            final @NonNull Quirks cameraQuirks) {
        mCameraCharacteristics = cameraCharacteristics;
        mControlUpdateCallback = controlUpdateCallback;
        mExecutor = executor;
        mVideoUsageControl = new VideoUsageControl(executor);
        mSessionCallback = new CameraControlSessionCallback(mExecutor);
        mSessionConfigBuilder.setTemplateType(mTemplate);
        mSessionConfigBuilder.addRepeatingCameraCaptureCallback(
                CaptureCallbackContainer.create(mSessionCallback));
        // Adding a callback via SessionConfigBuilder requires a expensive updateSessionConfig
        // call. mCameraCaptureCallbackset is for enabling dynamically add/remove
        // CameraCaptureCallback efficiently.
        mSessionConfigBuilder.addRepeatingCameraCaptureCallback(mCameraCaptureCallbackSet);

        mExposureControl = new ExposureControl(this, mCameraCharacteristics, mExecutor);
        mFocusMeteringControl = new FocusMeteringControl(
                this, scheduler, mExecutor, cameraQuirks);
        mZoomControl = new ZoomControl(this, mCameraCharacteristics, mExecutor);
        mTorchControl = new TorchControl(this, mCameraCharacteristics, mExecutor);
        mTorchStrength = mCameraCharacteristics.getDefaultTorchStrengthLevel();
        mLowLightBoostControl = new LowLightBoostControl(this, mCameraCharacteristics, mExecutor);
        if (Build.VERSION.SDK_INT >= 23) {
            mZslControl = new ZslControlImpl(mCameraCharacteristics, mExecutor);
        } else {
            mZslControl = new ZslControlNoOpImpl();
        }

        // Workarounds
        mAutoFlashAEModeDisabler = new AutoFlashAEModeDisabler(cameraQuirks);
        mCamera2CameraControl = new Camera2CameraControl(this, mExecutor);
        mCamera2CapturePipeline = new Camera2CapturePipeline(this, mCameraCharacteristics,
                cameraQuirks, mExecutor, scheduler);
    }

    /** Increments the use count of the control. */
    void incrementUseCount() {
        synchronized (mLock) {
            mUseCount++;
        }
    }

    /**
     * Decrements the use count of the control.
     *
     * @throws IllegalStateException if try to decrement the use count to less than zero
     */
    void decrementUseCount() {
        synchronized (mLock) {
            if (mUseCount == 0) {
                throw new IllegalStateException("Decrementing use count occurs more times than "
                        + "incrementing");
            }
            mUseCount--;
        }
    }

    /**
     * Returns the use count of the control.
     *
     * <p>Use count can be increased and decreased via {@link #incrementUseCount()} and
     * {@link #decrementUseCount()}. Camera control only accepts requests when the use count is
     * greater than 0.
     */
    @VisibleForTesting
    int getUseCount() {
        synchronized (mLock) {
            return mUseCount;
        }
    }

    public @NonNull ZoomControl getZoomControl() {
        return mZoomControl;
    }

    public @NonNull FocusMeteringControl getFocusMeteringControl() {
        return mFocusMeteringControl;
    }

    public @NonNull TorchControl getTorchControl() {
        return mTorchControl;
    }

    public @NonNull LowLightBoostControl getLowLightBoostControl() {
        return mLowLightBoostControl;
    }

    public @NonNull ExposureControl getExposureControl() {
        return mExposureControl;
    }

    public @NonNull ZslControl getZslControl() {
        return mZslControl;
    }

    public @NonNull Camera2CameraControl getCamera2CameraControl() {
        return mCamera2CameraControl;
    }

    @Override
    public void addInteropConfig(@NonNull Config config) {
        ListenableFuture<Void> future = mCamera2CameraControl.addCaptureRequestOptions(
                CaptureRequestOptions.Builder.from(config).build());
        future.addListener(() -> {
        }, CameraXExecutors.directExecutor());
    }

    @Override
    public void clearInteropConfig() {
        ListenableFuture<Void> future = mCamera2CameraControl.clearCaptureRequestOptions();
        future.addListener(() -> {
        }, CameraXExecutors.directExecutor());
    }

    @Override
    public @NonNull Config getInteropConfig() {
        return mCamera2CameraControl.getCamera2ImplConfig();
    }

    /**
     * Set current active state. Set active if it is ready to trigger camera control operation.
     *
     * <p>Most operations during inactive state do nothing. Some states are reset to default
     * once it is changed to inactive state.
     *
     * <p>This method should be executed by {@link #mExecutor} only.
     */
    @ExecutedBy("mExecutor")
    void setActive(boolean isActive) {
        Logger.d(TAG, "setActive: isActive = " + isActive);
        mFocusMeteringControl.setActive(isActive);
        mZoomControl.setActive(isActive);
        mLowLightBoostControl.setActive(isActive);
        mTorchControl.setActive(isActive);
        mExposureControl.setActive(isActive);
        mCamera2CameraControl.setActive(isActive);
        if (!isActive) {
            mScreenFlash = null;
            // Since the camera is no longer active, there should not be any recording ongoing with
            // this camera. If something like persistent recording wants to resume recording with
            // this camera again, it should update recording status again when being attached.
            mVideoUsageControl.resetDirectly(); // already in mExecutor i.e. camera thread
        }
    }

    @ExecutedBy("mExecutor")
    public void setPreviewAspectRatio(@Nullable Rational previewAspectRatio) {
        mFocusMeteringControl.setPreviewAspectRatio(previewAspectRatio);
    }

    @Override
    public @NonNull ListenableFuture<FocusMeteringResult> startFocusAndMetering(
            @NonNull FocusMeteringAction action) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(
                mFocusMeteringControl.startFocusAndMetering(action));
    }

    @Override
    public @NonNull ListenableFuture<Void> cancelFocusAndMetering() {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(mFocusMeteringControl.cancelFocusAndMetering());
    }

    @Override
    public @NonNull ListenableFuture<Void> setZoomRatio(float ratio) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(mZoomControl.setZoomRatio(ratio));
    }

    @Override
    public @NonNull ListenableFuture<Void> setLinearZoom(float linearZoom) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(mZoomControl.setLinearZoom(linearZoom));
    }

    @ImageCapture.FlashMode
    @Override
    public int getFlashMode() {
        return mFlashMode;
    }

    /** {@inheritDoc} */
    @Override
    public void setFlashMode(@ImageCapture.FlashMode int flashMode) {
        if (!isControlInUse()) {
            Logger.w(TAG, "Camera is not active.");
            return;
        }
        // update mFlashMode immediately so that following getFlashMode() returns correct value.
        mFlashMode = flashMode;
        Logger.d(TAG, "setFlashMode: mFlashMode = " + mFlashMode);

        // Disable ZSL when flash mode is ON or AUTO.
        mZslControl.setZslDisabledByFlashMode(mFlashMode == FLASH_MODE_ON
                || mFlashMode == FLASH_MODE_AUTO);

        // On some devices, AE precapture may not work properly if the repeating request to change
        // the flash mode is not completed. We need to store the future so that AE precapture can
        // wait for it.
        mFlashModeChangeSessionUpdateFuture = updateSessionConfigAsync();
    }

    /** {@inheritDoc} */
    @Override
    public void setScreenFlash(@Nullable ScreenFlash screenFlash) {
        mScreenFlash = screenFlash;
    }

    public @Nullable ScreenFlash getScreenFlash() {
        return mScreenFlash;
    }

    @Override
    public void addZslConfig(SessionConfig.@NonNull Builder sessionConfigBuilder) {
        mZslControl.addZslConfig(sessionConfigBuilder);
    }

    @Override
    public void clearZslConfig() {
        mZslControl.clearZslConfig();
    }

    @Override
    public void setZslDisabledByUserCaseConfig(boolean disabled) {
        mZslControl.setZslDisabledByUserCaseConfig(disabled);
    }

    @Override
    public boolean isZslDisabledByByUserCaseConfig() {
        return mZslControl.isZslDisabledByUserCaseConfig();
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull ListenableFuture<Void> enableTorch(final boolean torch) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(mTorchControl.enableTorch(torch));
    }

    @Override
    @ExecutedBy("mExecutor")
    public void setLowLightBoostDisabledByUseCaseSessionConfig(boolean disabled) {
        mLowLightBoostControl.setLowLightBoostDisabledByUseCaseSessionConfig(disabled);
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull ListenableFuture<Void> enableLowLightBoostAsync(final boolean lowLightBoost) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return Futures.nonCancellationPropagating(
                mLowLightBoostControl.enableLowLightBoost(lowLightBoost));
    }

    @ExecutedBy("mExecutor")
    private @NonNull ListenableFuture<Void> waitForSessionUpdateId(long sessionUpdateIdToWait) {
        return CallbackToFutureAdapter.getFuture(completer -> {
            addCaptureResultListener(captureResult -> {
                boolean updated = isSessionUpdated(captureResult, sessionUpdateIdToWait);
                if (updated) {
                    completer.set(null);
                    return true; // remove the callback
                }
                return false; // continue checking
            });
            return "waitForSessionUpdateId:" + sessionUpdateIdToWait;
        });
    }

    /**
     * Check if the sessionUpdateId in capture result is larger than the given sessionUpdateId.
     */
    static boolean isSessionUpdated(@NonNull TotalCaptureResult captureResult,
            long sessionUpdateId) {
        if (captureResult.getRequest() == null) {
            return false;
        }
        Object tag = captureResult.getRequest().getTag();
        if (tag instanceof TagBundle) {
            Long tagLong =
                    (Long) ((TagBundle) tag).getTag(Camera2CameraControlImpl.TAG_SESSION_UPDATE_ID);
            if (tagLong == null) {
                return false;
            }
            long sessionUpdateIdInCaptureResult = tagLong.longValue();
            // Check if session update is already done.
            if (sessionUpdateIdInCaptureResult >= sessionUpdateId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NonNull ListenableFuture<Integer> setExposureCompensationIndex(int exposure) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        return mExposureControl.setExposureCompensationIndex(exposure);
    }

    @Override
    public @NonNull ListenableFuture<Void> setTorchStrengthLevel(
            @IntRange(from = 1) int torchStrengthLevel) {
        if (!isControlInUse()) {
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }
        if (!mCameraCharacteristics.isTorchStrengthLevelSupported()) {
            return Futures.immediateFailedFuture(new UnsupportedOperationException(
                    "The device doesn't support configuring torch strength level."));
        }
        if (torchStrengthLevel < 1
                || torchStrengthLevel > mCameraCharacteristics.getMaxTorchStrengthLevel()) {
            return Futures.immediateFailedFuture(new IllegalArgumentException(
                    "The specified torch strength is not within the valid range."));
        }
        return Futures.nonCancellationPropagating(mTorchControl.setTorchStrengthLevel(
                Math.min(torchStrengthLevel, mCameraCharacteristics.getMaxTorchStrengthLevel())));
    }

    @ExecutedBy("mExecutor")
    void setTorchStrengthLevelInternal(@IntRange(from = 1) int torchStrengthLevel) {
        mTorchStrength = torchStrengthLevel;
        if (isTorchOn()) {
            updateSessionConfigSynchronous();
        }
    }

    /** {@inheritDoc} */
    @Override
    public @NonNull ListenableFuture<List<Void>> submitStillCaptureRequests(
            @NonNull List<CaptureConfig> captureConfigs,
            @ImageCapture.CaptureMode int captureMode,
            @ImageCapture.FlashType int flashType) {
        if (!isControlInUse()) {
            Logger.w(TAG, "Camera is not active.");
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }

        // Prior to submitStillCaptures, wait until the pending flash mode session change is
        // completed. On some devices, AE precapture triggered in submitStillCaptures may not
        // work properly if the repeating request to change the flash mode is not completed.
        int flashMode = getFlashMode();
        return FutureChain.from(Futures.nonCancellationPropagating(
                mFlashModeChangeSessionUpdateFuture)).transformAsync(
                    v -> mCamera2CapturePipeline.submitStillCaptures(captureConfigs, captureMode,
                        flashMode, flashType), mExecutor);
    }

    @Override
    public @NonNull ListenableFuture<CameraCapturePipeline> getCameraCapturePipelineAsync(
            @ImageCapture.CaptureMode int captureMode, @ImageCapture.FlashType int flashType) {
        if (!isControlInUse()) {
            Logger.w(TAG, "Camera is not active.");
            return Futures.immediateFailedFuture(
                    new OperationCanceledException("Camera is not active."));
        }

        int flashMode = getFlashMode();
        return FutureChain.from(
                Futures.nonCancellationPropagating(mFlashModeChangeSessionUpdateFuture)
        ).transformAsync(
                v -> Futures.immediateFuture(mCamera2CapturePipeline.getCameraCapturePipeline(
                        captureMode, flashMode, flashType
                )),
                mExecutor
        );
    }

    /** {@inheritDoc} */
    @Override
    @ExecutedBy("mExecutor")
    public @NonNull SessionConfig getSessionConfig() {
        mSessionConfigBuilder.setTemplateType(mTemplate);
        mSessionConfigBuilder.setImplementationOptions(getSessionOptions());
        mSessionConfigBuilder.addTag(TAG_SESSION_UPDATE_ID, mCurrentSessionUpdateId);
        return mSessionConfigBuilder.build();
    }

    @ExecutedBy("mExecutor")
    void setTemplate(int template) {
        mTemplate = template;

        mFocusMeteringControl.setTemplate(mTemplate);
        mCamera2CapturePipeline.setTemplate(mTemplate);
    }

    @ExecutedBy("mExecutor")
    void resetTemplate() {
        setTemplate(DEFAULT_TEMPLATE);
    }

    private boolean isControlInUse() {
        return getUseCount() > 0;
    }

    /**
     * Triggers an update to the session.
     */
    public void updateSessionConfig() {
        mExecutor.execute(this::updateSessionConfigSynchronous);
    }

    /**
     * Triggers an update to the session and returns a ListenableFuture which completes when the
     * session is updated successfully.
     */
    public @NonNull ListenableFuture<Void> updateSessionConfigAsync() {
        ListenableFuture<Void> future = CallbackToFutureAdapter.getFuture(completer -> {
            mExecutor.execute(() -> {
                long sessionUpdateId = updateSessionConfigSynchronous();
                Futures.propagate(waitForSessionUpdateId(sessionUpdateId), completer);
            });
            return "updateSessionConfigAsync";
        });

        return Futures.nonCancellationPropagating(future);
    }

    /**
     * Triggers an update to the session synchronously.
     *
     * <p>It will return an auto-incremented ID representing the session update request. The ID
     * will be put in the tag of SessionConfig using key {@link #TAG_SESSION_UPDATE_ID}. It can
     * then retrieve the ID in {@link TotalCaptureResult} to check if the session update is done or
     * not.
     */
    @ExecutedBy("mExecutor")
    long updateSessionConfigSynchronous() {
        mCurrentSessionUpdateId = mNextSessionUpdateId.getAndIncrement();
        mControlUpdateCallback.onCameraControlUpdateSessionConfig();
        return mCurrentSessionUpdateId;
    }

    @ExecutedBy("mExecutor")
    @NonNull Rect getCropSensorRegion() {
        return mZoomControl.getCropSensorRegion();
    }

    @ExecutedBy("mExecutor")
    void removeCaptureResultListener(@NonNull CaptureResultListener listener) {
        mSessionCallback.removeListener(listener);
    }

    @ExecutedBy("mExecutor")
    void addCaptureResultListener(@NonNull CaptureResultListener listener) {
        mSessionCallback.addListener(listener);
    }

    /** Adds a session {@link CameraCaptureCallback dynamically */
    void addSessionCameraCaptureCallback(@NonNull Executor executor,
            @NonNull CameraCaptureCallback cameraCaptureCallback) {
        mExecutor.execute(() -> {
            mCameraCaptureCallbackSet.addCaptureCallback(executor, cameraCaptureCallback);
        });
    }

    /** Removes the {@link CameraCaptureCallback} that was added previously */
    void removeSessionCameraCaptureCallback(@NonNull CameraCaptureCallback cameraCaptureCallback) {
        mExecutor.execute(() -> {
            mCameraCaptureCallbackSet.removeCaptureCallback(cameraCaptureCallback);
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void enableTorchInternal(@TorchControl.TorchStateInternal int torchState) {
        // When low-light boost is on, any torch related operations will be ignored.
        if (mIsLowLightBoostOn) {
            return;
        }

        mTorchState = torchState;
        if (torchState == TorchControl.OFF) {
            // On some devices, needs to reset the AE/flash state to ensure that the torch can be
            // turned off.
            resetAeFlashState();
        }
        updateSessionConfigSynchronous();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @ExecutedBy("mExecutor")
    void enableLowLightBoostInternal(boolean lowLightBoost) {
        if (mIsLowLightBoostOn == lowLightBoost) {
            return;
        }

        // Forces turn off torch before enabling low-light boost.
        if (lowLightBoost && isTorchOn()) {
            // On some devices, needs to reset the AE/flash state to ensure that the torch can be
            // turned off.
            resetAeFlashState();
            mTorchState = TorchControl.OFF;
            mTorchControl.forceUpdateTorchStateToOff();
        }

        mIsLowLightBoostOn = lowLightBoost;
        updateSessionConfigSynchronous();
    }

    private void resetAeFlashState() {
        // Send capture request with AE_MODE_ON + FLASH_MODE_OFF to reset the AE/flash state.
        CaptureConfig.Builder singleRequestBuilder = new CaptureConfig.Builder();
        singleRequestBuilder.setTemplateType(mTemplate);
        singleRequestBuilder.setUseRepeatingSurface(true);
        Camera2ImplConfig.Builder configBuilder = new Camera2ImplConfig.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,
                getSupportedAeMode(CaptureRequest.CONTROL_AE_MODE_ON));
        configBuilder.setCaptureRequestOption(CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_OFF);
        singleRequestBuilder.addImplementationOptions(configBuilder.build());
        submitCaptureRequestsInternal(
                Collections.singletonList(singleRequestBuilder.build()));
    }

    @ExecutedBy("mExecutor")
    boolean isTorchOn() {
        return mTorchState != TorchControl.OFF;
    }

    @ExecutedBy("mExecutor")
    boolean isLowLightBoostOn() {
        return mIsLowLightBoostOn;
    }

    @ExecutedBy("mExecutor")
    void submitCaptureRequestsInternal(final List<CaptureConfig> captureConfigs) {
        mControlUpdateCallback.onCameraControlCaptureRequests(captureConfigs);
    }

    /**
     * Gets session options by current status.
     *
     * <p>The session options are based on the current torch status, flash mode, focus area, crop
     * area, etc... They should be appended to the repeat request.
     */
    @VisibleForTesting
    @ExecutedBy("mExecutor")
    Config getSessionOptions() {
        Camera2ImplConfig.Builder builder = new Camera2ImplConfig.Builder();
        builder.setCaptureRequestOptionWithPriority(CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO, Config.OptionPriority.REQUIRED);

        // AF Mode is assigned in mFocusMeteringControl.
        mFocusMeteringControl.addFocusMeteringOptions(builder);

        mZoomControl.addZoomOption(builder);

        int aeMode = CaptureRequest.CONTROL_AE_MODE_ON;

        // Flash modes other than screen flash will override this AE mode later
        if (mFocusMeteringControl.isExternalFlashAeModeEnabled()) {
            aeMode = CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH;
        }

        if (mIsLowLightBoostOn) {
            aeMode = CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY;
        } else if (isTorchOn()) {
            builder.setCaptureRequestOptionWithPriority(CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH, Config.OptionPriority.REQUIRED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                if (mTorchState == TorchControl.ON) {
                    builder.setCaptureRequestOptionWithPriority(CaptureRequest.FLASH_STRENGTH_LEVEL,
                            mTorchStrength, Config.OptionPriority.REQUIRED);
                } else if (mTorchState == TorchControl.USED_AS_FLASH) {
                    // If torch is used as flash, use the default torch strength instead.
                    builder.setCaptureRequestOptionWithPriority(CaptureRequest.FLASH_STRENGTH_LEVEL,
                            mCameraCharacteristics.getDefaultTorchStrengthLevel(),
                            Config.OptionPriority.REQUIRED);
                }
            }
        } else {
            switch (mFlashMode) {
                case FLASH_MODE_OFF:
                    aeMode = CaptureRequest.CONTROL_AE_MODE_ON;
                    break;
                case FLASH_MODE_ON:
                    aeMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
                    break;
                case FLASH_MODE_AUTO:
                    aeMode = mAutoFlashAEModeDisabler.getCorrectedAeMode(
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
            }
        }
        builder.setCaptureRequestOptionWithPriority(CaptureRequest.CONTROL_AE_MODE,
                getSupportedAeMode(aeMode), Config.OptionPriority.REQUIRED);

        builder.setCaptureRequestOptionWithPriority(CaptureRequest.CONTROL_AWB_MODE,
                getSupportedAwbMode(CaptureRequest.CONTROL_AWB_MODE_AUTO),
                Config.OptionPriority.REQUIRED);

        mExposureControl.setCaptureRequestOption(builder);

        mCamera2CameraControl.applyOptionsToBuilder(builder);

        return builder.build();
    }

    /**
     * Returns a supported AF mode which will be preferredMode if it is supported.
     *
     * <p><pre>If preferredMode is not supported, fallback with the following priority (highest to
     * lowest).
     * 1) {@link CaptureRequest#CONTROL_AF_MODE_CONTINUOUS_PICTURE}
     * 2) {@link CaptureRequest#CONTROL_AF_MODE_AUTO)}
     * 3) {@link CaptureRequest#CONTROL_AF_MODE_OFF}
     * </pre>
     */
    @ExecutedBy("mExecutor")
    int getSupportedAfMode(int preferredMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (modes == null) {
            return CaptureRequest.CONTROL_AF_MODE_OFF;
        }

        // if preferredMode is supported, use it
        if (isModeInList(preferredMode, modes)) {
            return preferredMode;
        }

        // if not found, priority is CONTINUOUS_PICTURE > AUTO > OFF
        if (isModeInList(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE, modes)) {
            return CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
        } else if (isModeInList(CaptureRequest.CONTROL_AF_MODE_AUTO, modes)) {
            return CaptureRequest.CONTROL_AF_MODE_AUTO;
        }

        return CaptureRequest.CONTROL_AF_MODE_OFF;
    }

    /**
     * Returns a supported AE mode which will be preferredMode if it is supported.
     *
     * <p><pre>If preferredMode is not supported, fallback with the following priority (highest to
     * lowest).
     * 1) {@link CaptureRequest#CONTROL_AE_MODE_ON}
     * 2) {@link CaptureRequest#CONTROL_AE_MODE_OFF}
     * </pre>
     */
    @ExecutedBy("mExecutor")
    int getSupportedAeMode(int preferredMode) {
        return getSupportedAeMode(mCameraCharacteristics, preferredMode);
    }

    /**
     * Returns a supported AE mode which will be preferredMode if it is supported.
     *
     * @see #getSupportedAeMode(int preferredMode)
     */
    public static int getSupportedAeMode(
            @NonNull CameraCharacteristicsCompat cameraCharacteristics,
            int preferredMode
    ) {
        int[] modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);

        if (modes == null) {
            return CaptureRequest.CONTROL_AE_MODE_OFF;
        }

        // if preferredMode is supported, use it
        if (isModeInList(preferredMode, modes)) {
            return preferredMode;
        }

        // if not found, priority is AE_ON > AE_OFF
        if (isModeInList(CaptureRequest.CONTROL_AE_MODE_ON, modes)) {
            return CaptureRequest.CONTROL_AE_MODE_ON;
        }

        return CaptureRequest.CONTROL_AE_MODE_OFF;
    }

    /**
     * Returns a supported AWB mode which will be preferredMode if it is supported.
     *
     * <p><pre>If preferredMode is not supported, fallback with the following priority (highest to
     * lowest).
     * 1) {@link CaptureRequest#CONTROL_AWB_MODE_AUTO}
     * 2) {@link CaptureRequest#CONTROL_AWB_MODE_OFF)}
     * </pre>
     */
    @ExecutedBy("mExecutor")
    private int getSupportedAwbMode(int preferredMode) {
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);

        if (modes == null) {
            return CaptureRequest.CONTROL_AWB_MODE_OFF;
        }

        // if preferredMode is supported, use it
        if (isModeInList(preferredMode, modes)) {
            return preferredMode;
        }

        // if not found, priority is AWB_AUTO > AWB_OFF
        if (isModeInList(CaptureRequest.CONTROL_AWB_MODE_AUTO, modes)) {
            return CaptureRequest.CONTROL_AWB_MODE_AUTO;
        }

        return CaptureRequest.CONTROL_AWB_MODE_OFF;
    }

    @ExecutedBy("mExecutor")
    private static boolean isModeInList(int mode, int[] modeList) {
        for (int m : modeList) {
            if (mode == m) {
                return true;
            }
        }
        return false;
    }

    int getMaxAfRegionCount() {
        Integer count = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        return count == null ? 0 : count;
    }

    int getMaxAeRegionCount() {
        Integer count = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        return count == null ? 0 : count;
    }

    int getMaxAwbRegionCount() {
        Integer count = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB);
        return count == null ? 0 : count;
    }

    @VisibleForTesting
    long getCurrentSessionUpdateId() {
        return mCurrentSessionUpdateId;
    }

    @Override
    public void incrementVideoUsage() {
        mVideoUsageControl.incrementUsage();
    }

    @Override
    public void decrementVideoUsage() {
        mVideoUsageControl.decrementUsage();
    }

    @Override
    public boolean isInVideoUsage() {
        int currentVal = mVideoUsageControl.getUsage();
        Logger.d(TAG, "isInVideoUsage: mVideoUsageControl value = " + currentVal);
        return currentVal > 0;
    }

    /** An interface to listen to camera capture results. */
    public interface CaptureResultListener {
        /**
         * Callback to handle camera capture results.
         *
         * @param captureResult camera capture result.
         * @return true to finish listening, false to continue listening.
         */
        boolean onCaptureResult(@NonNull TotalCaptureResult captureResult);
    }

    static final class CameraControlSessionCallback extends CaptureCallback {

        /* synthetic accessor */final Set<CaptureResultListener> mResultListeners = new HashSet<>();
        @CameraExecutor
        private final Executor mExecutor;

        CameraControlSessionCallback(@CameraExecutor @NonNull Executor executor) {
            mExecutor = executor;
        }

        @ExecutedBy("mExecutor")
        void addListener(@NonNull CaptureResultListener listener) {
            mResultListeners.add(listener);
        }

        @ExecutedBy("mExecutor")
        void removeListener(@NonNull CaptureResultListener listener) {
            mResultListeners.remove(listener);
        }

        @Override
        public void onCaptureCompleted(
                @NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                final @NonNull TotalCaptureResult result) {

            mExecutor.execute(() -> {
                Set<CaptureResultListener> removeSet = new HashSet<>();
                for (CaptureResultListener listener : mResultListeners) {
                    boolean isFinished = listener.onCaptureResult(result);
                    if (isFinished) {
                        removeSet.add(listener);
                    }
                }

                if (!removeSet.isEmpty()) {
                    mResultListeners.removeAll(removeSet);
                }
            });
        }
    }

    /**
     * A set of {@link CameraCaptureCallback}s which is capable of adding/removing callbacks
     * dynamically.
     */
    static final class CameraCaptureCallbackSet extends CameraCaptureCallback {
        Set<CameraCaptureCallback> mCallbacks = new HashSet<>();
        Map<CameraCaptureCallback, Executor> mCallbackExecutors = new ArrayMap<>();

        @ExecutedBy("mExecutor")
        void addCaptureCallback(@NonNull Executor executor,
                @NonNull CameraCaptureCallback callback) {
            mCallbacks.add(callback);
            mCallbackExecutors.put(callback, executor);
        }

        @ExecutedBy("mExecutor")
        void removeCaptureCallback(@NonNull CameraCaptureCallback callback) {
            mCallbacks.remove(callback);
            mCallbackExecutors.remove(callback);
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCaptureCompleted(int captureConfigId,
                @NonNull CameraCaptureResult cameraCaptureResult) {
            for (CameraCaptureCallback callback : mCallbacks) {
                try {
                    mCallbackExecutors.get(callback).execute(() -> {
                        callback.onCaptureCompleted(captureConfigId, cameraCaptureResult);
                    });
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "Executor rejected to invoke onCaptureCompleted.", e);
                }
            }
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCaptureFailed(int captureConfigId, @NonNull CameraCaptureFailure failure) {
            for (CameraCaptureCallback callback : mCallbacks) {
                try {
                    mCallbackExecutors.get(callback).execute(() -> {
                        callback.onCaptureFailed(captureConfigId, failure);
                    });
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "Executor rejected to invoke onCaptureFailed.", e);
                }
            }
        }

        @ExecutedBy("mExecutor")
        @Override
        public void onCaptureCancelled(int captureConfigId) {
            for (CameraCaptureCallback callback : mCallbacks) {
                try {
                    mCallbackExecutors.get(callback).execute(() -> {
                        callback.onCaptureCancelled(captureConfigId);
                    });
                } catch (RejectedExecutionException e) {
                    Logger.e(TAG, "Executor rejected to invoke onCaptureCancelled.", e);
                }
            }
        }
    }
}
