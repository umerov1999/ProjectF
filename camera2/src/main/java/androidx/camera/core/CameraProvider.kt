/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.camera.core

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.lifecycle.LifecycleOwner

/**
 * A [CameraProvider] provides basic access to a set of cameras such as querying for camera
 * existence or information.
 *
 * A device might have multiple cameras. According to the applications' design, they might need to
 * search for a suitable camera which supports their functions. A [CameraProvider] allows the
 * applications to check whether any camera exists to fulfill the requirements or to get
 * [CameraInfo] instances of all cameras to retrieve the camera information.
 */
public interface CameraProvider {
    /**
     * The [CameraInfo] instances of the available cameras.
     *
     * While iterating through all the available [CameraInfo], if one of them meets some predefined
     * requirements, a [CameraSelector] that uniquely identifies its camera can be retrieved using
     * [CameraInfo.getCameraSelector], which can then be used to bind [use cases][UseCase] to that
     * camera.
     */
    public val availableCameraInfos: List<CameraInfo>

    /**
     * Returns list of [CameraInfo] instances of the available concurrent cameras.
     *
     * The available concurrent cameras include all combinations of cameras which could operate
     * concurrently on the device. Each list maps to one combination of these camera's [CameraInfo].
     *
     * For example, to select a front camera and a back camera and bind to [LifecycleOwner] with
     * preview [UseCase], this function could be used with `bindToLifecycle`.
     *
     * @sample androidx.camera.lifecycle.samples.bindConcurrentCameraSample
     * @return List of combinations of [CameraInfo].
     */
    @get:RestrictTo(Scope.LIBRARY_GROUP)
    public val availableConcurrentCameraInfos: List<List<CameraInfo>>

    /**
     * Returns whether there is a [ConcurrentCamera] bound.
     *
     * @return `true` if there is a [ConcurrentCamera] bound, otherwise `false`.
     */
    @get:RestrictTo(Scope.LIBRARY_GROUP) public val isConcurrentCameraModeOn: Boolean

    /**
     * Checks whether this provider supports at least one camera that meets the requirements from a
     * [CameraSelector].
     *
     * If this method returns `true`, then the camera selector can be used to bind use cases and
     * retrieve a [Camera] instance.
     *
     * @param cameraSelector the [CameraSelector] that filters available cameras.
     * @return `true` if the device has at least one available camera, otherwise `false`.
     * @throws CameraInfoUnavailableException if unable to access cameras, perhaps due to
     *   insufficient permissions.
     */
    @Throws(CameraInfoUnavailableException::class)
    public fun hasCamera(cameraSelector: CameraSelector): Boolean

    /**
     * Returns the [CameraInfo] instance of the camera resulted from the specified [CameraSelector].
     *
     * The returned [CameraInfo] corresponds to the camera that will be bound when calling
     * `bindToLifecycle` with the specified [CameraSelector].
     *
     * @param cameraSelector the [CameraSelector] to use for selecting the camera to receive
     *   information about.
     * @return the corresponding [CameraInfo].
     * @throws IllegalArgumentException if the given [CameraSelector] can't result in a valid camera
     *   to provide the [CameraInfo].
     */
    public fun getCameraInfo(cameraSelector: CameraSelector): CameraInfo {
        throw UnsupportedOperationException("The camera provider is not implemented properly.")
    }

    /** Returns the [CameraXConfig] implementation type. */
    @get:RestrictTo(Scope.LIBRARY_GROUP) @CameraXConfig.ImplType public val configImplType: Int
}
