package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.response.LikesListResponse
import io.reactivex.rxjava3.core.Single

interface ILikesApi {
    @CheckResult
    fun getList(
        type: String?, ownerId: Int?, itemId: Int?, pageUrl: String?, filter: String?,
        friendsOnly: Boolean?, offset: Int?, count: Int?, skipOwn: Boolean?, fields: String?
    ): Single<LikesListResponse>

    @CheckResult
    fun delete(type: String?, ownerId: Int?, itemId: Int, accessKey: String?): Single<Int>

    @CheckResult
    fun add(type: String?, ownerId: Int?, itemId: Int, accessKey: String?): Single<Int>

    @CheckResult
    fun isLiked(type: String?, ownerId: Int?, itemId: Int): Single<Boolean>

    @CheckResult
    fun checkAndAddLike(
        type: String?,
        ownerId: Int?,
        itemId: Int,
        accessKey: String?
    ): Single<Int>
}