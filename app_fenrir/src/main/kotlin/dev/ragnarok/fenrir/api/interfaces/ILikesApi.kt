package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.response.LikesListResponse
import kotlinx.coroutines.flow.Flow

interface ILikesApi {
    @CheckResult
    fun getList(
        type: String?, ownerId: Long?, itemId: Int?, pageUrl: String?, filter: String?,
        friendsOnly: Boolean?, offset: Int?, count: Int?, skipOwn: Boolean?, fields: String?
    ): Flow<LikesListResponse>

    @CheckResult
    fun delete(type: String?, ownerId: Long?, itemId: Int, accessKey: String?): Flow<Int>

    @CheckResult
    fun add(type: String?, ownerId: Long?, itemId: Int, accessKey: String?): Flow<Int>

    @CheckResult
    fun isLiked(type: String?, ownerId: Long?, itemId: Int): Flow<Boolean>

    @CheckResult
    fun checkAndAddLike(
        type: String?,
        ownerId: Long?,
        itemId: Int,
        accessKey: String?
    ): Flow<Int>
}