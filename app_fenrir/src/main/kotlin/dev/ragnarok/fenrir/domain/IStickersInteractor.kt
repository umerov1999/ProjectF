package dev.ragnarok.fenrir.domain

import android.content.Context
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.StickerSet
import kotlinx.coroutines.flow.Flow

interface IStickersInteractor {
    fun receiveAndStoreCustomStickerSets(accountId: Long): Flow<Boolean>
    fun receiveAndStoreStickerSets(accountId: Long): Flow<Boolean>
    fun receiveAndStoreKeywordsStickers(accountId: Long): Flow<Boolean>
    fun getStickerSets(accountId: Long): Flow<List<StickerSet>>
    fun getKeywordsStickers(accountId: Long, s: String?): Flow<List<Sticker>>
    fun placeToStickerCache(context: Context): Flow<Boolean>
}