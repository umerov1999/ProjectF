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
                    val json = cursor.getBlob(UsersDetColumns.DATA)
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
                    val json = cursor.getBlob(GroupsDetColumns.DATA)
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
                GroupsDetColumns.DATA,
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
                UsersDetColumns.DATA,
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
                    cv.put(UserColumns.USER_STATUS, it.status)
                }
                patch.online.requireNonNull {
                    cv.put(UserColumns.ONLINE, it.isOnline)
                    cv.put(UserColumns.LAST_SEEN, it.lastSeen)
                    cv.put(UserColumns.PLATFORM, it.platform)
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
            val uProjection = arrayOf(UserColumns.LAST_SEEN, UserColumns.ONLINE, UserColumns.SEX)
            val uri = getUserContentUriFor(accountId)
            val where = BaseColumns._ID + " = ?"
            val args = arrayOf(userId.toString())
            val cursor = context.contentResolver.query(uri, uProjection, where, args, null)
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    val online =
                        cursor.getBoolean(UserColumns.ONLINE)
                    val lastSeen =
                        cursor.getLong(UserColumns.LAST_SEEN)
                    val sex = cursor.getInt(UserColumns.SEX)
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
            val where = UserColumns.DOMAIN + " LIKE ?"
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
            val where = UserColumns.BDATE + " IS NOT NULL AND " + UserColumns.IS_FRIEND + " = 1"
            val cursor = contentResolver.query(uri, null, where, null, UserColumns.BDATE + " DESC")
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
            val where = GroupColumns.SCREEN_NAME + " LIKE ?"
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
            cv.put(GroupColumns.NAME, dbo.name)
            cv.put(GroupColumns.SCREEN_NAME, dbo.screenName)
            cv.put(GroupColumns.IS_CLOSED, dbo.closed)
            cv.put(GroupColumns.IS_BLACK_LISTED, dbo.isBlacklisted)
            cv.put(GroupColumns.IS_VERIFIED, dbo.isVerified)
            cv.put(GroupColumns.HAS_UNSEEN_STORIES, dbo.hasUnseenStories)
            cv.put(GroupColumns.IS_ADMIN, dbo.isAdmin)
            cv.put(GroupColumns.ADMIN_LEVEL, dbo.adminLevel)
            cv.put(GroupColumns.IS_MEMBER, dbo.isMember)
            cv.put(GroupColumns.MEMBER_STATUS, dbo.memberStatus)
            cv.put(GroupColumns.MEMBERS_COUNT, dbo.membersCount)
            cv.put(GroupColumns.TYPE, dbo.type)
            cv.put(GroupColumns.PHOTO_50, dbo.photo50)
            cv.put(GroupColumns.PHOTO_100, dbo.photo100)
            cv.put(GroupColumns.PHOTO_200, dbo.photo200)
            return cv
        }

        private fun createCv(dbo: UserEntity): ContentValues {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, dbo.id)
            cv.put(UserColumns.FIRST_NAME, dbo.firstName)
            cv.put(UserColumns.LAST_NAME, dbo.lastName)
            cv.put(UserColumns.ONLINE, dbo.isOnline)
            cv.put(UserColumns.ONLINE_MOBILE, dbo.isOnlineMobile)
            cv.put(UserColumns.ONLINE_APP, dbo.onlineApp)
            cv.put(UserColumns.PHOTO_50, dbo.photo50)
            cv.put(UserColumns.PHOTO_100, dbo.photo100)
            cv.put(UserColumns.PHOTO_200, dbo.photo200)
            cv.put(UserColumns.PHOTO_MAX, dbo.photoMax)
            cv.put(UserColumns.LAST_SEEN, dbo.lastSeen)
            cv.put(UserColumns.PLATFORM, dbo.platform)
            cv.put(UserColumns.USER_STATUS, dbo.status)
            cv.put(UserColumns.SEX, dbo.sex)
            cv.put(UserColumns.DOMAIN, dbo.domain)
            cv.put(UserColumns.IS_FRIEND, dbo.isFriend)
            cv.put(UserColumns.FRIEND_STATUS, dbo.friendStatus)
            cv.put(UserColumns.WRITE_MESSAGE_STATUS, dbo.canWritePrivateMessage)
            cv.put(UserColumns.BDATE, dbo.bdate)
            cv.put(UserColumns.IS_USER_BLACK_LIST, dbo.blacklisted_by_me)
            cv.put(UserColumns.IS_BLACK_LISTED, dbo.blacklisted)
            cv.put(UserColumns.IS_VERIFIED, dbo.isVerified)
            cv.put(UserColumns.HAS_UNSEEN_STORIES, dbo.hasUnseenStories)
            cv.put(UserColumns.IS_CAN_ACCESS_CLOSED, dbo.isCan_access_closed)
            cv.put(UserColumns.MAIDEN_NAME, dbo.maiden_name)
            return cv
        }

        internal fun mapCommunityDbo(cursor: Cursor): CommunityEntity {
            return CommunityEntity(cursor.getLong(BaseColumns._ID))
                .setName(cursor.getString(GroupColumns.NAME))
                .setScreenName(cursor.getString(GroupColumns.SCREEN_NAME))
                .setClosed(cursor.getInt(GroupColumns.IS_CLOSED))
                .setVerified(cursor.getBoolean(GroupColumns.IS_VERIFIED))
                .setHasUnseenStories(cursor.getBoolean(GroupColumns.HAS_UNSEEN_STORIES))
                .setBlacklisted(cursor.getBoolean(GroupColumns.IS_BLACK_LISTED))
                .setAdmin(cursor.getBoolean(GroupColumns.IS_ADMIN))
                .setAdminLevel(cursor.getInt(GroupColumns.ADMIN_LEVEL))
                .setMember(cursor.getBoolean(GroupColumns.IS_MEMBER))
                .setMemberStatus(cursor.getInt(GroupColumns.MEMBER_STATUS))
                .setMembersCount(cursor.getInt(GroupColumns.MEMBERS_COUNT))
                .setType(cursor.getInt(GroupColumns.TYPE))
                .setPhoto50(cursor.getString(GroupColumns.PHOTO_50))
                .setPhoto100(cursor.getString(GroupColumns.PHOTO_100))
                .setPhoto200(cursor.getString(GroupColumns.PHOTO_200))
        }

        internal fun mapUserDbo(cursor: Cursor): UserEntity {
            return UserEntity(cursor.getLong(BaseColumns._ID))
                .setFirstName(cursor.getString(UserColumns.FIRST_NAME))
                .setLastName(cursor.getString(UserColumns.LAST_NAME))
                .setOnline(cursor.getBoolean(UserColumns.ONLINE))
                .setOnlineMobile(cursor.getBoolean(UserColumns.ONLINE_MOBILE))
                .setOnlineApp(cursor.getInt(UserColumns.ONLINE_APP))
                .setPhoto50(cursor.getString(UserColumns.PHOTO_50))
                .setPhoto100(cursor.getString(UserColumns.PHOTO_100))
                .setPhoto200(cursor.getString(UserColumns.PHOTO_200))
                .setPhotoMax(cursor.getString(UserColumns.PHOTO_MAX))
                .setLastSeen(cursor.getLong(UserColumns.LAST_SEEN))
                .setPlatform(cursor.getInt(UserColumns.PLATFORM))
                .setStatus(cursor.getString(UserColumns.USER_STATUS))
                .setSex(cursor.getInt(UserColumns.SEX))
                .setDomain(cursor.getString(UserColumns.DOMAIN))
                .setFriend(cursor.getBoolean(UserColumns.IS_FRIEND))
                .setFriendStatus(cursor.getInt(UserColumns.FRIEND_STATUS))
                .setCanWritePrivateMessage(cursor.getBoolean(UserColumns.WRITE_MESSAGE_STATUS))
                .setBdate(cursor.getString(UserColumns.BDATE))
                .setBlacklisted_by_me(cursor.getBoolean(UserColumns.IS_USER_BLACK_LIST))
                .setBlacklisted(cursor.getBoolean(UserColumns.IS_BLACK_LISTED))
                .setVerified(cursor.getBoolean(UserColumns.IS_VERIFIED))
                .setHasUnseenStories(cursor.getBoolean(UserColumns.HAS_UNSEEN_STORIES))
                .setCan_access_closed(cursor.getBoolean(UserColumns.IS_CAN_ACCESS_CLOSED))
                .setMaiden_name(cursor.getString(UserColumns.MAIDEN_NAME))
        }
    }

}