package dev.ragnarok.fenrir.db.impl

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFriendListsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getGroupsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getGroupsDetContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getUserContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getUserDetContentUriFor
import dev.ragnarok.fenrir.db.column.FriendListsColumns
import dev.ragnarok.fenrir.db.column.GroupsColumns
import dev.ragnarok.fenrir.db.column.GroupsDetailsColumns
import dev.ragnarok.fenrir.db.column.UsersColumns
import dev.ragnarok.fenrir.db.column.UsersDetailsColumns
import dev.ragnarok.fenrir.db.interfaces.IOwnersStorage
import dev.ragnarok.fenrir.db.model.BanAction
import dev.ragnarok.fenrir.db.model.UserPatch
import dev.ragnarok.fenrir.db.model.entity.CommunityDetailsEntity
import dev.ragnarok.fenrir.db.model.entity.CommunityEntity
import dev.ragnarok.fenrir.db.model.entity.FriendListEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.UserDetailsEntity
import dev.ragnarok.fenrir.db.model.entity.UserEntity
import dev.ragnarok.fenrir.domain.mappers.Entity2Model
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.UserInfoResolveUtil.getUserActivityLine
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyListFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.msgpack.MsgPack

internal class OwnersStorage(context: AppStorages) : AbsStorage(context), IOwnersStorage {
    private val banActionsPublisher = createPublishSubject<BanAction>()
    private val managementActionsPublisher = createPublishSubject<Pair<Long, Manager>>()

    override fun fireBanAction(action: BanAction): Flow<Boolean> {
        return flow {
            banActionsPublisher.emit(action)
            emit(true)
        }
    }

    override fun observeBanActions(): SharedFlow<BanAction> {
        return banActionsPublisher
    }

    override fun fireManagementChangeAction(manager: Pair<Long, Manager>): Flow<Boolean> {
        return flow {
            managementActionsPublisher.emit(manager)
            emit(true)
        }
    }

    override fun observeManagementChanges(): SharedFlow<Pair<Long, Manager>> {
        return managementActionsPublisher
    }

    override fun getUserDetails(
        accountId: Long,
        userId: Long
    ): Flow<Optional<UserDetailsEntity>> {
        return flow {
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
            emit(wrap(details))
        }
    }

    override fun getGroupsDetails(
        accountId: Long,
        groupId: Long
    ): Flow<Optional<CommunityDetailsEntity>> {
        return flow {
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
            emit(wrap(details))
        }
    }

    override fun storeGroupsDetails(
        accountId: Long,
        groupId: Long,
        dbo: CommunityDetailsEntity
    ): Flow<Boolean> {
        return flow {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, groupId)
            cv.put(
                GroupsDetailsColumns.DATA,
                MsgPack.encodeToByteArrayEx(CommunityDetailsEntity.serializer(), dbo)
            )
            val uri = getGroupsDetContentUriFor(accountId)
            contentResolver.insert(uri, cv)
            emit(true)
        }
    }

    override fun storeUserDetails(
        accountId: Long,
        userId: Long,
        dbo: UserDetailsEntity
    ): Flow<Boolean> {
        return flow {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, userId)
            cv.put(
                UsersDetailsColumns.DATA,
                MsgPack.encodeToByteArrayEx(UserDetailsEntity.serializer(), dbo)
            )
            val uri = getUserDetContentUriFor(accountId)
            contentResolver.insert(uri, cv)
            emit(true)
        }
    }

    override fun applyPathes(accountId: Long, patches: List<UserPatch>): Flow<Boolean> {
        return if (patches.isEmpty()) {
            emptyTaskFlow()
        } else flow {
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
            emit(true)
        }
    }

    override fun findFriendsListsByIds(
        accountId: Long,
        userId: Long,
        ids: Collection<Long>
    ): Flow<MutableMap<Long, FriendListEntity>> {
        return flow {
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
                    if (!isActive()) {
                        break
                    }
                    val dbo = mapFriendsList(cursor)
                    map[dbo.id] = dbo
                }
                cursor.close()
            }
            emit(map)
        }
    }

    override fun getLocalizedUserActivity(accountId: Long, userId: Long): Flow<String?> {
        return flow {
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
                    emit(userActivityLine)
                    return@flow
                }
                cursor.close()
            }
            emit(null)
        }
    }

    override fun findUserDboById(accountId: Long, ownerId: Long): Flow<Optional<UserEntity>> {
        return flow {
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
            emit(wrap(dbo))
        }
    }

    override fun findCommunityDboById(
        accountId: Long,
        ownerId: Long
    ): Flow<Optional<CommunityEntity>> {
        return flow {
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
            emit(wrap(dbo))
        }
    }

    override fun findUserByDomain(accountId: Long, domain: String?): Flow<Optional<UserEntity>> {
        return flow {
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
            emit(wrap(entity))
        }
    }

    override fun findFriendBirtday(accountId: Long): Flow<List<User>> {
        return flow {
            val uri = getUserContentUriFor(accountId)
            val where = UsersColumns.BDATE + " IS NOT NULL AND " + UsersColumns.IS_FRIEND + " = 1"
            val cursor = contentResolver.query(uri, null, where, null, UsersColumns.BDATE + " DESC")
            val listEntity: ArrayList<User> = ArrayList()
            while (cursor?.moveToNext() == true) {
                Entity2Model.map(mapUserDbo(cursor))?.let { it1 -> listEntity.add(it1) }
            }
            cursor?.close()
            emit(listEntity)
        }
    }

    override fun findCommunityByDomain(
        accountId: Long,
        domain: String?
    ): Flow<Optional<CommunityEntity>> {
        return flow {
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
            emit(wrap(entity))
        }
    }

    override fun findUserDbosByIds(accountId: Long, ids: List<Long>): Flow<List<UserEntity>> {
        return if (ids.isEmpty()) {
            emptyListFlow()
        } else flow {
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
                    if (!isActive()) {
                        break
                    }
                    dbos.add(mapUserDbo(cursor))
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    override fun findCommunityDbosByIds(
        accountId: Long,
        ids: List<Long>
    ): Flow<List<CommunityEntity>> {
        return if (ids.isEmpty()) {
            emptyListFlow()
        } else flow {
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
                    if (!isActive()) {
                        break
                    }
                    dbos.add(mapCommunityDbo(cursor))
                }
                cursor.close()
            }
            emit(dbos)
        }
    }

    override fun storeUserDbos(accountId: Long, users: List<UserEntity>): Flow<Boolean> {
        return flow {
            val operations = ArrayList<ContentProviderOperation>(users.size)
            appendUsersInsertOperation(operations, accountId, users)
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun storeOwnerEntities(accountId: Long, entities: OwnerEntities?): Flow<Boolean> {
        return flow {
            if (entities == null) {
                emit(false)
            } else {
                val operations = ArrayList<ContentProviderOperation>(
                    entities.size()
                )
                appendUsersInsertOperation(operations, accountId, entities.userEntities)
                appendCommunitiesInsertOperation(operations, accountId, entities.communityEntities)
                contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
                emit(true)
            }
        }
    }

    override fun storeCommunityDbos(
        accountId: Long,
        communityEntities: List<CommunityEntity>
    ): Flow<Boolean> {
        return flow {
            val operations = ArrayList<ContentProviderOperation>(communityEntities.size)
            appendCommunitiesInsertOperation(operations, accountId, communityEntities)
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun getMissingUserIds(
        accountId: Long,
        ids: Collection<Long>
    ): Flow<Collection<Long>> {
        return if (ids.isEmpty()) {
            emptyListFlow()
        } else {
            flow {
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
                emit(copy)
            }
        }
    }

    override fun getMissingCommunityIds(
        accountId: Long,
        ids: Collection<Long>
    ): Flow<Collection<Long>> {
        return if (ids.isEmpty()) {
            emptyListFlow()
        } else {
            flow {
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
                emit(copy)
            }
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
