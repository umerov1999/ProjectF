package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getPostsContentUriFor
import dev.ragnarok.fenrir.db.column.PostsColumns
import dev.ragnarok.fenrir.db.impl.AttachmentsStorage.Companion.appendAttachOperationWithBackReference
import dev.ragnarok.fenrir.db.impl.OwnersStorage.Companion.appendOwnersInsertOperations
import dev.ragnarok.fenrir.db.interfaces.Cancelable
import dev.ragnarok.fenrir.db.interfaces.IWallStorage
import dev.ragnarok.fenrir.db.interfaces.IWallStorage.IClearWallTask
import dev.ragnarok.fenrir.db.model.PostPatch
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity.SourceDbo
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

internal class WallStorage(base: AppStorages) : AbsStorage(base), IWallStorage {
    override fun storeWallEntities(
        accountId: Int, posts: List<PostDboEntity>,
        owners: OwnerEntities?, clearWall: IClearWallTask?
    ): Single<IntArray> {
        return Single.create { emitter: SingleEmitter<IntArray> ->
            val operations = ArrayList<ContentProviderOperation>()
            if (clearWall != null) {
                operations.add(operationForClearWall(accountId, clearWall.ownerId))
            }
            val indexes = IntArray(posts.size)
            for (i in posts.indices) {
                val dbo = posts[i]
                val cv = createCv(dbo)
                val mainPostHeaderOperation = ContentProviderOperation
                    .newInsert(getPostsContentUriFor(accountId))
                    .withValues(cv)
                    .build()
                val mainPostHeaderIndex =
                    addToListAndReturnIndex(operations, mainPostHeaderOperation)
                indexes[i] = mainPostHeaderIndex
                appendDboAttachmentsAndCopies(dbo, operations, accountId, mainPostHeaderIndex)
            }
            if (owners != null) {
                appendOwnersInsertOperations(operations, accountId, owners)
            }
            val results =
                context.contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val ids = IntArray(posts.size)
            for (i in indexes.indices) {
                val index = indexes[i]
                val result = results[index]
                ids[i] = extractId(result)
            }
            emitter.onSuccess(ids)
        }
    }

    override fun replacePost(accountId: Int, post: PostDboEntity): Single<Int> {
        return Single.create { e: SingleEmitter<Int> ->
            val uri = getPostsContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>()
            val cv = createCv(post)
            if (post.dbid > 0) {
                cv.put(BaseColumns._ID, post.dbid)

                // если пост был сохранен ранее - удаляем старые данные
                // и сохраняем заново с тем же _ID
                operations.add(
                    ContentProviderOperation.newDelete(uri)
                        .withSelection(BaseColumns._ID + " = ?", arrayOf(post.dbid.toString()))
                        .build()
                )
            }
            val main = ContentProviderOperation.newInsert(uri)
                .withValues(cv)
                .build()
            val mainPostIndex = addToListAndReturnIndex(operations, main)
            appendDboAttachmentsAndCopies(post, operations, accountId, mainPostIndex)
            val results =
                context.contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val dbid = extractId(results[mainPostIndex])
            e.onSuccess(dbid)
        }
    }

    private fun insertNew(accountId: Int, vkId: Int, ownerId: Int, authorId: Int): Single<Int> {
        return Single.fromCallable {
            val uri = getPostsContentUriFor(accountId)
            val cv = ContentValues()
            cv.put(PostsColumns.POST_ID, vkId)
            cv.put(PostsColumns.OWNER_ID, ownerId)
            cv.put(PostsColumns.FROM_ID, authorId)
            val resultUri = contentResolver.insert(uri, cv)
            resultUri?.lastPathSegment?.toInt().orZero()
        }
    }

    override fun getEditingPost(
        accountId: Int,
        ownerId: Int,
        @EditingPostType type: Int,
        includeAttachment: Boolean
    ): Single<PostDboEntity> {
        val vkPostId = getVkPostIdForEditingType(type)
        return findPostById(accountId, ownerId, vkPostId, includeAttachment)
            .flatMap {
                if (it.nonEmpty()) {
                    return@flatMap Single.just(it.requireNonEmpty())
                }
                insertNew(accountId, vkPostId, ownerId, accountId)
                    .flatMap {
                        findPostById(accountId, ownerId, vkPostId, includeAttachment)
                            .map { obj -> obj.requireNonEmpty() }
                    }
            }
    }

    override fun deletePost(accountId: Int, dbid: Int): Completable {
        return Completable.create { e: CompletableEmitter ->
            contentResolver.delete(
                getPostsContentUriFor(accountId),
                BaseColumns._ID + " = ?", arrayOf(dbid.toString())
            )
            e.onComplete()
        }
    }

    override fun findPostById(accountId: Int, dbid: Int): Single<Optional<PostDboEntity>> {
        return Single.create { e: SingleEmitter<Optional<PostDboEntity>> ->
            val cancelable = object : Cancelable {
                override val isOperationCancelled: Boolean
                    get() = e.isDisposed
            }
            val uri = getPostsContentUriFor(accountId)
            val where = BaseColumns._ID + " = ?"
            val args = arrayOf(dbid.toString())
            val cursor = contentResolver.query(uri, null, where, args, null)
            var dbo: PostDboEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    dbo = mapDbo(
                        accountId, cursor,
                        includeAttachments = true,
                        forceAttachments = true,
                        cancelable = cancelable
                    )
                }
                cursor.close()
            }
            e.onSuccess(wrap(dbo))
        }
    }

    override fun findPostById(
        accountId: Int,
        ownerId: Int,
        vkpostId: Int,
        includeAttachment: Boolean
    ): Single<Optional<PostDboEntity>> {
        return Single.create { e: SingleEmitter<Optional<PostDboEntity>> ->
            val uri = getPostsContentUriFor(accountId)
            val cursor = contentResolver.query(
                uri,
                null,
                PostsColumns.OWNER_ID + " = ? AND " + PostsColumns.POST_ID + " = ?",
                arrayOf(ownerId.toString(), vkpostId.toString()),
                null
            )
            var dbo: PostDboEntity? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    dbo = mapDbo(
                        accountId,
                        cursor,
                        includeAttachment,
                        includeAttachment,
                        object : Cancelable {
                            override val isOperationCancelled: Boolean
                                get() = e.isDisposed
                        })
                }
                cursor.close()
            }
            e.onSuccess(wrap(dbo))
        }
    }

    override fun findDbosByCriteria(criteria: WallCriteria): Single<List<PostDboEntity>> {
        return Single.create { emitter: SingleEmitter<List<PostDboEntity>> ->
            val accountId = criteria.accountId
            val cursor = buildCursor(criteria)
            val cancelable = object : Cancelable {
                override val isOperationCancelled: Boolean
                    get() = emitter.isDisposed
            }
            val dbos: MutableList<PostDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    dbos.add(
                        mapDbo(
                            accountId, cursor,
                            includeAttachments = true,
                            forceAttachments = false,
                            cancelable = cancelable
                        )
                    )
                }
                cursor.close()
            }
            emitter.onSuccess(dbos)
        }
    }

    override fun update(accountId: Int, ownerId: Int, postId: Int, update: PostPatch): Completable {
        return Completable.create { e: CompletableEmitter ->
            val cv = ContentValues()
            update.deletePatch.requireNonNull {
                cv.put(PostsColumns.DELETED, it.isDeleted)
            }
            update.pinPatch.requireNonNull {
                cv.put(PostsColumns.IS_PINNED, it.isPinned)
            }
            update.likePatch.requireNonNull {
                cv.put(PostsColumns.LIKES_COUNT, it.count)
                cv.put(PostsColumns.USER_LIKES, it.isLiked)
            }
            val uri = getPostsContentUriFor(accountId)
            contentResolver.update(
                uri,
                cv,
                PostsColumns.POST_ID + " = ? AND " + PostsColumns.OWNER_ID + " = ?",
                arrayOf(postId.toString(), ownerId.toString())
            )
            e.onComplete()
        }
    }

    override fun invalidatePost(accountId: Int, postVkid: Int, postOwnerId: Int): Completable {
        return Completable.fromAction {
            val uri = getPostsContentUriFor(accountId)
            val where = PostsColumns.POST_ID + " = ? AND " + PostsColumns.OWNER_ID + " = ?"
            val args = arrayOf(postVkid.toString(), postOwnerId.toString())
            contentResolver.delete(uri, where, args)
        }
    }

    private fun buildCursor(criteria: WallCriteria): Cursor? {
        // не грузить посты, которые находятся в редактировании
        // или являються копией других постов
        var where = PostsColumns.POST_ID + " != " + DRAFT_POST_ID +
                " AND " + PostsColumns.POST_ID + " != " + TEMP_POST_ID +
                " AND " + PostsColumns.OWNER_ID + " = " + criteria.ownerId
        if (criteria.range != null) {
            where = where +
                    " AND " + BaseColumns._ID + " <= " + criteria.range?.last +
                    " AND " + BaseColumns._ID + " >= " + criteria.range?.first
        }
        when (criteria.mode) {
            WallCriteria.MODE_ALL ->                 // Загружаем все посты, кроме отложенных и предлагаемых
                where =
                    where + " AND " + PostsColumns.POST_TYPE + " NOT IN (" + VKApiPost.Type.POSTPONE + ", " + VKApiPost.Type.SUGGEST + ") "
            WallCriteria.MODE_OWNER -> where = where +
                    " AND " + PostsColumns.FROM_ID + " = " + criteria.ownerId +
                    " AND " + PostsColumns.POST_TYPE + " NOT IN (" + VKApiPost.Type.POSTPONE + ", " + VKApiPost.Type.SUGGEST + ") "
            WallCriteria.MODE_SCHEDULED -> where =
                where + " AND " + PostsColumns.POST_TYPE + " = " + VKApiPost.Type.POSTPONE
            WallCriteria.MODE_SUGGEST -> where =
                where + " AND " + PostsColumns.POST_TYPE + " = " + VKApiPost.Type.SUGGEST
            WallCriteria.MODE_DONUT -> where =
                where + " AND " + PostsColumns.POST_TYPE + " = " + VKApiPost.Type.DONUT
        }
        return contentResolver.query(
            getPostsContentUriFor(criteria.accountId), null, where, null,
            PostsColumns.IS_PINNED + " DESC, " + PostsColumns.POST_ID + " DESC"
        )
    }

    private fun mapDbo(
        accountId: Int,
        cursor: Cursor,
        includeAttachments: Boolean,
        forceAttachments: Boolean,
        cancelable: Cancelable
    ): PostDboEntity {
        val dbid = cursor.getInt(BaseColumns._ID)
        val attachmentsCount =
            cursor.getInt(PostsColumns.ATTACHMENTS_COUNT)
        val postId = cursor.getInt(PostsColumns.POST_ID)
        val ownerId = cursor.getInt(PostsColumns.OWNER_ID)
        val dbo = PostDboEntity().set(postId, ownerId)
            .setDbid(dbid)
            .setFromId(cursor.getInt(PostsColumns.FROM_ID))
            .setDate(cursor.getLong(PostsColumns.DATE))
            .setText(cursor.getString(PostsColumns.TEXT))
            .setReplyOwnerId(cursor.getInt(PostsColumns.REPLY_OWNER_ID))
            .setReplyPostId(cursor.getInt(PostsColumns.REPLY_POST_ID))
            .setFriendsOnly(cursor.getBoolean(PostsColumns.FRIENDS_ONLY))
            .setCommentsCount(cursor.getInt(PostsColumns.COMMENTS_COUNT))
            .setCanPostComment(cursor.getBoolean(PostsColumns.CAN_POST_COMMENT))
            .setLikesCount(cursor.getInt(PostsColumns.LIKES_COUNT))
            .setCanLike(cursor.getBoolean(PostsColumns.CAN_LIKE))
            .setUserLikes(cursor.getBoolean(PostsColumns.USER_LIKES))
            .setRepostCount(cursor.getInt(PostsColumns.REPOSTS_COUNT))
            .setCanPublish(cursor.getBoolean(PostsColumns.CAN_PUBLISH))
            .setUserReposted(cursor.getBoolean(PostsColumns.USER_REPOSTED))
            .setPostType(cursor.getInt(PostsColumns.POST_TYPE))
            .setSignedId(cursor.getInt(PostsColumns.SIGNED_ID))
            .setCreatedBy(cursor.getInt(PostsColumns.CREATED_BY))
            .setCanPin(cursor.getBoolean(PostsColumns.CAN_PIN))
            .setPinned(cursor.getBoolean(PostsColumns.IS_PINNED))
            .setDeleted(cursor.getBoolean(PostsColumns.DELETED))
            .setViews(cursor.getInt(PostsColumns.VIEWS))
            .setCanEdit(cursor.getBoolean(PostsColumns.CAN_EDIT))
            .setFavorite(cursor.getBoolean(PostsColumns.IS_FAVORITE))
        val postSourceText =
            cursor.getBlob(PostsColumns.POST_SOURCE)
        if (postSourceText.nonNullNoEmpty()) {
            dbo.setSource(MsgPack.decodeFromByteArrayEx(SourceDbo.serializer(), postSourceText))
        }
        cursor.getBlob(PostsColumns.COPYRIGHT_BLOB).nonNullNoEmpty {
            dbo.setCopyright(
                MsgPack.decodeFromByteArrayEx(
                    PostDboEntity.CopyrightDboEntity.serializer(),
                    it
                )
            )
        }
        val copiesDbos: MutableList<PostDboEntity> = ArrayList(0)
        if (includeAttachments && (attachmentsCount > 0 || forceAttachments)) {
            val attachments: MutableList<DboEntity> = stores
                .attachments()
                .getAttachmentsDbosSync(accountId, AttachToType.POST, dbid, cancelable)

            // Так как история репостов хранится вместе с вложениями,
            // в этом месте пересохраняем эту историю в другой список
            val iterator = attachments.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (next is PostDboEntity) {
                    copiesDbos.add(next)
                    iterator.remove()
                }
            }
            dbo.setAttachments(attachments)
        } else {
            dbo.setAttachments(null)
        }
        dbo.setCopyHierarchy(copiesDbos)
        return dbo
    }

    private fun operationForClearWall(accountId: Int, ownerId: Int): ContentProviderOperation {
        val where = PostsColumns.OWNER_ID + " = ? " +
                " AND " + PostsColumns.POST_ID + " != ? " +
                " AND " + PostsColumns.POST_ID + " != ?"
        val args = arrayOf(ownerId.toString(), DRAFT_POST_ID.toString(), TEMP_POST_ID.toString())
        val uri = getPostsContentUriFor(accountId)
        return ContentProviderOperation
            .newDelete(uri)
            .withSelection(where, args)
            .build()
    }

    companion object {
        /**
         * Идентификатор для сохранения "черновиков постов"
         */
        private const val DRAFT_POST_ID = -1

        /**
         * Идентификатор для сохранения временных постов, репостов, шаринга и прочего
         */
        private const val TEMP_POST_ID = -2
        internal fun appendDboAttachmentsAndCopies(
            dbo: PostDboEntity, operations: MutableList<ContentProviderOperation>,
            accountId: Int, mainPostHeaderIndex: Int
        ) {
            dbo.getAttachments().nonNullNoEmpty {
                for (attachmentEntity in it) {
                    appendAttachOperationWithBackReference(
                        operations,
                        accountId,
                        AttachToType.POST,
                        mainPostHeaderIndex,
                        attachmentEntity
                    )
                }
            }
            dbo.copyHierarchy.nonNullNoEmpty {
                for (copyDbo in it) {
                    appendAttachOperationWithBackReference(
                        operations,
                        accountId,
                        AttachToType.POST,
                        mainPostHeaderIndex,
                        copyDbo
                    )
                }
            }
        }

        internal fun createCv(dbo: PostDboEntity): ContentValues {
            val cv = ContentValues()
            cv.put(PostsColumns.POST_ID, dbo.id)
            cv.put(PostsColumns.OWNER_ID, dbo.ownerId)
            cv.put(PostsColumns.FROM_ID, dbo.fromId)
            cv.put(PostsColumns.DATE, dbo.date)
            cv.put(PostsColumns.TEXT, dbo.text)
            cv.put(PostsColumns.REPLY_OWNER_ID, dbo.replyOwnerId)
            cv.put(PostsColumns.REPLY_POST_ID, dbo.replyPostId)
            cv.put(PostsColumns.FRIENDS_ONLY, dbo.isFriendsOnly)
            cv.put(PostsColumns.COMMENTS_COUNT, dbo.commentsCount)
            cv.put(PostsColumns.CAN_POST_COMMENT, dbo.isCanPostComment)
            cv.put(PostsColumns.LIKES_COUNT, dbo.likesCount)
            cv.put(PostsColumns.USER_LIKES, dbo.isUserLikes)
            cv.put(PostsColumns.CAN_LIKE, dbo.isCanLike)
            cv.put(PostsColumns.CAN_PUBLISH, dbo.isCanPublish)
            cv.put(PostsColumns.CAN_EDIT, dbo.isCanEdit)
            cv.put(PostsColumns.IS_FAVORITE, dbo.isFavorite)
            cv.put(PostsColumns.REPOSTS_COUNT, dbo.repostCount)
            cv.put(PostsColumns.USER_REPOSTED, dbo.isUserReposted)
            cv.put(PostsColumns.POST_TYPE, dbo.postType)
            cv.put(PostsColumns.SIGNED_ID, dbo.signedId)
            cv.put(PostsColumns.CREATED_BY, dbo.createdBy)
            cv.put(PostsColumns.CAN_PIN, dbo.isCanPin)
            cv.put(PostsColumns.IS_PINNED, dbo.isPinned)
            cv.put(PostsColumns.DELETED, dbo.isDeleted)
            val attachmentsCount = safeCountOf(dbo.getAttachments())
            val copiesCount = safeCountOf(dbo.copyHierarchy)
            cv.put(PostsColumns.ATTACHMENTS_COUNT, attachmentsCount + copiesCount)
            dbo.source.ifNonNull({
                cv.put(
                    PostsColumns.POST_SOURCE,
                    MsgPack.encodeToByteArrayEx(SourceDbo.serializer(), it)
                )
            }, {
                cv.putNull(PostsColumns.POST_SOURCE)
            })
            dbo.copyright.ifNonNull({
                cv.put(
                    PostsColumns.COPYRIGHT_BLOB,
                    MsgPack.encodeToByteArrayEx(PostDboEntity.CopyrightDboEntity.serializer(), it)
                )
            }, {
                cv.putNull(PostsColumns.COPYRIGHT_BLOB)
            })
            cv.put(PostsColumns.VIEWS, dbo.views)
            return cv
        }

        internal fun getVkPostIdForEditingType(@EditingPostType type: Int): Int {
            return when (type) {
                EditingPostType.DRAFT -> DRAFT_POST_ID
                EditingPostType.TEMP -> TEMP_POST_ID
                else -> throw IllegalArgumentException()
            }
        }
    }
}