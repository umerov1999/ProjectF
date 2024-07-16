package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.StickerDboEntity
import dev.ragnarok.fenrir.db.model.entity.StickerSetEntity
import dev.ragnarok.fenrir.db.model.entity.StickersKeywordsEntity
import kotlinx.coroutines.flow.Flow

interface IStickersStorage : IStorage {
    fun storeStickerSets(accountId: Long, sets: List<StickerSetEntity>): Flow<Boolean>
    fun storeStickerSetsCustom(accountId: Long, sets: List<StickerSetEntity>): Flow<Boolean>
    fun storeKeyWords(accountId: Long, sets: List<StickersKeywordsEntity>): Flow<Boolean>
    fun getStickerSets(accountId: Long): Flow<List<StickerSetEntity>>
    fun getKeywordsStickers(accountId: Long, s: String?): Flow<List<StickerDboEntity>>
    fun clearAccount(accountId: Long): Flow<Boolean>
}