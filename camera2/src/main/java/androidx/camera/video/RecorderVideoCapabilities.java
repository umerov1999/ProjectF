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

import static androidx.camera.core.DynamicRange.ENCODING_HLG;
import static androidx.camera.core.DynamicRange.SDR;
import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.video.CapabilitiesByQuality.containsSupportedQuality;
import static androidx.camera.video.Quality.FHD;
import static androidx.camera.video.Quality.HD;
import static androidx.camera.video.Quality.QUALITY_SOURCE_HIGH_SPEED;
import static androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR;
import static androidx.camera.video.Quality.SD;
import static androidx.camera.video.Quality.getSortedQualities;
import static androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE;
import static androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES;
import static androidx.camera.video.Recorder.VIDEO_RECORDING_TYPE_HIGH_SPEED;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import android.util.Range;
import android.util.Size;

import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.DynamicRanges;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.Quirks;
import androidx.camera.video.Quality.QualitySource;
import androidx.camera.video.internal.BackupHdrProfileEncoderProfilesProvider;
import androidx.camera.video.internal.DynamicRangeMatchedEncoderProfilesProvider;
import androidx.camera.video.internal.QualityExploredEncoderProfilesProvider;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider;
import androidx.camera.video.internal.workaround.QualityAddedEncoderProfilesProvider;
import androidx.camera.video.internal.workaround.QualityResolutionModifiedEncoderProfilesProvider;
import androidx.camera.video.internal.workaround.QualityValidatedEncoderProfilesProvider;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RecorderVideoCapabilities is used to query video recording capabilities related to Recorder.
 *
 * <p>The {@link EncoderProfilesProxy} queried from RecorderVideoCapabilities will contain
 * {@link VideoProfileProxy}s matches with the target {@link DynamicRange}. When HDR is
 * supported, RecorderVideoCapabilities will try best to provide additional backup HDR
 * {@link VideoProfileProxy}s in case the information is lacked in the device.
 *
 * @see Recorder#getVideoCapabilities(CameraInfo)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RecorderVideoCapabilities implements VideoCapabilities {
    private static final String TAG = "RecorderVideoCapabilities";

    private final CameraInfoInternal mCameraInfo;
    private final EncoderProfilesProvider mProfilesProvider;
    private final boolean mIsStabilizationSupported;
    private final @QualitySource int mQualitySource;

    // Mappings of DynamicRange to recording capability information. The mappings are divided
    // into two collections based on the key's (DynamicRange) category, one for specified
    // DynamicRange and one for others. Specified DynamicRange means that its bit depth and
    // format are specified values, not some wildcards, such as: ENCODING_UNSPECIFIED,
    // ENCODING_HDR_UNSPECIFIED or BIT_DEPTH_UNSPECIFIED.
    private final Map<DynamicRange, CapabilitiesByQuality>
            mCapabilitiesMapForFullySpecifiedDynamicRange = new HashMap<>();
    private final Map<DynamicRange, CapabilitiesByQuality>
            mCapabilitiesMapForNonFullySpecifiedDynamicRange = new HashMap<>();

    /**
     * Creates a RecorderVideoCapabilities.
     *
     * @param videoCapabilitiesSource the video capabilities source. Possible values include
     *                                {@link Recorder#VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE}
     *                                and
     *                                {@link Recorder#VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES}.
     * @param cameraInfo              the cameraInfo.
     * @param videoEncoderInfoFinder  the VideoEncoderInfo finder.
     * @param videoCaptureType        the video capture type.
     * @throws IllegalArgumentException if unable to get the capability information from the
     *                                  CameraInfo or the videoCapabilitiesSource is not supported.
     */
    RecorderVideoCapabilities(@Recorder.VideoCapabilitiesSource int videoCapabilitiesSource,
            @NonNull CameraInfoInternal cameraInfo,
            @Recorder.VideoRecordingType int videoCaptureType,
            VideoEncoderInfo.@NonNull Finder videoEncoderInfoFinder) {
        checkArgument(videoCapabilitiesSource == VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE
                        || videoCapabilitiesSource == VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                "Not a supported video capabilities source: " + videoCapabilitiesSource);

        mCameraInfo = cameraInfo;
        mQualitySource = videoCaptureType == VIDEO_RECORDING_TYPE_HIGH_SPEED
                ? QUALITY_SOURCE_HIGH_SPEED : QUALITY_SOURCE_REGULAR;
        mProfilesProvider = getEncoderProfilesProvider(videoCapabilitiesSource, cameraInfo,
                videoEncoderInfoFinder, mQualitySource);

        // Group by dynamic range.
        for (DynamicRange dynamicRange : cameraInfo.getSupportedDynamicRanges()) {
            // Filter video profiles to include only the profiles match with the target dynamic
            // range.
            EncoderProfilesProvider constrainedProvider =
                    new DynamicRangeMatchedEncoderProfilesProvider(mProfilesProvider, dynamicRange);
            CapabilitiesByQuality capabilities = new CapabilitiesByQuality(constrainedProvider,
                    mQualitySource);

            if (!capabilities.getSupportedQualities().isEmpty()) {
                mCapabilitiesMapForFullySpecifiedDynamicRange.put(dynamicRange, capabilities);
            }
        }

        // Video stabilization
        mIsStabilizationSupported = cameraInfo.isVideoStabilizationSupported();
    }

    @Override
    public @NonNull Set<DynamicRange> getSupportedDynamicRanges() {
        return mCapabilitiesMapForFullySpecifiedDynamicRange.keySet();
    }

    @Override
    public @NonNull List<Quality> getSupportedQualities(@NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? new ArrayList<>() : capabilities.getSupportedQualities();
    }

    @Override
    public boolean isQualitySupported(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities != null && capabilities.isQualitySupported(quality);
    }

    @Override
    public @NonNull Set<Range<Integer>> getSupportedFrameRateRanges(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        if (mQualitySource == QUALITY_SOURCE_HIGH_SPEED) {
            return getHighSpeedSupportedFrameRateRanges(quality, dynamicRange);
        }

        // TODO: filter fps by maximum fps of quality.
        return mCameraInfo.getSupportedFrameRateRanges();
    }

    @Override
    public boolean isStabilizationSupported() {
        return mIsStabilizationSupported;
    }

    @Override
    public @Nullable VideoValidatedEncoderProfilesProxy getProfiles(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? null : capabilities.getProfiles(quality);
    }

    @Override
    public @Nullable VideoValidatedEncoderProfilesProxy
            findNearestHigherSupportedEncoderProfilesFor(
                    @NonNull Size size, @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? null
                : capabilities.findNearestHigherSupportedEncoderProfilesFor(size);
    }

    @Override
    public @NonNull Quality findNearestHigherSupportedQualityFor(@NonNull Size size,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? Quality.NONE
                : capabilities.findNearestHigherSupportedQualityFor(size);
    }

    private static @NonNull EncoderProfilesProvider getEncoderProfilesProvider(
            @Recorder.VideoCapabilitiesSource int videoCapabilitiesSource,
            @NonNull CameraInfoInternal cameraInfo,
            VideoEncoderInfo.@NonNull Finder videoEncoderInfoFinder,
            @QualitySource int qualitySource) {

        EncoderProfilesProvider encoderProfilesProvider = cameraInfo.getEncoderProfilesProvider();

        if (qualitySource == QUALITY_SOURCE_HIGH_SPEED) {

            if (!cameraInfo.isHighSpeedSupported()) {
                return EncoderProfilesProvider.EMPTY;
            }

            // TODO(b/399585664): explore high speed quality when video source is
            //  VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES
            return encoderProfilesProvider;
        }

        if (!containsSupportedQuality(encoderProfilesProvider, qualitySource)) {
            Logger.w(TAG, "Camera EncoderProfilesProvider doesn't contain any supported Quality.");
            // Limit maximum supported video resolution to 1080p(FHD).
            // While 2160p(UHD) may be reported as supported by the Camera and MediaCodec APIs,
            // testing on lab devices has shown that recording at this resolution is not always
            // reliable. This aligns with the Android 5.1 CDD, which recommends 1080p as the
            // supported resolution.
            // See: https://source.android.com/static/docs/compatibility/5.1/android-5.1-cdd.pdf,
            // 5.2. Video Encoding.
            List<Quality> targetQualities = Arrays.asList(FHD, HD, SD);
            encoderProfilesProvider = new DefaultEncoderProfilesProvider(cameraInfo,
                    targetQualities, videoEncoderInfoFinder);
        }

        Quirks deviceQuirks = DeviceQuirks.getAll();
        // Add extra supported quality.
        encoderProfilesProvider = new QualityAddedEncoderProfilesProvider(encoderProfilesProvider,
                deviceQuirks, cameraInfo, videoEncoderInfoFinder);

        if (videoCapabilitiesSource == VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES) {
            encoderProfilesProvider = new QualityExploredEncoderProfilesProvider(
                    encoderProfilesProvider,
                    getSortedQualities(),
                    singleton(SDR),
                    cameraInfo.getSupportedResolutions(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE),
                    videoEncoderInfoFinder);
        }

        // Modify qualities' matching resolution to the value supported by camera.
        encoderProfilesProvider = new QualityResolutionModifiedEncoderProfilesProvider(
                encoderProfilesProvider, deviceQuirks);

        // Add backup HDR video information. In the initial version, only HLG10 profile is added.
        if (isHlg10SupportedByCamera(cameraInfo)) {
            encoderProfilesProvider = new BackupHdrProfileEncoderProfilesProvider(
                    encoderProfilesProvider, videoEncoderInfoFinder);
        }

        // Filter out unsupported qualities.
        encoderProfilesProvider = new QualityValidatedEncoderProfilesProvider(
                encoderProfilesProvider, cameraInfo, deviceQuirks);

        return encoderProfilesProvider;
    }

    private @Nullable CapabilitiesByQuality getCapabilities(@NonNull DynamicRange dynamicRange) {
        if (dynamicRange.isFullySpecified()) {
            return mCapabilitiesMapForFullySpecifiedDynamicRange.get(dynamicRange);
        }

        // Handle dynamic range that is not fully specified.
        if (mCapabilitiesMapForNonFullySpecifiedDynamicRange.containsKey(dynamicRange)) {
            return mCapabilitiesMapForNonFullySpecifiedDynamicRange.get(dynamicRange);
        } else {
            CapabilitiesByQuality capabilities =
                    generateCapabilitiesForNonFullySpecifiedDynamicRange(dynamicRange);
            mCapabilitiesMapForNonFullySpecifiedDynamicRange.put(dynamicRange, capabilities);
            return capabilities;
        }
    }

    private static boolean isHlg10SupportedByCamera(
            @NonNull CameraInfoInternal cameraInfoInternal) {
        Set<DynamicRange> dynamicRanges = cameraInfoInternal.getSupportedDynamicRanges();
        for (DynamicRange dynamicRange : dynamicRanges) {
            Integer encoding = dynamicRange.getEncoding();
            int bitDepth = dynamicRange.getBitDepth();
            if (encoding.equals(ENCODING_HLG) && bitDepth == DynamicRange.BIT_DEPTH_10_BIT) {
                return true;
            }
        }

        return false;
    }

    private @Nullable CapabilitiesByQuality generateCapabilitiesForNonFullySpecifiedDynamicRange(
            @NonNull DynamicRange dynamicRange) {
        if (!DynamicRanges.canResolve(dynamicRange, getSupportedDynamicRanges())) {
            return null;
        }

        // Filter video profiles to include only the profiles match with the target dynamic
        // range.
        EncoderProfilesProvider constrainedProvider =
                new DynamicRangeMatchedEncoderProfilesProvider(mProfilesProvider, dynamicRange);
        return new CapabilitiesByQuality(constrainedProvider, mQualitySource);
    }

    private @NonNull Set<Range<Integer>> getHighSpeedSupportedFrameRateRanges(
            @NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        VideoValidatedEncoderProfilesProxy profiles = getProfiles(quality, dynamicRange);
        if (profiles == null) {
            return emptySet();
        }
        // Adopt the max frame rate if there are multiple.
        int maxProfileFps = 0;
        for (VideoProfileProxy videoProfile : profiles.getVideoProfiles()) {
            if (videoProfile.getFrameRate() > maxProfileFps) {
                maxProfileFps = videoProfile.getFrameRate();
            }
        }
        Set<Range<Integer>> supportedFpsRanges =
                mCameraInfo.getSupportedHighSpeedFrameRateRangesFor(
                        profiles.getDefaultVideoProfile().getResolution());

        // Retain FPS ranges where:
        // 1. the upper bound does not exceed the profile's frame rate.
        //   This is based on:
        //   * CTS limitations: CTS only verifies profile's frame rate, making higher rates
        //     potentially unreliable.
        //   * OEM considerations: While we don't know exactly why OEM chose this frame rate
        //     value, they often set a higher frame rate than just 120.
        //   * For the initial version, prioritizing low-risk implementations.
        // 2. the range is fixed (lower bound is equal to upper bound). Variable FPS ranges
        // are not allowed for dual-surface high-speed capture which makes it practically useless.
        // See CameraConstrainedHighSpeedCaptureSession#createHighSpeedRequestList.
        Set<Range<Integer>> filteredFpsRanges = new LinkedHashSet<>();
        for (Range<Integer> fpsRange : supportedFpsRanges) {
            if (fpsRange.getUpper() <= maxProfileFps
                    && fpsRange.getLower().equals(fpsRange.getUpper())) {
                filteredFpsRanges.add(fpsRange);
            }
        }
        return filteredFpsRanges;
    }
}
