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

package androidx.camera.video.internal.compat.quirk;

import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * Quirk requiring that the frame rate is not set on the MediaFormat during codec selection.
 *
 * <p>QuirkSummary
 *      Bug Id:      174630237
 *      Description: According to the documentation for
 *                   {@link android.media.MediaCodecList#findEncoderForFormat(MediaFormat)}, on
 *                   devices exhibiting this quirk, the {@link MediaFormat} argument must not
 *                   contain {@link MediaFormat#KEY_FRAME_RATE}, so special care must be taken to
 *                   remove this key before using the API to find the codec, but the frame rate
 *                   key may still be required to configure the codec correctly.
 *      Device(s):   All devices running Lollipop (API 21)
 *
 * @see <a href="https://developer.android.com/reference/android/media/MediaCodec#creation">
 *     MediaCodec Creation</a>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MediaFormatMustNotUseFrameRateToFindEncoderQuirk implements Quirk {

    static boolean load() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP;
    }
}
