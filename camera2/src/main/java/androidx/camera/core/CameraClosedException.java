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

package androidx.camera.core;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/** This exception is thrown when request is cancelled due to that camera is closed */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class CameraClosedException extends RuntimeException {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    CameraClosedException(String s, Throwable e) {
        super(s, e);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    CameraClosedException(String s) {
        super(s);
    }
}
