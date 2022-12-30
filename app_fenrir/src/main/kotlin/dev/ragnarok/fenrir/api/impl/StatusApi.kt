package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IStatusApi
import dev.ragnarok.fenrir.api.services.IStatusService
import io.reactivex.rxjava3.core.Single

internal class StatusApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IStatusApi {
    override fun set(text: String?, groupId: Int?): Single<Boolean> {
        return provideService(IStatusService(), TokenType.USER)
            .flatMap { service ->
                service.set(text, groupId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }
}