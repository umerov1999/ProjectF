package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.ReactionAssetEntity
import dev.ragnarok.fenrir.db.serialize.ISerializeAdapter
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.LogEvent
import dev.ragnarok.fenrir.model.ShortcutStored
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface ITempDataStorage {
    fun <T> getTemporaryData(
        ownerId: Long,
        sourceId: Int,
        serializer: ISerializeAdapter<T>
    ): Single<List<T>>

    fun <T> putTemporaryData(
        ownerId: Long,
        sourceId: Int,
        data: List<T>,
        serializer: ISerializeAdapter<T>
    ): Completable

    fun deleteTemporaryData(ownerId: Long): Completable

    fun getSearchQueries(sourceId: Int): Single<List<String>>

    fun insertSearchQuery(sourceId: Int, query: String?): Completable

    fun deleteSearch(sourceId: Int): Completable

    fun addLog(type: Int, tag: String, body: String): Single<LogEvent>

    fun getLogAll(type: Int): Single<List<LogEvent>>

    fun addShortcut(action: String, cover: String, name: String): Completable

    fun addShortcuts(list: List<ShortcutStored>): Completable

    fun deleteShortcut(action: String): Completable

    fun getShortcutAll(): Single<List<ShortcutStored>>

    fun getAudiosAll(sourceOwner: Long): Single<List<Audio>>

    fun addAudios(sourceOwner: Long, list: List<Audio>, clear: Boolean): Completable

    fun deleteAudios(): Completable

    fun deleteAudio(sourceOwner: Long, id: Int, ownerId: Long): Completable

    fun addReactionsAssets(accountId: Long, list: List<ReactionAssetEntity>): Completable

    fun getReactionsAssets(accountId: Long): Single<List<ReactionAssetEntity>>

    fun clearReactionAssets(accountId: Long): Completable
}