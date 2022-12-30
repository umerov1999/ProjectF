package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCheckedLink
import dev.ragnarok.fenrir.api.model.VKApiShortLink
import dev.ragnarok.fenrir.api.model.response.ResolveDomailResponse
import dev.ragnarok.fenrir.api.model.response.VKApiChatResponse
import dev.ragnarok.fenrir.api.model.response.VKApiLinkResponse
import io.reactivex.rxjava3.core.Single

interface IUtilsApi {
    @CheckResult
    fun resolveScreenName(screenName: String?): Single<ResolveDomailResponse>

    @CheckResult
    fun getShortLink(url: String?, t_private: Int?): Single<VKApiShortLink>

    @CheckResult
    fun getLastShortenedLinks(count: Int?, offset: Int?): Single<Items<VKApiShortLink>>

    @CheckResult
    fun deleteFromLastShortened(key: String?): Single<Int>

    @CheckResult
    fun checkLink(url: String?): Single<VKApiCheckedLink>

    @CheckResult
    fun joinChatByInviteLink(link: String?): Single<VKApiChatResponse>

    @CheckResult
    fun getInviteLink(peer_id: Int?, reset: Int?): Single<VKApiLinkResponse>

    @CheckResult
    fun customScript(code: String?): Single<Int>

    @CheckResult
    fun getServerTime(): Single<Long>
}