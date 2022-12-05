package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.GroupLongpollServer
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IGroupsApi {
    @CheckResult
    fun editManager(
        groupId: Int,
        userId: Int,
        role: String?,
        isContact: Boolean?,
        contactPosition: String?,
        contactEmail: String?,
        contactPhone: String?
    ): Completable

    @CheckResult
    fun edit(
        groupId: Int,
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
    fun unban(groupId: Int, ownerId: Int): Completable

    @CheckResult
    fun ban(
        groupId: Int,
        ownerId: Int,
        endDate: Long?,
        reason: Int?,
        comment: String?,
        commentVisible: Boolean?
    ): Completable

    @CheckResult
    fun getSettings(groupId: Int): Single<GroupSettingsDto>

    @CheckResult
    fun getMarketAlbums(owner_id: Int, offset: Int, count: Int): Single<Items<VKApiMarketAlbum>>

    @CheckResult
    fun getMarket(
        owner_id: Int,
        album_id: Int?,
        offset: Int,
        count: Int,
        extended: Int?
    ): Single<Items<VKApiMarket>>

    @CheckResult
    fun getMarketServices(
        owner_id: Int,
        offset: Int,
        count: Int,
        extended: Int?
    ): Single<Items<VKApiMarket>>

    @CheckResult
    fun getMarketById(ids: Collection<AccessIdPair>): Single<Items<VKApiMarket>>

    @CheckResult
    fun getBanned(
        groupId: Int,
        offset: Int?,
        count: Int?,
        fields: String?,
        userId: Int?
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
    fun leave(groupId: Int): Single<Boolean>

    @CheckResult
    fun join(groupId: Int, notSure: Int?): Single<Boolean>

    @CheckResult
    operator fun get(
        userId: Int?, extended: Boolean?, filter: String?,
        fields: String?, offset: Int?, count: Int?
    ): Single<Items<VKApiCommunity>>

    @CheckResult
    fun getById(
        ids: Collection<Int>, domains: Collection<String>?,
        groupId: String?, fields: String?
    ): Single<List<VKApiCommunity>>

    @CheckResult
    fun getLongPollServer(groupId: Int): Single<GroupLongpollServer>

    @CheckResult
    fun getChats(groupId: Int, offset: Int?, count: Int?): Single<Items<VKApiGroupChats>>
}