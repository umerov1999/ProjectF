package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCheckedLink
import dev.ragnarok.fenrir.api.model.VKApiShortLink
import dev.ragnarok.fenrir.api.model.response.ResolveDomailResponse
import dev.ragnarok.fenrir.api.model.response.VKApiChatResponse
import dev.ragnarok.fenrir.api.model.response.VKApiLinkResponse
import kotlinx.coroutines.flow.Flow

interface IUtilsApi {
    @CheckResult
    fun resolveScreenName(screenName: String?): Flow<ResolveDomailResponse>

    @CheckResult
    fun getShortLink(url: String?, t_private: Int?): Flow<VKApiShortLink>

    @CheckResult
    fun getLastShortenedLinks(count: Int?, offset: Int?): Flow<Items<VKApiShortLink>>

    @CheckResult
    fun deleteFromLastShortened(key: String?): Flow<Int>

    @CheckResult
    fun checkLink(url: String?): Flow<VKApiCheckedLink>

    @CheckResult
    fun joinChatByInviteLink(link: String?): Flow<VKApiChatResponse>

    @CheckResult
    fun getInviteLink(peer_id: Long?, reset: Int?): Flow<VKApiLinkResponse>

    @CheckResult
    fun customScript(code: String?): Flow<Int>

    @CheckResult
    fun getServerTime(): Flow<Long>
}