package dev.ragnarok.fenrir.db.impl

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.database.sqlite.transaction
import dev.ragnarok.fenrir.db.TempDataHelper
import dev.ragnarok.fenrir.db.column.AudiosColumns
import dev.ragnarok.fenrir.db.column.LogsColumns
import dev.ragnarok.fenrir.db.column.ReactionsColumns
import dev.ragnarok.fenrir.db.column.SearchRequestColumns
import dev.ragnarok.fenrir.db.column.ShortcutsColumns
import dev.ragnarok.fenrir.db.column.TempDataColumns
import dev.ragnarok.fenrir.db.interfaces.ITempDataStorage
import dev.ragnarok.fenrir.db.model.entity.ReactionAssetEntity
import dev.ragnarok.fenrir.db.serialize.ISerializeAdapter
import dev.ragnarok.fenrir.getBlob
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.LogEvent
import dev.ragnarok.fenrir.model.ShortcutStored
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Exestime.log
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.emptyTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.msgpack.MsgPack

class TempDataStorage internal constructor(context: Context) : ITempDataStorage {
    private val app: Context = context.applicationContext
    private val helper: TempDataHelper by lazy {
        TempDataHelper(app)
    }

    override fun <T> getTemporaryData(
        ownerId: Long,
        sourceId: Int,
        serializer: ISerializeAdapter<T>
    ): Flow<List<T>> {
        return flow {
            val start = System.currentTimeMillis()
            val where = TempDataColumns.OWNER_ID + " = ? AND " + TempDataColumns.SOURCE_ID + " = ?"
            val args = arrayOf(ownerId.toString(), sourceId.toString())
            val cursor = helper.writableDatabase.query(
                TempDataColumns.TABLENAME,
                PROJECTION_TEMPORARY, where, args, null, null, null
            )
            val data: MutableList<T> = ArrayList(cursor.count)
            cursor.use {
                while (it.moveToNext()) {
                    val raw = it.getBlob(3)
                    data.add(serializer.deserialize(raw))
                }
            }
            log("TempDataStorage.getData", start, "count: " + data.size)
            emit(data)
        }
    }

    override fun <T> putTemporaryData(
        ownerId: Long,
        sourceId: Int,
        data: List<T>,
        serializer: ISerializeAdapter<T>
    ): Flow<Boolean> {
        return flow {
            val start = System.currentTimeMillis()
            val db = helper.writableDatabase
            db.transaction {
                // clear
                delete(
                    TempDataColumns.TABLENAME,
                    TempDataColumns.OWNER_ID + " = ? AND " + TempDataColumns.SOURCE_ID + " = ?",
                    arrayOf(ownerId.toString(), sourceId.toString())
                )
                for (t in data) {
                    if (!isActive()) {
                        break
                    }
                    val cv = ContentValues()
                    cv.put(TempDataColumns.OWNER_ID, ownerId)
                    cv.put(TempDataColumns.SOURCE_ID, sourceId)
                    cv.put(TempDataColumns.DATA, serializer.serialize(t))
                    insert(TempDataColumns.TABLENAME, null, cv)
                }
            }
            log("TempDataStorage.put", start, "count: " + data.size)
            emit(true)
        }
    }

    override fun deleteTemporaryData(ownerId: Long): Flow<Boolean> {
        return flow {
            val start = System.currentTimeMillis()
            val count = helper.writableDatabase.delete(
                TempDataColumns.TABLENAME,
                TempDataColumns.OWNER_ID + " = ?", arrayOf(ownerId.toString())
            )
            log("TempDataStorage.delete", start, "count: $count")
            emit(true)
        }
    }

    override fun getSearchQueries(sourceId: Int): Flow<List<String>> {
        return flow {
            val start = System.currentTimeMillis()
            val where = SearchRequestColumns.SOURCE_ID + " = ?"
            val args = arrayOf(sourceId.toString())
            val cursor = helper.writableDatabase.query(
                SearchRequestColumns.TABLENAME,
                PROJECTION_SEARCH, where, args, null, null, BaseColumns._ID + " DESC"
            )
            val data: MutableList<String> = ArrayList(cursor.count)
            cursor.use {
                while (it.moveToNext()) {
                    data.add(it.getString(2))
                }
            }
            log("SearchRequestHelperStorage.getQueries", start, "count: " + data.size)
            emit(data)
        }
    }

    override fun insertSearchQuery(sourceId: Int, query: String?): Flow<Boolean> {
        if (query == null) {
            return emptyTaskFlow()
        }
        val queryClean = query.trim()
        return if (queryClean.isEmpty()) {
            emptyTaskFlow()
        } else flow {
            val db = helper.writableDatabase
            val cv = ContentValues()
            cv.put(SearchRequestColumns.SOURCE_ID, sourceId)
            cv.put(SearchRequestColumns.QUERY, queryClean)
            db.insert(SearchRequestColumns.TABLENAME, null, cv)
            emit(true)
        }
    }

    override fun deleteSearch(sourceId: Int): Flow<Boolean> {
        return flow {
            val start = System.currentTimeMillis()
            val count = helper.writableDatabase.delete(
                SearchRequestColumns.TABLENAME,
                SearchRequestColumns.SOURCE_ID + " = ?", arrayOf(sourceId.toString())
            )
            log("SearchRequestHelperStorage.delete", start, "count: $count")
            emit(true)
        }
    }

    override fun addShortcut(action: String, cover: String, name: String): Flow<Boolean> {
        return flow {
            val db = helper.writableDatabase
            db.delete(
                ShortcutsColumns.TABLENAME,
                ShortcutsColumns.ACTION + " = ?", arrayOf(action)
            )
            val cv = ContentValues()
            cv.put(ShortcutsColumns.ACTION, action)
            cv.put(ShortcutsColumns.NAME, name)
            cv.put(ShortcutsColumns.COVER, cover)
            db.insert(ShortcutsColumns.TABLENAME, null, cv)
            emit(true)
        }
    }

    override fun addShortcuts(list: List<ShortcutStored>): Flow<Boolean> {
        return flow {
            val db = helper.writableDatabase
            db.transaction {
                for (i in list) {
                    delete(
                        ShortcutsColumns.TABLENAME,
                        ShortcutsColumns.ACTION + " = ?", arrayOf(i.action)
                    )
                    val cv = ContentValues()
                    cv.put(ShortcutsColumns.ACTION, i.action)
                    cv.put(ShortcutsColumns.NAME, i.name)
                    cv.put(ShortcutsColumns.COVER, i.cover)
                    insert(ShortcutsColumns.TABLENAME, null, cv)
                }
                emit(true)
            }
        }
    }

    override fun addReactionsAssets(
        accountId: Long,
        list: List<ReactionAssetEntity>
    ): Flow<Boolean> {
        return flow {
            val db = helper.writableDatabase
            db.transaction {
                delete(
                    ReactionsColumns.TABLENAME,
                    ReactionsColumns.ACCOUNT_ID + " = ?", arrayOf(accountId.toString())
                )
                for (i in list) {
                    val cv = ContentValues()
                    cv.put(ReactionsColumns.REACTION_ID, i.reaction_id)
                    cv.put(ReactionsColumns.ACCOUNT_ID, accountId)
                    cv.put(ReactionsColumns.BIG_ANIMATION, i.big_animation)
                    cv.put(ReactionsColumns.SMALL_ANIMATION, i.small_animation)
                    cv.put(ReactionsColumns.STATIC, i.static)
                    insert(ReactionsColumns.TABLENAME, null, cv)
                }
                emit(true)
            }
        }
    }

    override fun getReactionsAssets(accountId: Long): Flow<List<ReactionAssetEntity>> {
        return flow {
            val start = System.currentTimeMillis()
            val where = ReactionsColumns.ACCOUNT_ID + " = ?"
            val args = arrayOf(accountId.toString())
            val cursor = helper.writableDatabase.query(
                ReactionsColumns.TABLENAME,
                PROJECTION_REACTION_ASSET,
                where,
                args,
                null,
                null,
                ReactionsColumns.REACTION_ID + " DESC"
            )
            val data: MutableList<ReactionAssetEntity> = ArrayList(cursor.count)
            cursor.use {
                while (it.moveToNext()) {
                    data.add(mapReactionAsset(it))
                }
            }
            log("SearchRequestHelperStorage.getReactionsAssets", start, "count: " + data.size)
            emit(data)
        }
    }

    override fun clearReactionAssets(accountId: Long): Flow<Boolean> {
        Settings.get().main().del_last_reaction_assets_sync(accountId)
        return flow {
            val db = TempDataHelper.helper.writableDatabase
            val whereDel = ReactionsColumns.ACCOUNT_ID + " = ?"
            db.delete(ReactionsColumns.TABLENAME, whereDel, arrayOf(accountId.toString()))
            emit(true)
        }
    }

    override fun deleteShortcut(action: String): Flow<Boolean> {
        return flow {
            val start = System.currentTimeMillis()
            val count = helper.writableDatabase.delete(
                ShortcutsColumns.TABLENAME,
                ShortcutsColumns.ACTION + " = ?", arrayOf(action)
            )
            log("SearchRequestHelperStorage.delete", start, "count: $count")
            emit(true)
        }
    }

    override fun getShortcutAll(): Flow<List<ShortcutStored>> {
        return flow {
            val cursor = helper.writableDatabase.query(
                ShortcutsColumns.TABLENAME,
                PROJECTION_SHORTCUT,
                null,
                null,
                null,
                null,
                BaseColumns._ID + " DESC"
            )
            val data: MutableList<ShortcutStored> = ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                data.add(mapShortcut(cursor))
            }
            cursor.close()
            emit(data)
        }
    }

    override fun addLog(type: Int, tag: String, body: String): Flow<LogEvent> {
        return flow {
            val now = System.currentTimeMillis()
            val cv = ContentValues()
            cv.put(LogsColumns.TYPE, type)
            cv.put(LogsColumns.TAG, tag)
            cv.put(LogsColumns.BODY, body)
            cv.put(LogsColumns.DATE, now)
            val id = helper.writableDatabase.insert(LogsColumns.TABLENAME, null, cv)
            emit(
                LogEvent(id.toInt())
                    .setBody(body)
                    .setTag(tag)
                    .setDate(now)
                    .setType(type)
            )
        }
    }

    override fun deleteAudio(sourceOwner: Long, id: Int, ownerId: Long): Flow<Boolean> {
        return flow {
            helper.writableDatabase.delete(
                AudiosColumns.TABLENAME,
                AudiosColumns.SOURCE_OWNER_ID + " = ? AND " + AudiosColumns.AUDIO_ID + " = ? AND " + AudiosColumns.AUDIO_OWNER_ID + " = ?",
                arrayOf(sourceOwner.toString(), id.toString(), ownerId.toString())
            )
            emit(true)
        }
    }

    override fun deleteAudios(): Flow<Boolean> {
        return flow {
            helper.writableDatabase.delete(
                AudiosColumns.TABLENAME,
                null, null
            )
            emit(true)
        }
    }

    override fun getAudiosAll(sourceOwner: Long): Flow<List<Audio>> {
        return flow {
            val cursor = helper.writableDatabase.query(
                AudiosColumns.TABLENAME,
                PROJECTION_AUDIO,
                AudiosColumns.SOURCE_OWNER_ID + " = ?",
                arrayOf(sourceOwner.toString()),
                null,
                null,
                AudiosColumns.DATE + " DESC"
            )
            val data: MutableList<Audio> = ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                data.add(mapAudio(cursor))
            }
            cursor.close()
            emit(data)
        }
    }

    override fun addAudios(sourceOwner: Long, list: List<Audio>, clear: Boolean): Flow<Boolean> {
        return flow {
            val db = helper.writableDatabase
            db.transaction {
                Settings.get().main().set_last_audio_sync(System.currentTimeMillis() / 1000L)
                if (clear) {
                    delete(
                        AudiosColumns.TABLENAME,
                        AudiosColumns.SOURCE_OWNER_ID + " = ?",
                        arrayOf(sourceOwner.toString())
                    )
                }
                for (i in list) {
                    val cv = ContentValues()
                    cv.put(AudiosColumns.SOURCE_OWNER_ID, sourceOwner)
                    cv.put(AudiosColumns.AUDIO_ID, i.id)
                    cv.put(AudiosColumns.AUDIO_OWNER_ID, i.ownerId)
                    cv.put(AudiosColumns.ARTIST, i.artist)
                    cv.put(AudiosColumns.TITLE, i.title)
                    cv.put(AudiosColumns.DURATION, i.duration)
                    cv.put(AudiosColumns.URL, i.url)
                    cv.put(AudiosColumns.LYRICS_ID, i.lyricsId)
                    cv.put(AudiosColumns.DATE, i.date)
                    cv.put(AudiosColumns.ALBUM_ID, i.albumId)
                    cv.put(AudiosColumns.ALBUM_OWNER_ID, i.album_owner_id)
                    cv.put(AudiosColumns.ALBUM_ACCESS_KEY, i.album_access_key)
                    cv.put(AudiosColumns.GENRE, i.genre)
                    cv.put(AudiosColumns.DELETED, i.isDeleted)
                    cv.put(AudiosColumns.ACCESS_KEY, i.accessKey)
                    cv.put(AudiosColumns.THUMB_IMAGE_BIG, i.thumb_image_big)
                    cv.put(AudiosColumns.THUMB_IMAGE_VERY_BIG, i.thumb_image_very_big)
                    cv.put(AudiosColumns.THUMB_IMAGE_LITTLE, i.thumb_image_little)
                    cv.put(AudiosColumns.ALBUM_TITLE, i.album_title)
                    i.main_artists.ifNonNull({
                        cv.put(
                            AudiosColumns.MAIN_ARTISTS,
                            MsgPack.encodeToByteArrayEx(
                                MapSerializer(
                                    String.serializer(),
                                    String.serializer()
                                ), it
                            )
                        )
                    }, {
                        cv.putNull(AudiosColumns.MAIN_ARTISTS)
                    })
                    cv.put(AudiosColumns.IS_HQ, i.isHq)
                    insert(AudiosColumns.TABLENAME, null, cv)
                }
                emit(true)
            }
        }
    }

    override fun getLogAll(type: Int): Flow<List<LogEvent>> {
        return flow {
            val cursor = helper.writableDatabase.query(
                LogsColumns.TABLENAME,
                PROJECTION_LOG,
                LogsColumns.TYPE + " = ?",
                arrayOf(type.toString()),
                null,
                null,
                BaseColumns._ID + " DESC"
            )
            val data: MutableList<LogEvent> = ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                data.add(mapLog(cursor))
            }
            cursor.close()
            emit(data)
        }
    }

    companion object {
        private val PROJECTION_TEMPORARY = arrayOf(
            BaseColumns._ID,
            TempDataColumns.OWNER_ID,
            TempDataColumns.SOURCE_ID,
            TempDataColumns.DATA
        )
        private val PROJECTION_SEARCH = arrayOf(
            BaseColumns._ID, SearchRequestColumns.SOURCE_ID, SearchRequestColumns.QUERY
        )
        private val PROJECTION_SHORTCUT = arrayOf(
            BaseColumns._ID, ShortcutsColumns.ACTION, ShortcutsColumns.COVER, ShortcutsColumns.NAME
        )
        private val PROJECTION_LOG = arrayOf(
            BaseColumns._ID,
            LogsColumns.TYPE,
            LogsColumns.DATE,
            LogsColumns.TAG,
            LogsColumns.BODY
        )

        private val PROJECTION_REACTION_ASSET = arrayOf(
            BaseColumns._ID,
            ReactionsColumns.REACTION_ID,
            ReactionsColumns.BIG_ANIMATION,
            ReactionsColumns.SMALL_ANIMATION,
            ReactionsColumns.STATIC
        )

        private val PROJECTION_AUDIO = arrayOf(
            BaseColumns._ID,
            AudiosColumns.SOURCE_OWNER_ID,
            AudiosColumns.AUDIO_ID,
            AudiosColumns.AUDIO_OWNER_ID,
            AudiosColumns.ARTIST,
            AudiosColumns.TITLE,
            AudiosColumns.DURATION,
            AudiosColumns.URL,
            AudiosColumns.LYRICS_ID,
            AudiosColumns.DATE,
            AudiosColumns.ALBUM_ID,
            AudiosColumns.ALBUM_OWNER_ID,
            AudiosColumns.ALBUM_ACCESS_KEY,
            AudiosColumns.GENRE,
            AudiosColumns.DELETED,
            AudiosColumns.ACCESS_KEY,
            AudiosColumns.THUMB_IMAGE_BIG,
            AudiosColumns.THUMB_IMAGE_VERY_BIG,
            AudiosColumns.THUMB_IMAGE_LITTLE,
            AudiosColumns.ALBUM_TITLE,
            AudiosColumns.MAIN_ARTISTS,
            AudiosColumns.IS_HQ
        )

        internal fun mapAudio(cursor: Cursor): Audio {
            val v = Audio()
                .setId(cursor.getInt(AudiosColumns.AUDIO_ID))
                .setOwnerId(cursor.getLong(AudiosColumns.AUDIO_OWNER_ID))
                .setArtist(cursor.getString(AudiosColumns.ARTIST))
                .setTitle(cursor.getString(AudiosColumns.TITLE))
                .setDuration(cursor.getInt(AudiosColumns.DURATION))
                .setUrl(cursor.getString(AudiosColumns.URL))
                .setLyricsId(cursor.getInt(AudiosColumns.LYRICS_ID))
                .setDate(cursor.getLong(AudiosColumns.DATE))
                .setAlbumId(cursor.getInt(AudiosColumns.ALBUM_ID))
                .setAlbum_owner_id(cursor.getLong(AudiosColumns.ALBUM_OWNER_ID))
                .setAlbum_access_key(cursor.getString(AudiosColumns.ALBUM_ACCESS_KEY))
                .setGenre(cursor.getInt(AudiosColumns.GENRE))
                .setAccessKey(cursor.getString(AudiosColumns.ACCESS_KEY))
                .setAlbum_title(cursor.getString(AudiosColumns.ALBUM_TITLE))
                .setThumb_image_big(cursor.getString(AudiosColumns.THUMB_IMAGE_BIG))
                .setThumb_image_little(cursor.getString(AudiosColumns.THUMB_IMAGE_LITTLE))
                .setThumb_image_very_big(cursor.getString(AudiosColumns.THUMB_IMAGE_VERY_BIG))
                .setIsHq(cursor.getBoolean(AudiosColumns.IS_HQ))
                .setDeleted(cursor.getBoolean(AudiosColumns.DELETED))
            val artistsSourceText =
                cursor.getBlob(AudiosColumns.MAIN_ARTISTS)
            if (artistsSourceText.nonNullNoEmpty()) {
                v.setMain_artists(
                    MsgPack.decodeFromByteArrayEx(
                        MapSerializer(
                            String.serializer(),
                            String.serializer()
                        ), artistsSourceText
                    )
                )
            }
            v.updateDownloadIndicator()
            return v
        }

        internal fun mapLog(cursor: Cursor): LogEvent {
            return LogEvent(cursor.getInt(BaseColumns._ID))
                .setType(cursor.getInt(LogsColumns.TYPE))
                .setDate(cursor.getLong(LogsColumns.DATE))
                .setTag(cursor.getString(LogsColumns.TAG))
                .setBody(cursor.getString(LogsColumns.BODY))
        }

        internal fun mapShortcut(cursor: Cursor): ShortcutStored {
            return ShortcutStored()
                .setAction(cursor.getString(ShortcutsColumns.ACTION)!!)
                .setName(cursor.getString(ShortcutsColumns.NAME)!!)
                .setCover(cursor.getString(ShortcutsColumns.COVER)!!)
        }

        internal fun mapReactionAsset(cursor: Cursor): ReactionAssetEntity {
            return ReactionAssetEntity()
                .setReactionId(cursor.getInt(ReactionsColumns.REACTION_ID))
                .setBigAnimation(cursor.getString(ReactionsColumns.BIG_ANIMATION))
                .setSmallAnimation(cursor.getString(ReactionsColumns.SMALL_ANIMATION))
                .setStatic(cursor.getString(ReactionsColumns.STATIC))
        }
    }
}