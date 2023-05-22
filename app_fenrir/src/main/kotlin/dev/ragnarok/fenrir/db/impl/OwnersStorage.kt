package dev.ragnarok.fenrir.db.impl

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFriendListsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getGroupsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getGroupsDetContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getUserContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getUserDetContentUriFor
import dev.ragnarok.fenrir.db.column.*
import dev.ragnarok.fenrir.db.interfaces.IOwnersStorage
import dev.ragnarok.fenrir.db.model.BanAction
import dev.ragnarok.fenrir.db.model.UserPatch
import dev.ragnarok.fenrir.db.model.entity.*
import dev.ragnarok.fenrir.domain.mappers.Entity2Model
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.UserInfoResolveUtil.getUserActivityLine
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.subjects.PublishSubject

internal class OwnersStorage(context: AppStorages) : AbsStorage(context), IOwnersStorage {
    private val banActionsPublisher: PublishSubject<BanAction> = PublishSubject.create()
    private val managementActionsPublisher: PublishSubject<Pair<Long, Manager>> =
        PublishSubject.create()

    override fun fireBanAction(action: BanAction): Completable {
        return Completable.fromAction { banActionsPublisher.onNext(action) }
    }

    override fun observeBanActions(): Observable<BanAction> {
        return banActionsPublisher
    }

    override fun fireManagementChangeAction(manager: Pair<Long, Manager>): Completable {
        return Completable.fromAction { managementActionsPublisher.onNext(manager) }
    }

    override fun observeManagementChanges(): Observable<Pair<Long, Manager>> {
        return managementActionsPublisher
    }

    override fun getUserDetails(
        accountId: Long,
        userId: Long
    ): Single<Optional<UserDetailsEntity>> {
        return Single.fromCallable {
            val uri = getUserDetContentUriFor(accountId)
            val where = BaseColumns._ID + " = ?"
            val args = arrayOf(userId.toString())
            val cursor = contentResolver.query(uri, null, where, args, null)
            var details: UserDetailsEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    val json = cursor.getBlob(UsersDetailsColumns.DATA)
                    if (json.nonNullNoEmpty()) {
                        details =
                            MsgPack.decodeFromByteArrayEx(UserDetailsEntity.serializer(), json)
                    }
                }
                cursor.close()
            }
            wrap(details)
        }
    }

    override fun getGroupsDetails(
        accountId: Long,
        groupId: Long
    ): Single<Optional<CommunityDetailsEntity>> {
        return Single.fromCallable {
            val uri = getGroupsDetContentUriFor(accountId)
            val where = BaseColumns._ID + " = ?"
            val args = arrayOf(groupId.toString())
            val cursor = contentResolver.query(uri, null, where, args, null)
            var details: CommunityDetailsEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    val json = cursor.getBlob(GroupsDetailsColumns.DATA)
                    if (json.nonNullNoEmpty()) {
                        details =
                            MsgPack.decodeFromByteArrayEx(CommunityDetailsEntity.serializer(), json)
                    }
                }
                cursor.close()
            }
            wrap(details)
        }
    }

    override fun storeGroupsDetails(
        accountId: Long,
        groupId: Long,
        dbo: CommunityDetailsEntity
    ): Completable {
        return Completable.fromAction {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, groupId)
            cv.put(
                GroupsDetailsColumns.DATA,
                MsgPack.encodeToByteArrayEx(CommunityDetailsEntity.serializer(), dbo)
            )
            val uri = getGroupsDetContentUriFor(accountId)
            contentResolver.insert(uri, cv)
        }
    }

    override fun storeUserDetails(
        accountId: Long,
        userId: Long,
        dbo: UserDetailsEntity
    ): Completable {
        return Completable.fromAction {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, userId)
            cv.put(
                UsersDetailsColumns.DATA,
                MsgPack.encodeToByteArrayEx(UserDetailsEntity.serializer(), dbo)
            )
            val uri = getUserDetContentUriFor(accountId)
            contentResolver.insert(uri, cv)
        }
    }

    override fun applyPathes(accountId: Long, patches: List<UserPatch>): Completable {
        return if (patches.isEmpty()) {
            Completable.complete()
        } else Completable.create { emitter: CompletableEmitter ->
            val uri = getUserContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>(patches.size)
            for (patch in patches) {
                val cv = ContentValues()
                patch.status.requireNonNull {
                    cv.put(UsersColumns.USER_STATUS, it.status)
                }
                patch.online.requireNonNull {
                    cv.put(UsersColumns.ONLINE, it.isOnline)
                    cv.put(UsersColumns.LAST_SEEN, it.lastSeen)
                    cv.put(UsersColumns.PLATFORM, it.platform)
                }
                if (cv.size() > 0) {
                    operations.add(
                        ContentProviderOperation.newUpdate(uri)
                            .withValues(cv)
                            .withSelection(
                                BaseColumns._ID + " = ?",
                                arrayOf(patch.userId.toString())
                            )
                            .build()
                    )
                }
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emitter.onComplete()
        }
    }

    override fun findFriendsListsByIds(
        accountId: Long,
        userId: Long,
        ids: Collection<Long>
    ): Single<MutableMap<Long, FriendListEntity>> {
        return Single.create { emitter: SingleEmitter<MutableMap<Long, FriendListEntity>> ->
            val uri = getFriendListsContentUriFor(accountId)
            val where =
                FriendListsColumns.USER_ID + " = ? " + " AND " + FriendListsColumns.LIST_ID + " IN(" + Utils.join(
                    ",",
                    ids
                ) + ")"
            val args = arrayOf(userId.toString())
            val cursor = context.contentResolver.query(uri, null, where, args, null)
            @SuppressLint("UseSparseArrays") val map: MutableMap<Long, FriendListEntity> =
                HashMap(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    val dbo = mapFriendsList(cursor)
                    map[dbo.id] = dbo
                }
                cursor.close()
            }
            emitter.onSuccess(map)
        }
    }

    override fun getLocalizedUserActivity(accountId: Long, userId: Long): Maybe<String> {
        return Maybe.create { e: MaybeEmitter<String> ->
            val uProjection = arrayOf(UsersColumns.LAST_SEEN, UsersColumns.ONLINE, UsersColumns.SEX)
            val uri = getUserContentUriFor(accountId)
            val where = BaseColumns._ID + " = ?"
            val args = arrayOf(userId.toString())
            val cursor = context.contentResolver.query(uri, uProjection, where, args, null)
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    val online =
                        cursor.getBoolean(UsersColumns.ONLINE)
                    val lastSeen =
                        cursor.getLong(UsersColumns.LAST_SEEN)
                    val sex = cursor.getInt(UsersColumns.SEX)
                    val userActivityLine =
                        getUserActivityLine(context, lastSeen, online, sex, false)
                    e.onSuccess(userActivityLine)
                }
                cursor.close()
            }
            e.onComplete()
        }
    }

    override fun findUserDboById(accountId: Long, ownerId: Long): Single<Optional<UserEntity>> {
        return Single.create { emitter: SingleEmitter<Optional<UserEntity>> ->
            val where = BaseColumns._ID + " = ?"
            val args = arrayOf(ownerId.toString())
            val uri = getUserContentUriFor(accountId)
            val cursor = context.contentResolver.query(uri, null, where, args, null)
            var dbo: UserEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    dbo = mapUserDbo(cursor)
                }
                cursor.close()
            }
            emitter.onSuccess(wrap(dbo))
        }
    }

    override fun findCommunityDboById(
        accountId: Long,
        ownerId: Long
    ): Single<Optional<CommunityEntity>> {
        return Single.create { emitter: SingleEmitter<Optional<CommunityEntity>> ->
            val where = BaseColumns._ID + " = ?"
            val args = arrayOf(ownerId.toString())
            val uri = getGroupsContentUriFor(accountId)
            val cursor = context.contentResolver.query(uri, null, where, args, null)
            var dbo: CommunityEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    dbo = mapCommunityDbo(cursor)
                }
                cursor.close()
            }
            emitter.onSuccess(wrap(dbo))
        }
    }

    override fun findUserByDomain(accountId: Long, domain: String?): Single<Optional<UserEntity>> {
        return Single.create { emitter: SingleEmitter<Optional<UserEntity>> ->
            val uri = getUserContentUriFor(accountId)
            val where = UsersColumns.DOMAIN + " LIKE ?"
            val args = arrayOf(domain)
            val cursor = contentResolver.query(uri, null, where, args, null)
            var entity: UserEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    entity = mapUserDbo(cursor)
                }
                cursor.close()
            }
            emitter.onSuccess(wrap(entity))
        }
    }

    override fun findFriendBirtday(accountId: Long): Single<List<User>> {
        return Single.create {
            val uri = getUserContentUriFor(accountId)
            val where = UsersColumns.BDATE + " IS NOT NULL AND " + UsersColumns.IS_FRIEND + " = 1"
            val cursor = contentResolver.query(uri, null, where, null, UsersColumns.BDATE + " DESC")
            val listEntity: ArrayList<User> = ArrayList()
            while (cursor?.moveToNext() == true) {
                Entity2Model.map(mapUserDbo(cursor))?.let { it1 -> listEntity.add(it1) }
            }
            cursor?.close()
            it.onSuccess(listEntity)
        }
    }

    override fun findCommunityByDomain(
        accountId: Long,
        domain: String?
    ): Single<Optional<CommunityEntity>> {
        return Single.create { emitter: SingleEmitter<Optional<CommunityEntity>> ->
            val uri = getGroupsContentUriFor(accountId)
            val where = GroupsColumns.SCREEN_NAME + " LIKE ?"
            val args = arrayOf(domain)
            val cursor = contentResolver.query(uri, null, where, args, null)
            var entity: CommunityEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    entity = mapCommunityDbo(cursor)
                }
                cursor.close()
            }
            emitter.onSuccess(wrap(entity))
        }
    }

    override fun findUserDbosByIds(accountId: Long, ids: List<Long>): Single<List<UserEntity>> {
        return if (ids.isEmpty()) {
            Single.just(emptyList())
        } else Single.create { emitter: SingleEmitter<List<UserEntity>> ->
            val where: String
            val args: Array<String>?
            val uri = getUserContentUriFor(accountId)
            if (ids.size == 1) {
                where = BaseColumns._ID + " = ?"
                args = arrayOf(ids[0].toString())
            } else {
                where = BaseColumns._ID + " IN (" + Utils.join(",", ids) + ")"
                args = null
            }
            val cursor = contentResolver.query(uri, null, where, args, null, null)
            val dbos: MutableList<UserEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    dbos.add(mapUserDbo(cursor))
                }
                cursor.close()
            }
            emitter.onSuccess(dbos)
        }
    }

    override fun findCommunityDbosByIds(
        accountId: Long,
        ids: List<Long>
    ): Single<List<CommunityEntity>> {
        return if (ids.isEmpty()) {
            Single.just(emptyList())
        } else Single.create { emitter: SingleEmitter<List<CommunityEntity>> ->
            val where: String
            val args: Array<String>?
            val uri = getGroupsContentUriFor(accountId)
            if (ids.size == 1) {
                where = BaseColumns._ID + " = ?"
                args = arrayOf(ids[0].toString())
            } else {
                where = BaseColumns._ID + " IN (" + Utils.join(",", ids) + ")"
                args = null
            }
            val cursor = contentResolver.query(uri, null, where, args, null, null)
            val dbos: MutableList<CommunityEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    dbos.add(mapCommunityDbo(cursor))
                }
                cursor.close()
            }
            emitter.onSuccess(dbos)
        }
    }

    override fun storeUserDbos(accountId: Long, users: List<UserEntity>): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            val operations = ArrayList<ContentProviderOperation>(users.size)
            appendUsersInsertOperation(operations, accountId, users)
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emitter.onComplete()
        }
    }

    override fun storeOwnerEntities(accountId: Long, entities: OwnerEntities?): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            entities ?: return@create emitter.onComplete()
            val operations = ArrayList<ContentProviderOperation>(
                entities.size()
            )
            appendUsersInsertOperation(operations, accountId, entities.userEntities)
            appendCommunitiesInsertOperation(operations, accountId, entities.communityEntities)
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emitter.onComplete()
        }
    }

    override fun storeCommunityDbos(
        accountId: Long,
        communityEntities: List<CommunityEntity>
    ): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            val operations = ArrayList<ContentProviderOperation>(communityEntities.size)
            appendCommunitiesInsertOperation(operations, accountId, communityEntities)
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emitter.onComplete()
        }
    }

    override fun getMissingUserIds(
        accountId: Long,
        ids: Collection<Long>
    ): Single<Collection<Long>> {
        return Single.create { e: SingleEmitter<Collection<Long>> ->
            if (ids.isEmpty()) {
                e.onSuccess(emptyList())
                return@create
            }
            val copy: MutableSet<Long> = HashSet(ids)
            val projection = arrayOf(BaseColumns._ID)
            val cursor = contentResolver.query(
                getUserContentUriFor(accountId),
                projection, BaseColumns._ID + " IN ( " + Utils.join(",", copy) + ")", null, null
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(BaseColumns._ID)
                    copy.remove(id)
                }
                cursor.close()
            }
            e.onSuccess(copy)
        }
    }

    override fun getMissingCommunityIds(
        accountId: Long,
        ids: Collection<Long>
    ): Single<Collection<Long>> {
        return Single.create { e: SingleEmitter<Collection<Long>> ->
            if (ids.isEmpty()) {
                e.onSuccess(emptyList())
                return@create
            }
            val copy: MutableSet<Long> = HashSet(ids)
            val projection = arrayOf(BaseColumns._ID)
            val cursor = contentResolver.query(
                getGroupsContentUriFor(accountId),
                projection, BaseColumns._ID + " IN ( " + Utils.join(",", copy) + ")", null, null
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(BaseColumns._ID)
                    copy.remove(id)
                }
                cursor.close()
            }
            e.onSuccess(copy)
        }
    }

    private fun mapFriendsList(cursor: Cursor): FriendListEntity {
        val id = cursor.getLong(FriendListsColumns.LIST_ID)
        val name = cursor.getString(FriendListsColumns.NAME)
        return FriendListEntity(id, name)
    }

    companion object {
        private fun appendUserInsertOperation(
            operations: MutableList<ContentProviderOperation>,
            uri: Uri,
            dbo: UserEntity
        ) {
            operations.add(
                ContentProviderOperation.newInsert(uri)
                    .withValues(createCv(dbo))
                    .build()
            )
        }

        private fun appendCommunityInsertOperation(
            operations: MutableList<ContentProviderOperation>,
            uri: Uri,
            dbo: CommunityEntity
        ) {
            operations.add(
                ContentProviderOperation.newInsert(uri)
                    .withValues(createCv(dbo))
                    .build()
            )
        }


        fun appendOwnersInsertOperations(
            operations: MutableList<ContentProviderOperation>,
            accountId: Long,
            ownerEntities: OwnerEntities?
        ) {
            ownerEntities ?: return
            appendUsersInsertOperation(operations, accountId, ownerEntities.userEntities)
            appendCommunitiesInsertOperation(operations, accountId, ownerEntities.communityEntities)
        }


        fun appendUsersInsertOperation(
            operations: MutableList<ContentProviderOperation>,
            accountId: Long,
            dbos: List<UserEntity>?
        ) {
            dbos ?: return
            val uri = getUserContentUriFor(accountId)
            for (dbo in dbos) {
                appendUserInsertOperation(operations, uri, dbo)
            }
        }


        fun appendCommunitiesInsertOperation(
            operations: MutableList<ContentProviderOperation>,
            accountId: Long,
            dbos: List<CommunityEntity>?
        ) {
            dbos ?: return
            val uri = getGroupsContentUriFor(accountId)
            for (dbo in dbos) {
                appendCommunityInsertOperation(operations, uri, dbo)
            }
        }

        private fun createCv(dbo: CommunityEntity): ContentValues {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, dbo.id)
            cv.put(GroupsColumns.NAME, dbo.name)
            cv.put(GroupsColumns.SCREEN_NAME, dbo.screenName)
            cv.put(GroupsColumns.IS_CLOSED, dbo.closed)
            cv.put(GroupsColumns.IS_BLACK_LISTED, dbo.isBlacklisted)
            cv.put(GroupsColumns.IS_VERIFIED, dbo.isVerified)
            cv.put(GroupsColumns.HAS_UNSEEN_STORIES, dbo.hasUnseenStories)
            cv.put(GroupsColumns.IS_ADMIN, dbo.isAdmin)
            cv.put(GroupsColumns.ADMIN_LEVEL, dbo.adminLevel)
            cv.put(GroupsColumns.IS_MEMBER, dbo.isMember)
            cv.put(GroupsColumns.MEMBER_STATUS, dbo.memberStatus)
            cv.put(GroupsColumns.MEMBERS_COUNT, dbo.membersCount)
            cv.put(GroupsColumns.TYPE, dbo.type)
            cv.put(GroupsColumns.PHOTO_50, dbo.photo50)
            cv.put(GroupsColumns.PHOTO_100, dbo.photo100)
            cv.put(GroupsColumns.PHOTO_200, dbo.photo200)
            return cv
        }

        private fun createCv(dbo: UserEntity): ContentValues {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, dbo.id)
            cv.put(UsersColumns.FIRST_NAME, dbo.firstName)
            cv.put(UsersColumns.LAST_NAME, dbo.lastName)
            cv.put(UsersColumns.ONLINE, dbo.isOnline)
            cv.put(UsersColumns.ONLINE_MOBILE, dbo.isOnlineMobile)
            cv.put(UsersColumns.ONLINE_APP, dbo.onlineApp)
            cv.put(UsersColumns.PHOTO_50, dbo.photo50)
            cv.put(UsersColumns.PHOTO_100, dbo.photo100)
            cv.put(UsersColumns.PHOTO_200, dbo.photo200)
            cv.put(UsersColumns.PHOTO_MAX, dbo.photoMax)
            cv.put(UsersColumns.LAST_SEEN, dbo.lastSeen)
            cv.put(UsersColumns.PLATFORM, dbo.platform)
            cv.put(UsersColumns.USER_STATUS, dbo.status)
            cv.put(UsersColumns.SEX, dbo.sex)
            cv.put(UsersColumns.DOMAIN, dbo.domain)
            cv.put(UsersColumns.IS_FRIEND, dbo.isFriend)
            cv.put(UsersColumns.FRIEND_STATUS, dbo.friendStatus)
            cv.put(UsersColumns.WRITE_MESSAGE_STATUS, dbo.canWritePrivateMessage)
            cv.put(UsersColumns.BDATE, dbo.bdate)
            cv.put(UsersColumns.IS_USER_BLACK_LIST, dbo.blacklisted_by_me)
            cv.put(UsersColumns.IS_BLACK_LISTED, dbo.blacklisted)
            cv.put(UsersColumns.IS_VERIFIED, dbo.isVerified)
            cv.put(UsersColumns.HAS_UNSEEN_STORIES, dbo.hasUnseenStories)
            cv.put(UsersColumns.IS_CAN_ACCESS_CLOSED, dbo.isCan_access_closed)
            cv.put(UsersColumns.MAIDEN_NAME, dbo.maiden_name)
            return cv
        }

        internal fun mapCommunityDbo(cursor: Cursor): CommunityEntity {
            return CommunityEntity(cursor.getLong(BaseColumns._ID))
                .setName(cursor.getString(GroupsColumns.NAME))
                .setScreenName(cursor.getString(GroupsColumns.SCREEN_NAME))
                .setClosed(cursor.getInt(GroupsColumns.IS_CLOSED))
                .setVerified(cursor.getBoolean(GroupsColumns.IS_VERIFIED))
                .setHasUnseenStories(cursor.getBoolean(GroupsColumns.HAS_UNSEEN_STORIES))
                .setBlacklisted(cursor.getBoolean(GroupsColumns.IS_BLACK_LISTED))
                .setAdmin(cursor.getBoolean(GroupsColumns.IS_ADMIN))
                .setAdminLevel(cursor.getInt(GroupsColumns.ADMIN_LEVEL))
                .setMember(cursor.getBoolean(GroupsColumns.IS_MEMBER))
                .setMemberStatus(cursor.getInt(GroupsColumns.MEMBER_STATUS))
                .setMembersCount(cursor.getInt(GroupsColumns.MEMBERS_COUNT))
                .setType(cursor.getInt(GroupsColumns.TYPE))
                .setPhoto50(cursor.getString(GroupsColumns.PHOTO_50))
                .setPhoto100(cursor.getString(GroupsColumns.PHOTO_100))
                .setPhoto200(cursor.getString(GroupsColumns.PHOTO_200))
        }

        internal fun mapUserDbo(cursor: Cursor): UserEntity {
            return UserEntity(cursor.getLong(BaseColumns._ID))
                .setFirstName(cursor.getString(UsersColumns.FIRST_NAME))
                .setLastName(cursor.getString(UsersColumns.LAST_NAME))
                .setOnline(cursor.getBoolean(UsersColumns.ONLINE))
                .setOnlineMobile(cursor.getBoolean(UsersColumns.ONLINE_MOBILE))
                .setOnlineApp(cursor.getInt(UsersColumns.ONLINE_APP))
                .setPhoto50(cursor.getString(UsersColumns.PHOTO_50))
                .setPhoto100(cursor.getString(UsersColumns.PHOTO_100))
                .setPhoto200(cursor.getString(UsersColumns.PHOTO_200))
                .setPhotoMax(cursor.getString(UsersColumns.PHOTO_MAX))
                .setLastSeen(cursor.getLong(UsersColumns.LAST_SEEN))
                .setPlatform(cursor.getInt(UsersColumns.PLATFORM))
                .setStatus(cursor.getString(UsersColumns.USER_STATUS))
                .setSex(cursor.getInt(UsersColumns.SEX))
                .setDomain(cursor.getString(UsersColumns.DOMAIN))
                .setFriend(cursor.getBoolean(UsersColumns.IS_FRIEND))
                .setFriendStatus(cursor.getInt(UsersColumns.FRIEND_STATUS))
                .setCanWritePrivateMessage(cursor.getBoolean(UsersColumns.WRITE_MESSAGE_STATUS))
                .setBdate(cursor.getString(UsersColumns.BDATE))
                .setBlacklisted_by_me(cursor.getBoolean(UsersColumns.IS_USER_BLACK_LIST))
                .setBlacklisted(cursor.getBoolean(UsersColumns.IS_BLACK_LISTED))
                .setVerified(cursor.getBoolean(UsersColumns.IS_VERIFIED))
                .setHasUnseenStories(cursor.getBoolean(UsersColumns.HAS_UNSEEN_STORIES))
                .setCan_access_closed(cursor.getBoolean(UsersColumns.IS_CAN_ACCESS_CLOSED))
                .setMaiden_name(cursor.getString(UsersColumns.MAIDEN_NAME))
        }
    }

}