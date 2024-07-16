package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.api.model.VKApiCheckedLink
import dev.ragnarok.fenrir.api.model.response.VKApiChatResponse
import dev.ragnarok.fenrir.api.model.response.VKApiLinkResponse
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Privacy
import dev.ragnarok.fenrir.model.ShortLink
import dev.ragnarok.fenrir.model.SimplePrivacy
import dev.ragnarok.fenrir.util.Optional
import kotlinx.coroutines.flow.Flow

interface IUtilsInteractor {
    fun createFullPrivacies(
        accountId: Long,
        orig: Map<Int, SimplePrivacy>
    ): Flow<Map<Int, Privacy>>

    fun resolveDomain(accountId: Long, domain: String?): Flow<Optional<Owner>>
    fun getShortLink(accountId: Long, url: String?, t_private: Int?): Flow<ShortLink>
    fun getLastShortenedLinks(accountId: Long, count: Int?, offset: Int?): Flow<List<ShortLink>>
    fun deleteFromLastShortened(accountId: Long, key: String?): Flow<Int>
    fun checkLink(accountId: Long, url: String?): Flow<VKApiCheckedLink>
    fun joinChatByInviteLink(accountId: Long, link: String?): Flow<VKApiChatResponse>
    fun getInviteLink(accountId: Long, peer_id: Long?, reset: Int?): Flow<VKApiLinkResponse>
    fun customScript(accountId: Long, code: String?): Flow<Int>
    fun getServerTime(accountId: Long): Flow<Long>
}