package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.BanAction
import dev.ragnarok.fenrir.db.model.UserPatch
import dev.ragnarok.fenrir.db.model.entity.CommunityDetailsEntity
import dev.ragnarok.fenrir.db.model.entity.CommunityEntity
import dev.ragnarok.fenrir.db.model.entity.FriendListEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.UserDetailsEntity
import dev.ragnarok.fenrir.db.model.entity.UserEntity
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface IOwnersStorage : IStorage {
    fun findFriendsListsByIds(
        accountId: Long,
        userId: Long,
        ids: Collection<Long>
    ): Flow<MutableMap<Long, FriendListEntity>>

    @CheckResult
    fun getLocalizedUserActivity(accountId: Long, userId: Long): Flow<String?>
    fun findUserDboById(accountId: Long, ownerId: Long): Flow<Optional<UserEntity>>
    fun findCommunityDboById(accountId: Long, ownerId: Long): Flow<Optional<CommunityEntity>>
    fun findUserByDomain(accountId: Long, domain: String?): Flow<Optional<UserEntity>>
    fun findCommunityByDomain(accountId: Long, domain: String?): Flow<Optional<CommunityEntity>>
    fun findUserDbosByIds(accountId: Long, ids: List<Long>): Flow<List<UserEntity>>
    fun findCommunityDbosByIds(accountId: Long, ids: List<Long>): Flow<List<CommunityEntity>>
    fun storeUserDbos(accountId: Long, users: List<UserEntity>): Flow<Boolean>
    fun storeCommunityDbos(accountId: Long, communityEntities: List<CommunityEntity>): Flow<Boolean>
    fun storeOwnerEntities(accountId: Long, entities: OwnerEntities?): Flow<Boolean>

    @CheckResult
    fun getMissingUserIds(accountId: Long, ids: Collection<Long>): Flow<Collection<Long>>

    @CheckResult
    fun getMissingCommunityIds(accountId: Long, ids: Collection<Long>): Flow<Collection<Long>>
    fun fireBanAction(action: BanAction): Flow<Boolean>
    fun observeBanActions(): SharedFlow<BanAction>
    fun fireManagementChangeAction(manager: Pair<Long, Manager>): Flow<Boolean>
    fun observeManagementChanges(): SharedFlow<Pair<Long, Manager>>
    fun getGroupsDetails(accountId: Long, groupId: Long): Flow<Optional<CommunityDetailsEntity>>
    fun storeGroupsDetails(
        accountId: Long,
        groupId: Long,
        dbo: CommunityDetailsEntity
    ): Flow<Boolean>

    fun getUserDetails(accountId: Long, userId: Long): Flow<Optional<UserDetailsEntity>>
    fun storeUserDetails(accountId: Long, userId: Long, dbo: UserDetailsEntity): Flow<Boolean>
    fun applyPathes(accountId: Long, patches: List<UserPatch>): Flow<Boolean>
    fun findFriendBirtday(accountId: Long): Flow<List<User>>
}