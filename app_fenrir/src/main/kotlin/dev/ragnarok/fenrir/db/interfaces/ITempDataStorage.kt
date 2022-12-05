package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.serialize.ISerializeAdapter
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.LogEvent
import dev.ragnarok.fenrir.model.ShortcutStored
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface ITempDataStorage {
    fun <T> getTemporaryData(
        ownerId: Int,
        sourceId: Int,
        serializer: ISerializeAdapter<T>
    ): Single<List<T>>

    fun <T> putTemporaryData(
        ownerId: Int,
        sourceId: Int,
        data: List<T>,
        serializer: ISerializeAdapter<T>
    ): Completable

    fun deleteTemporaryData(ownerId: Int): Completable

    fun getSearchQueries(sourceId: Int): Single<List<String>>

    fun insertSearchQuery(sourceId: Int, query: String?): Completable

    fun deleteSearch(sourceId: Int): Completable

    fun addLog(type: Int, tag: String, body: String): Single<LogEvent>

    fun getLogAll(type: Int): Single<List<LogEvent>>

    fun addShortcut(action: String, cover: String, name: String): Completable

    fun addShortcuts(list: List<ShortcutStored>): Completable

    fun deleteShortcut(action: String): Completable

    fun getShortcutAll(): Single<List<ShortcutStored>>

    fun getAudiosAll(sourceOwner: Int): Single<List<Audio>>

    fun addAudios(sourceOwner: Int, list: List<Audio>, clear: Boolean): Completable

    fun deleteAudios(): Completable

    fun deleteAudio(sourceOwner: Int, id: Int, ownerId: Int): Completable
}