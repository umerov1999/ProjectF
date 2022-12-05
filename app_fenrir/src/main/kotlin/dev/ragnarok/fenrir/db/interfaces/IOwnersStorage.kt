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
        accountId: Int,
        userId: Int,
        ids: Collection<Int>
    ): Single<MutableMap<Int, FriendListEntity>>

    @CheckResult
    fun getLocalizedUserActivity(accountId: Int, userId: Int): Maybe<String>
    fun findUserDboById(accountId: Int, ownerId: Int): Single<Optional<UserEntity>>
    fun findCommunityDboById(accountId: Int, ownerId: Int): Single<Optional<CommunityEntity>>
    fun findUserByDomain(accountId: Int, domain: String?): Single<Optional<UserEntity>>
    fun findCommunityByDomain(accountId: Int, domain: String?): Single<Optional<CommunityEntity>>
    fun findUserDbosByIds(accountId: Int, ids: List<Int>): Single<List<UserEntity>>
    fun findCommunityDbosByIds(accountId: Int, ids: List<Int>): Single<List<CommunityEntity>>
    fun storeUserDbos(accountId: Int, users: List<UserEntity>): Completable
    fun storeCommunityDbos(accountId: Int, communityEntities: List<CommunityEntity>): Completable
    fun storeOwnerEntities(accountId: Int, entities: OwnerEntities?): Completable

    @CheckResult
    fun getMissingUserIds(accountId: Int, ids: Collection<Int>): Single<Collection<Int>>

    @CheckResult
    fun getMissingCommunityIds(accountId: Int, ids: Collection<Int>): Single<Collection<Int>>
    fun fireBanAction(action: BanAction): Completable
    fun observeBanActions(): Observable<BanAction>
    fun fireManagementChangeAction(manager: Pair<Int, Manager>): Completable
    fun observeManagementChanges(): Observable<Pair<Int, Manager>>
    fun getGroupsDetails(accountId: Int, groupId: Int): Single<Optional<CommunityDetailsEntity>>
    fun storeGroupsDetails(accountId: Int, groupId: Int, dbo: CommunityDetailsEntity): Completable
    fun getUserDetails(accountId: Int, userId: Int): Single<Optional<UserDetailsEntity>>
    fun storeUserDetails(accountId: Int, userId: Int, dbo: UserDetailsEntity): Completable
    fun applyPathes(accountId: Int, patches: List<UserPatch>): Completable
    fun findFriendBirtday(accountId: Int): Single<List<User>>
}