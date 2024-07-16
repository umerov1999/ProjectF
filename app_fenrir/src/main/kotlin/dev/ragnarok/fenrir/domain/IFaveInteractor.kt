package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.FaveLink
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Video
import kotlinx.coroutines.flow.Flow

interface IFaveInteractor {
    fun getPosts(accountId: Long, count: Int, offset: Int): Flow<List<Post>>
    fun getCachedPosts(accountId: Long): Flow<List<Post>>
    fun getCachedPhotos(accountId: Long): Flow<List<Photo>>
    fun getPhotos(accountId: Long, count: Int, offset: Int): Flow<List<Photo>>
    fun getCachedVideos(accountId: Long): Flow<List<Video>>
    fun getCachedArticles(accountId: Long): Flow<List<Article>>
    fun getCachedProducts(accountId: Long): Flow<List<Market>>
    fun getProducts(accountId: Long, count: Int, offset: Int): Flow<List<Market>>
    fun getVideos(accountId: Long, count: Int, offset: Int): Flow<List<Video>>
    fun getArticles(accountId: Long, count: Int, offset: Int): Flow<List<Article>>
    fun getOwnerPublishedArticles(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Flow<List<Article>>

    fun getCachedPages(accountId: Long, isUser: Boolean): Flow<List<FavePage>>
    fun getPages(
        accountId: Long,
        count: Int,
        offset: Int,
        isUser: Boolean
    ): Flow<List<FavePage>>

    fun removePage(accountId: Long, ownerId: Long, isUser: Boolean): Flow<Boolean>
    fun getCachedLinks(accountId: Long): Flow<List<FaveLink>>
    fun getLinks(accountId: Long, count: Int, offset: Int): Flow<List<FaveLink>>
    fun removeLink(accountId: Long, id: String?): Flow<Boolean>
    fun removeArticle(accountId: Long, owner_id: Long?, article_id: Int?): Flow<Boolean>
    fun removeProduct(accountId: Long, id: Int?, owner_id: Long?): Flow<Boolean>
    fun addProduct(accountId: Long, id: Int, owner_id: Long, access_key: String?): Flow<Boolean>
    fun removePost(accountId: Long, owner_id: Long?, id: Int?): Flow<Boolean>
    fun removeVideo(accountId: Long, owner_id: Long?, id: Int?): Flow<Boolean>
    fun pushFirst(accountId: Long, owner_id: Long): Flow<Boolean>
    fun addPage(accountId: Long, ownerId: Long): Flow<Boolean>
    fun addLink(accountId: Long, link: String?): Flow<Boolean>
    fun addVideo(accountId: Long, owner_id: Long?, id: Int?, access_key: String?): Flow<Boolean>
    fun addArticle(accountId: Long, url: String?): Flow<Boolean>
    fun addPost(accountId: Long, owner_id: Long?, id: Int?, access_key: String?): Flow<Boolean>
    fun getByLinksArticles(
        accountId: Long,
        links: String?
    ): Flow<List<Article>>
}