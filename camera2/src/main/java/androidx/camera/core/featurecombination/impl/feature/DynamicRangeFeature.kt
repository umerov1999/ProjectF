/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core.featurecombination.impl.feature

import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.featurecombination.Feature

/**
 * Denotes the dynamic range applied to all [UseCase]s which are not of image capture type in a
 * feature combination, i.e. [Preview] and `VideoCapture` use cases currently.
 *
 * This feature can not be instantiated directly, instead use the [Feature.HDR_HLG10] object.
 */
internal class DynamicRangeFeature(val dynamicRange: DynamicRange) : Feature() {
    override val featureTypeInternal: FeatureTypeInternal = FeatureTypeInternal.DYNAMIC_RANGE

    override fun toString(): String {
        return "DynamicRangeFeature(dynamicRange=$dynamicRange)"
    }

    companion object {
        @JvmField val DEFAULT_DYNAMIC_RANGE = DynamicRange.SDR
    }
}
