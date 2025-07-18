/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.featurecombinationquery;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;

import androidx.annotation.RequiresApi;

import org.jspecify.annotations.NonNull;

/**
 * A Android framework based {@link CameraDeviceSetupCompat} implementation.
 */
@RequiresApi(api = 35)
class Camera2CameraDeviceSetupCompatProvider implements CameraDeviceSetupCompatProvider {

    private final CameraManager mCameraManager;

    Camera2CameraDeviceSetupCompatProvider(@NonNull Context context) {
        mCameraManager = context.getSystemService(CameraManager.class);
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException if
     * {@link android.hardware.camera2.CameraDevice.CameraDeviceSetup} cannot be created for the
     * provided {@code cameraId}
     */
    @Override
    public @NonNull CameraDeviceSetupCompat getCameraDeviceSetupCompat(@NonNull String cameraId)
            throws CameraAccessException {
        return new Camera2CameraDeviceSetupCompat(mCameraManager, cameraId);
    }
}
