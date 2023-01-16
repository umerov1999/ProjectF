package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.api.model.VKApiCheckedLink
import dev.ragnarok.fenrir.api.model.response.VKApiChatResponse
import dev.ragnarok.fenrir.api.model.response.VKApiLinkResponse
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Privacy
import dev.ragnarok.fenrir.model.ShortLink
import dev.ragnarok.fenrir.model.SimplePrivacy
import dev.ragnarok.fenrir.util.Optional
import io.reactivex.rxjava3.core.Single

interface IUtilsInteractor {
    fun createFullPrivacies(
        accountId: Long,
        orig: Map<Int, SimplePrivacy>
    ): Single<Map<Int, Privacy>>

    fun resolveDomain(accountId: Long, domain: String?): Single<Optional<Owner>>
    fun getShortLink(accountId: Long, url: String?, t_private: Int?): Single<ShortLink>
    fun getLastShortenedLinks(accountId: Long, count: Int?, offset: Int?): Single<List<ShortLink>>
    fun deleteFromLastShortened(accountId: Long, key: String?): Single<Int>
    fun checkLink(accountId: Long, url: String?): Single<VKApiCheckedLink>
    fun joinChatByInviteLink(accountId: Long, link: String?): Single<VKApiChatResponse>
    fun getInviteLink(accountId: Long, peer_id: Long?, reset: Int?): Single<VKApiLinkResponse>
    fun customScript(accountId: Long, code: String?): Single<Int>
    fun getServerTime(accountId: Long): Single<Long>
}