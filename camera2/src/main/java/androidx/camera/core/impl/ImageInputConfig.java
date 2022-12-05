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

package androidx.camera.core.impl;

import androidx.annotation.RequiresApi;
import androidx.camera.core.Camera;

/** Configuration containing options for configuring the input image data of a pipeline. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ImageInputConfig extends ReadableConfig {
    Config.Option<Integer> OPTION_INPUT_FORMAT =
            Config.Option.create("camerax.core.imageInput.inputFormat", int.class);

    /**
     * Retrieve the input image format.
     *
     * <p>This is the format that is required as input and it must be satisfied by the producer.
     * It is used to determine if the producer, such as a {@link Camera} can actually support the
     * {@link android.view.Surface}.
     *
     * <p>Except for ImageFormat.JPEG or ImageFormat.YUV, other image formats like SurfaceTexture or
     * MediaCodec classes will be mapped to internal format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED
     * (0x22) in StreamConfigurationMap.java. 0x22 is also the code for ImageFormat.PRIVATE. But
     * there is no ImageFormat.PRIVATE supported before Android level 23. There is same internal
     * code 0x22 for internal corresponding format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED.
     * Therefore, setting 0x22 as default image format.
     */
    default int getInputFormat() {
        return retrieveOption(OPTION_INPUT_FORMAT);
    }
}
