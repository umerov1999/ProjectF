package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IPagesApi
import dev.ragnarok.fenrir.api.model.VKApiWikiPage
import dev.ragnarok.fenrir.api.services.IPagesService
import io.reactivex.rxjava3.core.Single

internal class PagesApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), IPagesApi {
    override fun get(
        ownerId: Int,
        pageId: Int,
        global: Boolean?,
        sitePreview: Boolean?,
        title: String?,
        needSource: Boolean?,
        needHtml: Boolean?
    ): Single<VKApiWikiPage> {
        return provideService(IPagesService::class.java, TokenType.USER)
            .flatMap { service ->
                service[ownerId, pageId, integerFromBoolean(global), integerFromBoolean(sitePreview), title, integerFromBoolean(
                    needSource
                ), integerFromBoolean(needHtml)]
                    .map(extractResponseWithErrorHandling())
            }
    }
}