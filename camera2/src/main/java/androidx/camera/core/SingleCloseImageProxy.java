/*
 * Copyright (C) 2019 The Android Open Source Project
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

import androidx.annotation.RequiresApi;

import java.util.concurrent.atomic.AtomicBoolean;

/** A {@link ImageProxy} which filters out redundant calls to {@link #close()}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class SingleCloseImageProxy extends ForwardingImageProxy {
    private final AtomicBoolean mClosed = new AtomicBoolean(false);

    /**
     * Creates a new instances which wraps the given image.
     *
     * @param image to wrap
     */
    SingleCloseImageProxy(ImageProxy image) {
        super(image);
    }

    @Override
    public void close() {
        if (!mClosed.getAndSet(true)) {
            super.close();
        }
    }
}
