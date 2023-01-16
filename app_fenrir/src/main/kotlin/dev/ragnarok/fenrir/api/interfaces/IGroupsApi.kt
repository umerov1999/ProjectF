package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.GroupLongpollServer
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

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
    ): Completable

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
    ): Completable

    @CheckResult
    fun unban(groupId: Long, ownerId: Long): Completable

    @CheckResult
    fun ban(
        groupId: Long,
        ownerId: Long,
        endDate: Long?,
        reason: Int?,
        comment: String?,
        commentVisible: Boolean?
    ): Completable

    @CheckResult
    fun getSettings(groupId: Long): Single<GroupSettingsDto>

    @CheckResult
    fun getMarketAlbums(owner_id: Long, offset: Int, count: Int): Single<Items<VKApiMarketAlbum>>

    @CheckResult
    fun getMarket(
        owner_id: Long,
        album_id: Int?,
        offset: Int,
        count: Int,
        extended: Int?
    ): Single<Items<VKApiMarket>>

    @CheckResult
    fun getMarketServices(
        owner_id: Long,
        offset: Int,
        count: Int,
        extended: Int?
    ): Single<Items<VKApiMarket>>

    @CheckResult
    fun getMarketById(ids: Collection<AccessIdPair>): Single<Items<VKApiMarket>>

    @CheckResult
    fun getBanned(
        groupId: Long,
        offset: Int?,
        count: Int?,
        fields: String?,
        userId: Long?
    ): Single<Items<VKApiBanned>>

    @CheckResult
    fun getWallInfo(groupId: String?, fields: String?): Single<VKApiCommunity>

    @CheckResult
    fun getMembers(
        groupId: String?, sort: Int?, offset: Int?,
        count: Int?, fields: String?, filter: String?
    ): Single<Items<VKApiUser>>

    @CheckResult
    fun search(
        query: String?, type: String?, filter: String?, countryId: Int?, cityId: Int?,
        future: Boolean?, market: Boolean?, sort: Int?, offset: Int?, count: Int?
    ): Single<Items<VKApiCommunity>>

    @CheckResult
    fun leave(groupId: Long): Single<Boolean>

    @CheckResult
    fun join(groupId: Long, notSure: Int?): Single<Boolean>

    @CheckResult
    operator fun get(
        userId: Long?, extended: Boolean?, filter: String?,
        fields: String?, offset: Int?, count: Int?
    ): Single<Items<VKApiCommunity>>

    @CheckResult
    fun getById(
        ids: Collection<Long>, domains: Collection<String>?,
        groupId: String?, fields: String?
    ): Single<List<VKApiCommunity>>

    @CheckResult
    fun getLongPollServer(groupId: Long): Single<GroupLongpollServer>

    @CheckResult
    fun getChats(groupId: Long, offset: Int?, count: Int?): Single<Items<VKApiGroupChats>>
}