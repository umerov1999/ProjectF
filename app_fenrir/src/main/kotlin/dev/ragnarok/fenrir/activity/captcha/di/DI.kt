package dev.ragnarok.fenrir.activity.captcha.di

import android.content.Context
import android.os.Looper
import dev.ragnarok.fenrir.activity.captcha.sensors.SensorsDataRepository
import dev.ragnarok.fenrir.activity.captcha.sensors.SensorsDataRepositoryImpl
import dev.ragnarok.fenrir.activity.captcha.web.NetworkConnectionObserver

internal class DI(private val applicationContext: Context) {

    internal val sensorsDataRepository: SensorsDataRepository by lazy {
        SensorsDataRepositoryImpl.create(applicationContext)
    }

    internal val networkConnectionObserver: NetworkConnectionObserver by lazy {
        NetworkConnectionObserver(applicationContext)
    }

    internal val mainLooper = Looper.getMainLooper()

    companion object {

        @Volatile
        private var _di: DI? = null

        val di: DI
            get() = _di ?: throw IllegalStateException("DI is not initialized!")

        fun init(applicationContext: Context) {
            if (_di == null) {
                synchronized(DI::class.java) {
                    if (_di == null) {
                        _di = DI(applicationContext)
                    }
                }
            }
        }
    }
}