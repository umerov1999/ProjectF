package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IUtilsApi
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCheckedLink
import dev.ragnarok.fenrir.api.model.VKApiShortLink
import dev.ragnarok.fenrir.api.model.response.ResolveDomailResponse
import dev.ragnarok.fenrir.api.model.response.VKApiChatResponse
import dev.ragnarok.fenrir.api.model.response.VKApiLinkResponse
import dev.ragnarok.fenrir.api.services.IUtilsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class UtilsApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IUtilsApi {
    override fun resolveScreenName(screenName: String?): Flow<ResolveDomailResponse> {
        return provideService(
            IUtilsService(),
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMapConcat {
                it.resolveScreenName(screenName)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getShortLink(url: String?, t_private: Int?): Flow<VKApiShortLink> {
        return provideService(
            IUtilsService(),
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMapConcat {
                it.getShortLink(url, t_private)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLastShortenedLinks(count: Int?, offset: Int?): Flow<Items<VKApiShortLink>> {
        return provideService(IUtilsService(), TokenType.USER)
            .flatMapConcat {
                it.getLastShortenedLinks(count, offset)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deleteFromLastShortened(key: String?): Flow<Int> {
        return provideService(IUtilsService(), TokenType.USER)
            .flatMapConcat {
                it.deleteFromLastShortened(key)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun checkLink(url: String?): Flow<VKApiCheckedLink> {
        return provideService(
            IUtilsService(),
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMapConcat {
                it.checkLink(url)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun joinChatByInviteLink(link: String?): Flow<VKApiChatResponse> {
        return provideService(IUtilsService(), TokenType.USER)
            .flatMapConcat {
                it.joinChatByInviteLink(link)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getInviteLink(peer_id: Long?, reset: Int?): Flow<VKApiLinkResponse> {
        return provideService(IUtilsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getInviteLink(peer_id, reset)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun customScript(code: String?): Flow<Int> {
        return provideService(
            IUtilsService(),
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMapConcat {
                it.customScript(code)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getServerTime(): Flow<Long> {
        return provideService(
            IUtilsService(),
            TokenType.USER,
            TokenType.COMMUNITY,
            TokenType.SERVICE
        )
            .flatMapConcat {
                it.getServerTime()
                    .map(extractResponseWithErrorHandling())
            }
    }
}