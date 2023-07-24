package dev.ragnarok.filegallery

import android.app.Application
import android.os.Handler
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.ImageProcessingUtil
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.filegallery.activity.crash.CrashUtils
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.picasso.PicassoInstance
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.util.Camera2ImageProcessingUtil
import dev.ragnarok.filegallery.util.ErrorLocalizer
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.existfile.FileExistJVM
import dev.ragnarok.filegallery.util.existfile.FileExistNative
import dev.ragnarok.filegallery.util.toast.CustomToast.Companion.createCustomToast
import io.reactivex.rxjava3.plugins.RxJavaPlugins

class App : Application() {
    override fun onCreate() {
        sInstanse = this

        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Settings.get().main().nightMode)
        if (Settings.get().main().isDeveloper_mode) {
            CrashUtils.install(this)
        }

        FenrirNative.loadNativeLibrary(object : FenrirNative.NativeOnException {
            override fun onException(e: Error) {
                e.printStackTrace()
            }
        })
        FenrirNative.updateAppContext(this)
        FenrirNative.updateDensity(object : FenrirNative.OnGetDensity {
            override fun get(): Float {
                return Utils.density
            }
        })

        if (FenrirNative.isNativeLoaded) {
            MusicPlaybackController.tracksExist = FileExistNative()
            ImageProcessingUtil.setProcessingUtil(Camera2ImageProcessingUtil)
        } else {
            MusicPlaybackController.tracksExist = FileExistJVM()
        }
        Utils.isCompressIncomingTraffic = Settings.get().main().isCompress_incoming_traffic
        Utils.currentParser = Settings.get().main().currentParser
        PicassoInstance.init(this)
        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            Handler(mainLooper).post {
                if (Settings.get().main().isDeveloper_mode) {
                    createCustomToast(this, null)?.showToastError(
                        ErrorLocalizer.localizeThrowable(
                            this,
                            it
                        )
                    )
                }
            }
        }
    }

    companion object {
        @Volatile
        private var sInstanse: App? = null

        val instance: App
            get() {
                checkNotNull(sInstanse) { "App instance is null!!! WTF???" }
                return sInstanse!!
            }
    }
}
