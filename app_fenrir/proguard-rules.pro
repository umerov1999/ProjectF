-dontobfuscate

# Disable the annoying "Parameter specified as non-null is null" exceptions
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

-keep public class * extends java.lang.Exception

# BASE APP
-keep class dev.ragnarok.fenrir.** { *; }
-keep class dev.ragnarok.filegallery.** { *; }


#Native Library
-keep class com.google.zxing.** { *; }
-keep class com.github.luben.zstd.** { *; }
-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keep class androidx.media3.decoder.opus.** { *; }

#Camera2
-keep public class androidx.camera.camera2.Camera2Config$DefaultProvider { *; }
-keep,allowobfuscation,allowshrinking class ** implements androidx.camera.core.impl.Quirk


#Firebase Installation
-keep class com.google.firebase.installations.** { *; }


#image
-keep class com.yalantis.ucrop.** { *; }
-keep class me.minetsh.imaging.** { *; }



#material
# CoordinatorLayout resolves the behaviors of its child components with reflection.
-keep public class * extends androidx.coordinatorlayout.widget.CoordinatorLayout$Behavior {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>();
}
# Make sure we keep annotations for CoordinatorLayout's DefaultBehavior
-keepattributes RuntimeVisible*Annotation*
-if class androidx.appcompat.app.AppCompatViewInflater
-keep class com.google.android.material.theme.MaterialComponentsViewInflater {
    <init>();
}


#picasso
-keep class com.squareup.picasso3.** { *; }


#preferences
-keep class de.maxr1998.modernpreferences.** { *; }



#viewpager2
-keep class androidx.recyclerview.** { *; }
-keep class androidx.viewpager2.** { *; }
