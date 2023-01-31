package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.FaveLinkDto
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiArticle
import dev.ragnarok.fenrir.api.model.VKApiMarket
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.api.model.response.FavePageResponse
import dev.ragnarok.fenrir.api.model.response.FavePostsResponse
import io.reactivex.rxjava3.core.Single

interface IFaveApi {
    @CheckResult
    fun getPages(
        offset: Int?,
        count: Int?,
        fields: String?,
        type: String?
    ): Single<Items<FavePageResponse>>

    @CheckResult
    fun getPhotos(offset: Int?, count: Int?): Single<Items<VKApiPhoto>>

    @CheckResult
    fun getVideos(offset: Int?, count: Int?): Single<List<VKApiVideo>>

    @CheckResult
    fun getArticles(offset: Int?, count: Int?): Single<List<VKApiArticle>>

    @CheckResult
    fun getProducts(offset: Int?, count: Int?): Single<List<VKApiMarket>>

    @CheckResult
    fun getOwnerPublishedArticles(
        owner_id: Long?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiArticle>>

    @CheckResult
    fun getPosts(offset: Int?, count: Int?): Single<FavePostsResponse>

    @CheckResult
    fun getLinks(offset: Int?, count: Int?): Single<Items<FaveLinkDto>>

    @CheckResult
    fun addPage(userId: Long?, groupId: Long?): Single<Boolean>

    @CheckResult
    fun addLink(link: String?): Single<Boolean>

    @CheckResult
    fun removePage(userId: Long?, groupId: Long?): Single<Boolean>

    @CheckResult
    fun removeLink(linkId: String?): Single<Boolean>

    @CheckResult
    fun removeArticle(owner_id: Long?, article_id: Int?): Single<Boolean>

    @CheckResult
    fun removePost(owner_id: Long?, id: Int?): Single<Boolean>

    @CheckResult
    fun removeVideo(owner_id: Long?, id: Int?): Single<Boolean>

    @CheckResult
    fun pushFirst(owner_id: Long): Single<Boolean>

    @CheckResult
    fun addVideo(owner_id: Long?, id: Int?, access_key: String?): Single<Boolean>

    @CheckResult
    fun addArticle(url: String?): Single<Boolean>

    @CheckResult
    fun addProduct(id: Int, owner_id: Long, access_key: String?): Single<Boolean>

    @CheckResult
    fun removeProduct(id: Int?, owner_id: Long?): Single<Boolean>

    @CheckResult
    fun addPost(owner_id: Long?, id: Int?, access_key: String?): Single<Boolean>
}