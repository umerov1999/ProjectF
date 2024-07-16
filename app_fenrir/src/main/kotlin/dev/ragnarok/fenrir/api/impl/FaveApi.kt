package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IFaveApi
import dev.ragnarok.fenrir.api.model.FaveLinkDto
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiArticle
import dev.ragnarok.fenrir.api.model.VKApiMarket
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.api.model.response.FavePageResponse
import dev.ragnarok.fenrir.api.model.response.FavePostsResponse
import dev.ragnarok.fenrir.api.services.IFaveService
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class FaveApi(accountId: Long, provider: IServiceProvider) : AbsApi(accountId, provider),
    IFaveApi {
    override fun getPages(
        offset: Int?,
        count: Int?,
        fields: String?,
        type: String?
    ): Flow<Items<FavePageResponse>> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.getPages(offset, count, type, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPhotos(offset: Int?, count: Int?): Flow<Items<VKApiPhoto>> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.getPhotos(offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getVideos(offset: Int?, count: Int?): Flow<List<VKApiVideo>> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.getVideos(offset, count, "video", 1, Fields.FIELDS_BASE_OWNER)
                    .map(extractResponseWithErrorHandling())
                    .map { t ->
                        val temp = listEmptyIfNull(t.items)
                        val videos: MutableList<VKApiVideo> = ArrayList()
                        for (i in temp) {
                            if (i.attachment is VKApiVideo) videos.add(i.attachment)
                        }
                        videos
                    }
            }
    }

    override fun getArticles(offset: Int?, count: Int?): Flow<List<VKApiArticle>> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.getArticles(offset, count, "article", 1, Fields.FIELDS_BASE_OWNER)
                    .map(extractResponseWithErrorHandling())
                    .map { t ->
                        val temp = listEmptyIfNull(t.items)
                        val articles: MutableList<VKApiArticle> = ArrayList()
                        for (i in temp) {
                            if (i.attachment is VKApiArticle) articles.add(i.attachment)
                        }
                        articles
                    }
            }
    }

    override fun getOwnerPublishedArticles(
        owner_id: Long?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiArticle>> {
        return provideService(IFaveService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getOwnerPublishedArticles(
                    owner_id,
                    offset,
                    count,
                    "date",
                    1,
                    Fields.FIELDS_BASE_OWNER
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getByLinksArticles(
        links: String?
    ): Flow<List<VKApiArticle>> {
        return provideService(IFaveService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getByLinksArticles(
                    links,
                    1,
                    Fields.FIELDS_BASE_OWNER
                )
                    .map(extractResponseWithErrorHandling())
                    .map { t ->
                        val res = listEmptyIfNull(t.items)
                        val ret = ArrayList<VKApiArticle>()
                        for (i in res) {
                            if (i.owner_id != 0L) {
                                ret.add(i)
                            }
                        }
                        ret
                    }
            }
    }

    override fun getPosts(offset: Int?, count: Int?): Flow<FavePostsResponse> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.getPosts(offset, count, "post", 1, Fields.FIELDS_BASE_OWNER)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLinks(offset: Int?, count: Int?): Flow<Items<FaveLinkDto>> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.getLinks(offset, count, "link", 1, Fields.FIELDS_BASE_OWNER)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getProducts(offset: Int?, count: Int?): Flow<List<VKApiMarket>> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.getProducts(offset, count, "product", 1, Fields.FIELDS_BASE_OWNER)
                    .map(extractResponseWithErrorHandling())
                    .map { t ->
                        val temp = listEmptyIfNull(t.items)
                        val markets: MutableList<VKApiMarket> = ArrayList()
                        for (i in temp) {
                            if (i.attachment is VKApiMarket) markets.add(i.attachment)
                        }
                        markets
                    }
            }
    }

    override fun addPage(userId: Long?, groupId: Long?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.addPage(userId, groupId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun addLink(link: String?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.addLink(link)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun addVideo(owner_id: Long?, id: Int?, access_key: String?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.addVideo(owner_id, id, access_key)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun addArticle(url: String?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.addArticle(url)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun addProduct(id: Int, owner_id: Long, access_key: String?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.addProduct(id, owner_id, access_key)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun addPost(owner_id: Long?, id: Int?, access_key: String?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.addPost(owner_id, id, access_key)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun removePage(userId: Long?, groupId: Long?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.removePage(userId, groupId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun removeLink(linkId: String?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.removeLink(linkId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun removeArticle(owner_id: Long?, article_id: Int?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.removeArticle(owner_id, article_id)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun removeProduct(id: Int?, owner_id: Long?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.removeProduct(id, owner_id)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun removePost(owner_id: Long?, id: Int?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.removePost(owner_id, id)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun removeVideo(owner_id: Long?, id: Int?): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.removeVideo(owner_id, id)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun pushFirst(owner_id: Long): Flow<Boolean> {
        return provideService(IFaveService(), TokenType.USER)
            .flatMapConcat {
                it.pushFirst(
                    "var owner_id = Args.owner_id;\n" +
                            "if (owner_id >= 0) {\n" +
                            "   var ret = API.fave.removePage({\"v\":\"" + Constants.API_VERSION + "\", \"user_id\":owner_id});\n" +
                            "   if (ret != 1) {\n" +
                            "       return 0;\n" +
                            "   }\n" +
                            "   ret = API.fave.addPage({\"v\":\"" + Constants.API_VERSION + "\", \"user_id\":owner_id});\n" +
                            "   if (ret != 1) {\n" +
                            "       return 0;\n" +
                            "   }\n" +
                            "} else {\n" +
                            "   var ret = API.fave.removePage({\"v\":\"" + Constants.API_VERSION + "\", \"group_id\":-owner_id});\n" +
                            "   if (ret != 1) {\n" +
                            "       return 0;\n" +
                            "   }\n" +
                            "   ret = API.fave.addPage({\"v\":\"" + Constants.API_VERSION + "\", \"group_id\":-owner_id});\n" +
                            "   if (ret != 1) {\n" +
                            "       return 0;\n" +
                            "   }\n" +
                            "}\n" +
                            "return 1;", owner_id
                )
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }
}