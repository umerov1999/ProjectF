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

package androidx.camera.camera2.internal.compat.workaround;

import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.quirk.CrashWhenTakingPhotoWithAutoFlashAEModeQuirk;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.ImageCaptureFailWithAutoFlashQuirk;
import androidx.camera.core.impl.Quirks;

/**
 * A workaround to turn off the auto flash AE mode if device has the
 * {@link CrashWhenTakingPhotoWithAutoFlashAEModeQuirk} or
 * {@link ImageCaptureFailWithAutoFlashQuirk}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AutoFlashAEModeDisabler {

    private final boolean mIsImageCaptureFailWithAutoFlashQuirkEnabled;
    private final boolean mIsCrashWhenTakingPhotoWithAutoFlashAEModeQuirkEnabled;


    public AutoFlashAEModeDisabler(@NonNull final Quirks quirks) {
        mIsImageCaptureFailWithAutoFlashQuirkEnabled =
                quirks.contains(ImageCaptureFailWithAutoFlashQuirk.class);
        mIsCrashWhenTakingPhotoWithAutoFlashAEModeQuirkEnabled = DeviceQuirks.get(
                CrashWhenTakingPhotoWithAutoFlashAEModeQuirk.class) != null;
    }

    /**
     * Get AE mode corrected by the {@link CrashWhenTakingPhotoWithAutoFlashAEModeQuirk} and
     * {@link ImageCaptureFailWithAutoFlashQuirk}.
     */
    public int getCorrectedAeMode(int aeMode) {
        if ((mIsImageCaptureFailWithAutoFlashQuirkEnabled
                || mIsCrashWhenTakingPhotoWithAutoFlashAEModeQuirkEnabled)
                && aeMode == CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH) {
            return CaptureRequest.CONTROL_AE_MODE_ON;
        }
        return aeMode;
    }
}
