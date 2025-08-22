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

package androidx.camera.camera2.internal;

import static androidx.camera.camera2.internal.CameraIdUtil.isBackwardCompatible;
import static androidx.camera.core.internal.StreamSpecsCalculator.NO_OP_STREAM_SPECS_CALCULATOR;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.concurrent.Camera2CameraCoordinator;
import androidx.camera.core.CameraIdentifier;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.InitializationException;
import androidx.camera.core.Logger;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraStateRegistry;
import androidx.camera.core.impl.CameraThreadConfig;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.internal.StreamSpecsCalculator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The factory class that creates {@link Camera2CameraImpl} instances.
 */
public final class Camera2CameraFactory implements CameraFactory {
    private static final String TAG = "Camera2CameraFactory";
    private static final int DEFAULT_ALLOWED_CONCURRENT_OPEN_CAMERAS = 1;

    private final @NonNull Context mContext;

    private final CameraCoordinator mCameraCoordinator;
    private final CameraThreadConfig mThreadConfig;
    private final CameraStateRegistry mCameraStateRegistry;
    private final CameraManagerCompat mCameraManager;
    private final DisplayInfoManager mDisplayInfoManager;
    private final long mCameraOpenRetryMaxTimeoutInMs;
    private final Map<String, Camera2CameraInfoImpl> mCameraInfos = new HashMap<>();
    private final StreamSpecsCalculator mStreamSpecsCalculator;
    private final @Nullable CameraXConfig mCameraXConfig;
    private final Camera2PresenceSource mCameraPresenceObservable;
    private final CameraSelector mAvailableCamerasSelector;

    private final Object mLock = new Object();
    // This list now holds the final, filtered list of IDs and can be updated.
    private List<String> mAvailableCameraIds = new ArrayList<>();

    @VisibleForTesting
    public Camera2CameraFactory(@NonNull Context context,
            @NonNull CameraThreadConfig threadConfig,
            @Nullable CameraSelector availableCamerasSelector,
            long cameraOpenRetryMaxTimeoutInMs) throws InitializationException {
        this(context, threadConfig, availableCamerasSelector, cameraOpenRetryMaxTimeoutInMs, null,
                NO_OP_STREAM_SPECS_CALCULATOR);
    }

    /** Creates a Camera2 implementation of CameraFactory */
    public Camera2CameraFactory(@NonNull Context context,
            @NonNull CameraThreadConfig threadConfig,
            @Nullable CameraSelector availableCamerasSelector,
            long cameraOpenRetryMaxTimeoutInMs,
            @Nullable CameraXConfig cameraXConfig,
            @NonNull StreamSpecsCalculator streamSpecsCalculator)
            throws InitializationException {
        mContext = context;
        mThreadConfig = threadConfig;
        mCameraManager = CameraManagerCompat.from(context, mThreadConfig.getSchedulerHandler());
        mDisplayInfoManager = DisplayInfoManager.getInstance(context);

        mCameraCoordinator = new Camera2CameraCoordinator(mCameraManager);
        mCameraStateRegistry = new CameraStateRegistry(mCameraCoordinator,
                DEFAULT_ALLOWED_CONCURRENT_OPEN_CAMERAS);
        mCameraCoordinator.addListener(mCameraStateRegistry);
        mCameraOpenRetryMaxTimeoutInMs = cameraOpenRetryMaxTimeoutInMs;
        mStreamSpecsCalculator = streamSpecsCalculator;
        mCameraXConfig = cameraXConfig;
        mAvailableCamerasSelector = availableCamerasSelector;

        // Initial population of the camera list.
        List<String> cameraIds;
        try {
            cameraIds = Arrays.asList(mCameraManager.getCameraIdList());
        } catch (CameraAccessExceptionCompat e) {
            throw new InitializationException(CameraUnavailableExceptionHelper.createFrom(e));
        }
        mCameraPresenceObservable = new Camera2PresenceSource(cameraIds, mCameraManager,
                mThreadConfig.getCameraExecutor());
        onCameraIdsUpdated(cameraIds);
    }

    /**
     * Handles updates to the raw list of camera IDs from the hardware.
     * This method applies all necessary filters and updates the final available list.
     */
    @Override
    public void onCameraIdsUpdated(@NonNull List<String> cameraIds) throws InitializationException {
        try {
            List<String> rawIdList = new ArrayList<>(cameraIds);
            List<String> optimizedIds = CameraSelectionOptimizer.getSelectedAvailableCameraIds(
                    this, mAvailableCamerasSelector, rawIdList);
            List<String> filteredIds = getBackwardCompatibleCameraIds(optimizedIds);
            synchronized (mLock) {
                if (mAvailableCameraIds.equals(filteredIds)) {
                    return; // No change
                }
                Logger.d(TAG, "Updated available camera list: " + mAvailableCameraIds + " -> "
                        + filteredIds);
                mAvailableCameraIds = filteredIds;
            }
        } catch (InitializationException e) {
            // This could happen if characteristics cannot be fetched. Log and clear the list.
            Log.e(TAG, "Unable to get backward compatible camera ids", e);
            throw e;
        }
    }

    @Override
    public @NonNull CameraInternal getCamera(@NonNull String cameraId)
            throws CameraUnavailableException {
        synchronized (mLock) {
            if (!mAvailableCameraIds.contains(cameraId)) {
                throw new IllegalArgumentException(
                        "The given camera id is not on the available camera id list.");
            }
        }
        return new Camera2CameraImpl(mContext, mCameraManager,
                cameraId,
                getCameraInfo(cameraId),
                mCameraCoordinator,
                mCameraStateRegistry,
                mThreadConfig.getCameraExecutor(),
                mThreadConfig.getSchedulerHandler(),
                mDisplayInfoManager,
                mCameraOpenRetryMaxTimeoutInMs,
                mCameraXConfig);
    }

    Camera2CameraInfoImpl getCameraInfo(@NonNull String cameraId)
            throws CameraUnavailableException {
        try {
            Camera2CameraInfoImpl camera2CameraInfoImpl = mCameraInfos.get(cameraId);
            if (camera2CameraInfoImpl == null) {
                camera2CameraInfoImpl = new Camera2CameraInfoImpl(
                        cameraId, mCameraManager, mStreamSpecsCalculator);
                mCameraInfos.put(cameraId, camera2CameraInfoImpl);
            }
            return camera2CameraInfoImpl;
        } catch (CameraAccessExceptionCompat e) {
            throw CameraUnavailableExceptionHelper.createFrom(e);
        }
    }

    @Override
    public @NonNull Set<String> getAvailableCameraIds() {
        // Use a LinkedHashSet to preserve order
        synchronized (mLock) {
            return new LinkedHashSet<>(mAvailableCameraIds);
        }
    }

    @Override
    public @NonNull CameraCoordinator getCameraCoordinator() {
        return mCameraCoordinator;
    }

    @Override
    public @NonNull CameraManagerCompat getCameraManager() {
        return mCameraManager;
    }

    @Override
    public @NonNull Observable<List<CameraIdentifier>> getCameraPresenceSource() {
        return mCameraPresenceObservable;
    }

    @Override
    public void shutdown() {
        mCameraCoordinator.shutdown();
        mCameraPresenceObservable.stopMonitoring();
    }

    private List<String> getBackwardCompatibleCameraIds(
            @NonNull List<String> availableCameraIds) throws InitializationException {
        List<String> backwardCompatibleCameraIds = new ArrayList<>();

        for (String cameraId : availableCameraIds) {
            // Skips camera id 0 and 1 because mostly they should be the ids of the back and
            // front camera by default.
            if (cameraId.equals("0") || cameraId.equals("1")) {
                backwardCompatibleCameraIds.add(cameraId);
                continue;
            } else if (isBackwardCompatible(mCameraManager, cameraId)) {
                backwardCompatibleCameraIds.add(cameraId);
            } else {
                Logger.d(TAG, "Camera " + cameraId + " is filtered out because its capabilities "
                        + "do not contain REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE.");
            }
        }

        return backwardCompatibleCameraIds;
    }
}
