package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.model.VKApiWikiPage
import io.reactivex.rxjava3.core.Single

interface IPagesApi {
    operator fun get(
        ownerId: Long, pageId: Int, global: Boolean?, sitePreview: Boolean?,
        title: String?, needSource: Boolean?, needHtml: Boolean?
    ): Single<VKApiWikiPage>
}