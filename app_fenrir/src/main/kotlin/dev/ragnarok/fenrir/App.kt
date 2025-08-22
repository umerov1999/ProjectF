package dev.ragnarok.fenrir

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.ImageProcessingUtil
import androidx.camera.core.impl.utils.SurfaceUtil
import dev.ragnarok.fenrir.activity.crash.CrashUtils
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.service.ErrorLocalizer
import dev.ragnarok.fenrir.service.KeepLongpollService
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Camera2ImageProcessingUtil
import dev.ragnarok.fenrir.util.Camera2SurfaceUtil
import dev.ragnarok.fenrir.util.PersistentLogger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.existfile.FileExistJVM
import dev.ragnarok.fenrir.util.existfile.FileExistNative
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat

class App : Application() {
    override fun onCreate() {
        sInstanse = this

        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(Settings.get().ui().nightMode)
        if (Settings.get().main().isDeveloper_mode) {
            CrashUtils.install(this)
        }

        if (Settings.get().main().isEnable_native) {
            FenrirNative.loadNativeLibrary(object : FenrirNative.NativeOnException {
                override fun onException(e: Error) {
                    PersistentLogger.logThrowable("NativeError", e)
                }
            })
        }
        FenrirNative.updateAppContext(this)
        FenrirNative.updateDensity(object : FenrirNative.OnGetDensity {
            override fun get(): Float {
                return Utils.density
            }
        })

        if (FenrirNative.isNativeLoaded) {
            MusicPlaybackController.tracksExist = FileExistNative()
            ImageProcessingUtil.setProcessingUtil(Camera2ImageProcessingUtil)
            SurfaceUtil.setSurfaceUtil(Camera2SurfaceUtil)
        } else {
            MusicPlaybackController.tracksExist = FileExistJVM()
        }
        Utils.isCompressIncomingTraffic = Settings.get().main().isCompress_incoming_traffic
        Utils.isCompressOutgoingTraffic = Settings.get().main().isCompress_outgoing_traffic
        Utils.currentParser = Settings.get().main().currentParser
        PicassoInstance.init(this, Includes.proxySettings)
        if (Settings.get().main().isKeepLongpoll) {
            KeepLongpollService.start(this)
        }

        messages
            .observePeerUpdates()
            .flatMapConcat {
                it.asFlow()
            }
            .sharedFlowToMain { update ->
                update.readIn?.let {
                    NotificationHelper.tryCancelNotificationForPeer(
                        this,
                        update.accountId,
                        update.peerId
                    )
                }
            }

        messages
            .observeSentMessages()
            .sharedFlowToMain {
                NotificationHelper.tryCancelNotificationForPeer(
                    this,
                    it.accountId,
                    it.peerId
                )
            }

        messages
            .observeMessagesSendErrors()
            .sharedFlowToMain {
                createCustomToast(this).showToastError(
                    ErrorLocalizer.localizeThrowable(this, it)
                )
                it.printStackTrace()
            }
    }

    companion object {
        @Volatile
        private var sInstanse: App? = null

        val instance: App
            get() {
                return sInstanse ?: throw IllegalStateException("App instance is null!!!")
            }
    }
}
