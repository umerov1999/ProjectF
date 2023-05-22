package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFriendListsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getRelativeshipContentUriFor
import dev.ragnarok.fenrir.db.column.FriendListsColumns
import dev.ragnarok.fenrir.db.column.RelationshipsColumns
import dev.ragnarok.fenrir.db.column.RelationshipsColumns.getCV
import dev.ragnarok.fenrir.db.impl.OwnersStorage.Companion.appendCommunitiesInsertOperation
import dev.ragnarok.fenrir.db.impl.OwnersStorage.Companion.appendUsersInsertOperation
import dev.ragnarok.fenrir.db.interfaces.IRelativeshipStorage
import dev.ragnarok.fenrir.db.model.entity.CommunityEntity
import dev.ragnarok.fenrir.db.model.entity.FriendListEntity
import dev.ragnarok.fenrir.db.model.entity.UserEntity
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import kotlin.math.abs

internal class RelativeshipStorage(base: AppStorages) : AbsStorage(base), IRelativeshipStorage {
    override fun storeFriendsList(
        accountId: Long,
        userId: Long,
        data: Collection<FriendListEntity>
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val uri = getFriendListsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>(data.size)
            operations.add(
                ContentProviderOperation.newDelete(uri)
                    .withSelection(
                        FriendListsColumns.FULL_USER_ID + " = ?",
                        arrayOf(userId.toString())
                    )
                    .build()
            )
            for (item in data) {
                val cv = ContentValues()
                cv.put(FriendListsColumns.LIST_ID, item.id)
                cv.put(FriendListsColumns.NAME, item.name)
                operations.add(
                    ContentProviderOperation.newInsert(uri)
                        .withValues(cv)
                        .build()
                )
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            e.onComplete()
        }
    }

    override fun storeFriends(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Completable {
        return completableStoreForType(
            accountId,
            users,
            objectId,
            RelationshipsColumns.TYPE_FRIEND,
            clearBeforeStore
        )
    }

    private fun completableStoreForType(
        accountId: Long,
        userEntities: List<UserEntity>,
        objectId: Long,
        relationType: Int,
        clear: Boolean
    ): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            val uri = getRelativeshipContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clear) {
                operations.add(clearOperationFor(accountId, objectId, relationType))
            }
            appendInsertHeaders(uri, operations, objectId, userEntities, relationType)
            appendUsersInsertOperation(operations, accountId, userEntities)
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emitter.onComplete()
        }
    }

    override fun storeFollowers(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Completable {
        return completableStoreForType(
            accountId,
            users,
            objectId,
            RelationshipsColumns.TYPE_FOLLOWER,
            clearBeforeStore
        )
    }

    override fun storeRequests(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Completable {
        return completableStoreForType(
            accountId,
            users,
            objectId,
            RelationshipsColumns.TYPE_REQUESTS,
            clearBeforeStore
        )
    }

    override fun storeGroupMembers(
        accountId: Long,
        users: List<UserEntity>,
        objectId: Long,
        clearBeforeStore: Boolean
    ): Completable {
        return completableStoreForType(
            accountId,
            users,
            objectId,
            RelationshipsColumns.TYPE_GROUP_MEMBER,
            clearBeforeStore
        )
    }

    override fun getGroupMembers(accountId: Long, groupId: Long): Single<List<UserEntity>> {
        return getUsersForType(accountId, groupId, RelationshipsColumns.TYPE_GROUP_MEMBER)
    }

    override fun getFriends(accountId: Long, objectId: Long): Single<List<UserEntity>> {
        return getUsersForType(accountId, objectId, RelationshipsColumns.TYPE_FRIEND)
    }

    override fun getFollowers(accountId: Long, objectId: Long): Single<List<UserEntity>> {
        return getUsersForType(accountId, objectId, RelationshipsColumns.TYPE_FOLLOWER)
    }

    override fun getRequests(accountId: Long): Single<List<UserEntity>> {
        return getUsersForType(accountId, accountId, RelationshipsColumns.TYPE_REQUESTS)
    }

    override fun getCommunities(accountId: Long, ownerId: Long): Single<List<CommunityEntity>> {
        return Single.create { emitter: SingleEmitter<List<CommunityEntity>> ->
            val cursor = getCursorForType(accountId, ownerId, RelationshipsColumns.TYPE_MEMBER)
            val dbos: MutableList<CommunityEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    dbos.add(mapCommunity(cursor))
                }
                cursor.close()
            }
            emitter.onSuccess(dbos)
        }
    }

    override fun storeComminities(
        accountId: Long,
        communities: List<CommunityEntity>,
        userId: Long,
        invalidateBefore: Boolean
    ): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            val uri = getRelativeshipContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>(communities.size * 2 + 1)
            if (invalidateBefore) {
                operations.add(
                    clearOperationFor(
                        accountId,
                        userId,
                        RelationshipsColumns.TYPE_MEMBER
                    )
                )
            }
            for (dbo in communities) {
                operations.add(
                    ContentProviderOperation.newInsert(uri)
                        .withValues(getCV(userId, -dbo.id, RelationshipsColumns.TYPE_MEMBER))
                        .build()
                )
            }
            appendCommunitiesInsertOperation(operations, accountId, communities)
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emitter.onComplete()
        }
    }

    private fun getCursorForType(accountId: Long, objectId: Long, relationType: Int): Cursor? {
        val uri = getRelativeshipContentUriFor(accountId)
        val where =
            RelationshipsColumns.FULL_TYPE + " = ? AND " + RelationshipsColumns.OBJECT_ID + " = ?"
        val args = arrayOf(relationType.toString(), objectId.toString())
        return contentResolver.query(uri, null, where, args, null)
    }

    private fun getUsersForType(
        accountId: Long,
        objectId: Long,
        relationType: Int
    ): Single<List<UserEntity>> {
        return Single.create { emitter: SingleEmitter<List<UserEntity>> ->
            val cursor = getCursorForType(accountId, objectId, relationType)
            val dbos: MutableList<UserEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    dbos.add(mapDbo(cursor))
                }
                cursor.close()
            }
            emitter.onSuccess(dbos)
        }
    }

    private fun appendInsertHeaders(
        uri: Uri,
        operations: MutableList<ContentProviderOperation>,
        objectId: Long,
        dbos: List<UserEntity>,
        type: Int
    ) {
        for (dbo in dbos) {
            operations.add(
                ContentProviderOperation
                    .newInsert(uri)
                    .withValues(getCV(objectId, dbo.id, type))
                    .build()
            )
        }
    }

    private fun clearOperationFor(
        accountId: Long,
        objectId: Long,
        type: Int
    ): ContentProviderOperation {
        val uri = getRelativeshipContentUriFor(accountId)
        val clearWhere =
            RelationshipsColumns.OBJECT_ID + " = ? AND " + RelationshipsColumns.TYPE + " = ?"
        val clearWhereArgs = arrayOf(objectId.toString(), type.toString())
        return ContentProviderOperation
            .newDelete(uri)
            .withSelection(clearWhere, clearWhereArgs)
            .build()
    }

    companion object {
        internal fun mapCommunity(cursor: Cursor): CommunityEntity {
            return CommunityEntity(cursor.getLong(RelationshipsColumns.SUBJECT_ID))
                .setName(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_NAME))
                .setScreenName(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_SCREEN_NAME))
                .setClosed(cursor.getInt(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_IS_CLOSED))
                .setBlacklisted(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_IS_BLACK_LISTED))
                .setVerified(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_IS_VERIFIED))
                .setAdmin(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_IS_ADMIN))
                .setAdminLevel(cursor.getInt(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_ADMIN_LEVEL))
                .setMember(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_IS_MEMBER))
                .setMemberStatus(cursor.getInt(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_MEMBER_STATUS))
                .setMembersCount(cursor.getInt(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_MEMBERS_COUNT))
                .setType(cursor.getInt(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_TYPE))
                .setHasUnseenStories(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_HAS_UNSEEN_STORIES))
                .setPhoto50(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_PHOTO_50))
                .setPhoto100(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_PHOTO_100))
                .setPhoto200(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_GROUP_PHOTO_200))
        }

        internal fun mapDbo(cursor: Cursor): UserEntity {
            val gid =
                abs(cursor.getLong(RelationshipsColumns.SUBJECT_ID))
            return UserEntity(gid)
                .setFirstName(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_USER_FIRST_NAME))
                .setLastName(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_USER_LAST_NAME))
                .setOnline(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_USER_ONLINE))
                .setOnlineMobile(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_USER_ONLINE))
                .setOnlineApp(cursor.getInt(RelationshipsColumns.FOREIGN_SUBJECT_USER_ONLINE_APP))
                .setPhoto50(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_USER_PHOTO_50))
                .setPhoto100(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_USER_PHOTO_100))
                .setPhoto200(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_USER_PHOTO_200))
                .setPhotoMax(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_USER_PHOTO_MAX))
                .setLastSeen(cursor.getLong(RelationshipsColumns.FOREIGN_SUBJECT_USER_LAST_SEEN))
                .setPlatform(cursor.getInt(RelationshipsColumns.FOREIGN_SUBJECT_USER_PLATFORM))
                .setStatus(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_USER_STATUS))
                .setSex(cursor.getInt(RelationshipsColumns.FOREIGN_SUBJECT_USER_SEX))
                .setFriend(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_USER_IS_FRIEND))
                .setFriendStatus(cursor.getInt(RelationshipsColumns.FOREIGN_SUBJECT_USER_FRIEND_STATUS))
                .setCanWritePrivateMessage(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_USER_WRITE_MESSAGE_STATUS))
                .setBdate(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_USER_BDATE))
                .setBlacklisted_by_me(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_USER_IS_USER_BLACK_LIST))
                .setBlacklisted(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_USER_IS_BLACK_LISTED))
                .setCan_access_closed(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_USER_IS_CAN_ACCESS_CLOSED))
                .setVerified(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_USER_IS_VERIFIED))
                .setMaiden_name(cursor.getString(RelationshipsColumns.FOREIGN_SUBJECT_USER_MAIDEN_NAME))
                .setHasUnseenStories(cursor.getBoolean(RelationshipsColumns.FOREIGN_SUBJECT_USER_HAS_UNSEEN_STORIES))
        }
    }
}