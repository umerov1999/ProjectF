package dev.ragnarok.fenrir

import android.app.Application
import android.os.Handler
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.ImageProcessingUtil
import dev.ragnarok.fenrir.activity.crash.CrashUtils
import dev.ragnarok.fenrir.domain.Repository.messages
import dev.ragnarok.fenrir.longpoll.NotificationHelper
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.service.ErrorLocalizer
import dev.ragnarok.fenrir.service.KeepLongpollService
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.PersistentLogger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.existfile.FileExistJVM
import dev.ragnarok.fenrir.util.existfile.FileExistNative
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins

class App : Application() {
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate() {
        sInstanse = this
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(Settings.get().ui().nightMode)
        if (Settings.get().other().isDeveloper_mode) {
            CrashUtils.install(this)
        }

        if (Settings.get().other().isEnable_native) {
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
        } else {
            MusicPlaybackController.tracksExist = FileExistJVM()
        }

        Utils.isCompressIncomingTraffic = Settings.get().other().isCompress_incoming_traffic
        Utils.isCompressOutgoingTraffic = Settings.get().other().isCompress_outgoing_traffic
        Utils.currentParser = Settings.get().other().currentParser
        MusicPlaybackController.registerBroadcast(this)
        PicassoInstance.init(this, Includes.proxySettings)
        if (Settings.get().other().isKeepLongpoll) {
            KeepLongpollService.start(this)
        }
        compositeDisposable.add(messages
            .observePeerUpdates()
            .flatMap { source -> Flowable.fromIterable(source) }
            .subscribe({ update ->
                if (update.readIn != null) {
                    NotificationHelper.tryCancelNotificationForPeer(
                        this,
                        update.accountId,
                        update.peerId
                    )
                }
            }, RxUtils.ignore())
        )
        compositeDisposable.add(
            messages
                .observeSentMessages()
                .subscribe({ sentMsg ->
                    NotificationHelper.tryCancelNotificationForPeer(
                        this,
                        sentMsg.accountId,
                        sentMsg.peerId
                    )
                }, RxUtils.ignore())
        )
        compositeDisposable.add(
            messages
                .observeMessagesSendErrors()
                .toMainThread()
                .subscribe({ throwable ->
                    run {
                        createCustomToast(this).showToastError(
                            ErrorLocalizer.localizeThrowable(this, throwable)
                        ); throwable.printStackTrace()
                    }
                }, RxUtils.ignore())
        )
        RxJavaPlugins.setErrorHandler {
            it.printStackTrace()
            Handler(mainLooper).post {
                if (Settings.get().other().isDeveloper_mode) {
                    createCustomToast(this).showToastError(
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
                sInstanse?.let {
                    return it
                } ?: throw IllegalStateException("App instance is null!!! WTF???")
            }
    }
}
