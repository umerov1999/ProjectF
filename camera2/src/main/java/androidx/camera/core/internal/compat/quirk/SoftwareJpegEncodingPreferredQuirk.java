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

package androidx.camera.core.internal.compat.quirk;


import androidx.camera.core.impl.Quirk;

/**
 * A Quirk interface which denotes CameraX should prefer producing JPEGs itself from other
 * formats rather than the camera producing JPEGs directly.
 *
 * <p>Subclasses of this quirk may prefer CameraX produces JPEGs itself (likely from a YUV
 * format) for compatibility or performance reasons.
 */
public interface SoftwareJpegEncodingPreferredQuirk extends Quirk {
}
