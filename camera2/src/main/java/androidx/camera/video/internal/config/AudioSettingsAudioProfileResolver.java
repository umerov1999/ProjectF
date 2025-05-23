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

package androidx.camera.video.internal.config;

import android.util.Range;
import android.util.Rational;

import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy;
import androidx.camera.video.AudioSpec;
import androidx.camera.video.internal.audio.AudioSettings;
import androidx.core.util.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An {@link AudioSettings} supplier that resolves requested audio settings from an
 * {@link AudioSpec} using an {@link AudioProfileProxy}.
 */
public final class AudioSettingsAudioProfileResolver implements Supplier<AudioSettings> {

    private static final String TAG = "AudioSrcAdPrflRslvr";

    private final AudioSpec mAudioSpec;
    private final AudioProfileProxy mAudioProfile;
    @Nullable
    private final Rational mCaptureToEncodeRatio;

    /**
     * Constructor for an AudioSettingsAudioProfileResolver.
     *
     * @param audioProfile  The {@link AudioProfileProxy} used to resolve automatic and range
     *                      settings.
     * @param audioSpec     The {@link AudioSpec} which defines the settings that should be used
     *                      with the audio source.
     * @param captureToEncodeRatio The capture to encode sample rate ratio.
     */
    public AudioSettingsAudioProfileResolver(@NonNull AudioSpec audioSpec,
            @NonNull AudioProfileProxy audioProfile, @Nullable Rational captureToEncodeRatio) {
        mAudioSpec = audioSpec;
        mAudioProfile = audioProfile;
        mCaptureToEncodeRatio = captureToEncodeRatio;
    }

    @Override
    public @NonNull AudioSettings get() {
        // Resolve audio source
        int resolvedAudioSource = AudioConfigUtil.resolveAudioSource(mAudioSpec);

        // Resolve source format
        int resolvedSourceFormat = AudioConfigUtil.resolveAudioSourceFormat(mAudioSpec);

        int audioSpecChannelCount = mAudioSpec.getChannelCount();
        int resolvedChannelCount;
        int audioProfileChannelCount = mAudioProfile.getChannels();
        if (audioSpecChannelCount == AudioSpec.CHANNEL_COUNT_AUTO) {
            resolvedChannelCount = audioProfileChannelCount;
            Logger.d(TAG, "Resolved AUDIO channel count from AudioProfile: "
                    + resolvedChannelCount);
        } else {
            resolvedChannelCount = audioSpecChannelCount;
            Logger.d(TAG, "Media spec AUDIO channel count overrides AudioProfile "
                    + "[AudioProfile channel count: " + audioProfileChannelCount
                    + ", Resolved Channel Count: " + resolvedChannelCount + "]");
        }

        Range<Integer> audioSpecSampleRate = mAudioSpec.getSampleRate();
        int audioProfileSampleRate = mAudioProfile.getSampleRate();
        CaptureEncodeRates resolvedSampleRates = AudioConfigUtil.resolveSampleRates(
                audioSpecSampleRate, audioProfileSampleRate, resolvedChannelCount,
                resolvedSourceFormat, mCaptureToEncodeRatio);
        Logger.d(TAG, "Using resolved AUDIO sample rate or nearest supported from "
                + "AudioProfile: Capture sample rate: " + resolvedSampleRates.getCaptureRate()
                + "Hz. Encode sample rate: " + resolvedSampleRates.getEncodeRate() + "Hz. "
                + "[AudioProfile sample rate: " + audioProfileSampleRate + "Hz]");

        return AudioSettings.builder()
                .setAudioSource(resolvedAudioSource)
                .setAudioFormat(resolvedSourceFormat)
                .setChannelCount(resolvedChannelCount)
                .setCaptureSampleRate(resolvedSampleRates.getCaptureRate())
                .setEncodeSampleRate(resolvedSampleRates.getEncodeRate())
                .build();
    }
}
