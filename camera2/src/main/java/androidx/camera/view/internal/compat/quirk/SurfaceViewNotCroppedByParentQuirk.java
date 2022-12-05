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

package androidx.camera.view.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * A quirk where a scaled up SurfaceView is not cropped by the parent View.
 *
 * <p>QuirkSummary
 *     Bug Id: 211370840
 *     Description: On certain Xiaomi devices, when the scale type is FILL_* and the preview is
 *                  scaled up to be larger than its parent, the SurfaceView is not cropped by its
 *                  parent. As the result, the preview incorrectly covers the neighboring UI
 *                  elements.
 *     Device(s): XIAOMI M2101K7AG
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SurfaceViewNotCroppedByParentQuirk implements Quirk {

    private static final String XIAOMI = "XIAOMI";
    private static final String RED_MI_NOTE_10_MODEL = "M2101K7AG";

    static boolean load() {
        return XIAOMI.equalsIgnoreCase(Build.MANUFACTURER)
                && RED_MI_NOTE_10_MODEL.equalsIgnoreCase(Build.MODEL);
    }

}
