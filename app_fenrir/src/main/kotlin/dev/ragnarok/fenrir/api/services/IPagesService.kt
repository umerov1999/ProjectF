package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.VKApiWikiPage
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow

class IPagesService : IServiceRest() {
    //https://vk.ru/dev/pages.get
    operator fun get(
        ownerId: Long,
        pageId: Int,
        global: Int?,
        sitePreview: Int?,
        title: String?,
        needSource: Int?,
        needHtml: Int?
    ): Flow<BaseResponse<VKApiWikiPage>> {
        return rest.request(
            "pages.get", form(
                "owner_id" to ownerId,
                "page_id" to pageId,
                "global" to global,
                "site_preview" to sitePreview,
                "title" to title,
                "need_source" to needSource,
                "need_html" to needHtml
            ), base(VKApiWikiPage.serializer())
        )
    }
}