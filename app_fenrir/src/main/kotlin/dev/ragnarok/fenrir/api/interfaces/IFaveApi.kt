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
import kotlinx.coroutines.flow.Flow

interface IFaveApi {
    @CheckResult
    fun getPages(
        offset: Int?,
        count: Int?,
        fields: String?,
        type: String?
    ): Flow<Items<FavePageResponse>>

    @CheckResult
    fun getPhotos(offset: Int?, count: Int?): Flow<Items<VKApiPhoto>>

    @CheckResult
    fun getVideos(offset: Int?, count: Int?): Flow<List<VKApiVideo>>

    @CheckResult
    fun getArticles(offset: Int?, count: Int?): Flow<List<VKApiArticle>>

    @CheckResult
    fun getProducts(offset: Int?, count: Int?): Flow<List<VKApiMarket>>

    @CheckResult
    fun getOwnerPublishedArticles(
        owner_id: Long?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiArticle>>

    @CheckResult
    fun getByLinksArticles(
        links: String?
    ): Flow<List<VKApiArticle>>

    @CheckResult
    fun getPosts(offset: Int?, count: Int?): Flow<FavePostsResponse>

    @CheckResult
    fun getLinks(offset: Int?, count: Int?): Flow<Items<FaveLinkDto>>

    @CheckResult
    fun addPage(userId: Long?, groupId: Long?): Flow<Boolean>

    @CheckResult
    fun addLink(link: String?): Flow<Boolean>

    @CheckResult
    fun removePage(userId: Long?, groupId: Long?): Flow<Boolean>

    @CheckResult
    fun removeLink(linkId: String?): Flow<Boolean>

    @CheckResult
    fun removeArticle(owner_id: Long?, article_id: Int?): Flow<Boolean>

    @CheckResult
    fun removePost(owner_id: Long?, id: Int?): Flow<Boolean>

    @CheckResult
    fun removeVideo(owner_id: Long?, id: Int?): Flow<Boolean>

    @CheckResult
    fun pushFirst(owner_id: Long): Flow<Boolean>

    @CheckResult
    fun addVideo(owner_id: Long?, id: Int?, access_key: String?): Flow<Boolean>

    @CheckResult
    fun addArticle(url: String?): Flow<Boolean>

    @CheckResult
    fun addProduct(id: Int, owner_id: Long, access_key: String?): Flow<Boolean>

    @CheckResult
    fun removeProduct(id: Int?, owner_id: Long?): Flow<Boolean>

    @CheckResult
    fun addPost(owner_id: Long?, id: Int?, access_key: String?): Flow<Boolean>
}