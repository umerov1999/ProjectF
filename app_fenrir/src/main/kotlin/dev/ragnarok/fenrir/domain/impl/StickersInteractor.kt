package dev.ragnarok.fenrir.domain.impl

import android.content.Context
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.VKApiStickerSet.Product
import dev.ragnarok.fenrir.api.model.VKApiStickersKeywords
import dev.ragnarok.fenrir.db.interfaces.IStickersStorage
import dev.ragnarok.fenrir.db.model.entity.StickerDboEntity
import dev.ragnarok.fenrir.db.model.entity.StickerSetEntity
import dev.ragnarok.fenrir.db.model.entity.StickersKeywordsEntity
import dev.ragnarok.fenrir.domain.IStickersInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapSticker
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapStickerSet
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildStickerFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.map
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAllMutable
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.model.Sticker.LocalSticker
import dev.ragnarok.fenrir.model.StickerSet
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermissionSimple
import dev.ragnarok.fenrir.util.Utils.getCachedMyStickers
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNullMutable
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.util.Arrays

class StickersInteractor(private val networker: INetworker, private val storage: IStickersStorage) :
    IStickersInteractor {
    override fun reciveAndStoreCustomStickerSets(accountId: Long): Completable {
        return networker.vkDefault(accountId)
            .store().recentStickers
            .flatMapCompletable { items ->
                val temp = StickerSetEntity(-1).setTitle("recent")
                    .setStickers(mapAll(listEmptyIfNull(listEmptyIfNull(items.items))) {
                        mapSticker(
                            it
                        )
                    }).setActive(true).setPurchased(true)
                if (items.items.isNullOrEmpty()) {
                    Settings.get().main().del_last_sticker_sets_custom_sync(accountId)
                }
                storage.storeStickerSetsCustom(accountId, listOf(temp))
            }
    }

    override fun reciveAndStoreStickerSets(accountId: Long): Completable {
        return networker.vkDefault(accountId)
            .store()
            .stickersSets
            .flatMapCompletable { items ->
                val list: MutableList<Product> = listEmptyIfNullMutable(items.items)
                if (Settings.get().ui().isStickers_by_new) {
                    list.reverse()
                }
                val ret = mapAllMutable(list) { mapStickerSet(it) }
                if (list.isEmpty()) {
                    Settings.get().main().del_last_sticker_sets_sync(accountId)
                }
                storage.storeStickerSets(accountId, ret)
            }
    }

    override fun reciveAndStoreKeywordsStickers(accountId: Long): Completable {
        return networker.vkDefault(accountId)
            .store()
            .stickerKeywords
            .flatMapCompletable { items ->
                val list: MutableList<VKApiStickersKeywords> =
                    listEmptyIfNullMutable(items.dictionary)
                val temp: MutableList<StickersKeywordsEntity> = ArrayList()
                for (i in list) {
                    val userStickers = ArrayList<StickerDboEntity>(i.user_stickers?.size.orZero())
                    val stickersKeywords = ArrayList<String>(i.words?.size.orZero())
                    for (s in i.user_stickers.orEmpty()) {
                        s?.let { mapSticker(it) }?.let { userStickers.add(it) }
                    }
                    for (s in i.words.orEmpty()) {
                        s.nonNullNoEmpty {
                            stickersKeywords.add(it)
                        }
                    }
                    temp.add(StickersKeywordsEntity(stickersKeywords, userStickers))
                }
                if (list.isEmpty()) {
                    Settings.get().main().del_last_sticker_keywords_sync(accountId)
                }
                storage.storeKeyWords(accountId, temp)
            }
    }

    override fun getStickerSets(accountId: Long): Single<List<StickerSet>> {
        return storage.getStickerSets(accountId)
            .map { entities ->
                mapAll(entities) {
                    map(
                        it
                    )
                }
            }
    }

    override fun getKeywordsStickers(accountId: Long, s: String?): Single<List<Sticker>> {
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
                val temp = File(Settings.get().main().stickerDir)
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