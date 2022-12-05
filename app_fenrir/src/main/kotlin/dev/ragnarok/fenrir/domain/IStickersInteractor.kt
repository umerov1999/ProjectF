package dev.ragnarok.fenrir.domain

import android.content.Context
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.StickerSet
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IStickersInteractor {
    fun getAndStoreStickerSets(accountId: Int): Completable
    fun getStickerSets(accountId: Int): Single<List<StickerSet>>
    fun getKeywordsStickers(accountId: Int, s: String?): Single<List<Sticker>>
    fun placeToStickerCache(context: Context): Completable
}