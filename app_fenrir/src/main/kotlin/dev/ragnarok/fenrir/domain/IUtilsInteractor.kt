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
        accountId: Int,
        orig: Map<Int, SimplePrivacy>
    ): Single<Map<Int, Privacy>>

    fun resolveDomain(accountId: Int, domain: String?): Single<Optional<Owner>>
    fun getShortLink(accountId: Int, url: String?, t_private: Int?): Single<ShortLink>
    fun getLastShortenedLinks(accountId: Int, count: Int?, offset: Int?): Single<List<ShortLink>>
    fun deleteFromLastShortened(accountId: Int, key: String?): Single<Int>
    fun checkLink(accountId: Int, url: String?): Single<VKApiCheckedLink>
    fun joinChatByInviteLink(accountId: Int, link: String?): Single<VKApiChatResponse>
    fun getInviteLink(accountId: Int, peer_id: Int?, reset: Int?): Single<VKApiLinkResponse>
    fun customScript(accountId: Int, code: String?): Single<Int>
    fun getServerTime(accountId: Int): Single<Long>
}