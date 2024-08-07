package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.FaveLinkDto
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.db.model.entity.ArticleDboEntity
import dev.ragnarok.fenrir.db.model.entity.CommunityEntity
import dev.ragnarok.fenrir.db.model.entity.FaveLinkEntity
import dev.ragnarok.fenrir.db.model.entity.MarketDboEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PhotoDboEntity
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity
import dev.ragnarok.fenrir.db.model.entity.UserEntity
import dev.ragnarok.fenrir.db.model.entity.VideoDboEntity
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapArticle
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapCommunity
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapFavePage
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapMarket
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapOwners
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapPhoto
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapPost
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapUser
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapVideo
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformAttachmentsPosts
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformFaveUser
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transformOwners
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildArticleFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildFaveUsersFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildMarketFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildPostFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildVideoFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillPostOwnerIds
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.map
import dev.ragnarok.fenrir.domain.mappers.MapUtil.mapAll
import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.FaveLink
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.FavePageType
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.criteria.FaveArticlesCriteria
import dev.ragnarok.fenrir.model.criteria.FavePhotosCriteria
import dev.ragnarok.fenrir.model.criteria.FavePostsCriteria
import dev.ragnarok.fenrir.model.criteria.FaveProductsCriteria
import dev.ragnarok.fenrir.model.criteria.FaveVideosCriteria
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.VKOwnIds
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class FaveInteractor(
    private val networker: INetworker,
    private val cache: IStorages,
    private val ownersRepository: IOwnersRepository
) : IFaveInteractor {
    override fun getPosts(accountId: Long, count: Int, offset: Int): Flow<List<Post>> {
        return networker.vkDefault(accountId)
            .fave()
            .getPosts(offset, count)
            .flatMapConcat { response ->
                val dtos = listEmptyIfNull(response.posts)
                val owners = transformOwners(response.profiles, response.groups)
                val ids = VKOwnIds()
                for (dto in dtos) {
                    if (dto.attachment is VKApiPost) ids.append(dto.attachment)
                }
                val ownerEntities = mapOwners(response.profiles, response.groups)
                val dbos: MutableList<PostDboEntity> = ArrayList(safeCountOf(response.posts))
                response.posts.requireNonNull {
                    for (dto in it) {
                        if (dto.attachment is VKApiPost) dbos.add(mapPost(dto.attachment))
                    }
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ids.all,
                    IOwnersRepository.MODE_ANY,
                    owners
                )
                    .map { transformAttachmentsPosts(dtos, it) }
                    .flatMapConcat { posts ->
                        cache.fave()
                            .storePosts(accountId, dbos, ownerEntities, offset == 0)
                            .map {
                                posts
                            }
                    }
            }
    }

    override fun getCachedPosts(accountId: Long): Flow<List<Post>> {
        return cache.fave().getFavePosts(FavePostsCriteria(accountId))
            .flatMapConcat { postDbos ->
                val ids = VKOwnIds()
                for (dbo in postDbos) {
                    fillPostOwnerIds(ids, dbo)
                }
                ownersRepository.findBaseOwnersDataAsBundle(
                    accountId,
                    ids.all,
                    IOwnersRepository.MODE_ANY
                )
                    .map {
                        val posts: MutableList<Post> = ArrayList()
                        for (dbo in postDbos) {
                            posts.add(buildPostFromDbo(dbo, it))
                        }
                        posts
                    }
            }
    }

    override fun getCachedPhotos(accountId: Long): Flow<List<Photo>> {
        val criteria = FavePhotosCriteria(accountId)
        return cache.fave()
            .getPhotos(criteria)
            .map { photoDbos ->
                val photos: MutableList<Photo> = ArrayList(photoDbos.size)
                for (dbo in photoDbos) {
                    photos.add(map(dbo))
                }
                photos
            }
    }

    override fun getPhotos(accountId: Long, count: Int, offset: Int): Flow<List<Photo>> {
        return networker.vkDefault(accountId)
            .fave()
            .getPhotos(offset, count)
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val dbos: MutableList<PhotoDboEntity> = ArrayList(dtos.size)
                val photos: MutableList<Photo> = ArrayList(dtos.size)
                for (dto in dtos) {
                    dbos.add(mapPhoto(dto))
                    photos.add(transform(dto))
                }
                cache.fave().storePhotos(accountId, dbos, offset == 0)
                    .map { photos }
            }
    }

    override fun getCachedVideos(accountId: Long): Flow<List<Video>> {
        val criteria = FaveVideosCriteria(accountId)
        return cache.fave()
            .getVideos(criteria)
            .map { videoDbos ->
                val videos: MutableList<Video> = ArrayList(videoDbos.size)
                for (dbo in videoDbos) {
                    videos.add(buildVideoFromDbo(dbo))
                }
                videos
            }
    }

    override fun getCachedArticles(accountId: Long): Flow<List<Article>> {
        val criteria = FaveArticlesCriteria(accountId)
        return cache.fave()
            .getArticles(criteria)
            .map { articleDbos ->
                val articles: MutableList<Article> = ArrayList(articleDbos.size)
                for (dbo in articleDbos) {
                    articles.add(buildArticleFromDbo(dbo))
                }
                articles
            }
    }

    override fun getCachedProducts(accountId: Long): Flow<List<Market>> {
        val criteria = FaveProductsCriteria(accountId)
        return cache.fave()
            .getProducts(criteria)
            .map { productDbos ->
                val markets: MutableList<Market> = ArrayList(productDbos.size)
                for (dbo in productDbos) {
                    markets.add(buildMarketFromDbo(dbo))
                }
                markets
            }
    }

    override fun getVideos(accountId: Long, count: Int, offset: Int): Flow<List<Video>> {
        return networker.vkDefault(accountId)
            .fave()
            .getVideos(offset, count)
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(
                    items
                )
                val dbos: MutableList<VideoDboEntity> = ArrayList(dtos.size)
                val videos: MutableList<Video> = ArrayList(dtos.size)
                for (dto in dtos) {
                    dbos.add(mapVideo(dto))
                    videos.add(transform(dto))
                }
                cache.fave().storeVideos(accountId, dbos, offset == 0)
                    .map { videos }
            }
    }

    override fun getArticles(accountId: Long, count: Int, offset: Int): Flow<List<Article>> {
        return networker.vkDefault(accountId)
            .fave()
            .getArticles(offset, count)
            .flatMapConcat {
                val dbos: MutableList<ArticleDboEntity> = ArrayList(it.size)
                val articles: MutableList<Article> = ArrayList(it.size)
                for (dto in it) {
                    dbos.add(mapArticle(dto))
                    articles.add(transform(dto))
                }
                cache.fave().storeArticles(accountId, dbos, offset == 0)
                    .map { articles }
            }
    }

    override fun getProducts(accountId: Long, count: Int, offset: Int): Flow<List<Market>> {
        return networker.vkDefault(accountId)
            .fave()
            .getProducts(offset, count)
            .flatMapConcat {
                val dbos: MutableList<MarketDboEntity> = ArrayList(it.size)
                val markets: MutableList<Market> = ArrayList(it.size)
                for (dto in it) {
                    dbos.add(mapMarket(dto))
                    markets.add(transform(dto))
                }
                cache.fave().storeProducts(accountId, dbos, offset == 0)
                    .map { markets }
            }
    }

    override fun getOwnerPublishedArticles(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Flow<List<Article>> {
        return networker.vkDefault(accountId)
            .fave()
            .getOwnerPublishedArticles(ownerId, offset, count)
            .map { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val articles: MutableList<Article> = ArrayList(dtos.size)
                for (dto in dtos) {
                    articles.add(transform(dto))
                }
                articles
            }
    }

    override fun getCachedPages(accountId: Long, isUser: Boolean): Flow<List<FavePage>> {
        return if (isUser) {
            cache.fave()
                .getFaveUsers(accountId)
                .map { obj -> buildFaveUsersFromDbo(obj) }
        } else {
            cache.fave()
                .getFaveGroups(accountId)
                .map { obj -> buildFaveUsersFromDbo(obj) }
        }
    }

    override fun getByLinksArticles(
        accountId: Long,
        links: String?
    ): Flow<List<Article>> {
        return networker.vkDefault(accountId)
            .fave().getByLinksArticles(links).map { items ->
                val articles = ArrayList<Article>(items.size)
                for (dto in items) {
                    articles.add(transform(dto))
                }
                articles
            }
    }

    override fun getPages(
        accountId: Long,
        count: Int,
        offset: Int,
        isUser: Boolean
    ): Flow<List<FavePage>> {
        return networker.vkDefault(accountId)
            .fave()
            .getPages(offset, count, Fields.FIELDS_BASE_OWNER, if (isUser) "users" else "groups")
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val userEntities: MutableList<UserEntity> = ArrayList()
                val communityEntities: MutableList<CommunityEntity> = ArrayList()
                for (item in dtos) {
                    when (item.type) {
                        FavePageType.USER -> userEntities.add(mapUser(item.user ?: continue))
                        FavePageType.COMMUNITY -> communityEntities.add(
                            mapCommunity(
                                item.group ?: continue
                            )
                        )
                    }
                }
                val entities = mapAll(dtos) {
                    mapFavePage(it)
                }
                val pages = mapAll(dtos) {
                    transformFaveUser(it)
                }
                if (isUser) {
                    cache.fave()
                        .storePages(accountId, entities, offset == 0)
                        .andThen(
                            cache.owners().storeOwnerEntities(
                                accountId,
                                OwnerEntities(userEntities, communityEntities)
                            )
                        )
                        .map {
                            pages
                        }
                } else {
                    cache.fave()
                        .storeGroups(accountId, entities, offset == 0)
                        .andThen(
                            cache.owners().storeOwnerEntities(
                                accountId,
                                OwnerEntities(userEntities, communityEntities)
                            )
                        )
                        .map {
                            pages
                        }
                }
            }
    }

    override fun getCachedLinks(accountId: Long): Flow<List<FaveLink>> {
        return cache.fave()
            .getFaveLinks(accountId)
            .map { entities ->
                val links: MutableList<FaveLink> = ArrayList(entities.size)
                for (entity in entities) {
                    links.add(createLinkFromEntity(entity))
                }
                links
            }
    }

    override fun getLinks(accountId: Long, count: Int, offset: Int): Flow<List<FaveLink>> {
        return networker.vkDefault(accountId)
            .fave()
            .getLinks(offset, count)
            .flatMapConcat { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val links: MutableList<FaveLink> = ArrayList(dtos.size)
                val entities: MutableList<FaveLinkEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    val entity = createLinkEntityFromDto(dto)
                    links.add(createLinkFromEntity(entity))
                    entities.add(entity)
                }
                cache.fave()
                    .storeLinks(accountId, entities, offset == 0)
                    .map {
                        links
                    }
            }
    }

    override fun removeLink(accountId: Long, id: String?): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .removeLink(id)
            .flatMapConcat {
                cache.fave()
                    .removeLink(accountId, id)
            }
    }

    override fun removeArticle(
        accountId: Long,
        owner_id: Long?,
        article_id: Int?
    ): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .removeArticle(owner_id, article_id)
    }

    override fun removeProduct(accountId: Long, id: Int?, owner_id: Long?): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .removeProduct(id, owner_id)
    }

    override fun removePost(accountId: Long, owner_id: Long?, id: Int?): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .removePost(owner_id, id)
    }

    override fun removeVideo(accountId: Long, owner_id: Long?, id: Int?): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .removeVideo(owner_id, id)
    }

    override fun pushFirst(accountId: Long, owner_id: Long): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .pushFirst(owner_id)
    }

    override fun addPage(accountId: Long, ownerId: Long): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .addPage(
                if (ownerId > 0) ownerId else null,
                if (ownerId < 0) abs(ownerId) else null
            )
            .ignoreElement()
    }

    override fun addLink(accountId: Long, link: String?): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .addLink(link)
            .ignoreElement()
    }

    override fun addVideo(
        accountId: Long,
        owner_id: Long?,
        id: Int?,
        access_key: String?
    ): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .addVideo(owner_id, id, access_key)
            .ignoreElement()
    }

    override fun addArticle(accountId: Long, url: String?): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .addArticle(url)
            .ignoreElement()
    }

    override fun addProduct(
        accountId: Long,
        id: Int,
        owner_id: Long,
        access_key: String?
    ): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .addProduct(id, owner_id, access_key)
            .ignoreElement()
    }

    override fun addPost(
        accountId: Long,
        owner_id: Long?,
        id: Int?,
        access_key: String?
    ): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .addPost(owner_id, id, access_key)
            .ignoreElement()
    }

    override fun removePage(accountId: Long, ownerId: Long, isUser: Boolean): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .fave()
            .removePage(
                if (ownerId > 0) ownerId else null,
                if (ownerId < 0) abs(ownerId) else null
            )
            .flatMapConcat {
                cache.fave().removePage(accountId, ownerId, isUser)
            }
    }

    companion object {
        internal fun createLinkFromEntity(entity: FaveLinkEntity): FaveLink {
            return FaveLink(entity.id)
                .setDescription(entity.description)
                .setPhoto(entity.photo?.let { map(it) })
                .setTitle(entity.title)
                .setUrl(entity.url)
        }

        internal fun createLinkEntityFromDto(dto: FaveLinkDto): FaveLinkEntity {
            return FaveLinkEntity(dto.id, dto.url)
                .setDescription(dto.description)
                .setTitle(dto.title)
                .setPhoto(dto.photo?.let { mapPhoto(it) })
        }
    }
}