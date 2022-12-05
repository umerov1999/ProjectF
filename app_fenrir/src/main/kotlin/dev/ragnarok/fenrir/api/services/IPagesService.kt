package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.VKApiWikiPage
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IPagesService {
    //https://vk.com/dev/pages.get
    @FormUrlEncoded
    @POST("pages.get")
    operator fun get(
        @Field("owner_id") ownerId: Int,
        @Field("page_id") pageId: Int,
        @Field("global") global: Int?,
        @Field("site_preview") sitePreview: Int?,
        @Field("title") title: String?,
        @Field("need_source") needSource: Int?,
        @Field("need_html") needHtml: Int?
    ): Single<BaseResponse<VKApiWikiPage>>
}