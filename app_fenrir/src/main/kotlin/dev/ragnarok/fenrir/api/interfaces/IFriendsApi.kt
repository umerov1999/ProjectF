package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiFriendList
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.DeleteFriendResponse
import dev.ragnarok.fenrir.api.model.response.OnlineFriendsResponse
import io.reactivex.rxjava3.core.Single

interface IFriendsApi {
    @CheckResult
    fun getOnline(
        userId: Int, order: String?, count: Int,
        offset: Int, fields: String?
    ): Single<OnlineFriendsResponse>

    @CheckResult
    operator fun get(
        userId: Int?, order: String?, listId: Int?, count: Int?, offset: Int?,
        fields: String?, nameCase: String?
    ): Single<Items<VKApiUser>>

    @CheckResult
    fun getByPhones(phones: String?, fields: String?): Single<List<VKApiUser>>

    @CheckResult
    fun getRecommendations(
        count: Int?,
        fields: String?,
        nameCase: String?
    ): Single<Items<VKApiUser>>

    @CheckResult
    fun getLists(userId: Int?, returnSystem: Boolean?): Single<Items<VKApiFriendList>>

    @CheckResult
    fun delete(userId: Int): Single<DeleteFriendResponse>

    @CheckResult
    fun add(userId: Int, text: String?, follow: Boolean?): Single<Int>

    @CheckResult
    fun search(
        userId: Int, query: String?, fields: String?, nameCase: String?,
        offset: Int?, count: Int?
    ): Single<Items<VKApiUser>>

    @CheckResult
    fun getMutual(
        sourceUid: Int?,
        targetUid: Int,
        count: Int,
        offset: Int,
        fields: String?
    ): Single<List<VKApiUser>>
}