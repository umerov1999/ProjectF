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

package androidx.camera.camera2.interop;

import android.hardware.camera2.CameraCharacteristics;
import android.util.Pair;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.camera2.internal.Camera2CameraInfoImpl;
import androidx.camera.camera2.internal.Camera2PhysicalCameraInfoImpl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.impl.AdapterCameraInfo;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.SessionProcessor;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An interface for retrieving Camera2-related camera information.
 */
@ExperimentalCamera2Interop
public final class Camera2CameraInfo {
    private static final String TAG = "Camera2CameraInfo";

    private @Nullable Camera2CameraInfoImpl mCamera2CameraInfoImpl;

    // TODO: clean up by passing in CameraId, CameraCharacteristicCompat and CameraManager
    //  instead of concrete implementation.
    private @Nullable Camera2PhysicalCameraInfoImpl mCamera2PhysicalCameraInfo;
    private @Nullable List<Pair<CameraCharacteristics.Key, Object>> mExtensionsSpecificChars;
    /**
     * Creates a new logical camera information with Camera2 implementation.
     *
     */
    @RestrictTo(Scope.LIBRARY)
    public Camera2CameraInfo(@NonNull Camera2CameraInfoImpl camera2CameraInfoImpl) {
        mCamera2CameraInfoImpl = camera2CameraInfoImpl;
    }

    /**
     * Creates a new physical camera information with Camera2 implementation.
     */
    @RestrictTo(Scope.LIBRARY)
    public Camera2CameraInfo(@NonNull Camera2PhysicalCameraInfoImpl camera2PhysicalCameraInfo) {
        mCamera2PhysicalCameraInfo = camera2PhysicalCameraInfo;
    }

    private Camera2CameraInfo(@NonNull Camera2CameraInfoImpl camera2CameraInfoImpl,
            List<Pair<CameraCharacteristics.Key, Object>> extensionsSpecificChars) {
        mCamera2CameraInfoImpl = camera2CameraInfoImpl;
        mExtensionsSpecificChars = extensionsSpecificChars;
    }

    /**
     * Gets the {@link Camera2CameraInfo} from a {@link CameraInfo}.
     *
     * <p>If the {@link CameraInfo} is retrieved by an Extensions-enabled {@link CameraSelector},
     * calling {@link #getCameraCharacteristic(CameraCharacteristics.Key)} will return any available
     * Extensions-specific characteristics if exists.
     *
     * @param cameraInfo The {@link CameraInfo} to get from.
     * @return The camera information with Camera2 implementation.
     * @throws IllegalArgumentException if the camera info does not contain the camera2 information
     *                                  (e.g., if CameraX was not initialized with a
     *                                  {@link androidx.camera.camera2.Camera2Config}).
     */
    public static @NonNull Camera2CameraInfo from(@NonNull CameraInfo cameraInfo) {
        if (cameraInfo instanceof Camera2PhysicalCameraInfoImpl) {
            return ((Camera2PhysicalCameraInfoImpl) cameraInfo).getCamera2CameraInfo();
        }

        CameraInfoInternal cameraInfoImpl =
                ((CameraInfoInternal) cameraInfo).getImplementation();
        Preconditions.checkArgument(cameraInfoImpl instanceof Camera2CameraInfoImpl,
                "CameraInfo doesn't contain Camera2 implementation.");
        Camera2CameraInfo camera2CameraInfo =
                ((Camera2CameraInfoImpl) cameraInfoImpl).getCamera2CameraInfo();
        if (cameraInfo instanceof AdapterCameraInfo) {
            SessionProcessor sessionProcessor =
                    ((AdapterCameraInfo) cameraInfo).getSessionProcessor();
            if (sessionProcessor != null) {
                camera2CameraInfo = new Camera2CameraInfo(
                        camera2CameraInfo.mCamera2CameraInfoImpl,
                        sessionProcessor.getAvailableCharacteristicsKeyValues()
                );
            }
        }
        return camera2CameraInfo;
    }

    /**
     * Gets the string camera ID.
     *
     * <p>The camera ID is the same as the camera ID that would be obtained from
     * {@link android.hardware.camera2.CameraManager#getCameraIdList()}. The ID that is retrieved
     * is not static and can change depending on the current internal configuration of the
     * {@link androidx.camera.core.Camera} from which the CameraInfo was retrieved.
     *
     * The Camera is a logical camera which can be backed by multiple
     * {@link android.hardware.camera2.CameraDevice}. However, only one CameraDevice is active at
     * one time. When the CameraDevice changes then the camera id will change.
     *
     * @return the camera ID.
     * @throws IllegalStateException if the camera info does not contain the camera 2 camera ID
     *                               (e.g., if CameraX was not initialized with a
     *                               {@link androidx.camera.camera2.Camera2Config}).
     */
    public @NonNull String getCameraId() {
        if (mCamera2PhysicalCameraInfo != null) {
            return mCamera2PhysicalCameraInfo.getCameraId();
        }
        return mCamera2CameraInfoImpl.getCameraId();
    }

    /**
     * Gets a camera characteristic value.
     *
     * <p>The characteristic value is the same as the value in the {@link CameraCharacteristics}
     * that would be obtained from
     * {@link android.hardware.camera2.CameraManager#getCameraCharacteristics(String)}.
     *
     * @param <T> The type of the characteristic value.
     * @param key The {@link CameraCharacteristics.Key} of the characteristic.
     * @return the value of the characteristic.
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getCameraCharacteristic(CameraCharacteristics.@NonNull Key<T> key) {
        if (mCamera2PhysicalCameraInfo != null) {
            return mCamera2PhysicalCameraInfo.getCameraCharacteristicsCompat().get(key);
        }

        if (mExtensionsSpecificChars != null) {
            for (Pair<CameraCharacteristics.Key, Object> pair : mExtensionsSpecificChars) {
                if (pair.first.equals(key)) {
                    return (T) pair.second;
                }
            }
        }
        return mCamera2CameraInfoImpl.getCameraCharacteristicsCompat().get(key);
    }

    /**
     * Returns the {@link CameraCharacteristics} for this camera.
     *
     * <p>The CameraCharacteristics will be the ones that would be obtained by
     * {@link android.hardware.camera2.CameraManager#getCameraCharacteristics(String)}. The
     * CameraCharacteristics that are retrieved are not static and can change depending on the
     * current internal configuration of the camera.
     *
     * @param cameraInfo The {@link CameraInfo} to extract the CameraCharacteristics from.
     * @throws IllegalStateException if the camera info does not contain the camera 2
     *                               characteristics(e.g., if CameraX was not initialized with a
     *                               {@link androidx.camera.camera2.Camera2Config}).
     */
    // TODO: Hidden until new extensions API released.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static @NonNull CameraCharacteristics extractCameraCharacteristics(
            @NonNull CameraInfo cameraInfo) {
        if (cameraInfo instanceof Camera2PhysicalCameraInfoImpl) {
            return ((Camera2PhysicalCameraInfoImpl) cameraInfo)
                    .getCameraCharacteristicsCompat()
                    .toCameraCharacteristics();
        }

        CameraInfoInternal cameraInfoImpl = ((CameraInfoInternal) cameraInfo).getImplementation();
        Preconditions.checkState(cameraInfoImpl instanceof Camera2CameraInfoImpl,
                "CameraInfo does not contain any Camera2 information.");
        Camera2CameraInfoImpl impl = (Camera2CameraInfoImpl) cameraInfoImpl;
        return impl.getCameraCharacteristicsCompat().toCameraCharacteristics();
    }

    /**
     * Returns a map consisting of the camera ids and the {@link CameraCharacteristics}s.
     *
     * <p>For every camera, the map contains at least the CameraCharacteristics for the camera id.
     * If the camera is logical camera, it will also contain associated physical camera ids and
     * their CameraCharacteristics.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Map<String, CameraCharacteristics> getCameraCharacteristicsMap() {
        if (mCamera2PhysicalCameraInfo != null) {
            return Collections.emptyMap();
        }
        return mCamera2CameraInfoImpl.getCameraCharacteristicsMap();
    }
}
