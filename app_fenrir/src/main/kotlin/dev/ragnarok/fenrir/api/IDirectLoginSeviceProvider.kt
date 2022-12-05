package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.api.services.IAuthService
import io.reactivex.rxjava3.core.Single

interface IDirectLoginSeviceProvider {
    fun provideAuthService(): Single<IAuthService>
}