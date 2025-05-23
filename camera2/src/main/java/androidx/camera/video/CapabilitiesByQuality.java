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

package androidx.camera.video;

import static androidx.camera.core.internal.utils.SizeUtil.findNearestHigherFor;

import android.util.Size;

import androidx.annotation.RestrictTo;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.video.Quality.QualitySource;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class implements the video capabilities query logic related to quality and resolution.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CapabilitiesByQuality {
    private static final String TAG = "CapabilitiesByQuality";

    /**
     * Maps quality to supported {@link VideoValidatedEncoderProfilesProxy}. The order is from
     * size large to small.
     */
    private final Map<Quality, VideoValidatedEncoderProfilesProxy> mSupportedProfilesMap =
            new LinkedHashMap<>();
    private final TreeMap<Size, Quality> mAreaSortedSizeToQualityMap =
            new TreeMap<>(new CompareSizesByArea());
    private final VideoValidatedEncoderProfilesProxy mHighestProfiles;
    private final VideoValidatedEncoderProfilesProxy mLowestProfiles;

    public CapabilitiesByQuality(@NonNull EncoderProfilesProvider provider,
            @QualitySource int qualitySource) {
        // Construct supported profile map.
        for (Quality quality : Quality.getSortedQualities()) {
            EncoderProfilesProxy profiles = getEncoderProfiles(quality, provider, qualitySource);
            if (profiles == null) {
                continue;
            }

            // Validate that EncoderProfiles contain video information.
            Logger.d(TAG, "profiles = " + profiles);
            VideoValidatedEncoderProfilesProxy validatedProfiles = toValidatedProfiles(
                    profiles);
            if (validatedProfiles == null) {
                Logger.w(TAG, "EncoderProfiles of quality " + quality + " has no video "
                        + "validated profiles.");
                continue;
            }

            EncoderProfilesProxy.VideoProfileProxy videoProfile =
                    validatedProfiles.getDefaultVideoProfile();
            mAreaSortedSizeToQualityMap.put(videoProfile.getResolution(), quality);

            // SortedQualities is from size large to small.
            mSupportedProfilesMap.put(quality, validatedProfiles);
        }
        if (mSupportedProfilesMap.isEmpty()) {
            Logger.e(TAG, "No supported EncoderProfiles");
            mLowestProfiles = null;
            mHighestProfiles = null;
        } else {
            Deque<VideoValidatedEncoderProfilesProxy> profileQueue = new ArrayDeque<>(
                    mSupportedProfilesMap.values());
            mHighestProfiles = profileQueue.peekFirst();
            mLowestProfiles = profileQueue.peekLast();
        }
    }

    /**
     * Gets the supported quality list.
     *
     * <p>The returned list is sorted by quality size from largest to smallest. For the qualities
     * in the returned list, {@link #isQualitySupported(Quality)} will return {@code true}.
     *
     * <p>Note: Constants {@link Quality#HIGHEST} and {@link Quality#LOWEST} are not included in
     * the returned list, but their corresponding qualities are included.
     */
    public @NonNull List<Quality> getSupportedQualities() {
        return new ArrayList<>(mSupportedProfilesMap.keySet());
    }

    /**
     * Checks whether the quality is supported.
     *
     * <p>If this method is called with {@link Quality#LOWEST} or {@link Quality#HIGHEST}, it
     * will return {@code true} except the case that none of the qualities can be supported.
     */
    public boolean isQualitySupported(@NonNull Quality quality) {
        checkQualityConstantsOrThrow(quality);
        return getProfiles(quality) != null;
    }

    /**
     * Gets a {@link VideoValidatedEncoderProfilesProxy} for the input quality or {@code null} if
     * the quality is not supported.
     */
    public @Nullable VideoValidatedEncoderProfilesProxy getProfiles(@NonNull Quality quality) {
        checkQualityConstantsOrThrow(quality);
        if (quality == Quality.HIGHEST) {
            return mHighestProfiles;
        } else if (quality == Quality.LOWEST) {
            return mLowestProfiles;
        }
        return mSupportedProfilesMap.get(quality);
    }

    /**
     * Finds the nearest higher supported {@link VideoValidatedEncoderProfilesProxy} for the
     * input size.
     */
    public @Nullable VideoValidatedEncoderProfilesProxy
            findNearestHigherSupportedEncoderProfilesFor(@NonNull Size size) {
        VideoValidatedEncoderProfilesProxy encoderProfiles = null;
        Quality highestSupportedQuality = findNearestHigherSupportedQualityFor(size);
        Logger.d(TAG,
                "Using supported quality of " + highestSupportedQuality + " for size " + size);
        if (highestSupportedQuality != Quality.NONE) {
            encoderProfiles = getProfiles(highestSupportedQuality);
            if (encoderProfiles == null) {
                throw new AssertionError("Camera advertised available quality but did not "
                        + "produce EncoderProfiles for advertised quality.");
            }
        }
        return encoderProfiles;
    }

    /** Finds the nearest higher supported {@link Quality} for the input size. */
    public @NonNull Quality findNearestHigherSupportedQualityFor(@NonNull Size size) {
        Quality quality = findNearestHigherFor(size, mAreaSortedSizeToQualityMap);
        return quality != null ? quality : Quality.NONE;
    }

    private @Nullable EncoderProfilesProxy getEncoderProfiles(@NonNull Quality quality,
            @NonNull EncoderProfilesProvider provider, @QualitySource int qualitySource) {
        Preconditions.checkState(quality instanceof Quality.ConstantQuality,
                "Currently only support ConstantQuality");
        Quality.ConstantQuality constantQuality = (Quality.ConstantQuality) quality;
        int qualityValue = constantQuality.getQualityValue(qualitySource);

        return provider.getAll(qualityValue);
    }

    private @Nullable VideoValidatedEncoderProfilesProxy toValidatedProfiles(
            @NonNull EncoderProfilesProxy profiles) {
        // According to the document, the first profile is the default video profile.
        List<EncoderProfilesProxy.VideoProfileProxy> videoProfiles =
                profiles.getVideoProfiles();
        if (videoProfiles.isEmpty()) {
            return null;
        }

        return VideoValidatedEncoderProfilesProxy.from(profiles);
    }

    /**
     * Checks if the {@link EncoderProfilesProvider} contains at least one supported quality.
     */
    public static boolean containsSupportedQuality(@NonNull EncoderProfilesProvider provider,
            @QualitySource int qualitySource) {
        return !new CapabilitiesByQuality(provider, qualitySource)
                .getSupportedQualities().isEmpty();
    }

    private static void checkQualityConstantsOrThrow(@NonNull Quality quality) {
        Preconditions.checkArgument(Quality.containsQuality(quality),
                "Unknown quality: " + quality);
    }
}
