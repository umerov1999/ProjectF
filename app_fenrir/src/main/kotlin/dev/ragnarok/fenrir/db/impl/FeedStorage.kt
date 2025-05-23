package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getFeedListsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getNewsContentUriFor
import dev.ragnarok.fenrir.db.column.FeedListsColumns
import dev.ragnarok.fenrir.db.column.FeedListsColumns.getCV
import dev.ragnarok.fenrir.db.column.NewsColumns
import dev.ragnarok.fenrir.db.column.PostsColumns
import dev.ragnarok.fenrir.db.interfaces.IFeedStorage
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.db.model.entity.FeedListEntity
import dev.ragnarok.fenrir.db.model.entity.NewsDboEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.FeedSourceCriteria
import dev.ragnarok.fenrir.model.criteria.FeedCriteria
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.join
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.msgpack.MsgPack

internal class FeedStorage(base: AppStorages) : AbsStorage(base), IFeedStorage {
    private val storeLock = Any()
    override fun findByCriteria(criteria: FeedCriteria): Flow<List<NewsDboEntity>> {
        return flow {
            val uri = getNewsContentUriFor(criteria.accountId)
            val data: MutableList<NewsDboEntity> = ArrayList()
            val cursor: Cursor? = if (criteria.range != null) {
                val range = criteria.range
                context.contentResolver.query(
                    uri,
                    null,
                    BaseColumns._ID + " >= ? AND " + BaseColumns._ID + " <= ?",
                    arrayOf(range?.first.toString(), range?.last.toString()),
                    null
                )
            } else {
                context.contentResolver.query(uri, null, null, null, null)
            }
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    data.add(mapNewsBase(cursor))
                }
                cursor.close()
            }
            emit(data)
        }
    }

    override fun store(
        accountId: Long,
        data: List<NewsDboEntity>,
        owners: OwnerEntities?,
        clearBeforeStore: Boolean
    ): Flow<IntArray> {
        return flow {
            val uri = getNewsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            if (clearBeforeStore) {
                // for performance test (before - 500-600ms, after - 200-300ms)
                //operations.add(ContentProviderOperation.newDelete(FenrirContentProvider.getNewsAttachmentsContentUriFor(accountId))
                //        .build());
                operations.add(ContentProviderOperation.newDelete(uri).build())
            }
            val indexes = IntArray(data.size)
            for (i in data.indices) {
                val dbo = data[i]
                val cv = getCV(dbo)
                val mainPostHeaderOperation = ContentProviderOperation
                    .newInsert(uri)
                    .withValues(cv)
                    .build()
                val mainPostHeaderIndex =
                    addToListAndReturnIndex(operations, mainPostHeaderOperation)
                indexes[i] = mainPostHeaderIndex
            }
            if (owners != null) {
                OwnersStorage.appendOwnersInsertOperations(operations, accountId, owners)
            }
            var results: Array<ContentProviderResult>
            synchronized(storeLock) {
                results = context.contentResolver.applyBatch(
                    FenrirContentProvider.AUTHORITY,
                    operations
                )
            }
            val ids = IntArray(data.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emit(ids)
        }
    }

    override fun storeLists(accountId: Long, entities: List<FeedListEntity>): Flow<Boolean> {
        return flow {
            val uri = getFeedListsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            operations.add(
                ContentProviderOperation.newDelete(uri)
                    .build()
            )
            for (entity in entities) {
                operations.add(
                    ContentProviderOperation.newInsert(uri)
                        .withValues(getCV(entity))
                        .build()
                )
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun getAllLists(criteria: FeedSourceCriteria): Flow<List<FeedListEntity>> {
        return flow {
            val uri = getFeedListsContentUriFor(criteria.accountId)
            val cursor = contentResolver.query(uri, null, null, null, null)
            val data: MutableList<FeedListEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    data.add(mapList(cursor))
                }
                cursor.close()
            }
            emit(data)
        }
    }

    /*private void fillAttachmentsOperations(int accountId, @NonNull VKApiAttachment attachment, @NonNull List<ContentProviderOperation> target,
                                           int parentPostHeaderOperationIndex) {
        Logger.d("fillAttachmentsOperations", "attachment: " + attachment.toAttachmentString());

        ContentValues cv = new ContentValues();
        cv.put(NewsAttachmentsColumns.TYPE, Types.from(attachment.getType()));
        cv.put(NewsAttachmentsColumns.DATA, serializeAttachment(attachment));

        target.add(ContentProviderOperation.newInsert(FenrirContentProvider.getNewsAttachmentsContentUriFor(accountId))
                .withValues(cv)
                .withValueBackReference(NewsAttachmentsColumns.N_ID, parentPostHeaderOperationIndex)
                .build());
    }*/
    private fun mapNewsBase(cursor: Cursor): NewsDboEntity {
        val friendString = cursor.getString(NewsColumns.TAG_FRIENDS)
        val dbo = NewsDboEntity()
        if (friendString.nonNullNoEmpty()) {
            val strArray = friendString.split(Regex(",")).toTypedArray()
            val longArray = ArrayList<Long>(strArray.size)
            for (i in strArray.indices) {
                longArray.add(strArray[i].toLong())
            }
            dbo.setFriendsTags(longArray)
        } else {
            dbo.setFriendsTags(null)
        }
        dbo.setType(cursor.getString(NewsColumns.TYPE))
            .setSourceId(cursor.getLong(NewsColumns.SOURCE_ID))
            .setDate(cursor.getLong(NewsColumns.DATE))
            .setPostId(cursor.getInt(NewsColumns.POST_ID))
            .setPostType(cursor.getString(NewsColumns.POST_TYPE))
            .setFinalPost(cursor.getBoolean(NewsColumns.FINAL_POST))
            .setCopyOwnerId(cursor.getLong(NewsColumns.COPY_OWNER_ID))
            .setCopyPostId(cursor.getInt(NewsColumns.COPY_POST_ID))
            .setCopyPostDate(cursor.getLong(NewsColumns.COPY_POST_DATE))
            .setText(cursor.getString(NewsColumns.TEXT))
            .setCanEdit(cursor.getBoolean(NewsColumns.CAN_EDIT))
            .setCanDelete(cursor.getBoolean(NewsColumns.CAN_DELETE))
            .setCommentCount(cursor.getInt(NewsColumns.COMMENT_COUNT))
            .setCanPostComment(cursor.getBoolean(NewsColumns.COMMENT_CAN_POST))
            .setLikesCount(cursor.getInt(NewsColumns.LIKE_COUNT))
            .setUserLikes(cursor.getBoolean(NewsColumns.USER_LIKE))
            .setCanLike(cursor.getBoolean(NewsColumns.CAN_LIKE))
            .setCanPublish(cursor.getBoolean(NewsColumns.CAN_PUBLISH))
            .setRepostCount(cursor.getInt(NewsColumns.REPOSTS_COUNT))
            .setUserReposted(cursor.getBoolean(NewsColumns.USER_REPOSTED))
            .setViews(cursor.getInt(NewsColumns.VIEWS))
            .setDonut(cursor.getBoolean(NewsColumns.IS_DONUT))
        cursor.getBlob(PostsColumns.COPYRIGHT_BLOB).nonNullNoEmpty {
            dbo.setCopyright(
                MsgPack.decodeFromByteArrayEx(
                    NewsDboEntity.CopyrightDboEntity.serializer(),
                    it
                )
            )
        }
        val attachmentsJson =
            cursor.getBlob(NewsColumns.ATTACHMENTS_BLOB)
        if (attachmentsJson.nonNullNoEmpty()) {
            val attachmentsEntity =
                MsgPack.decodeFromByteArrayEx(
                    ListSerializer(DboEntity.serializer()),
                    attachmentsJson
                )
            if (attachmentsEntity.nonNullNoEmpty()) {
                val attachmentsOnly: MutableList<DboEntity> = ArrayList(attachmentsEntity.size)
                val copiesOnly: MutableList<PostDboEntity> = ArrayList(0)
                for (a in attachmentsEntity) {
                    if (a is PostDboEntity) {
                        copiesOnly.add(a)
                    } else {
                        attachmentsOnly.add(a)
                    }
                }
                dbo.setAttachments(attachmentsOnly)
                dbo.setCopyHistory(copiesOnly)
            } else {
                dbo.setAttachments(null)
                dbo.setCopyHistory(null)
            }
        } else {
            dbo.setAttachments(null)
            dbo.setCopyHistory(null)
        }
        return dbo
    }

    companion object {
        fun getCV(dbo: NewsDboEntity): ContentValues {
            val cv = ContentValues()
            cv.put(NewsColumns.TYPE, dbo.type)
            cv.put(NewsColumns.SOURCE_ID, dbo.sourceId)
            cv.put(NewsColumns.DATE, dbo.date)
            cv.put(NewsColumns.POST_ID, dbo.postId)
            cv.put(NewsColumns.POST_TYPE, dbo.postType)
            cv.put(NewsColumns.FINAL_POST, dbo.isFinalPost)
            cv.put(NewsColumns.COPY_OWNER_ID, dbo.copyOwnerId)
            cv.put(NewsColumns.COPY_POST_ID, dbo.copyPostId)
            cv.put(NewsColumns.COPY_POST_DATE, dbo.copyPostDate)
            cv.put(NewsColumns.TEXT, dbo.text)
            cv.put(NewsColumns.CAN_EDIT, dbo.isCanEdit)
            cv.put(NewsColumns.CAN_DELETE, dbo.isCanDelete)
            cv.put(NewsColumns.COMMENT_COUNT, dbo.commentCount)
            cv.put(NewsColumns.COMMENT_CAN_POST, dbo.isCanPostComment)
            cv.put(NewsColumns.LIKE_COUNT, dbo.likesCount)
            cv.put(NewsColumns.USER_LIKE, dbo.isUserLikes)
            cv.put(NewsColumns.CAN_LIKE, dbo.isCanLike)
            cv.put(NewsColumns.CAN_PUBLISH, dbo.isCanPublish)
            cv.put(NewsColumns.REPOSTS_COUNT, dbo.repostCount)
            cv.put(NewsColumns.USER_REPOSTED, dbo.isUserReposted)
            cv.put(NewsColumns.IS_DONUT, dbo.isDonut)
            cv.put(
                NewsColumns.TAG_FRIENDS,
                dbo.friendsTags?.let { join(",", it) }
            )
            dbo.copyright.ifNonNull({
                cv.put(
                    NewsColumns.COPYRIGHT_BLOB,
                    MsgPack.encodeToByteArrayEx(NewsDboEntity.CopyrightDboEntity.serializer(), it)
                )
            }, {
                cv.putNull(NewsColumns.COPYRIGHT_BLOB)
            })
            cv.put(NewsColumns.VIEWS, dbo.views)
            if (dbo.copyHistory.nonNullNoEmpty() || dbo.attachments.nonNullNoEmpty()) {
                val attachmentsEntities: MutableList<DboEntity> = ArrayList()
                dbo.attachments.nonNullNoEmpty {
                    attachmentsEntities.addAll(it)
                }
                dbo.copyHistory.nonNullNoEmpty {
                    attachmentsEntities.addAll(it)
                }
                if (attachmentsEntities.nonNullNoEmpty()) {
                    cv.put(
                        NewsColumns.ATTACHMENTS_BLOB,
                        MsgPack.encodeToByteArrayEx(
                            ListSerializer(DboEntity.serializer()),
                            attachmentsEntities
                        )
                    )
                } else {
                    cv.putNull(NewsColumns.ATTACHMENTS_BLOB)
                }
            }
            return cv
        }

        internal fun mapList(cursor: Cursor): FeedListEntity {
            val id = cursor.getInt(BaseColumns._ID)
            val title = cursor.getString(FeedListsColumns.TITLE)
            val entity = FeedListEntity(id).setTitle(title)
            val sources =
                cursor.getString(FeedListsColumns.SOURCE_IDS)
            var sourceIds: LongArray? = null
            if (sources.nonNullNoEmpty()) {
                val ids = sources.split(Regex(",")).toTypedArray()
                sourceIds = LongArray(ids.size)
                for (i in ids.indices) {
                    sourceIds[i] = ids[i].toLong()
                }
            }
            return entity.setSourceIds(sourceIds)
                .setNoReposts(cursor.getBoolean(FeedListsColumns.NO_REPOSTS))
        }
    }
}