package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.ILikesApi
import dev.ragnarok.fenrir.api.model.response.LikesListResponse
import dev.ragnarok.fenrir.api.services.ILikesService
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkIntOverZero
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class LikesApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), ILikesApi {
    override fun getList(
        type: String?, ownerId: Long?, itemId: Int?, pageUrl: String?,
        filter: String?, friendsOnly: Boolean?, offset: Int?,
        count: Int?, skipOwn: Boolean?, fields: String?
    ): Flow<LikesListResponse> {
        return provideService(ILikesService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getList(
                    type, ownerId, itemId, pageUrl, filter, integerFromBoolean(friendsOnly),
                    1, offset, count, integerFromBoolean(skipOwn), fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun delete(
        type: String?,
        ownerId: Long?,
        itemId: Int,
        accessKey: String?
    ): Flow<Int> {
        return provideService(ILikesService(), TokenType.USER)
            .flatMapConcat { s ->
                s.delete(type, ownerId, itemId, accessKey)
                    .map(extractResponseWithErrorHandling())
                    .map { it.likes }
            }
    }

    override fun add(type: String?, ownerId: Long?, itemId: Int, accessKey: String?): Flow<Int> {
        return provideService(ILikesService(), TokenType.USER)
            .flatMapConcat { s ->
                s.add(type, ownerId, itemId, accessKey)
                    .map(extractResponseWithErrorHandling())
                    .map { it.likes }
            }
    }

    override fun isLiked(type: String?, ownerId: Long?, itemId: Int): Flow<Boolean> {
        return provideService(ILikesService(), TokenType.USER)
            .flatMapConcat { s ->
                s.isLiked(type, ownerId, itemId)
                    .map(extractResponseWithErrorHandling())
                    .map { it.liked }
                    .checkIntOverZero()
            }
    }

    override fun checkAndAddLike(
        type: String?,
        ownerId: Long?,
        itemId: Int,
        accessKey: String?
    ): Flow<Int> {
        return provideService(ILikesService(), TokenType.USER)
            .flatMapConcat {
                it.checkAndAddLike(
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