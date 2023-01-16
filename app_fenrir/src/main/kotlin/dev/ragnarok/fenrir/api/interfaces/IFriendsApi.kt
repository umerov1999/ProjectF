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
        userId: Long, order: String?, count: Int,
        offset: Int, fields: String?
    ): Single<OnlineFriendsResponse>

    @CheckResult
    operator fun get(
        userId: Long?, order: String?, listId: Int?, count: Int?, offset: Int?,
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
    fun deleteSubscriber(
        subscriber_id: Long
    ): Single<Int>

    @CheckResult
    fun getLists(userId: Long?, returnSystem: Boolean?): Single<Items<VKApiFriendList>>

    @CheckResult
    fun delete(userId: Long): Single<DeleteFriendResponse>

    @CheckResult
    fun add(userId: Long, text: String?, follow: Boolean?): Single<Int>

    @CheckResult
    fun search(
        userId: Long, query: String?, fields: String?, nameCase: String?,
        offset: Int?, count: Int?
    ): Single<Items<VKApiUser>>

    @CheckResult
    fun getMutual(
        sourceUid: Long?,
        targetUid: Long,
        count: Int,
        offset: Int,
        fields: String?
    ): Single<List<VKApiUser>>
}