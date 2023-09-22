package dev.ragnarok.fenrir

import android.content.Context
import dev.ragnarok.fenrir.App.Companion.instance
import dev.ragnarok.fenrir.api.CaptchaProvider
import dev.ragnarok.fenrir.api.ICaptchaProvider
import dev.ragnarok.fenrir.api.IValidateProvider
import dev.ragnarok.fenrir.api.ValidateProvider
import dev.ragnarok.fenrir.api.impl.Networker
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.db.impl.AppStorages
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.IBlacklistRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.domain.impl.AttachmentsRepository
import dev.ragnarok.fenrir.domain.impl.BlacklistRepository
import dev.ragnarok.fenrir.media.story.AppStoryPlayerFactory
import dev.ragnarok.fenrir.media.story.IStoryPlayerFactory
import dev.ragnarok.fenrir.media.voice.IVoicePlayerFactory
import dev.ragnarok.fenrir.media.voice.VoicePlayerFactory
import dev.ragnarok.fenrir.push.IDeviceIdProvider
import dev.ragnarok.fenrir.push.IPushRegistrationResolver
import dev.ragnarok.fenrir.push.PushRegistrationResolver
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.ISettings
import dev.ragnarok.fenrir.settings.ProxySettingsImpl
import dev.ragnarok.fenrir.settings.SettingsImpl
import dev.ragnarok.fenrir.upload.IUploadManager
import dev.ragnarok.fenrir.upload.UploadManagerImpl
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.io.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler

object Includes {
    val proxySettings: IProxySettings by lazy {
        ProxySettingsImpl(provideApplicationContext())
    }

    val storyPlayerFactory: IStoryPlayerFactory by lazy {
        AppStoryPlayerFactory(proxySettings)
    }

    val voicePlayerFactory: IVoicePlayerFactory by lazy {
        VoicePlayerFactory(
            provideApplicationContext(),
            proxySettings,
            settings.main()
        )
    }

    val pushRegistrationResolver: IPushRegistrationResolver by lazy {
        PushRegistrationResolver(
            object : IDeviceIdProvider {
                override val deviceId: String
                    get() = Utils.getDeviceId(provideApplicationContext())
            },
            settings,
            networkInterfaces
        )
    }


    val uploadManager: IUploadManager by lazy {
        UploadManagerImpl(
            instance, networkInterfaces,
            stores, attachmentsRepository, walls
        )
    }

    val captchaProvider: ICaptchaProvider by lazy {
        CaptchaProvider(provideApplicationContext(), provideMainThreadScheduler())
    }

    val validationProvider: IValidateProvider by lazy {
        ValidateProvider(provideApplicationContext())
    }


    val attachmentsRepository: IAttachmentsRepository by lazy {
        AttachmentsRepository(stores.attachments(), owners)
    }

    val networkInterfaces: INetworker by lazy {
        Networker(proxySettings)
    }

    val stores: IStorages by lazy {
        AppStorages(instance)
    }

    val blacklistRepository: IBlacklistRepository by lazy {
        BlacklistRepository()
    }

    val settings: ISettings by lazy {
        SettingsImpl(instance)
    }


    fun provideMainThreadScheduler(): Scheduler {
        return AndroidSchedulers.mainThread()
    }

    fun provideApplicationContext(): Context {
        return instance
    }
}