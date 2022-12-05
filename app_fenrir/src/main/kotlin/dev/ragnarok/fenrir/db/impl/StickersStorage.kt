package dev.ragnarok.fenrir.db.impl

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.db.TempDataHelper
import dev.ragnarok.fenrir.db.column.StickersKeywordsColumns
import dev.ragnarok.fenrir.db.column.StikerSetColumns
import dev.ragnarok.fenrir.db.interfaces.IStickersStorage
import dev.ragnarok.fenrir.db.model.entity.StickerDboEntity
import dev.ragnarok.fenrir.db.model.entity.StickerSetEntity
import dev.ragnarok.fenrir.db.model.entity.StickersKeywordsEntity
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Exestime.log
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.util.*

internal class StickersStorage(base: AppStorages) : AbsStorage(base), IStickersStorage {
    override fun storeStickerSets(accountId: Int, sets: List<StickerSetEntity>): Completable {
        return Completable.create { e: CompletableEmitter ->
            val start = System.currentTimeMillis()
            val db = TempDataHelper.helper.writableDatabase
            db.beginTransaction()
            try {
                val whereDel = StikerSetColumns.ACCOUNT_ID + " = ?"
                db.delete(StikerSetColumns.TABLENAME, whereDel, arrayOf(accountId.toString()))
                for ((i, entity) in sets.withIndex()) {
                    db.insert(StikerSetColumns.TABLENAME, null, createCv(accountId, entity, i))
                }
                db.setTransactionSuccessful()
                db.endTransaction()
                e.onComplete()
            } catch (exception: Exception) {
                db.endTransaction()
                e.tryOnError(exception)
            }
            log("StickersStorage.storeStickerSets", start, "count: " + safeCountOf(sets))
        }
    }

    override fun clearAccount(accountId: Int): Completable {
        Settings.get().other().del_last_stikers_sync(accountId)
        return Completable.create { e: CompletableEmitter ->
            val db = TempDataHelper.helper.writableDatabase
            db.beginTransaction()
            try {
                val whereDel = StikerSetColumns.ACCOUNT_ID + " = ?"
                db.delete(StikerSetColumns.TABLENAME, whereDel, arrayOf(accountId.toString()))
                val whereDelK = StickersKeywordsColumns.ACCOUNT_ID + " = ?"
                db.delete(
                    StickersKeywordsColumns.TABLENAME,
                    whereDelK,
                    arrayOf(accountId.toString())
                )
                db.setTransactionSuccessful()
                db.endTransaction()
                e.onComplete()
            } catch (exception: Exception) {
                db.endTransaction()
                e.tryOnError(exception)
            }
        }
    }

    override fun storeKeyWords(accountId: Int, sets: List<StickersKeywordsEntity>): Completable {
        return Completable.create { e: CompletableEmitter ->
            val start = System.currentTimeMillis()
            val db = TempDataHelper.helper.writableDatabase
            db.beginTransaction()
            try {
                val whereDel = StickersKeywordsColumns.ACCOUNT_ID + " = ?"
                db.delete(
                    StickersKeywordsColumns.TABLENAME,
                    whereDel,
                    arrayOf(accountId.toString())
                )
                for ((id, entity) in sets.withIndex()) {
                    db.insert(
                        StickersKeywordsColumns.TABLENAME,
                        null,
                        createCvStickersKeywords(accountId, entity, id)
                    )
                }
                db.setTransactionSuccessful()
                db.endTransaction()
                e.onComplete()
            } catch (exception: Exception) {
                db.endTransaction()
                e.tryOnError(exception)
            }
            log("StickersStorage.storeKeyWords", start, "count: " + safeCountOf(sets))
        }
    }

    override fun getPurchasedAndActive(accountId: Int): Single<List<StickerSetEntity>> {
        return Single.create { e: SingleEmitter<List<StickerSetEntity>> ->
            val start = System.currentTimeMillis()
            val where =
                "${StikerSetColumns.ACCOUNT_ID} = ? AND ${StikerSetColumns.PURCHASED} = ? AND ${StikerSetColumns.ACTIVE} = ?"
            val args = arrayOf(accountId.toString(), "1", "1")
            val cursor = TempDataHelper.helper.readableDatabase.query(
                StikerSetColumns.TABLENAME,
                COLUMNS_STICKER_SET,
                where,
                args,
                null,
                null,
                null
            )
            val stickers: MutableList<StickerSetEntity> = ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                if (e.isDisposed) {
                    break
                }
                stickers.add(mapStickerSet(cursor))
            }
            Collections.sort(stickers, COMPARATOR_STICKER_SET)
            cursor.close()
            e.onSuccess(stickers)
            log("StickersStorage.get", start, "count: " + stickers.size)
        }
    }

    override fun getKeywordsStickers(accountId: Int, s: String?): Single<List<StickerDboEntity>> {
        return Single.create { e: SingleEmitter<List<StickerDboEntity>> ->
            val where = "${StickersKeywordsColumns.ACCOUNT_ID} = ?"
            val args = arrayOf(accountId.toString())
            val cursor = TempDataHelper.helper.readableDatabase.query(
                StickersKeywordsColumns.TABLENAME,
                KEYWORDS_STICKER_COLUMNS,
                where,
                args,
                null,
                null,
                null
            )
            val stickers: MutableList<StickerDboEntity> = ArrayList(safeCountOf(cursor))
            while (cursor.moveToNext()) {
                if (e.isDisposed) {
                    break
                }
                val entity = mapStickersKeywords(cursor)
                for (v in entity.keywords.orEmpty()) {
                    if (s.equals(v, ignoreCase = true)) {
                        entity.stickers?.let { stickers.addAll(it) }
                        cursor.close()
                        e.onSuccess(stickers)
                        return@create
                    }
                }
            }
            cursor.close()
            e.onSuccess(stickers)
        }
    }

    companion object {
        private val COLUMNS_STICKER_SET = arrayOf(
            BaseColumns._ID,
            StikerSetColumns.ACCOUNT_ID,
            StikerSetColumns.POSITION,
            StikerSetColumns.TITLE,
            StikerSetColumns.ICON,
            StikerSetColumns.PURCHASED,
            StikerSetColumns.PROMOTED,
            StikerSetColumns.ACTIVE,
            StikerSetColumns.STICKERS
        )
        private val KEYWORDS_STICKER_COLUMNS = arrayOf(
            BaseColumns._ID,
            StickersKeywordsColumns.ACCOUNT_ID,
            StickersKeywordsColumns.KEYWORDS,
            StickersKeywordsColumns.STICKERS
        )
        private val COMPARATOR_STICKER_SET =
            Comparator { rhs: StickerSetEntity, lhs: StickerSetEntity ->
                lhs.position.compareTo(rhs.position)
            }

        internal fun createCv(accountId: Int, entity: StickerSetEntity, pos: Int): ContentValues {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, entity.id)
            cv.put(StikerSetColumns.ACCOUNT_ID, accountId)
            cv.put(StikerSetColumns.POSITION, pos)
            entity.icon.ifNonNull({
                cv.put(
                    StikerSetColumns.ICON,
                    MsgPack.encodeToByteArrayEx(
                        ListSerializer(StickerSetEntity.Img.serializer()),
                        it
                    )
                )
            }, {
                cv.putNull(StikerSetColumns.ICON)
            })
            cv.put(StikerSetColumns.TITLE, entity.title)
            cv.put(StikerSetColumns.PURCHASED, entity.isPurchased)
            cv.put(StikerSetColumns.PROMOTED, entity.isPromoted)
            cv.put(StikerSetColumns.ACTIVE, entity.isActive)

            entity.stickers.ifNonNull({
                cv.put(
                    StikerSetColumns.STICKERS,
                    MsgPack.encodeToByteArrayEx(ListSerializer(StickerDboEntity.serializer()), it)
                )
            }, {
                cv.putNull(StikerSetColumns.STICKERS)
            })
            return cv
        }

        internal fun createCvStickersKeywords(
            accountId: Int,
            entity: StickersKeywordsEntity,
            id: Int
        ): ContentValues {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, id)
            cv.put(StickersKeywordsColumns.ACCOUNT_ID, accountId)
            entity.keywords.ifNonNull({
                cv.put(
                    StickersKeywordsColumns.KEYWORDS,
                    MsgPack.encodeToByteArrayEx(ListSerializer(String.serializer()), it)
                )
            }, {
                cv.putNull(StickersKeywordsColumns.KEYWORDS)
            })
            entity.stickers.ifNonNull({
                cv.put(
                    StickersKeywordsColumns.STICKERS,
                    MsgPack.encodeToByteArrayEx(ListSerializer(StickerDboEntity.serializer()), it)
                )
            }, {
                cv.putNull(StickersKeywordsColumns.STICKERS)
            })
            return cv
        }

        internal fun mapStickerSet(cursor: Cursor): StickerSetEntity {
            val stickersJson = cursor.getBlob(StikerSetColumns.STICKERS)
            val iconJson = cursor.getBlob(StikerSetColumns.ICON)
            return StickerSetEntity(cursor.getInt(BaseColumns._ID))
                .setStickers(
                    if (stickersJson == null) null else MsgPack.decodeFromByteArrayEx(
                        ListSerializer(StickerDboEntity.serializer()),
                        stickersJson
                    )
                )
                .setActive(cursor.getBoolean(StikerSetColumns.ACTIVE))
                .setPurchased(cursor.getBoolean(StikerSetColumns.PURCHASED))
                .setPromoted(cursor.getBoolean(StikerSetColumns.PROMOTED))
                .setIcon(
                    if (iconJson == null) null else MsgPack.decodeFromByteArrayEx(
                        ListSerializer(
                            StickerSetEntity.Img.serializer()
                        ), iconJson
                    )
                )
                .setPosition(cursor.getInt(StikerSetColumns.POSITION))
                .setTitle(cursor.getString(StikerSetColumns.TITLE))
        }

        internal fun mapStickersKeywords(cursor: Cursor): StickersKeywordsEntity {
            val stickersJson =
                cursor.getBlob(StickersKeywordsColumns.STICKERS)
            val keywordsJson =
                cursor.getBlob(StickersKeywordsColumns.KEYWORDS)
            return StickersKeywordsEntity(
                if (keywordsJson == null) null else MsgPack.decodeFromByteArrayEx(
                    ListSerializer(
                        String.serializer()
                    ), keywordsJson
                ),
                if (stickersJson == null) null else MsgPack.decodeFromByteArrayEx(
                    ListSerializer(
                        StickerDboEntity.serializer()
                    ), stickersJson
                ),
            )
        }
    }
}