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

package androidx.camera.video.impl;

import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.internal.ThreadConfig;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoOutput;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;

import org.jspecify.annotations.NonNull;

/**
 * Config for a video capture use case.
 *
 * <p>In the earlier stage, the VideoCapture is deprioritized.
 *
 * @param <T> the type of VideoOutput
 */
public final class VideoCaptureConfig<T extends VideoOutput>
        implements UseCaseConfig<VideoCapture<T>>,
        ImageOutputConfig,
        ThreadConfig {

    // Option Declarations:
    // *********************************************************************************************

    public static final Option<VideoOutput> OPTION_VIDEO_OUTPUT =
            Option.create("camerax.video.VideoCapture.videoOutput", VideoOutput.class);

    public static final Option<VideoEncoderInfo.Finder>
            OPTION_VIDEO_ENCODER_INFO_FINDER =
            Option.create("camerax.video.VideoCapture.videoEncoderInfoFinder",
                    VideoEncoderInfo.Finder.class);

    public static final Option<Boolean> OPTION_FORCE_ENABLE_SURFACE_PROCESSING = Option.create(
            "camerax.video.VideoCapture.forceEnableSurfaceProcessing", Boolean.class);

    // *********************************************************************************************

    private final OptionsBundle mConfig;

    public VideoCaptureConfig(@NonNull OptionsBundle config) {
        checkArgument(config.containsOption(OPTION_VIDEO_OUTPUT));
        mConfig = config;
    }

    @SuppressWarnings("unchecked")
    public @NonNull T getVideoOutput() {
        return (T) requireNonNull(retrieveOption(OPTION_VIDEO_OUTPUT));
    }

    public VideoEncoderInfo.@NonNull Finder getVideoEncoderInfoFinder() {
        return requireNonNull(retrieveOption(OPTION_VIDEO_ENCODER_INFO_FINDER));
    }

    public boolean isSurfaceProcessingForceEnabled() {
        return requireNonNull(retrieveOption(OPTION_FORCE_ENABLE_SURFACE_PROCESSING, false));
    }

    /**
     * Retrieves the format of the image that is fed as input.
     *
     * <p>This should always be PRIVATE for VideoCapture.
     */
    @Override
    public int getInputFormat() {
        return ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
    }

    @Override
    public @NonNull Config getConfig() {
        return mConfig;
    }
}
