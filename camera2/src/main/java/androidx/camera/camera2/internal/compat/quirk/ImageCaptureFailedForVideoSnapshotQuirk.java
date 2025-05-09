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

package androidx.camera.camera2.internal.compat.quirk;

import static java.util.Arrays.asList;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.camera.core.impl.Quirk;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: b/344704367, b/349542870, b/359062845
 *     Description: When taking pictures with {@link CameraDevice#TEMPLATE_VIDEO_SNAPSHOT}, there
 *                  is no response from camera HAL. On itel l6006, itel w6004, moto g(20), moto
 *                  e13, moto e20, rmx3231, rmx3511, sm-a032f, sm-a035m, it happens when there
 *                  are only two surfaces (JPEG + ANY) are configured to camera capture session.
 *                  On tecno mobile bf6, it fails when there is no GraphicBufferSource (ex: when
 *                  OpenGL pipeline is used, the Surface is from SurfaceTexture) no matter how
 *                  many surfaces are configured to camera capture session. All the above devices
 *                  adopt UniSoc chipset. The workaround is to use
 *                  {@link CaptureRequest#CONTROL_CAPTURE_INTENT_STILL_CAPTURE} instead of
 *                  {@link CaptureRequest#CONTROL_CAPTURE_INTENT_VIDEO_SNAPSHOT} on UniSoc
 *                  chipset devices.
 *                  On the Huawei P Smart (b/349542870) and Samsung sm-f946u1 (b/359062845), taking
 *                  pictures consistently fails when using CONTROL_CAPTURE_INTENT_VIDEO_SNAPSHOT,
 *                  regardless of the surface combinations or capture intent specified in repeated
 *                  request.
 *     Device(s): itel l6006, itel w6004, moto g(20), moto e13, moto e20, rmx3231, rmx3511,
 *                sm-a032f, sm-a035m, sm-f946u1, tecno mobile bf6, Huawei P Smart.
 */
public class ImageCaptureFailedForVideoSnapshotQuirk implements Quirk {

    private static final Set<String> PROBLEMATIC_UNI_SOC_MODELS = new HashSet<>(asList(
            "itel l6006",
            "itel w6004",
            "moto g(20)",
            "moto e13",
            "moto e20",
            "rmx3231",
            "rmx3511",
            "sm-a032f",
            "sm-a035m",
            "sm-f946u1",
            "tecno mobile bf6"
    ));

    static boolean load() {
        return isUniSocChipsetDevice() || isHuaweiPSmart();
    }

    private static boolean isUniSocChipsetDevice() {
        // There is no clear way to determine whether a device is UniSoc or not. In addition to
        // known devices, possible properties are checked. See b/344704367#comment2 for details.
        return PROBLEMATIC_UNI_SOC_MODELS.contains(Build.MODEL.toLowerCase(Locale.US))
                || (Build.VERSION.SDK_INT >= 31 && "Spreadtrum".equalsIgnoreCase(
                Build.SOC_MANUFACTURER))
                || Build.HARDWARE.toLowerCase(Locale.US).startsWith("ums")
                || ("itel".equalsIgnoreCase(Build.BRAND) && Build.HARDWARE.toLowerCase(
                Locale.US).startsWith("sp"));
    }

    private static boolean isHuaweiPSmart() {
        return "HUAWEI".equalsIgnoreCase(Build.BRAND) && "FIG-LX1".equalsIgnoreCase(Build.MODEL);
    }
}
