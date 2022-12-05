package dev.ragnarok.filegallery

import android.content.Context
import dev.ragnarok.filegallery.App.Companion.instance
import dev.ragnarok.filegallery.api.impl.Networker
import dev.ragnarok.filegallery.api.interfaces.INetworker
import dev.ragnarok.filegallery.db.impl.AppStorages
import dev.ragnarok.filegallery.db.interfaces.IStorages
import dev.ragnarok.filegallery.settings.ISettings
import dev.ragnarok.filegallery.settings.SettingsImpl
import dev.ragnarok.filegallery.upload.IUploadManager
import dev.ragnarok.filegallery.upload.UploadManagerImpl
import dev.ragnarok.filegallery.util.rxutils.io.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler

object Includes {
    val networkInterfaces: INetworker by lazy {
        Networker(settings.main())
    }

    val stores: IStorages by lazy {
        AppStorages(instance)
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

    val uploadManager: IUploadManager by lazy {
        UploadManagerImpl(
            instance, networkInterfaces
        )
    }
}
