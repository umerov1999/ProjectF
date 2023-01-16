package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.BanAction
import dev.ragnarok.fenrir.db.model.UserPatch
import dev.ragnarok.fenrir.db.model.entity.*
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface IOwnersStorage : IStorage {
    fun findFriendsListsByIds(
        accountId: Long,
        userId: Long,
        ids: Collection<Long>
    ): Single<MutableMap<Long, FriendListEntity>>

    @CheckResult
    fun getLocalizedUserActivity(accountId: Long, userId: Long): Maybe<String>
    fun findUserDboById(accountId: Long, ownerId: Long): Single<Optional<UserEntity>>
    fun findCommunityDboById(accountId: Long, ownerId: Long): Single<Optional<CommunityEntity>>
    fun findUserByDomain(accountId: Long, domain: String?): Single<Optional<UserEntity>>
    fun findCommunityByDomain(accountId: Long, domain: String?): Single<Optional<CommunityEntity>>
    fun findUserDbosByIds(accountId: Long, ids: List<Long>): Single<List<UserEntity>>
    fun findCommunityDbosByIds(accountId: Long, ids: List<Long>): Single<List<CommunityEntity>>
    fun storeUserDbos(accountId: Long, users: List<UserEntity>): Completable
    fun storeCommunityDbos(accountId: Long, communityEntities: List<CommunityEntity>): Completable
    fun storeOwnerEntities(accountId: Long, entities: OwnerEntities?): Completable

    @CheckResult
    fun getMissingUserIds(accountId: Long, ids: Collection<Long>): Single<Collection<Long>>

    @CheckResult
    fun getMissingCommunityIds(accountId: Long, ids: Collection<Long>): Single<Collection<Long>>
    fun fireBanAction(action: BanAction): Completable
    fun observeBanActions(): Observable<BanAction>
    fun fireManagementChangeAction(manager: Pair<Long, Manager>): Completable
    fun observeManagementChanges(): Observable<Pair<Long, Manager>>
    fun getGroupsDetails(accountId: Long, groupId: Long): Single<Optional<CommunityDetailsEntity>>
    fun storeGroupsDetails(accountId: Long, groupId: Long, dbo: CommunityDetailsEntity): Completable
    fun getUserDetails(accountId: Long, userId: Long): Single<Optional<UserDetailsEntity>>
    fun storeUserDetails(accountId: Long, userId: Long, dbo: UserDetailsEntity): Completable
    fun applyPathes(accountId: Long, patches: List<UserPatch>): Completable
    fun findFriendBirtday(accountId: Long): Single<List<User>>
}