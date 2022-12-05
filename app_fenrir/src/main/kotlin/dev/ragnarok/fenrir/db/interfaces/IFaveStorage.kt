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
        accountId: Int,
        posts: List<PostDboEntity>,
        owners: OwnerEntities?,
        clearBeforeStore: Boolean
    ): Completable

    @CheckResult
    fun getFaveLinks(accountId: Int): Single<List<FaveLinkEntity>>
    fun removeLink(accountId: Int, id: String?): Completable
    fun storeLinks(
        accountId: Int,
        entities: List<FaveLinkEntity>,
        clearBefore: Boolean
    ): Completable

    @CheckResult
    fun storePages(
        accountId: Int,
        users: List<FavePageEntity>,
        clearBeforeStore: Boolean
    ): Completable

    fun getFaveUsers(accountId: Int): Single<List<FavePageEntity>>
    fun removePage(accountId: Int, ownerId: Int, isUser: Boolean): Completable

    @CheckResult
    fun storePhotos(
        accountId: Int,
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
        accountId: Int,
        videos: List<VideoDboEntity>,
        clearBeforeStore: Boolean
    ): Single<IntArray>

    @CheckResult
    fun storeArticles(
        accountId: Int,
        articles: List<ArticleDboEntity>,
        clearBeforeStore: Boolean
    ): Single<IntArray>

    @CheckResult
    fun storeProducts(
        accountId: Int,
        products: List<MarketDboEntity>,
        clearBeforeStore: Boolean
    ): Single<IntArray>

    fun getFaveGroups(accountId: Int): Single<List<FavePageEntity>>
    fun storeGroups(
        accountId: Int,
        groups: List<FavePageEntity>,
        clearBeforeStore: Boolean
    ): Completable
}