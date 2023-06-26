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

package androidx.camera.video.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: b/223643510
 *     Description: Quirk indicates Preview is delayed on some Huawei devices when the Preview uses
 *                  certain resolutions and VideoCapture is bound.
 *     Device(s): Some Huawei devices.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class PreviewDelayWhenVideoCaptureIsBoundQuirk implements Quirk {

    private static final Set<String> HUAWEI_DEVICE_LIST = new HashSet<>(Arrays.asList(
            "HWELE",  // P30
            "HW-02L", // P30 Pro
            "HWVOG",  // P30 Pro
            "HWYAL",  // Nova 5T
            "HWLYA",  // Mate 20 Pro
            "HWCOL",  // Honor 10
            "HWPAR"   // Nova 3
    ));

    private static final Set<String> HUAWEI_MODEL_LIST = new HashSet<>(Arrays.asList(
            "ELS-AN00", "ELS-TN00", "ELS-NX9", "ELS-N04"  // P40 Pro
    ));

    static boolean load() {
        return "Huawei".equalsIgnoreCase(Build.MANUFACTURER)
                && (HUAWEI_DEVICE_LIST.contains(Build.DEVICE.toUpperCase(Locale.US))
                || HUAWEI_MODEL_LIST.contains(Build.MODEL.toUpperCase(Locale.US)));
    }
}
