package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.ILikesApi
import dev.ragnarok.fenrir.api.model.response.LikesListResponse
import dev.ragnarok.fenrir.api.services.ILikesService
import io.reactivex.rxjava3.core.Single

internal class LikesApi(accountId: Int, provider: IServiceProvider) :
    AbsApi(accountId, provider), ILikesApi {
    override fun getList(
        type: String?, ownerId: Int?, itemId: Int?, pageUrl: String?,
        filter: String?, friendsOnly: Boolean?, offset: Int?,
        count: Int?, skipOwn: Boolean?, fields: String?
    ): Single<LikesListResponse> {
        return provideService(ILikesService::class.java, TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getList(
                        type, ownerId, itemId, pageUrl, filter, integerFromBoolean(friendsOnly),
                        1, offset, count, integerFromBoolean(skipOwn), fields
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun delete(
        type: String?,
        ownerId: Int?,
        itemId: Int,
        accessKey: String?
    ): Single<Int> {
        return provideService(ILikesService::class.java, TokenType.USER)
            .flatMap { service ->
                service.delete(type, ownerId, itemId, accessKey)
                    .map(extractResponseWithErrorHandling())
                    .map { it.likes }
            }
    }

    override fun add(type: String?, ownerId: Int?, itemId: Int, accessKey: String?): Single<Int> {
        return provideService(ILikesService::class.java, TokenType.USER)
            .flatMap { service ->
                service.add(type, ownerId, itemId, accessKey)
                    .map(extractResponseWithErrorHandling())
                    .map { response -> response.likes }
            }
    }

    override fun isLiked(type: String?, ownerId: Int?, itemId: Int): Single<Boolean> {
        return provideService(ILikesService::class.java, TokenType.USER)
            .flatMap { service ->
                service.isLiked(type, ownerId, itemId)
                    .map(extractResponseWithErrorHandling())
                    .map { it.liked != 0 }
            }
    }

    override fun checkAndAddLike(
        type: String?,
        ownerId: Int?,
        itemId: Int,
        accessKey: String?
    ): Single<Int> {
        return provideService(ILikesService::class.java, TokenType.USER)
            .flatMap { service ->
                service.checkAndAddLike(
                    "var type = Args.type; var owner_id = Args.owner_id; var item_id = Args.item_id; var access_key = Args.access_key; if(API.likes.isLiked({\"v\":\"" + Constants.API_VERSION + "\", \"type\": type, \"owner_id\": owner_id, \"item_id\": item_id}).liked == 0) {return API.likes.add({\"v\":\"" + Constants.API_VERSION + "\", \"type\": type, \"owner_id\": owner_id, \"item_id\": item_id, \"access_key\": access_key}).likes;} return 0;",
                    type,
                    ownerId,
                    itemId,
                    accessKey
                )
                    .map(extractResponseWithErrorHandling())
            }
    }
}