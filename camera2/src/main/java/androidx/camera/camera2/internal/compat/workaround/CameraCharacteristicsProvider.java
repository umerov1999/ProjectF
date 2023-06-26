/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * An interface for providing camera characteristics from
 * {@link android.hardware.camera2.CameraCharacteristics.Key}.
 *
 * <p>This layer of abstraction can be used to customize the behavior of providing
 * characteristics for testing.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CameraCharacteristicsProvider {
    /** Retrieves the camera characteristic value from the provided key. */
    @Nullable
    <T> T get(@NonNull CameraCharacteristics.Key<T> key);
}
