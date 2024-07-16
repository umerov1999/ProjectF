package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.GroupSettingsDto
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiBanned
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiGroupChats
import dev.ragnarok.fenrir.api.model.VKApiMarket
import dev.ragnarok.fenrir.api.model.VKApiMarketAlbum
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.GroupByIdResponse
import dev.ragnarok.fenrir.api.model.response.GroupLongpollServer
import kotlinx.coroutines.flow.Flow

interface IGroupsApi {
    @CheckResult
    fun editManager(
        groupId: Long,
        userId: Long,
        role: String?,
        isContact: Boolean?,
        contactPosition: String?,
        contactEmail: String?,
        contactPhone: String?
    ): Flow<Boolean>

    @CheckResult
    fun edit(
        groupId: Long,
        title: String?,
        description: String?,
        screen_name: String?,
        access: Int?,
        website: String?,
        public_category: Int?,
        public_date: String?,
        age_limits: Int?,
        obscene_filter: Int?,
        obscene_stopwords: Int?,
        obscene_words: String?
    ): Flow<Boolean>

    @CheckResult
    fun unban(groupId: Long, ownerId: Long): Flow<Boolean>

    @CheckResult
    fun ban(
        groupId: Long,
        ownerId: Long,
        endDate: Long?,
        reason: Int?,
        comment: String?,
        commentVisible: Boolean?
    ): Flow<Boolean>

    @CheckResult
    fun getSettings(groupId: Long): Flow<GroupSettingsDto>

    @CheckResult
    fun getMarketAlbums(owner_id: Long, offset: Int, count: Int): Flow<Items<VKApiMarketAlbum>>

    @CheckResult
    fun getMarket(
        owner_id: Long,
        album_id: Int?,
        offset: Int,
        count: Int,
        extended: Int?
    ): Flow<Items<VKApiMarket>>

    @CheckResult
    fun getMarketServices(
        owner_id: Long,
        offset: Int,
        count: Int,
        extended: Int?
    ): Flow<Items<VKApiMarket>>

    @CheckResult
    fun getMarketById(ids: Collection<AccessIdPair>): Flow<Items<VKApiMarket>>

    @CheckResult
    fun getBanned(
        groupId: Long,
        offset: Int?,
        count: Int?,
        fields: String?,
        userId: Long?
    ): Flow<Items<VKApiBanned>>

    @CheckResult
    fun getWallInfo(groupId: String?, fields: String?): Flow<VKApiCommunity>

    @CheckResult
    fun getMembers(
        groupId: String?, sort: Int?, offset: Int?,
        count: Int?, fields: String?, filter: String?
    ): Flow<Items<VKApiUser>>

    @CheckResult
    fun search(
        query: String?, type: String?, filter: String?, countryId: Int?, cityId: Int?,
        future: Boolean?, market: Boolean?, sort: Int?, offset: Int?, count: Int?
    ): Flow<Items<VKApiCommunity>>

    @CheckResult
    fun leave(groupId: Long): Flow<Boolean>

    @CheckResult
    fun join(groupId: Long, notSure: Int?): Flow<Boolean>

    @CheckResult
    operator fun get(
        userId: Long?, extended: Boolean?, filter: String?,
        fields: String?, offset: Int?, count: Int?
    ): Flow<Items<VKApiCommunity>>

    @CheckResult
    fun getById(
        ids: Collection<Long>, domains: Collection<String>?,
        groupId: String?, fields: String?
    ): Flow<GroupByIdResponse>

    @CheckResult
    fun getLongPollServer(groupId: Long): Flow<GroupLongpollServer>

    @CheckResult
    fun getChats(groupId: Long, offset: Int?, count: Int?): Flow<Items<VKApiGroupChats>>
}