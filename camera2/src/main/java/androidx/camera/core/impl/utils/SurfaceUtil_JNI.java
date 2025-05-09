package androidx.camera.core.impl.utils;

import android.graphics.Bitmap;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.ImageOutputConfig;

import java.nio.ByteBuffer;

public interface SurfaceUtil_JNI {
    int[] nativeGetSurfaceInfo(@Nullable Surface surface);
}