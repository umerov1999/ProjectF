package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IGroupSettingsInteractor {
    fun getGroupSettings(accountId: Long, groupId: Long): Single<GroupSettings>
    fun edit(
        accountId: Long,
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

    fun ban(
        accountId: Long,
        groupId: Long,
        ownerId: Long,
        endDateUnixtime: Long?,
        reason: Int,
        comment: String?,
        commentVisible: Boolean
    ): Completable

    fun editManager(
        accountId: Long,
        groupId: Long,
        user: User,
        role: String?,
        asContact: Boolean,
        position: String?,
        email: String?,
        phone: String?
    ): Completable

    fun unban(accountId: Long, groupId: Long, ownerId: Long): Completable
    fun getBanned(
        accountId: Long,
        groupId: Long,
        startFrom: IntNextFrom,
        count: Int
    ): Single<Pair<List<Banned>, IntNextFrom>>

    fun getManagers(accountId: Long, groupId: Long): Single<List<Manager>>
    fun getContacts(accountId: Long, groupId: Long): Single<List<ContactInfo>>
}