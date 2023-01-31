package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getCommentsAttachmentsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getMessagesAttachmentsContentUriFor
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getPostsAttachmentsContentUriFor
import dev.ragnarok.fenrir.db.column.attachments.CommentsAttachmentsColumns
import dev.ragnarok.fenrir.db.column.attachments.MessagesAttachmentsColumns
import dev.ragnarok.fenrir.db.column.attachments.WallAttachmentsColumns
import dev.ragnarok.fenrir.db.interfaces.Cancelable
import dev.ragnarok.fenrir.db.interfaces.IAttachmentsStorage
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter

internal class AttachmentsStorage(base: AppStorages) : AbsStorage(base), IAttachmentsStorage {
    override fun attachDbos(
        accountId: Long,
        attachToType: Int,
        attachToDbid: Int,
        entities: List<DboEntity>
    ): Single<IntArray> {
        return Single.create { emitter: SingleEmitter<IntArray> ->
            val operations = ArrayList<ContentProviderOperation>(entities.size)
            val indexes = IntArray(entities.size)
            for (i in entities.indices) {
                val entity = entities[i]
                indexes[i] = appendAttachOperationWithStableAttachToId(
                    operations,
                    accountId,
                    attachToType,
                    attachToDbid,
                    entity
                )
            }
            val results = contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            val ids = IntArray(entities.size)
            for (i in indexes.indices) {
                val result = results[indexes[i]]
                val dbid = result.uri?.pathSegments?.get(1)?.toInt()
                ids[i] = dbid ?: continue
            }
            emitter.onSuccess(ids)
        }
    }

    override fun getAttachmentsDbosWithIds(
        accountId: Long,
        @AttachToType attachToType: Int,
        attachToDbid: Int
    ): Single<List<Pair<Int, DboEntity>>> {
        return Single.create { emitter: SingleEmitter<List<Pair<Int, DboEntity>>> ->
            val cursor = createCursor(accountId, attachToType, attachToDbid)
            val dbos: MutableList<Pair<Int, DboEntity>> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (emitter.isDisposed) {
                        break
                    }
                    val id = cursor.getInt(idColumnFor(attachToType))
                    val json =
                        cursor.getBlob(dataColumnFor(attachToType))
                    val entity = deserializeDbo(json ?: continue)
                    dbos.add(create(id, entity))
                }
                cursor.close()
            }
            emitter.onSuccess(dbos)
        }
    }

    private fun createCursor(accountId: Long, attachToType: Int, attachToDbid: Int): Cursor? {
        val uri = uriForType(attachToType, accountId)
        return contentResolver.query(
            uri, null,
            attachToIdColumnFor(attachToType) + " = ?", arrayOf(attachToDbid.toString()), null
        )
    }

    override fun getAttachmentsDbosSync(
        accountId: Long,
        attachToType: Int,
        attachToDbid: Int,
        cancelable: Cancelable
    ): MutableList<DboEntity> {
        val cursor = createCursor(accountId, attachToType, attachToDbid)
        val entities: MutableList<DboEntity> = ArrayList(safeCountOf(cursor))
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (cancelable.isOperationCancelled) {
                    break
                }
                val json =
                    cursor.getBlob(dataColumnFor(attachToType))
                entities.add(deserializeDbo(json ?: continue))
            }
            cursor.close()
        }
        return entities
    }

    override fun remove(
        accountId: Long,
        @AttachToType attachToType: Int,
        attachToDbid: Int,
        generatedAttachmentId: Int
    ): Completable {
        return Completable.create { e: CompletableEmitter ->
            val uri = uriForType(attachToType, accountId)
            val selection = idColumnFor(attachToType) + " = ?"
            val args = arrayOf(generatedAttachmentId.toString())
            val count = context.contentResolver.delete(uri, selection, args)
            if (count > 0) {
                e.onComplete()
            } else {
                e.tryOnError(NotFoundException())
            }
        }
    }

    override fun getCount(accountId: Long, attachToType: Int, attachToDbid: Int): Single<Int> {
        return Single.fromCallable {
            val uri = uriForType(attachToType, accountId)
            val selection = attachToIdColumnFor(attachToType) + " = ?"
            val args = arrayOf(attachToDbid.toString())
            val cursor = contentResolver.query(uri, null, selection, args, null)
            val count = safeCountOf(cursor)
            cursor?.close()
            count
        }
    }

    companion object {
        internal fun uriForType(@AttachToType type: Int, accountId: Long): Uri {
            return when (type) {
                AttachToType.COMMENT -> getCommentsAttachmentsContentUriFor(
                    accountId
                )

                AttachToType.MESSAGE -> getMessagesAttachmentsContentUriFor(
                    accountId
                )

                AttachToType.POST -> getPostsAttachmentsContentUriFor(accountId)
                else -> throw IllegalArgumentException()
            }
        }


        fun appendAttachOperationWithBackReference(
            operations: MutableList<ContentProviderOperation>, accountId: Long,
            @AttachToType attachToType: Int, attachToBackReferenceIndex: Int, dboEntity: DboEntity
        ) {
            val cv = ContentValues()
            cv.put(dataColumnFor(attachToType), serializeDbo(dboEntity))
            operations.add(
                ContentProviderOperation.newInsert(uriForType(attachToType, accountId))
                    .withValues(cv)
                    .withValueBackReference(
                        attachToIdColumnFor(attachToType),
                        attachToBackReferenceIndex
                    )
                    .build()
            )
        }


        fun appendAttachOperationWithStableAttachToId(
            operations: MutableList<ContentProviderOperation>,
            accountId: Long, @AttachToType attachToType: Int,
            attachToDbid: Int, dboEntity: DboEntity
        ): Int {
            val cv = ContentValues()
            cv.put(attachToIdColumnFor(attachToType), attachToDbid)
            cv.put(dataColumnFor(attachToType), serializeDbo(dboEntity))
            return addToListAndReturnIndex(
                operations, ContentProviderOperation.newInsert(uriForType(attachToType, accountId))
                    .withValues(cv)
                    .build()
            )
        }

        internal fun idColumnFor(@AttachToType type: Int): String {
            when (type) {
                AttachToType.COMMENT, AttachToType.MESSAGE, AttachToType.POST -> return BaseColumns._ID
            }
            throw IllegalArgumentException()
        }

        internal fun attachToIdColumnFor(@AttachToType type: Int): String {
            when (type) {
                AttachToType.COMMENT -> return CommentsAttachmentsColumns.C_ID
                AttachToType.MESSAGE -> return MessagesAttachmentsColumns.M_ID
                AttachToType.POST -> return WallAttachmentsColumns.P_ID
            }
            throw IllegalArgumentException()
        }

        internal fun dataColumnFor(@AttachToType type: Int): String {
            when (type) {
                AttachToType.COMMENT -> return CommentsAttachmentsColumns.DATA
                AttachToType.MESSAGE -> return MessagesAttachmentsColumns.DATA
                AttachToType.POST -> return WallAttachmentsColumns.DATA
            }
            throw IllegalArgumentException()
        }

        private fun serializeDbo(dboEntity: DboEntity): ByteArray {
            return MsgPack.encodeToByteArrayEx(DboEntity.serializer(), dboEntity)
        }

        internal fun deserializeDbo(json: ByteArray): DboEntity {
            return MsgPack.decodeFromByteArrayEx(DboEntity.serializer(), json)
        }
    }
}