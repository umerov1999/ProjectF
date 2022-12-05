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

import static androidx.core.util.Preconditions.checkArgument;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of {@link UseCase}.
 *
 * When the {@link UseCaseGroup} is bound to {@link Lifecycle}, it binds all the
 * {@link UseCase}s to the same {@link Lifecycle}. {@link UseCase}s inside of a
 * {@link UseCaseGroup} usually share some common properties like the FOV defined by
 * {@link ViewPort}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class UseCaseGroup {
    @Nullable
    private final ViewPort mViewPort;
    @NonNull
    private final List<UseCase> mUseCases;
    @NonNull
    private final List<CameraEffect> mEffects;

    UseCaseGroup(@Nullable ViewPort viewPort, @NonNull List<UseCase> useCases,
            @NonNull List<CameraEffect> effects) {
        mViewPort = viewPort;
        mUseCases = useCases;
        mEffects = effects;
    }

    /**
     * Gets the {@link ViewPort} shared by the {@link UseCase} collection.
     */
    @Nullable
    public ViewPort getViewPort() {
        return mViewPort;
    }

    /**
     * Gets the {@link UseCase}s.
     */
    @NonNull
    public List<UseCase> getUseCases() {
        return mUseCases;
    }

    /**
     * Gets the {@link CameraEffect}s.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public List<CameraEffect> getEffects() {
        return mEffects;
    }

    /**
     * A builder for generating {@link UseCaseGroup}.
     */
    public static final class Builder {
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
        @NonNull
        public Builder setViewPort(@NonNull ViewPort viewPort) {
            mViewPort = viewPort;
            return this;
        }

        /**
         * Adds a {@link CameraEffect} to the collection
         *
         * <p>Once added, CameraX will use the {@link CameraEffect}s to process the outputs of
         * the {@link UseCase}s.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public Builder addEffect(@NonNull CameraEffect cameraEffect) {
            mEffects.add(cameraEffect);
            return this;
        }

        /**
         * Adds {@link UseCase} to the collection.
         */
        @NonNull
        public Builder addUseCase(@NonNull UseCase useCase) {
            mUseCases.add(useCase);
            return this;
        }

        /**
         * Builds a {@link UseCaseGroup} from the current state.
         */
        @NonNull
        public UseCaseGroup build() {
            checkArgument(!mUseCases.isEmpty(), "UseCase must not be empty.");
            return new UseCaseGroup(mViewPort, mUseCases, mEffects);
        }
    }
}
