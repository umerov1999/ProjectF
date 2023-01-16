package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.entity.*
import dev.ragnarok.fenrir.model.criteria.*
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IFaveStorage : IStorage {
    @CheckResult
    fun getFavePosts(criteria: FavePostsCriteria): Single<List<PostDboEntity>>

    @CheckResult
    fun storePosts(
        accountId: Long,
        posts: List<PostDboEntity>,
        owners: OwnerEntities?,
        clearBeforeStore: Boolean
    ): Completable

    @CheckResult
    fun getFaveLinks(accountId: Long): Single<List<FaveLinkEntity>>
    fun removeLink(accountId: Long, id: String?): Completable
    fun storeLinks(
        accountId: Long,
        entities: List<FaveLinkEntity>,
        clearBefore: Boolean
    ): Completable

    @CheckResult
    fun storePages(
        accountId: Long,
        users: List<FavePageEntity>,
        clearBeforeStore: Boolean
    ): Completable

    fun getFaveUsers(accountId: Long): Single<List<FavePageEntity>>
    fun removePage(accountId: Long, ownerId: Long, isUser: Boolean): Completable

    @CheckResult
    fun storePhotos(
        accountId: Long,
        photos: List<PhotoDboEntity>,
        clearBeforeStore: Boolean
    ): Single<IntArray>

    @CheckResult
    fun getPhotos(criteria: FavePhotosCriteria): Single<List<PhotoDboEntity>>

    @CheckResult
    fun getVideos(criteria: FaveVideosCriteria): Single<List<VideoDboEntity>>

    @CheckResult
    fun getArticles(criteria: FaveArticlesCriteria): Single<List<ArticleDboEntity>>

    @CheckResult
    fun getProducts(criteria: FaveProductsCriteria): Single<List<MarketDboEntity>>

    @CheckResult
    fun storeVideos(
        accountId: Long,
        videos: List<VideoDboEntity>,
        clearBeforeStore: Boolean
    ): Single<IntArray>

    @CheckResult
    fun storeArticles(
        accountId: Long,
        articles: List<ArticleDboEntity>,
        clearBeforeStore: Boolean
    ): Single<IntArray>

    @CheckResult
    fun storeProducts(
        accountId: Long,
        products: List<MarketDboEntity>,
        clearBeforeStore: Boolean
    ): Single<IntArray>

    fun getFaveGroups(accountId: Long): Single<List<FavePageEntity>>
    fun storeGroups(
        accountId: Long,
        groups: List<FavePageEntity>,
        clearBeforeStore: Boolean
    ): Completable
}