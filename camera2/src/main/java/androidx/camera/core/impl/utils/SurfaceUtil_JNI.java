package androidx.camera.core.impl.utils;

import android.view.Surface;

import org.jspecify.annotations.Nullable;

public interface SurfaceUtil_JNI {
    int[] nativeGetSurfaceInfo(@Nullable Surface surface);
}