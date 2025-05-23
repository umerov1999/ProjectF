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

package androidx.camera.video.internal.workaround;

import static androidx.core.util.Preconditions.checkState;

import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.Quirks;
import androidx.camera.video.internal.compat.quirk.ExtraSupportedQualityQuirk;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation that adds extra supported qualities.
 *
 * @see ExtraSupportedQualityQuirk
 */
public class QualityAddedEncoderProfilesProvider implements EncoderProfilesProvider {

    private final EncoderProfilesProvider mProvider;
    private @Nullable Map<Integer, EncoderProfilesProxy> mExtraQualityToEncoderProfiles;

    public QualityAddedEncoderProfilesProvider(
            @NonNull EncoderProfilesProvider provider,
            @NonNull Quirks quirks,
            @NonNull CameraInfoInternal cameraInfo,
            VideoEncoderInfo.@NonNull Finder videoEncoderInfoFinder) {
        mProvider = provider;

        List<ExtraSupportedQualityQuirk> extraQuirks = quirks.getAll(
                ExtraSupportedQualityQuirk.class);
        if (!extraQuirks.isEmpty()) {
            checkState(extraQuirks.size() == 1);
            Map<Integer, EncoderProfilesProxy> extraEncoderProfiles = extraQuirks.get(0)
                    .getExtraEncoderProfiles(cameraInfo, mProvider, videoEncoderInfoFinder);
            if (extraEncoderProfiles != null) {
                mExtraQualityToEncoderProfiles = new HashMap<>(extraEncoderProfiles);
            }
        }
    }

    @Override
    public boolean hasProfile(int quality) {
        return getProfilesInternal(quality) != null;
    }

    @Override
    public @Nullable EncoderProfilesProxy getAll(int quality) {
        return getProfilesInternal(quality);
    }

    private @Nullable EncoderProfilesProxy getProfilesInternal(int quality) {
        if (mExtraQualityToEncoderProfiles != null
                && mExtraQualityToEncoderProfiles.containsKey(quality)) {
            return mExtraQualityToEncoderProfiles.get(quality);
        }
        return mProvider.getAll(quality);
    }
}
