package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IStatusApi
import dev.ragnarok.fenrir.api.services.IStatusService
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class StatusApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IStatusApi {
    override fun set(text: String?, groupId: Long?): Flow<Boolean> {
        return provideService(IStatusService(), TokenType.USER)
            .flatMapConcat {
                it.set(text, groupId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }
}