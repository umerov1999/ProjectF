package dev.ragnarok.filegallery.picasso

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.StatFs
import androidx.core.content.ContextCompat
import com.squareup.picasso3.BitmapSafeResize
import com.squareup.picasso3.Picasso
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.util.CoverSafeResize
import dev.ragnarok.filegallery.util.Logger
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File

class PicassoInstance @SuppressLint("CheckResult") private constructor(
    private val app: Context
) {
    private var cache_data: Cache? = null

    @Volatile
    private var singleton: Picasso? = null

    private fun getSingleton(): Picasso {
        if (singleton == null) {
            synchronized(this) {
                if (singleton == null) {
                    singleton = create()
                }
            }
        }
        return singleton!!
    }

    private fun getCache_data() {
        if (cache_data == null) {
            val cache = File(app.cacheDir, "picasso-cache")
            if (!cache.exists()) {
                cache.mkdirs()
            }
            cache_data = Cache(cache, calculateDiskCacheSize(cache))
        }
    }

    private fun create(): Picasso {
        Logger.d(TAG, "Picasso singleton creation")
        getCache_data()
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .cache(cache_data).addNetworkInterceptor(Interceptor { chain: Interceptor.Chain ->
                chain.proceed(chain.request()).newBuilder()
                    .header("Cache-Control", "max-age=86400").build()
            })
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", Constants.USER_AGENT).build()
                chain.proceed(request)
            })
        BitmapSafeResize.setMaxResolution(Settings.get().main().getMaxBitmapResolution())
        BitmapSafeResize.setHardwareRendering(Settings.get().main().getRendering_mode())
        CoverSafeResize.setMaxResolution(Settings.get().main().getMaxThumbResolution())
        return Picasso.Builder(app)
            .defaultBitmapConfig(Bitmap.Config.ARGB_8888)
            .client(builder.build())
            .withCacheSize(calculateMemoryCacheSize(app))
            .addRequestHandler(PicassoFileManagerHandler(app))
            .build()
    }

    fun clear_cache() {
        synchronized(this) {
            if (singleton != null) {
                singleton?.shutdown()
                singleton = null
                cache_data?.flush()
            }
            Logger.d(TAG, "Picasso singleton shutdown")
            instance?.getCache_data()
            instance?.cache_data?.delete()
            instance?.cache_data = null
        }
    }

    companion object {
        private val TAG = PicassoInstance::class.java.simpleName

        @SuppressLint("StaticFieldLeak")
        private var instance: PicassoInstance? = null

        fun init(context: Context) {
            instance = PicassoInstance(context.applicationContext)
        }

        fun with(): Picasso {
            return instance!!.getSingleton()
        }

        fun clear_cache() {
            instance?.clear_cache()
        }

        fun getCoversPath(context: Context): File {
            val cache = File(context.cacheDir, "covers-cache")
            //val cache = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "covers-cache")
            if (!cache.exists()) {
                cache.mkdirs()
            }
            return cache
        }

        // from picasso sources
        internal fun calculateDiskCacheSize(dir: File): Long {
            var size = 5242880L
            try {
                val statFs = StatFs(dir.absolutePath)
                val blockCount = statFs.blockCountLong
                val blockSize = statFs.blockSizeLong
                val available = blockCount * blockSize
                size = available / 50L
            } catch (ignored: IllegalArgumentException) {
            }
            return size.coerceAtMost(52428800L).coerceAtLeast(5242880L)
        }

        internal fun calculateMemoryCacheSize(context: Context): Int {
            val limit_cache_images = Settings.get().main().isLimitImage_cache

            val am = ContextCompat.getSystemService(context, ActivityManager::class.java)
            am ?: return (if (limit_cache_images > 2) limit_cache_images else 256) * 1024 * 1024
            val largeHeap = context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0
            val memoryClass = if (largeHeap) am.largeMemoryClass else am.memoryClass
            if (limit_cache_images > 2 && (1024L * 1024L * memoryClass / 7).toInt() > limit_cache_images * 1024 * 1024
            ) {
                return limit_cache_images * 1024 * 1024
            }
            return (1024L * 1024L * memoryClass / when (limit_cache_images) {
                0 -> 20
                1 -> 10
                else -> 7
            }).toInt()
        }
    }
}
