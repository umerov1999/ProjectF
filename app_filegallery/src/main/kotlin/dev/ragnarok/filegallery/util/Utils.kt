package dev.ragnarok.filegallery.util

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.media3.common.MediaItem
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.filegallery.BuildConfig
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.media.exo.OkHttpDataSource
import dev.ragnarok.filegallery.model.Lang
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.util.AppTextUtils.updateDateLang
import dev.ragnarok.filegallery.view.natives.animation.ThorVGLottieView
import dev.ragnarok.filegallery.view.pager.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.Closeable
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

object Utils {
    private val registeredParcels: MutableSet<Long> = HashSet()
    var isCompressIncomingTraffic = true
    var currentParser = 0
    private val displaySize = Point()
    var density = 1f
        private set

    var shouldSelectPhoto = false
    val listSelected: ArrayList<String> = ArrayList()

    fun registerParcelNative(pointer: Long) {
        registeredParcels.add(pointer)
    }

    fun unregisterParcelNative(pointer: Long) {
        registeredParcels.remove(pointer)
    }

    fun isParcelNativeRegistered(pointer: Long): Boolean {
        return registeredParcels.contains(pointer)
    }

    fun stringEmptyIfNull(orig: String?): String {
        return orig ?: ""
    }

    fun getCauseIfRuntime(throwable: Throwable): Throwable {
        var target = throwable
        while (target is RuntimeException) {
            if (target.cause == null) {
                break
            }
            target.cause?.let {
                target = it
            }
        }
        return target
    }

    fun is600dp(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.is_tablet)
    }

    fun hasScopedStorage(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.R
    }

    fun hasTiramisuTarget(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.TIRAMISU
    }

    fun hasVanillaIceCreamTarget(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }

    fun finishActivityImmediate(activity: Activity) {
        activity.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("deprecation")
            activity.overridePendingTransition(0, 0)
        } else {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
    }

    fun firstNonEmptyString(vararg array: String?): String? {
        for (s in array) {
            if (!s.isNullOrEmpty()) {
                return s
            }
        }
        return null
    }

    fun getAppVersionName(context: Context): String? {
        return try {
            val packageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ) else context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    @SafeVarargs
    fun <T> firstNonNull(vararg items: T): T? {
        for (t in items) {
            if (t != null) {
                return t
            }
        }
        return null
    }

    fun safelyClose(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (_: IOException) {
            }
        }
    }

    fun safeCountOf(collection: Collection<*>?): Int {
        return collection?.size ?: 0
    }

    /**
     * Добавляет прозрачность к цвету
     *
     * @param color  цвет
     * @param factor степень прозрачности
     * @return прозрачный цвет
     */
    fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                capitalize(model)
            } else {
                capitalize(manufacturer) + " " + model
            }
        }

    private fun capitalize(s: String?): String {
        if (s.isNullOrEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first).toString() + s.substring(1)
        }
    }

    fun setTint(view: ImageView?, @ColorInt color: Int) {
        view?.imageTintList = ColorStateList.valueOf(color)
    }

    fun setBackgroundTint(view: View?, @ColorInt color: Int) {
        view?.backgroundTintList = ColorStateList.valueOf(color)
    }

    fun setColorFilter(view: ImageView?, @ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        } else {
            view?.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        }
    }

    fun setColorFilter(view: FloatingActionButton?, @ColorInt color: Int) {
        if (view == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        } else {
            view.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        }
    }

    fun doWavesLottie(visual: ThorVGLottieView?, Play: Boolean) {
        if (Play) {
            visual?.setRepeat(true)
            visual?.fromRes(R.raw.waves)
        } else {
            visual?.setRepeat(false)
            visual?.fromRes(R.raw.waves_end)
        }
        visual?.startAnimation()
    }

    fun doWavesLottieBig(visual: ThorVGLottieView?, Play: Boolean) {
        if (Play) {
            visual?.setRepeat(true)
            visual?.fromRes(R.raw.s_waves)
        } else {
            visual?.setRepeat(false)
            visual?.fromRes(R.raw.s_waves_end)
        }
        visual?.startAnimation()
    }

    fun isColorDark(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    fun dp(value: Float): Int {
        return if (value == 0f) {
            0
        } else ceil((density * value).toDouble())
            .toInt()
    }

    fun dpf2(value: Float): Float {
        return if (value == 0f) {
            0f
        } else density * value
    }

    fun prepareDensity(context: Context) {
        density = context.resources.displayMetrics.density
        var display: Display? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display = context.display
        } else {
            val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            if (manager != null) {
                @Suppress("deprecation")
                display = manager.defaultDisplay
            }
        }
        if (display != null) {
            val configuration = context.resources.configuration
            if (configuration.screenWidthDp != Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
                val newSize = ceil((configuration.screenWidthDp * density).toDouble())
                    .toInt()
                if (abs(displaySize.x - newSize) > 3) {
                    displaySize.x = newSize
                }
            }
            if (configuration.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
                val newSize = ceil((configuration.screenHeightDp * density).toDouble())
                    .toInt()
                if (abs(displaySize.y - newSize) > 3) {
                    displaySize.y = newSize
                }
            }
        }
    }

    fun clamp(value: Float, min: Float, max: Float): Float {
        if (value > max) {
            return max
        } else if (value < min) {
            return min
        }
        return value
    }

    fun createOkHttp(timeouts: Long): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(timeouts, TimeUnit.SECONDS)
            .readTimeout(timeouts, TimeUnit.SECONDS)
            .writeTimeout(timeouts, TimeUnit.SECONDS)
            .callTimeout(timeouts, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                chain.proceed(
                    chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT)
                        .build()
                )
            })
    }

    fun checkValues(values: Collection<Boolean?>): Boolean {
        for (i in values) {
            if (i != true) {
                return false
            }
        }
        return true
    }

    fun makeMutablePendingIntent(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.S) {
            if (flags == 0) {
                PendingIntent.FLAG_MUTABLE
            } else {
                flags or PendingIntent.FLAG_MUTABLE
            }
        } else flags
    }

    fun makeImmutablePendingIntent(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.S) {
            if (flags == 0) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                flags or PendingIntent.FLAG_IMMUTABLE
            }
        } else flags
    }

    fun BytesToSize(Bytes: Long): String {
        val tb = 1099511627776L
        val gb: Long = 1073741824
        val mb: Long = 1048576
        val kb: Long = 1024
        return when {
            Bytes >= tb -> String.format(
                appLocale,
                "%.2f TB",
                Bytes.toDouble() / tb
            )

            Bytes >= gb -> String.format(
                appLocale,
                "%.2f GB",
                Bytes.toDouble() / gb
            )

            Bytes >= mb -> String.format(
                appLocale,
                "%.2f MB",
                Bytes.toDouble() / mb
            )

            Bytes >= kb -> String.format(
                appLocale,
                "%.2f KB",
                Bytes.toDouble() / kb
            )

            else -> String.format(appLocale, "%d Bytes", Bytes)
        }
    }

    private fun getLocaleSettings(@Lang lang: Int): Locale {
        when (lang) {
            Lang.ENGLISH -> {
                return Locale.ENGLISH
            }

            Lang.RUSSIA -> {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    Locale.of("ru", "RU")
                } else {
                    @Suppress("deprecation")
                    Locale("ru", "RU")
                }
            }

            Lang.DEFAULT -> {}
        }
        return Locale.getDefault()
    }

    private fun setSystemLocaleLegacy(config: Configuration, locale: Locale) {
        config.setLocale(locale)
    }

    val appLocale: Locale
        get() = getLocaleSettings(get().main().language)

    fun updateActivityContext(base: Context): Context {
        val size = get().main().fontSize
        @Lang val lang = get().main().language
        val locale = getLocaleSettings(lang)
        updateDateLang(locale)
        return if (size == 0) {
            if (lang == Lang.DEFAULT) {
                base
            } else {
                val res = base.resources
                val config = Configuration(res.configuration)
                setSystemLocaleLegacy(config, getLocaleSettings(lang))
                base.createConfigurationContext(config)
            }
        } else {
            val res = base.resources
            val config = Configuration(res.configuration)
            config.fontScale = res.configuration.fontScale + 0.05f * size
            if (lang != Lang.DEFAULT) {
                setSystemLocaleLegacy(config, getLocaleSettings(lang))
            }
            base.createConfigurationContext(config)
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getExoPlayerFactory(userAgent: String?): OkHttpDataSource.Factory {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(Constants.EXO_PLAYER_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(Constants.EXO_PLAYER_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.EXO_PLAYER_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(Constants.EXO_PLAYER_TIMEOUT, TimeUnit.SECONDS)
        return OkHttpDataSource.Factory(builder.build()).setUserAgent(userAgent)
    }

    fun makeMediaItem(url: String?): MediaItem {
        return MediaItem.Builder().setUri(url).build()
    }

    fun createPageTransform(@Transformers_Types type: Int): ViewPager2.PageTransformer? {
        return when (type) {
            Transformers_Types.SLIDER_TRANSFORMER -> SliderTransformer(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT)
            Transformers_Types.CLOCK_SPIN_TRANSFORMER -> ClockSpinTransformer()
            Transformers_Types.BACKGROUND_TO_FOREGROUND_TRANSFORMER -> BackgroundToForegroundTransformer()
            Transformers_Types.CUBE_IN_DEPTH_TRANSFORMER -> CubeInDepthTransformer()
            Transformers_Types.DEPTH_TRANSFORMER -> DepthTransformer()
            Transformers_Types.FAN_TRANSFORMER -> FanTransformer()
            Transformers_Types.GATE_TRANSFORMER -> GateTransformer()
            Transformers_Types.OFF -> null
            Transformers_Types.ZOOM_OUT_TRANSFORMER -> ZoomOutTransformer()
            else -> null
        }
    }
}
