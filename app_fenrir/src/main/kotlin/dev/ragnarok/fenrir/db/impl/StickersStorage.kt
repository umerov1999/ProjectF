package dev.ragnarok.fenrir.db.impl

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.database.sqlite.transaction
import dev.ragnarok.fenrir.db.TempDataHelper
import dev.ragnarok.fenrir.db.column.StickerSetsColumns
import dev.ragnarok.fenrir.db.column.StickerSetsCustomColumns
import dev.ragnarok.fenrir.db.column.StickersKeywordsColumns
import dev.ragnarok.fenrir.db.interfaces.IStickersStorage
import dev.ragnarok.fenrir.db.model.entity.StickerDboEntity
import dev.ragnarok.fenrir.db.model.entity.StickerSetEntity
import dev.ragnarok.fenrir.db.model.entity.StickersKeywordsEntity
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.insert
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Exestime.log
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.msgpack.MsgPack

internal class StickersStorage(base: AppStorages) : AbsStorage(base), IStickersStorage {
    override fun storeStickerSets(accountId: Long, sets: List<StickerSetEntity>): Flow<Boolean> {
        return flow {
            val start = System.currentTimeMillis()
            val db = TempDataHelper.helper.writableDatabase
            db.transaction {
                val whereDel = StickerSetsColumns.ACCOUNT_ID + " = ?"
                db.delete(StickerSetsColumns.TABLENAME, whereDel, arrayOf(accountId.toString()))
                for ((i, entity) in sets.withIndex()) {
                    db.insert(StickerSetsColumns.TABLENAME, null, createCv(accountId, entity, i))
                }
            }
            log("StickersStorage.storeStickerSets", start, "count: " + safeCountOf(sets))
            emit(true)
        }
    }

    override fun storeStickerSetsCustom(
        accountId: Long,
        sets: List<StickerSetEntity>
    ): Flow<Boolean> {
        return flow {
            val start = System.currentTimeMillis()
            val db = TempDataHelper.helper.writableDatabase
            db.transaction {
                val whereDel = StickerSetsCustomColumns.ACCOUNT_ID + " = ?"
                db.delete(
                    StickerSetsCustomColumns.TABLENAME,
                    whereDel,
                    arrayOf(accountId.toString())
                )
                for ((i, entity) in sets.withIndex()) {
                    db.insert(
                        StickerSetsCustomColumns.TABLENAME,
                        null,
                        createCvCustom(accountId, entity, i)
                    )
                }
            }
            log("StickersStorage.storeStickerSetsCustom", start, "count: " + safeCountOf(sets))
            emit(true)
        }
    }

    override fun clearAccount(accountId: Long): Flow<Boolean> {
        Settings.get().main().del_last_sticker_sets_sync(accountId)
        Settings.get().main().del_last_sticker_sets_custom_sync(accountId)
        Settings.get().main().del_last_sticker_keywords_sync(accountId)
        return flow {
            val db = TempDataHelper.helper.writableDatabase
            db.transaction {
                val whereDel = StickerSetsColumns.ACCOUNT_ID + " = ?"
                db.delete(StickerSetsColumns.TABLENAME, whereDel, arrayOf(accountId.toString()))
                val whereDelC = StickerSetsCustomColumns.ACCOUNT_ID + " = ?"
                db.delete(
                    StickerSetsCustomColumns.TABLENAME,
                    whereDelC,
                    arrayOf(accountId.toString())
                )
                val whereDelK = StickersKeywordsColumns.ACCOUNT_ID + " = ?"
                db.delete(
                    StickersKeywordsColumns.TABLENAME,
                    whereDelK,
                    arrayOf(accountId.toString())
                )
            }
            emit(true)
        }
    }

    override fun storeKeyWords(accountId: Long, sets: List<StickersKeywordsEntity>): Flow<Boolean> {
        return flow {
            val start = System.currentTimeMillis()
            val db = TempDataHelper.helper.writableDatabase
            db.transaction {
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
            }
            log("StickersStorage.storeKeyWords", start, "count: " + safeCountOf(sets))
            emit(true)
        }
    }

    override fun getStickerSets(accountId: Long): Flow<List<StickerSetEntity>> {
        return flow {
            val start = System.currentTimeMillis()
            val where =
                "${StickerSetsColumns.ACCOUNT_ID} = ?"
            val whereCustom =
                "${StickerSetsCustomColumns.ACCOUNT_ID} = ?"
            val args = arrayOf(accountId.toString())
            var cursor = TempDataHelper.helper.writableDatabase.query(
                StickerSetsColumns.TABLENAME,
                COLUMNS_STICKER_SET,
                where,
                args,
                null,
                null,
                null
            )
            val stickers: MutableList<StickerSetEntity> = ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                if (!isActive()) {
                    break
                }
                stickers.add(mapStickerSet(cursor))
            }
            stickers.sortWith(COMPARATOR_STICKER_SET)
            cursor.close()

            if (isActive()) {
                cursor = TempDataHelper.helper.writableDatabase.query(
                    StickerSetsCustomColumns.TABLENAME,
                    COLUMNS_STICKER_SET_CUSTOM,
                    whereCustom,
                    args,
                    null,
                    null,
                    null
                )
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    stickers.insert(0, mapStickerSetCustom(cursor))
                }
            }
            log("StickersStorage.get", start, "count: " + stickers.size)
            emit(stickers)
        }
    }

    override fun getKeywordsStickers(accountId: Long, s: String?): Flow<List<StickerDboEntity>> {
        return flow {
            val where = "${StickersKeywordsColumns.ACCOUNT_ID} = ?"
            val args = arrayOf(accountId.toString())
            val cursor = TempDataHelper.helper.writableDatabase.query(
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
                if (!isActive()) {
                    break
                }
                val entity = mapStickersKeywords(cursor)
                for (v in entity.keywords) {
                    if (s.equals(v, ignoreCase = true)) {
                        entity.stickers.let { stickers.addAll(it) }
                        cursor.close()
                        emit(stickers)
                        return@flow
                    }
                }
            }
            cursor.close()
            emit(stickers)
        }
    }

    companion object {
        private val COLUMNS_STICKER_SET = arrayOf(
            BaseColumns._ID,
            StickerSetsColumns.ACCOUNT_ID,
            StickerSetsColumns.POSITION,
            StickerSetsColumns.TITLE,
            StickerSetsColumns.ICON,
            StickerSetsColumns.PURCHASED,
            StickerSetsColumns.PROMOTED,
            StickerSetsColumns.ACTIVE,
            StickerSetsColumns.STICKERS
        )
        private val COLUMNS_STICKER_SET_CUSTOM = arrayOf(
            BaseColumns._ID,
            StickerSetsCustomColumns.ACCOUNT_ID,
            StickerSetsCustomColumns.POSITION,
            StickerSetsCustomColumns.TITLE,
            StickerSetsCustomColumns.ICON,
            StickerSetsCustomColumns.PURCHASED,
            StickerSetsCustomColumns.PROMOTED,
            StickerSetsCustomColumns.ACTIVE,
            StickerSetsCustomColumns.STICKERS
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

        internal fun createCv(accountId: Long, entity: StickerSetEntity, pos: Int): ContentValues {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, entity.id)
            cv.put(StickerSetsColumns.ACCOUNT_ID, accountId)
            cv.put(StickerSetsColumns.POSITION, pos)
            entity.icon.ifNonNull({
                cv.put(
                    StickerSetsColumns.ICON,
                    MsgPack.encodeToByteArrayEx(
                        ListSerializer(StickerSetEntity.Img.serializer()),
                        it
                    )
                )
            }, {
                cv.putNull(StickerSetsColumns.ICON)
            })
            cv.put(StickerSetsColumns.TITLE, entity.title)
            cv.put(StickerSetsColumns.PURCHASED, entity.isPurchased)
            cv.put(StickerSetsColumns.PROMOTED, entity.isPromoted)
            cv.put(StickerSetsColumns.ACTIVE, entity.isActive)

            entity.stickers.ifNonNull({
                cv.put(
                    StickerSetsColumns.STICKERS,
                    MsgPack.encodeToByteArrayEx(ListSerializer(StickerDboEntity.serializer()), it)
                )
            }, {
                cv.putNull(StickerSetsColumns.STICKERS)
            })
            return cv
        }

        internal fun createCvCustom(
            accountId: Long,
            entity: StickerSetEntity,
            pos: Int
        ): ContentValues {
            val cv = ContentValues()
            cv.put(BaseColumns._ID, entity.id)
            cv.put(StickerSetsCustomColumns.ACCOUNT_ID, accountId)
            cv.put(StickerSetsCustomColumns.POSITION, pos)
            entity.icon.ifNonNull({
                cv.put(
                    StickerSetsCustomColumns.ICON,
                    MsgPack.encodeToByteArrayEx(
                        ListSerializer(StickerSetEntity.Img.serializer()),
                        it
                    )
                )
            }, {
                cv.putNull(StickerSetsCustomColumns.ICON)
            })
            cv.put(StickerSetsCustomColumns.TITLE, entity.title)
            cv.put(StickerSetsCustomColumns.PURCHASED, entity.isPurchased)
            cv.put(StickerSetsCustomColumns.PROMOTED, entity.isPromoted)
            cv.put(StickerSetsCustomColumns.ACTIVE, entity.isActive)

            entity.stickers.ifNonNull({
                cv.put(
                    StickerSetsCustomColumns.STICKERS,
                    MsgPack.encodeToByteArrayEx(ListSerializer(StickerDboEntity.serializer()), it)
                )
            }, {
                cv.putNull(StickerSetsCustomColumns.STICKERS)
            })
            return cv
        }

        internal fun createCvStickersKeywords(
            accountId: Long,
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
            val stickersJson = cursor.getBlob(StickerSetsColumns.STICKERS)
            val iconJson = cursor.getBlob(StickerSetsColumns.ICON)
            return StickerSetEntity(cursor.getInt(BaseColumns._ID))
                .setStickers(
                    if (stickersJson == null) null else MsgPack.decodeFromByteArrayEx(
                        ListSerializer(StickerDboEntity.serializer()),
                        stickersJson
                    )
                )
                .setActive(cursor.getBoolean(StickerSetsColumns.ACTIVE))
                .setPurchased(cursor.getBoolean(StickerSetsColumns.PURCHASED))
                .setPromoted(cursor.getBoolean(StickerSetsColumns.PROMOTED))
                .setIcon(
                    if (iconJson == null) null else MsgPack.decodeFromByteArrayEx(
                        ListSerializer(
                            StickerSetEntity.Img.serializer()
                        ), iconJson
                    )
                )
                .setPosition(cursor.getInt(StickerSetsColumns.POSITION))
                .setTitle(cursor.getString(StickerSetsColumns.TITLE))
        }

        internal fun mapStickerSetCustom(cursor: Cursor): StickerSetEntity {
            val stickersJson = cursor.getBlob(StickerSetsCustomColumns.STICKERS)
            val iconJson = cursor.getBlob(StickerSetsCustomColumns.ICON)
            return StickerSetEntity(cursor.getInt(BaseColumns._ID))
                .setStickers(
                    if (stickersJson == null) null else MsgPack.decodeFromByteArrayEx(
                        ListSerializer(StickerDboEntity.serializer()),
                        stickersJson
                    )
                )
                .setActive(cursor.getBoolean(StickerSetsCustomColumns.ACTIVE))
                .setPurchased(cursor.getBoolean(StickerSetsCustomColumns.PURCHASED))
                .setPromoted(cursor.getBoolean(StickerSetsCustomColumns.PROMOTED))
                .setIcon(
                    if (iconJson == null) null else MsgPack.decodeFromByteArrayEx(
                        ListSerializer(
                            StickerSetEntity.Img.serializer()
                        ), iconJson
                    )
                )
                .setPosition(cursor.getInt(StickerSetsCustomColumns.POSITION))
                .setTitle(cursor.getString(StickerSetsCustomColumns.TITLE))
        }

        internal fun mapStickersKeywords(cursor: Cursor): StickersKeywordsEntity {
            val stickersJson =
                cursor.getBlob(StickersKeywordsColumns.STICKERS)
            val keywordsJson =
                cursor.getBlob(StickersKeywordsColumns.KEYWORDS)
            return StickersKeywordsEntity(
                if (keywordsJson == null) emptyList() else MsgPack.decodeFromByteArrayEx(
                    ListSerializer(
                        String.serializer()
                    ), keywordsJson
                ),
                if (stickersJson == null) emptyList() else MsgPack.decodeFromByteArrayEx(
                    ListSerializer(
                        StickerDboEntity.serializer()
                    ), stickersJson
                ),
            )
        }
    }
}