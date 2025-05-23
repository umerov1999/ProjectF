/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.video.internal.encoder;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Executor;

/** Factory to create {@link Encoder} */
public interface EncoderFactory {

    /** Factory method to create {@link Encoder}. */
    @NonNull Encoder createEncoder(@NonNull Executor executor, @NonNull EncoderConfig encoderConfig,
            int sessionType) throws InvalidConfigException;
}
