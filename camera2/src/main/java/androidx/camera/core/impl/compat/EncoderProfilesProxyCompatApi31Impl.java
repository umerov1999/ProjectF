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

package androidx.camera.core.impl.compat;

import android.media.EncoderProfiles;
import android.media.EncoderProfiles.AudioProfile;
import android.media.EncoderProfiles.VideoProfile;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(31)
class EncoderProfilesProxyCompatApi31Impl {

    /** Creates an EncoderProfilesProxy instance from {@link EncoderProfiles}. */
    public static @NonNull EncoderProfilesProxy from(
            @NonNull EncoderProfiles encoderProfiles) {
        return ImmutableEncoderProfilesProxy.create(
                encoderProfiles.getDefaultDurationSeconds(),
                encoderProfiles.getRecommendedFileFormat(),
                fromAudioProfiles(encoderProfiles.getAudioProfiles()),
                fromVideoProfiles(encoderProfiles.getVideoProfiles())
        );
    }

    /** Creates VideoProfileProxy instances from a list of {@link VideoProfile}. */
    private static @NonNull List<VideoProfileProxy> fromVideoProfiles(
            @NonNull List<VideoProfile> profiles) {
        List<VideoProfileProxy> proxies = new ArrayList<>();
        for (VideoProfile profile : profiles) {
            proxies.add(VideoProfileProxy.create(
                    profile.getCodec(),
                    profile.getMediaType(),
                    profile.getBitrate(),
                    profile.getFrameRate(),
                    profile.getWidth(),
                    profile.getHeight(),
                    profile.getProfile(),
                    VideoProfileProxy.BIT_DEPTH_8,
                    VideoProfile.YUV_420,
                    VideoProfile.HDR_NONE
            ));
        }
        return proxies;
    }

    /** Creates AudioProfileProxy instances from a list of {@link AudioProfile}. */
    private static @NonNull List<AudioProfileProxy> fromAudioProfiles(
            @NonNull List<AudioProfile> profiles) {
        List<AudioProfileProxy> proxies = new ArrayList<>();
        for (AudioProfile profile : profiles) {
            proxies.add(AudioProfileProxy.create(
                    profile.getCodec(),
                    profile.getMediaType(),
                    profile.getBitrate(),
                    profile.getSampleRate(),
                    profile.getChannels(),
                    profile.getProfile()
            ));
        }
        return proxies;
    }

    // Class should not be instantiated.
    private EncoderProfilesProxyCompatApi31Impl() {
    }
}
