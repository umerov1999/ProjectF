package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiFeedList
import dev.ragnarok.fenrir.api.model.response.IgnoreItemResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedBanResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedResponse
import dev.ragnarok.fenrir.api.model.response.NewsfeedSearchResponse
import kotlinx.coroutines.flow.Flow

interface INewsfeedApi {
    @CheckResult
    fun getLists(listIds: Collection<Int>?): Flow<Items<VKApiFeedList>>

    @CheckResult
    fun search(
        query: String?, extended: Boolean?, count: Int?,
        latitude: Double?, longitude: Double?, startTime: Long?,
        endTime: Long?, startFrom: String?, fields: String?
    ): Flow<NewsfeedSearchResponse>

    @CheckResult
    fun saveList(title: String?, listIds: Collection<Long>?): Flow<Int>

    @CheckResult
    fun deleteList(list_id: Int?): Flow<Int>

    @CheckResult
    fun getComments(
        count: Int?, filters: String?, reposts: String?,
        startTime: Long?, endTime: Long?, lastCommentsCount: Int?,
        startFrom: String?, fields: String?
    ): Flow<NewsfeedCommentsResponse>

    @CheckResult
    fun getMentions(
        owner_id: Long?,
        count: Int?,
        offset: Int?,
        startTime: Long?,
        endTime: Long?
    ): Flow<NewsfeedCommentsResponse>

    @CheckResult
    operator fun get(
        filters: String?, returnBanned: Boolean?, startTime: Long?, endTime: Long?,
        maxPhotoCount: Int?, sourceIds: String?, startFrom: String?, count: Int?, fields: String?
    ): Flow<NewsfeedResponse>

    @CheckResult
    fun getTop(
        filters: String?, returnBanned: Boolean?, startTime: Long?,
        endTime: Long?, maxPhotoCount: Int?, sourceIds: String?,
        startFrom: String?, count: Int?, fields: String?
    ): Flow<NewsfeedResponse>

    @CheckResult
    fun getRecommended(
        startTime: Long?, endTime: Long?,
        maxPhotoCount: Int?, startFrom: String?, count: Int?, fields: String?
    ): Flow<NewsfeedResponse>

    @CheckResult
    fun getFeedLikes(
        maxPhotoCount: Int?,
        startFrom: String?,
        count: Int?,
        fields: String?
    ): Flow<NewsfeedResponse>

    @CheckResult
    fun addBan(listIds: Collection<Long>): Flow<Int>

    @CheckResult
    fun ignoreItem(type: String?, owner_id: Long?, item_id: Int?): Flow<IgnoreItemResponse>

    @CheckResult
    fun deleteBan(listIds: Collection<Long>): Flow<Int>

    @CheckResult
    fun getBanned(): Flow<NewsfeedBanResponse>
}