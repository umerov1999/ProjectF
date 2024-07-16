package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.model.Banned
import dev.ragnarok.fenrir.model.ContactInfo
import dev.ragnarok.fenrir.model.GroupSettings
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow

interface IGroupSettingsInteractor {
    fun getGroupSettings(accountId: Long, groupId: Long): Flow<GroupSettings>
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
    ): Flow<Boolean>

    fun ban(
        accountId: Long,
        groupId: Long,
        ownerId: Long,
        endDateUnixtime: Long?,
        reason: Int,
        comment: String?,
        commentVisible: Boolean
    ): Flow<Boolean>

    fun editManager(
        accountId: Long,
        groupId: Long,
        user: User,
        role: String?,
        asContact: Boolean,
        position: String?,
        email: String?,
        phone: String?
    ): Flow<Boolean>

    fun unban(accountId: Long, groupId: Long, ownerId: Long): Flow<Boolean>
    fun getBanned(
        accountId: Long,
        groupId: Long,
        startFrom: IntNextFrom,
        count: Int
    ): Flow<Pair<List<Banned>, IntNextFrom>>

    fun getManagers(accountId: Long, groupId: Long): Flow<List<Manager>>
    fun getContacts(accountId: Long, groupId: Long): Flow<List<ContactInfo>>
}