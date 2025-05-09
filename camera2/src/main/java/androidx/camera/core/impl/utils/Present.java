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

package androidx.camera.core.impl.utils;

import androidx.core.util.Preconditions;
import androidx.core.util.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of an {@link Optional} containing a reference.
 *
 * <p>Copied and adapted from Guava.
 */
final class Present<T> extends Optional<T> {
    private final T mReference;

    Present(T reference) {
        mReference = reference;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public @NonNull T get() {
        return mReference;
    }

    @Override
    public @NonNull T or(@NonNull T defaultValue) {
        Preconditions.checkNotNull(defaultValue,
                "use Optional.orNull() instead of Optional.or(null)");
        return mReference;
    }

    @Override
    public @NonNull Optional<T> or(@NonNull Optional<? extends T> secondChoice) {
        Preconditions.checkNotNull(secondChoice);
        return this;
    }

    @Override
    public @NonNull T or(@NonNull Supplier<? extends T> supplier) {
        Preconditions.checkNotNull(supplier);
        return mReference;
    }

    @Override
    public T orNull() {
        return mReference;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof Present) {
            Present<?> other = (Present<?>) object;
            return mReference.equals(other.mReference);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0x598df91c + mReference.hashCode();
    }

    @Override
    public @NonNull String toString() {
        return "Optional.of(" + mReference + ")";
    }

    private static final long serialVersionUID = 0;
}
