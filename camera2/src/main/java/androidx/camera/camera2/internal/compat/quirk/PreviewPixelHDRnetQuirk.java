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

package androidx.camera.camera2.internal.compat.quirk;

import android.hardware.camera2.CameraDevice;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * <p>QuirkSummary
 *     Bug Id: 170598016
 *     Description: Quirk denotes the devices to turn on WYSIWYG viewfinder. The default
 *                  setting of the {@link CameraDevice#TEMPLATE_STILL_CAPTURE} enables the HDR+
 *                  for the still image capture request on Pixel phones, it leads to a better still
 *                  image quality than the viewfinder output. To align the viewfinder quality
 *                  with the final photo, we need to set TONEMAP_MODE to HIGH_QUALITY (the
 *                  default is FAST) on the viewfinder stream to enable the WYSIWYG preview.
 *                  This quirk will not be applied when preview resolution is 16:9, more details in
 *                  b/266459202.
 *     Device(s): Pixel 4a, Pixel 4a (5G), Pixel 5, Pixel 5a (5G)
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class PreviewPixelHDRnetQuirk implements Quirk {

    /** The devices that support wysiwyg preview in 3rd party apps (go/p20-wysiwyg-hdr) */
    private static final List<String> SUPPORTED_DEVICES =
            Arrays.asList("sunfish", "bramble", "redfin", "barbet");

    static boolean load() {
        return "Google".equals(Build.MANUFACTURER) && SUPPORTED_DEVICES.contains(
                Build.DEVICE.toLowerCase(Locale.getDefault()));
    }
}
