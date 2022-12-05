package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.*
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IFaveInteractor {
    fun getPosts(accountId: Int, count: Int, offset: Int): Single<List<Post>>
    fun getCachedPosts(accountId: Int): Single<List<Post>>
    fun getCachedPhotos(accountId: Int): Single<List<Photo>>
    fun getPhotos(accountId: Int, count: Int, offset: Int): Single<List<Photo>>
    fun getCachedVideos(accountId: Int): Single<List<Video>>
    fun getCachedArticles(accountId: Int): Single<List<Article>>
    fun getCachedProducts(accountId: Int): Single<List<Market>>
    fun getProducts(accountId: Int, count: Int, offset: Int): Single<List<Market>>
    fun getVideos(accountId: Int, count: Int, offset: Int): Single<List<Video>>
    fun getArticles(accountId: Int, count: Int, offset: Int): Single<List<Article>>
    fun getOwnerPublishedArticles(
        accountId: Int,
        ownerId: Int,
        count: Int,
        offset: Int
    ): Single<List<Article>>

    fun getCachedPages(accountId: Int, isUser: Boolean): Single<List<FavePage>>
    fun getPages(
        accountId: Int,
        count: Int,
        offset: Int,
        isUser: Boolean
    ): Single<List<FavePage>>

    fun removePage(accountId: Int, ownerId: Int, isUser: Boolean): Completable
    fun getCachedLinks(accountId: Int): Single<List<FaveLink>>
    fun getLinks(accountId: Int, count: Int, offset: Int): Single<List<FaveLink>>
    fun removeLink(accountId: Int, id: String?): Completable
    fun removeArticle(accountId: Int, owner_id: Int?, article_id: Int?): Single<Boolean>
    fun removeProduct(accountId: Int, id: Int?, owner_id: Int?): Single<Boolean>
    fun addProduct(accountId: Int, id: Int, owner_id: Int, access_key: String?): Completable
    fun removePost(accountId: Int, owner_id: Int?, id: Int?): Single<Boolean>
    fun removeVideo(accountId: Int, owner_id: Int?, id: Int?): Single<Boolean>
    fun pushFirst(accountId: Int, owner_id: Int): Single<Boolean>
    fun addPage(accountId: Int, ownerId: Int): Completable
    fun addLink(accountId: Int, link: String?): Completable
    fun addVideo(accountId: Int, owner_id: Int?, id: Int?, access_key: String?): Completable
    fun addArticle(accountId: Int, url: String?): Completable
    fun addPost(accountId: Int, owner_id: Int?, id: Int?, access_key: String?): Completable
}