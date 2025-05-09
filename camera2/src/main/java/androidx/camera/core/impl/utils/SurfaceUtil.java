/*
 * Copyright 2024 The Android Open Source Project
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

import android.graphics.ImageFormat;
import android.view.Surface;

import org.jspecify.annotations.NonNull;

public class SurfaceUtil {
    private static SurfaceUtil_JNI jcall_util;
    public static void setSurfaceUtil(SurfaceUtil_JNI pjcall_util) {
        jcall_util = pjcall_util;
    }
    private SurfaceUtil() {
    }

    /**
     * A class to store surface related information.
     */
    public static class SurfaceInfo {
        /**
         * The surface format.
         */
        public int format = ImageFormat.UNKNOWN;
        /**
         * The surface width;
         */
        public int width = 0;
        /**
         * The surface height.
         */
        public int height = 0;
    }

    /**
     * Returns the surface pixel format.
     */
    public static @NonNull SurfaceInfo getSurfaceInfo(@NonNull Surface surface) {
        int[] surfaceInfoArray = jcall_util.nativeGetSurfaceInfo(surface);
        SurfaceInfo surfaceInfo = new SurfaceInfo();
        surfaceInfo.format = surfaceInfoArray[0];
        surfaceInfo.width = surfaceInfoArray[1];
        surfaceInfo.height = surfaceInfoArray[2];
        return surfaceInfo;
    }
}
