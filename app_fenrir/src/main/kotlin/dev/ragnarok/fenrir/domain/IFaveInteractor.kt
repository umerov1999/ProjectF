package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.FaveLink
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Video
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IFaveInteractor {
    fun getPosts(accountId: Long, count: Int, offset: Int): Single<List<Post>>
    fun getCachedPosts(accountId: Long): Single<List<Post>>
    fun getCachedPhotos(accountId: Long): Single<List<Photo>>
    fun getPhotos(accountId: Long, count: Int, offset: Int): Single<List<Photo>>
    fun getCachedVideos(accountId: Long): Single<List<Video>>
    fun getCachedArticles(accountId: Long): Single<List<Article>>
    fun getCachedProducts(accountId: Long): Single<List<Market>>
    fun getProducts(accountId: Long, count: Int, offset: Int): Single<List<Market>>
    fun getVideos(accountId: Long, count: Int, offset: Int): Single<List<Video>>
    fun getArticles(accountId: Long, count: Int, offset: Int): Single<List<Article>>
    fun getOwnerPublishedArticles(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Single<List<Article>>

    fun getCachedPages(accountId: Long, isUser: Boolean): Single<List<FavePage>>
    fun getPages(
        accountId: Long,
        count: Int,
        offset: Int,
        isUser: Boolean
    ): Single<List<FavePage>>

    fun removePage(accountId: Long, ownerId: Long, isUser: Boolean): Completable
    fun getCachedLinks(accountId: Long): Single<List<FaveLink>>
    fun getLinks(accountId: Long, count: Int, offset: Int): Single<List<FaveLink>>
    fun removeLink(accountId: Long, id: String?): Completable
    fun removeArticle(accountId: Long, owner_id: Long?, article_id: Int?): Single<Boolean>
    fun removeProduct(accountId: Long, id: Int?, owner_id: Long?): Single<Boolean>
    fun addProduct(accountId: Long, id: Int, owner_id: Long, access_key: String?): Completable
    fun removePost(accountId: Long, owner_id: Long?, id: Int?): Single<Boolean>
    fun removeVideo(accountId: Long, owner_id: Long?, id: Int?): Single<Boolean>
    fun pushFirst(accountId: Long, owner_id: Long): Single<Boolean>
    fun addPage(accountId: Long, ownerId: Long): Completable
    fun addLink(accountId: Long, link: String?): Completable
    fun addVideo(accountId: Long, owner_id: Long?, id: Int?, access_key: String?): Completable
    fun addArticle(accountId: Long, url: String?): Completable
    fun addPost(accountId: Long, owner_id: Long?, id: Int?, access_key: String?): Completable
}