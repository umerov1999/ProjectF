package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiFriendList
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.DeleteFriendResponse
import dev.ragnarok.fenrir.api.model.response.OnlineFriendsResponse
import kotlinx.coroutines.flow.Flow

interface IFriendsApi {
    @CheckResult
    fun getOnline(
        userId: Long, order: String?, count: Int,
        offset: Int, fields: String?
    ): Flow<OnlineFriendsResponse>

    @CheckResult
    operator fun get(
        userId: Long?, order: String?, listId: Int?, count: Int?, offset: Int?,
        fields: String?, nameCase: String?
    ): Flow<Items<VKApiUser>>

    @CheckResult
    fun getByPhones(phones: String?, fields: String?): Flow<List<VKApiUser>>

    @CheckResult
    fun getRecommendations(
        count: Int?,
        fields: String?,
        nameCase: String?
    ): Flow<Items<VKApiUser>>

    @CheckResult
    fun deleteSubscriber(
        subscriber_id: Long
    ): Flow<Int>

    @CheckResult
    fun getLists(userId: Long?, returnSystem: Boolean?): Flow<Items<VKApiFriendList>>

    @CheckResult
    fun delete(userId: Long): Flow<DeleteFriendResponse>

    @CheckResult
    fun add(userId: Long, text: String?, follow: Boolean?): Flow<Int>

    @CheckResult
    fun search(
        userId: Long, query: String?, fields: String?, nameCase: String?,
        offset: Int?, count: Int?
    ): Flow<Items<VKApiUser>>

    @CheckResult
    fun getMutual(
        sourceUid: Long?,
        targetUid: Long,
        count: Int,
        offset: Int,
        fields: String?
    ): Flow<List<VKApiUser>>
}