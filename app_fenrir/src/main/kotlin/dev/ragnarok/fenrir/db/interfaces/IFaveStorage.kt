package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.entity.ArticleDboEntity
import dev.ragnarok.fenrir.db.model.entity.FaveLinkEntity
import dev.ragnarok.fenrir.db.model.entity.FavePageEntity
import dev.ragnarok.fenrir.db.model.entity.MarketDboEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PhotoDboEntity
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity
import dev.ragnarok.fenrir.db.model.entity.VideoDboEntity
import dev.ragnarok.fenrir.model.criteria.FaveArticlesCriteria
import dev.ragnarok.fenrir.model.criteria.FavePhotosCriteria
import dev.ragnarok.fenrir.model.criteria.FavePostsCriteria
import dev.ragnarok.fenrir.model.criteria.FaveProductsCriteria
import dev.ragnarok.fenrir.model.criteria.FaveVideosCriteria
import kotlinx.coroutines.flow.Flow

interface IFaveStorage : IStorage {
    @CheckResult
    fun getFavePosts(criteria: FavePostsCriteria): Flow<List<PostDboEntity>>

    @CheckResult
    fun storePosts(
        accountId: Long,
        posts: List<PostDboEntity>,
        owners: OwnerEntities?,
        clearBeforeStore: Boolean
    ): Flow<Boolean>

    @CheckResult
    fun getFaveLinks(accountId: Long): Flow<List<FaveLinkEntity>>
    fun removeLink(accountId: Long, id: String?): Flow<Boolean>
    fun storeLinks(
        accountId: Long,
        entities: List<FaveLinkEntity>,
        clearBefore: Boolean
    ): Flow<Boolean>

    @CheckResult
    fun storePages(
        accountId: Long,
        users: List<FavePageEntity>,
        clearBeforeStore: Boolean
    ): Flow<Boolean>

    fun getFaveUsers(accountId: Long): Flow<List<FavePageEntity>>
    fun removePage(accountId: Long, ownerId: Long, isUser: Boolean): Flow<Boolean>

    @CheckResult
    fun storePhotos(
        accountId: Long,
        photos: List<PhotoDboEntity>,
        clearBeforeStore: Boolean
    ): Flow<IntArray>

    @CheckResult
    fun getPhotos(criteria: FavePhotosCriteria): Flow<List<PhotoDboEntity>>

    @CheckResult
    fun getVideos(criteria: FaveVideosCriteria): Flow<List<VideoDboEntity>>

    @CheckResult
    fun getArticles(criteria: FaveArticlesCriteria): Flow<List<ArticleDboEntity>>

    @CheckResult
    fun getProducts(criteria: FaveProductsCriteria): Flow<List<MarketDboEntity>>

    @CheckResult
    fun storeVideos(
        accountId: Long,
        videos: List<VideoDboEntity>,
        clearBeforeStore: Boolean
    ): Flow<IntArray>

    @CheckResult
    fun storeArticles(
        accountId: Long,
        articles: List<ArticleDboEntity>,
        clearBeforeStore: Boolean
    ): Flow<IntArray>

    @CheckResult
    fun storeProducts(
        accountId: Long,
        products: List<MarketDboEntity>,
        clearBeforeStore: Boolean
    ): Flow<IntArray>

    fun getFaveGroups(accountId: Long): Flow<List<FavePageEntity>>
    fun storeGroups(
        accountId: Long,
        groups: List<FavePageEntity>,
        clearBeforeStore: Boolean
    ): Flow<Boolean>
}