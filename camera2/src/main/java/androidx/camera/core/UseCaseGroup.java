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

package androidx.camera.core;

import static androidx.camera.core.CameraEffect.IMAGE_CAPTURE;
import static androidx.camera.core.CameraEffect.PREVIEW;
import static androidx.camera.core.CameraEffect.VIDEO_CAPTURE;
import static androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;
import static androidx.camera.core.processing.TargetUtils.checkSupportedTargets;
import static androidx.camera.core.processing.TargetUtils.getHumanReadableName;
import static androidx.core.util.Preconditions.checkArgument;

import android.util.Range;

import androidx.annotation.RestrictTo;
import androidx.lifecycle.Lifecycle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Represents a collection of {@link UseCase}.
 *
 * <p>When the {@link UseCaseGroup} is bound to {@link Lifecycle}, it binds all the
 * {@link UseCase}s to the same {@link Lifecycle}. {@link UseCase}s inside of a
 * {@link UseCaseGroup} usually share some common properties like the FOV defined by
 * {@link ViewPort}.
 */
public final class UseCaseGroup {
    private final @Nullable ViewPort mViewPort;
    private final @NonNull List<UseCase> mUseCases;
    private final @NonNull List<CameraEffect> mEffects;
    private final @NonNull Range<Integer> mTargetHighSpeedFrameRate;

    UseCaseGroup(@Nullable ViewPort viewPort, @NonNull List<UseCase> useCases,
            @NonNull List<CameraEffect> effects) {
        mViewPort = viewPort;
        mUseCases = useCases;
        mEffects = effects;
        mTargetHighSpeedFrameRate = FRAME_RATE_RANGE_UNSPECIFIED;
    }

    /**
     * Gets the {@link ViewPort} shared by the {@link UseCase} collection.
     */
    public @Nullable ViewPort getViewPort() {
        return mViewPort;
    }

    /**
     * Gets the {@link UseCase}s.
     */
    public @NonNull List<UseCase> getUseCases() {
        return mUseCases;
    }

    /**
     * Gets the {@link CameraEffect}s.
     */
    public @NonNull List<CameraEffect> getEffects() {
        return mEffects;
    }

    /**
     * Gets the target high speed frame rate.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Range<Integer> getTargetHighSpeedFrameRate() {
        return mTargetHighSpeedFrameRate;
    }

    /**
     * A builder for generating {@link UseCaseGroup}.
     */
    public static final class Builder {

        // Allow-list effect targets supported by CameraX.
        private static final List<Integer> SUPPORTED_TARGETS = Arrays.asList(
                PREVIEW,
                VIDEO_CAPTURE,
                IMAGE_CAPTURE,
                PREVIEW | VIDEO_CAPTURE,
                PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE);

        private ViewPort mViewPort;
        private final List<UseCase> mUseCases;
        private final List<CameraEffect> mEffects;

        public Builder() {
            mUseCases = new ArrayList<>();
            mEffects = new ArrayList<>();
        }

        /**
         * Sets {@link ViewPort} shared by the {@link UseCase}s.
         */
        public @NonNull Builder setViewPort(@NonNull ViewPort viewPort) {
            mViewPort = viewPort;
            return this;
        }

        /**
         * Adds a {@link CameraEffect} to the collection.
         *
         * <p>The value of {@link CameraEffect#getTargets()} must be one of the supported values
         * below:
         * <ul>
         * <li>{@link CameraEffect#PREVIEW}
         * <li>{@link CameraEffect#VIDEO_CAPTURE}
         * <li>{@link CameraEffect#IMAGE_CAPTURE}
         * <li>{@link CameraEffect#VIDEO_CAPTURE} | {@link CameraEffect#PREVIEW}
         * <li>{@link CameraEffect#VIDEO_CAPTURE} | {@link CameraEffect#PREVIEW} |
         * {@link CameraEffect#IMAGE_CAPTURE}
         * </ul>
         *
         * <p>The targets must be mutually exclusive of each other, otherwise, the {@link #build()}
         * method will throw {@link IllegalArgumentException}. For example, it's invalid to have
         * one {@link CameraEffect} with target {@link CameraEffect#PREVIEW} and another
         * {@link CameraEffect} with target {@link CameraEffect#PREVIEW} |
         * {@link CameraEffect#VIDEO_CAPTURE}, since they both target {@link Preview}.
         *
         * <p>Once added, CameraX will use the {@link CameraEffect}s to process the outputs of
         * the {@link UseCase}s.
         */
        public @NonNull Builder addEffect(@NonNull CameraEffect cameraEffect) {
            mEffects.add(cameraEffect);
            return this;
        }

        /**
         * Checks effect targets and throw {@link IllegalArgumentException}.
         *
         * <p>Throws exception if the effects 1) contains conflicting targets or 2) contains
         * effects that is not in the allowlist.
         */
        private void checkEffectTargets() {
            int existingTargets = 0;
            for (CameraEffect effect : mEffects) {
                int targets = effect.getTargets();
                checkSupportedTargets(SUPPORTED_TARGETS, targets);
                int overlappingTargets = existingTargets & targets;
                if (overlappingTargets > 0) {
                    throw new IllegalArgumentException(String.format(Locale.US,
                            "More than one effects has targets %s.",
                            getHumanReadableName(overlappingTargets)));
                }
                existingTargets |= targets;
            }
        }


        /**
         * Adds {@link UseCase} to the collection.
         */
        public @NonNull Builder addUseCase(@NonNull UseCase useCase) {
            mUseCases.add(useCase);
            return this;
        }

        /**
         * Builds a {@link UseCaseGroup} from the current state.
         */
        public @NonNull UseCaseGroup build() {
            checkArgument(!mUseCases.isEmpty(), "UseCase must not be empty.");
            checkEffectTargets();
            return new UseCaseGroup(mViewPort, mUseCases, mEffects);
        }
    }
}
