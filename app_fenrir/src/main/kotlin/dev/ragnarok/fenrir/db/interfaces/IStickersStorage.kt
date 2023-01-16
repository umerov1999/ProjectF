package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.StickerDboEntity
import dev.ragnarok.fenrir.db.model.entity.StickerSetEntity
import dev.ragnarok.fenrir.db.model.entity.StickersKeywordsEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IStickersStorage : IStorage {
    fun storeStickerSets(accountId: Long, sets: List<StickerSetEntity>): Completable
    fun storeKeyWords(accountId: Long, sets: List<StickersKeywordsEntity>): Completable
    fun getPurchasedAndActive(accountId: Long): Single<List<StickerSetEntity>>
    fun getKeywordsStickers(accountId: Long, s: String?): Single<List<StickerDboEntity>>
    fun clearAccount(accountId: Long): Completable
}