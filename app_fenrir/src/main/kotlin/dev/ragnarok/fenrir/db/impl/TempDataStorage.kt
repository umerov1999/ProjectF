package dev.ragnarok.fenrir.db.impl

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.TempDataHelper
import dev.ragnarok.fenrir.db.column.AudioColumns
import dev.ragnarok.fenrir.db.column.LogColumns
import dev.ragnarok.fenrir.db.column.SearchRequestColumns
import dev.ragnarok.fenrir.db.column.ShortcutColumns
import dev.ragnarok.fenrir.db.column.TempDataColumns
import dev.ragnarok.fenrir.db.interfaces.ITempDataStorage
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
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class TempDataStorage internal constructor(context: Context) : ITempDataStorage {
    private val app: Context = context.applicationContext
    private val helper: TempDataHelper by lazy {
        TempDataHelper(app)
    }

    override fun <T> getTemporaryData(
        ownerId: Long,
        sourceId: Int,
        serializer: ISerializeAdapter<T>
    ): Single<List<T>> {
        return Single.fromCallable {
            val start = System.currentTimeMillis()
            val where = TempDataColumns.OWNER_ID + " = ? AND " + TempDataColumns.SOURCE_ID + " = ?"
            val args = arrayOf(ownerId.toString(), sourceId.toString())
            val cursor = helper.readableDatabase.query(
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
            data
        }
    }

    override fun <T> putTemporaryData(
        ownerId: Long,
        sourceId: Int,
        data: List<T>,
        serializer: ISerializeAdapter<T>
    ): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            val start = System.currentTimeMillis()
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                // clear
                db.delete(
                    TempDataColumns.TABLENAME,
                    TempDataColumns.OWNER_ID + " = ? AND " + TempDataColumns.SOURCE_ID + " = ?",
                    arrayOf(ownerId.toString(), sourceId.toString())
                )
                for (t in data) {
                    if (emitter.isDisposed) {
                        break
                    }
                    val cv = ContentValues()
                    cv.put(TempDataColumns.OWNER_ID, ownerId)
                    cv.put(TempDataColumns.SOURCE_ID, sourceId)
                    cv.put(TempDataColumns.DATA, serializer.serialize(t))
                    db.insert(TempDataColumns.TABLENAME, null, cv)
                }
                if (!emitter.isDisposed) {
                    db.setTransactionSuccessful()
                }
            } finally {
                db.endTransaction()
            }
            log("TempDataStorage.put", start, "count: " + data.size)
            emitter.onComplete()
        }
    }

    override fun deleteTemporaryData(ownerId: Long): Completable {
        return Completable.fromAction {
            val start = System.currentTimeMillis()
            val count = helper.writableDatabase.delete(
                TempDataColumns.TABLENAME,
                TempDataColumns.OWNER_ID + " = ?", arrayOf(ownerId.toString())
            )
            log("TempDataStorage.delete", start, "count: $count")
        }
    }

    override fun getSearchQueries(sourceId: Int): Single<List<String>> {
        return Single.fromCallable {
            val start = System.currentTimeMillis()
            val where = SearchRequestColumns.SOURCE_ID + " = ?"
            val args = arrayOf(sourceId.toString())
            val cursor = helper.readableDatabase.query(
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
            data
        }
    }

    override fun insertSearchQuery(sourceId: Int, query: String?): Completable {
        if (query == null) {
            return Completable.complete()
        }
        val queryClean = query.trim { it <= ' ' }
        return if (queryClean.isEmpty()) {
            Completable.complete()
        } else Completable.create { emitter: CompletableEmitter ->
            val db = helper.writableDatabase
            db.beginTransaction()
            if (emitter.isDisposed) {
                db.endTransaction()
                emitter.onComplete()
                return@create
            }
            db.delete(
                SearchRequestColumns.TABLENAME,
                SearchRequestColumns.QUERY + " = ?", arrayOf(queryClean)
            )
            try {
                val cv = ContentValues()
                cv.put(SearchRequestColumns.SOURCE_ID, sourceId)
                cv.put(SearchRequestColumns.QUERY, queryClean)
                db.insert(SearchRequestColumns.TABLENAME, null, cv)
                if (!emitter.isDisposed) {
                    db.setTransactionSuccessful()
                }
            } finally {
                db.endTransaction()
            }
            emitter.onComplete()
        }
    }

    override fun deleteSearch(sourceId: Int): Completable {
        return Completable.fromAction {
            val start = System.currentTimeMillis()
            val count = helper.writableDatabase.delete(
                SearchRequestColumns.TABLENAME,
                SearchRequestColumns.SOURCE_ID + " = ?", arrayOf(sourceId.toString())
            )
            log("SearchRequestHelperStorage.delete", start, "count: $count")
        }
    }

    override fun addShortcut(action: String, cover: String, name: String): Completable {
        return Completable.create {
            val db = helper.writableDatabase
            db.beginTransaction()
            if (it.isDisposed) {
                db.endTransaction()
                it.onComplete()
                return@create
            }
            db.delete(
                ShortcutColumns.TABLENAME,
                ShortcutColumns.ACTION + " = ?", arrayOf(action)
            )
            try {
                val cv = ContentValues()
                cv.put(ShortcutColumns.ACTION, action)
                cv.put(ShortcutColumns.NAME, name)
                cv.put(ShortcutColumns.COVER, cover)
                db.insert(ShortcutColumns.TABLENAME, null, cv)
                if (!it.isDisposed) {
                    db.setTransactionSuccessful()
                }
            } finally {
                db.endTransaction()
            }
            it.onComplete()
        }
    }

    override fun addShortcuts(list: List<ShortcutStored>): Completable {
        return Completable.create {
            val db = helper.writableDatabase
            db.beginTransaction()
            if (it.isDisposed) {
                db.endTransaction()
                it.onComplete()
                return@create
            }
            try {
                for (i in list) {
                    db.delete(
                        ShortcutColumns.TABLENAME,
                        ShortcutColumns.ACTION + " = ?", arrayOf(i.action)
                    )
                    val cv = ContentValues()
                    cv.put(ShortcutColumns.ACTION, i.action)
                    cv.put(ShortcutColumns.NAME, i.name)
                    cv.put(ShortcutColumns.COVER, i.cover)
                    db.insert(ShortcutColumns.TABLENAME, null, cv)
                }
                if (!it.isDisposed) {
                    db.setTransactionSuccessful()
                }
            } finally {
                db.endTransaction()
            }
            it.onComplete()
        }
    }

    override fun deleteShortcut(action: String): Completable {
        return Completable.fromAction {
            val start = System.currentTimeMillis()
            val count = helper.writableDatabase.delete(
                ShortcutColumns.TABLENAME,
                ShortcutColumns.ACTION + " = ?", arrayOf(action)
            )
            log("SearchRequestHelperStorage.delete", start, "count: $count")
        }
    }

    override fun getShortcutAll(): Single<List<ShortcutStored>> {
        return Single.fromCallable {
            val cursor = helper.readableDatabase.query(
                ShortcutColumns.TABLENAME,
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
            data
        }
    }

    override fun addLog(type: Int, tag: String, body: String): Single<LogEvent> {
        return Single.fromCallable {
            val now = System.currentTimeMillis()
            val cv = ContentValues()
            cv.put(LogColumns.TYPE, type)
            cv.put(LogColumns.TAG, tag)
            cv.put(LogColumns.BODY, body)
            cv.put(LogColumns.DATE, now)
            val id = helper.writableDatabase.insert(LogColumns.TABLENAME, null, cv)
            LogEvent(id.toInt())
                .setBody(body)
                .setTag(tag)
                .setDate(now)
                .setType(type)
        }
    }

    override fun deleteAudio(sourceOwner: Long, id: Int, ownerId: Long): Completable {
        return Completable.fromAction {
            helper.writableDatabase.delete(
                AudioColumns.TABLENAME,
                AudioColumns.SOURCE_OWNER_ID + " = ? AND " + AudioColumns.AUDIO_ID + " = ? AND " + AudioColumns.AUDIO_OWNER_ID + " = ?",
                arrayOf(sourceOwner.toString(), id.toString(), ownerId.toString())
            )
        }
    }

    override fun deleteAudios(): Completable {
        return Completable.fromAction {
            helper.writableDatabase.delete(
                AudioColumns.TABLENAME,
                null, null
            )
        }
    }

    override fun getAudiosAll(sourceOwner: Long): Single<List<Audio>> {
        return Single.fromCallable {
            val cursor = helper.readableDatabase.query(
                AudioColumns.TABLENAME,
                PROJECTION_AUDIO,
                AudioColumns.SOURCE_OWNER_ID + " = ?",
                arrayOf(sourceOwner.toString()),
                null,
                null,
                AudioColumns.DATE + " DESC"
            )
            val data: MutableList<Audio> = ArrayList(cursor.count)
            while (cursor.moveToNext()) {
                data.add(mapAudio(cursor))
            }
            cursor.close()
            data
        }
    }

    override fun addAudios(sourceOwner: Long, list: List<Audio>, clear: Boolean): Completable {
        return Completable.create { it1 ->
            val db = helper.writableDatabase
            db.beginTransaction()
            if (it1.isDisposed) {
                db.endTransaction()
                it1.onComplete()
                return@create
            }
            Settings.get().other().set_last_audio_sync(System.currentTimeMillis() / 1000L)
            try {
                if (clear) {
                    db.delete(
                        AudioColumns.TABLENAME,
                        AudioColumns.SOURCE_OWNER_ID + " = ?", arrayOf(sourceOwner.toString())
                    )
                }
                for (i in list) {
                    val cv = ContentValues()
                    cv.put(AudioColumns.SOURCE_OWNER_ID, sourceOwner)
                    cv.put(AudioColumns.AUDIO_ID, i.id)
                    cv.put(AudioColumns.AUDIO_OWNER_ID, i.ownerId)
                    cv.put(AudioColumns.ARTIST, i.artist)
                    cv.put(AudioColumns.TITLE, i.title)
                    cv.put(AudioColumns.DURATION, i.duration)
                    cv.put(AudioColumns.URL, i.url)
                    cv.put(AudioColumns.LYRICS_ID, i.lyricsId)
                    cv.put(AudioColumns.DATE, i.date)
                    cv.put(AudioColumns.ALBUM_ID, i.albumId)
                    cv.put(AudioColumns.ALBUM_OWNER_ID, i.album_owner_id)
                    cv.put(AudioColumns.ALBUM_ACCESS_KEY, i.album_access_key)
                    cv.put(AudioColumns.GENRE, i.genre)
                    cv.put(AudioColumns.DELETED, i.isDeleted)
                    cv.put(AudioColumns.ACCESS_KEY, i.accessKey)
                    cv.put(AudioColumns.THUMB_IMAGE_BIG, i.thumb_image_big)
                    cv.put(AudioColumns.THUMB_IMAGE_VERY_BIG, i.thumb_image_very_big)
                    cv.put(AudioColumns.THUMB_IMAGE_LITTLE, i.thumb_image_little)
                    cv.put(AudioColumns.ALBUM_TITLE, i.album_title)
                    i.main_artists.ifNonNull({
                        cv.put(
                            AudioColumns.MAIN_ARTISTS,
                            MsgPack.encodeToByteArrayEx(
                                MapSerializer(
                                    String.serializer(),
                                    String.serializer()
                                ), it
                            )
                        )
                    }, {
                        cv.putNull(AudioColumns.MAIN_ARTISTS)
                    })
                    cv.put(AudioColumns.IS_HQ, i.isHq)
                    db.insert(AudioColumns.TABLENAME, null, cv)
                }
                if (!it1.isDisposed) {
                    db.setTransactionSuccessful()
                }
            } finally {
                db.endTransaction()
            }
            it1.onComplete()
        }
    }

    override fun getLogAll(type: Int): Single<List<LogEvent>> {
        return Single.fromCallable {
            val cursor = helper.readableDatabase.query(
                LogColumns.TABLENAME,
                PROJECTION_LOG,
                LogColumns.TYPE + " = ?",
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
            data
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
            BaseColumns._ID, ShortcutColumns.ACTION, ShortcutColumns.COVER, ShortcutColumns.NAME
        )
        private val PROJECTION_LOG = arrayOf(
            BaseColumns._ID,
            LogColumns.TYPE,
            LogColumns.DATE,
            LogColumns.TAG,
            LogColumns.BODY
        )

        private val PROJECTION_AUDIO = arrayOf(
            BaseColumns._ID,
            AudioColumns.SOURCE_OWNER_ID,
            AudioColumns.AUDIO_ID,
            AudioColumns.AUDIO_OWNER_ID,
            AudioColumns.ARTIST,
            AudioColumns.TITLE,
            AudioColumns.DURATION,
            AudioColumns.URL,
            AudioColumns.LYRICS_ID,
            AudioColumns.DATE,
            AudioColumns.ALBUM_ID,
            AudioColumns.ALBUM_OWNER_ID,
            AudioColumns.ALBUM_ACCESS_KEY,
            AudioColumns.GENRE,
            AudioColumns.DELETED,
            AudioColumns.ACCESS_KEY,
            AudioColumns.THUMB_IMAGE_BIG,
            AudioColumns.THUMB_IMAGE_VERY_BIG,
            AudioColumns.THUMB_IMAGE_LITTLE,
            AudioColumns.ALBUM_TITLE,
            AudioColumns.MAIN_ARTISTS,
            AudioColumns.IS_HQ
        )

        internal fun mapAudio(cursor: Cursor): Audio {
            val v = Audio()
                .setId(cursor.getInt(AudioColumns.AUDIO_ID))
                .setOwnerId(cursor.getLong(AudioColumns.AUDIO_OWNER_ID))
                .setArtist(cursor.getString(AudioColumns.ARTIST))
                .setTitle(cursor.getString(AudioColumns.TITLE))
                .setDuration(cursor.getInt(AudioColumns.DURATION))
                .setUrl(cursor.getString(AudioColumns.URL))
                .setLyricsId(cursor.getInt(AudioColumns.LYRICS_ID))
                .setDate(cursor.getLong(AudioColumns.DATE))
                .setAlbumId(cursor.getInt(AudioColumns.ALBUM_ID))
                .setAlbum_owner_id(cursor.getLong(AudioColumns.ALBUM_OWNER_ID))
                .setAlbum_access_key(cursor.getString(AudioColumns.ALBUM_ACCESS_KEY))
                .setGenre(cursor.getInt(AudioColumns.GENRE))
                .setAccessKey(cursor.getString(AudioColumns.ACCESS_KEY))
                .setAlbum_title(cursor.getString(AudioColumns.ALBUM_TITLE))
                .setThumb_image_big(cursor.getString(AudioColumns.THUMB_IMAGE_BIG))
                .setThumb_image_little(cursor.getString(AudioColumns.THUMB_IMAGE_LITTLE))
                .setThumb_image_very_big(cursor.getString(AudioColumns.THUMB_IMAGE_VERY_BIG))
                .setIsHq(cursor.getBoolean(AudioColumns.IS_HQ))
                .setDeleted(cursor.getBoolean(AudioColumns.DELETED))
            val artistsSourceText =
                cursor.getBlob(AudioColumns.MAIN_ARTISTS)
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
                .setType(cursor.getInt(LogColumns.TYPE))
                .setDate(cursor.getLong(LogColumns.DATE))
                .setTag(cursor.getString(LogColumns.TAG))
                .setBody(cursor.getString(LogColumns.BODY))
        }

        internal fun mapShortcut(cursor: Cursor): ShortcutStored {
            return ShortcutStored()
                .setAction(cursor.getString(ShortcutColumns.ACTION)!!)
                .setName(cursor.getString(ShortcutColumns.NAME)!!)
                .setCover(cursor.getString(ShortcutColumns.COVER)!!)
        }
    }
}