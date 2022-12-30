package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.FavePageResponse
import dev.ragnarok.fenrir.api.model.response.FavePostsResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IFaveService : IServiceRest() {
    fun getPages(
        offset: Int?,
        count: Int?,
        type: String?,
        fields: String?
    ): Single<BaseResponse<Items<FavePageResponse>>> {
        return rest.request(
            "fave.getPages", form(
                "offset" to offset,
                "count" to count,
                "type" to type,
                "fields" to fields
            ), items(FavePageResponse.serializer())
        )
    }

    fun getVideos(
        offset: Int?,
        count: Int?,
        item_type: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<Items<VKApiAttachments.Entry>>> {
        return rest.request(
            "fave.get", form(
                "offset" to offset,
                "count" to count,
                "item_type" to item_type,
                "extended" to extended,
                "fields" to fields
            ), items(VKApiAttachments.Entry.serializer())
        )
    }

    fun getArticles(
        offset: Int?,
        count: Int?,
        item_type: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<Items<VKApiAttachments.Entry>>> {
        return rest.request(
            "fave.get", form(
                "offset" to offset,
                "count" to count,
                "item_type" to item_type,
                "extended" to extended,
                "fields" to fields
            ), items(VKApiAttachments.Entry.serializer())
        )
    }

    fun getOwnerPublishedArticles(
        owner_id: Int?,
        offset: Int?,
        count: Int?,
        sort_by: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<Items<VKApiArticle>>> {
        return rest.request(
            "articles.getOwnerPublished", form(
                "owner_id" to owner_id,
                "offset" to offset,
                "count" to count,
                "sort_by" to sort_by,
                "extended" to extended,
                "fields" to fields
            ), items(VKApiArticle.serializer())
        )
    }

    fun getPosts(
        offset: Int?,
        count: Int?,
        item_type: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<FavePostsResponse>> {
        return rest.request(
            "fave.get", form(
                "offset" to offset,
                "count" to count,
                "item_type" to item_type,
                "extended" to extended,
                "fields" to fields
            ), base(FavePostsResponse.serializer())
        )
    }

    fun getLinks(
        offset: Int?,
        count: Int?,
        item_type: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<Items<FaveLinkDto>>> {
        return rest.request(
            "fave.get", form(
                "offset" to offset,
                "count" to count,
                "item_type" to item_type,
                "extended" to extended,
                "fields" to fields
            ), items(FaveLinkDto.serializer())
        )
    }

    fun getProducts(
        offset: Int?,
        count: Int?,
        item_type: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<Items<VKApiAttachments.Entry>>> {
        return rest.request(
            "fave.get", form(
                "offset" to offset,
                "count" to count,
                "item_type" to item_type,
                "extended" to extended,
                "fields" to fields
            ), items(VKApiAttachments.Entry.serializer())
        )
    }

    fun getPhotos(
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>> {
        return rest.request(
            "fave.getPhotos", form(
                "offset" to offset,
                "count" to count
            ), items(VKApiPhoto.serializer())
        )
    }

    fun addLink(link: String?): Single<BaseResponse<Int>> {
        return rest.request("fave.addLink", form("link" to link), baseInt)
    }

    fun addPage(
        userId: Int?,
        groupId: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "fave.addPage", form(
                "user_id" to userId,
                "group_id" to groupId
            ), baseInt
        )
    }

    fun addVideo(
        owner_id: Int?,
        id: Int?,
        access_key: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "fave.addVideo", form(
                "owner_id" to owner_id,
                "id" to id,
                "access_key" to access_key
            ), baseInt
        )
    }

    fun addArticle(url: String?): Single<BaseResponse<Int>> {
        return rest.request("fave.addArticle", form("url" to url), baseInt)
    }

    fun addProduct(
        id: Int,
        owner_id: Int,
        access_key: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "fave.addProduct", form(
                "id" to id,
                "owner_id" to owner_id,
                "access_key" to access_key
            ), baseInt
        )
    }

    fun addPost(
        owner_id: Int?,
        id: Int?,
        access_key: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "fave.addPost", form(
                "owner_id" to owner_id,
                "id" to id,
                "access_key" to access_key
            ), baseInt
        )
    }

    //https://vk.com/dev/fave.removePage
    fun removePage(
        userId: Int?,
        groupId: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "fave.removePage", form(
                "user_id" to userId,
                "group_id" to groupId
            ), baseInt
        )
    }

    fun removeLink(linkId: String?): Single<BaseResponse<Int>> {
        return rest.request("fave.removeLink", form("link_id" to linkId), baseInt)
    }

    fun removeArticle(
        owner_id: Int?,
        article_id: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "fave.removeArticle", form(
                "owner_id" to owner_id,
                "article_id" to article_id
            ), baseInt
        )
    }

    fun removeProduct(
        id: Int?,
        owner_id: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "fave.removeProduct", form(
                "id" to id,
                "owner_id" to owner_id
            ), baseInt
        )
    }

    fun removePost(
        owner_id: Int?,
        id: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "fave.removePost", form(
                "owner_id" to owner_id,
                "id" to id
            ), baseInt
        )
    }

    fun removeVideo(
        owner_id: Int?,
        id: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "fave.removeVideo", form(
                "owner_id" to owner_id,
                "id" to id
            ), baseInt
        )
    }

    fun pushFirst(
        code: String?,
        ownerId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "execute", form(
                "code" to code,
                "owner_id" to ownerId
            ), baseInt
        )
    }
}