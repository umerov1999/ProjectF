package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IPagesApi
import dev.ragnarok.fenrir.api.model.VKApiWikiPage
import dev.ragnarok.fenrir.api.services.IPagesService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class PagesApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IPagesApi {
    override fun get(
        ownerId: Long,
        pageId: Int,
        global: Boolean?,
        sitePreview: Boolean?,
        title: String?,
        needSource: Boolean?,
        needHtml: Boolean?
    ): Flow<VKApiWikiPage> {
        return provideService(IPagesService(), TokenType.USER)
            .flatMapConcat {
                it[ownerId, pageId, integerFromBoolean(global), integerFromBoolean(sitePreview), title, integerFromBoolean(
                    needSource
                ), integerFromBoolean(needHtml)]
                    .map(extractResponseWithErrorHandling())
            }
    }
}