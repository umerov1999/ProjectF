package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.FeedOwnersEntity
import dev.ragnarok.fenrir.db.model.entity.ReactionAssetEntity
import dev.ragnarok.fenrir.db.serialize.ISerializeAdapter
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.LogEvent
import dev.ragnarok.fenrir.model.ShortcutStored
import kotlinx.coroutines.flow.Flow

interface ITempDataStorage {
    fun <T> getTemporaryData(
        ownerId: Long,
        sourceId: Int,
        serializer: ISerializeAdapter<T>
    ): Flow<List<T>>

    fun <T> putTemporaryData(
        ownerId: Long,
        sourceId: Int,
        data: List<T>,
        serializer: ISerializeAdapter<T>
    ): Flow<Boolean>

    fun deleteTemporaryData(ownerId: Long): Flow<Boolean>

    fun getSearchQueries(sourceId: Int): Flow<List<String>>
    fun insertSearchQuery(sourceId: Int, query: String?): Flow<Boolean>
    fun deleteSearch(sourceId: Int): Flow<Boolean>

    fun addLog(type: Int, tag: String, body: String): Flow<LogEvent>
    fun getLogAll(type: Int): Flow<List<LogEvent>>

    fun addShortcut(action: String, cover: String, name: String): Flow<Boolean>
    fun addShortcuts(list: List<ShortcutStored>): Flow<Boolean>
    fun deleteShortcut(action: String): Flow<Boolean>
    fun getShortcutAll(): Flow<List<ShortcutStored>>

    fun getAudiosAll(sourceOwner: Long): Flow<List<Audio>>
    fun addAudios(sourceOwner: Long, list: List<Audio>, clear: Boolean): Flow<Boolean>
    fun deleteAudios(): Flow<Boolean>
    fun deleteAudio(sourceOwner: Long, id: Int, ownerId: Long): Flow<Boolean>

    fun addReactionsAssets(accountId: Long, list: List<ReactionAssetEntity>): Flow<Boolean>
    fun getReactionsAssets(accountId: Long): Flow<List<ReactionAssetEntity>>
    fun clearReactionAssets(accountId: Long): Flow<Boolean>

    fun addFeedOwners(title: String, owners: LongArray): Flow<FeedOwnersEntity>
    fun storeFeedOwners(list: List<FeedOwnersEntity>, clear: Boolean): Flow<Boolean>
    fun getFeedOwners(): Flow<List<FeedOwnersEntity>>
    fun getFeedOwnersById(id: Long): Flow<FeedOwnersEntity?>
    fun deleteFeedOwners(id: Long): Flow<Boolean>
    fun renameFeedOwners(id: Long, newTitle: String?): Flow<Boolean>
    fun updateFeedOwners(id: Long, owners: LongArray): Flow<Boolean>
}