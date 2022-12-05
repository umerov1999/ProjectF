package dev.ragnarok.fenrir.domain.impl

import android.content.Context
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiSticker
import dev.ragnarok.fenrir.api.model.VKApiStickerSet.Product
import dev.ragnarok.fenrir.api.model.VKApiStickerSetsData
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import dev.ragnarok.fenrir.db.interfaces.IStickersStorage
import dev.ragnarok.fenrir.db.model.entity.StickerSetEntity
import dev.ragnarok.fenrir.db.model.entity.StickersKeywordsEntity
import dev.ragnarok.fenrir.domain.IStickersInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapSticker
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapStikerSet
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildStickerFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.map
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAllMutable
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.Sticker.LocalSticker
import dev.ragnarok.fenrir.model.StickerSet
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermissionSimple
import dev.ragnarok.fenrir.util.Utils.getCachedMyStickers
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNullMutable
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.util.*

class StickersInteractor(private val networker: INetworker, private val storage: IStickersStorage) :
    IStickersInteractor {
    override fun getAndStoreStickerSets(accountId: Int): Completable {
        val stickerSet = networker.vkDefault(accountId)
            .store()
            .stickers
            .flatMapCompletable { items: VKApiStickerSetsData? ->
                val list: MutableList<Product> = listEmptyIfNullMutable(items?.sticker_pack?.items)
                if (Settings.get().ui().isStickers_by_new) {
                    list.reverse()
                }
                val temp = StickerSetEntity(-1).setTitle("recent")
                    .setStickers(mapAll(listEmptyIfNull(listEmptyIfNull(items?.recent?.items))) {
                        mapSticker(
                            it
                        )
                    }).setActive(true).setPurchased(true)
                val ret =
                    mapAllMutable(list) { mapStikerSet(it) }
                ret.add(temp)
                storage.storeStickerSets(accountId, ret)
            }
        return if (Settings.get().other().isHint_stickers) {
            stickerSet.andThen(
                networker.vkDefault(accountId)
                    .store()
                    .stickerKeywords
                    .flatMapCompletable { hint: VKApiStickersKeywords ->
                        getStickersKeywordsAndStore(
                            accountId,
                            hint
                        )
                    })
        } else stickerSet
    }

    private fun generateKeywords(
        s: List<List<VKApiSticker>?>,
        w: List<List<String>?>
    ): List<StickersKeywordsEntity> {
        val ret: MutableList<StickersKeywordsEntity> = ArrayList(w.size)
        for (i in w.indices) {
            if (w[i].isNullOrEmpty()) {
                continue
            }
            if (s[i].isNullOrEmpty()) {
                continue
            }
            ret.add(
                StickersKeywordsEntity(
                    w[i], mapAll(
                        s[i]
                    ) { mapSticker(it) })
            )
        }
        return ret
    }

    private fun getStickersKeywordsAndStore(
        accountId: Int,
        items: VKApiStickersKeywords
    ): Completable {
        val s: List<List<VKApiSticker>?> = listEmptyIfNull(items.words_stickers)
        val w: List<List<String>?> = listEmptyIfNull(items.keywords)
        val temp: MutableList<StickersKeywordsEntity> = ArrayList()
        if (w.isEmpty() || s.isEmpty() || w.size != s.size) {
            return storage.storeKeyWords(accountId, temp)
        }
        temp.addAll(generateKeywords(s, w))
        return storage.storeKeyWords(accountId, temp)
    }

    override fun getStickerSets(accountId: Int): Single<List<StickerSet>> {
        return storage.getPurchasedAndActive(accountId)
            .map { entities ->
                mapAll(entities) {
                    map(
                        it
                    )
                }
            }
    }

    override fun getKeywordsStickers(accountId: Int, s: String?): Single<List<Sticker>> {
        return storage.getKeywordsStickers(accountId, s)
            .map { entities ->
                mapAll(entities) {
                    buildStickerFromDbo(
                        it
                    )
                }
            }
    }

    override fun placeToStickerCache(context: Context): Completable {
        return if (!hasReadStoragePermissionSimple(context)) {
            Completable.complete()
        } else {
            Completable.create { t ->
                val temp = File(Settings.get().other().stickerDir)
                if (!temp.exists()) {
                    t.onComplete()
                    return@create
                }
                val file_list = temp.listFiles()
                if (file_list == null || file_list.isEmpty()) {
                    t.onComplete()
                    return@create
                }
                Arrays.sort(file_list) { o1: File, o2: File ->
                    o2.lastModified().compareTo(o1.lastModified())
                }
                getCachedMyStickers().clear()
                for (u in file_list) {
                    if (u.isFile && (u.name.contains(".png") || u.name.contains(".webp"))) {
                        getCachedMyStickers().add(LocalSticker(u.absolutePath, false))
                    } else if (u.isFile && u.name.contains(".json")) {
                        getCachedMyStickers().add(LocalSticker(u.absolutePath, true))
                    }
                }
            }
        }
    }
}