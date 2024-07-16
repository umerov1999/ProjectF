package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.IdPair
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.PostsResponse
import dev.ragnarok.fenrir.api.model.response.RepostReponse
import dev.ragnarok.fenrir.api.model.response.WallResponse
import dev.ragnarok.fenrir.api.model.response.WallSearchResponse
import kotlinx.coroutines.flow.Flow

interface IWallApi {
    fun search(
        ownerId: Long, query: String?, ownersOnly: Boolean?,
        count: Int, offset: Int, extended: Boolean?, fields: String?
    ): Flow<WallSearchResponse>

    @CheckResult
    fun edit(
        ownerId: Long?, postId: Int?, friendsOnly: Boolean?, message: String?,
        attachments: Collection<IAttachmentToken>?, services: String?,
        signed: Boolean?, publishDate: Long?, latitude: Double?,
        longitude: Double?, placeId: Int?, markAsAds: Boolean?
    ): Flow<Boolean>

    @CheckResult
    fun pin(ownerId: Long?, postId: Int): Flow<Boolean>

    @CheckResult
    fun unpin(ownerId: Long?, postId: Int): Flow<Boolean>

    @CheckResult
    fun repost(
        postOwnerId: Long,
        postId: Int,
        message: String?,
        groupId: Long?,
        markAsAds: Boolean?
    ): Flow<RepostReponse>

    @CheckResult
    fun post(
        ownerId: Long?, friendsOnly: Boolean?, fromGroup: Boolean?, message: String?,
        attachments: Collection<IAttachmentToken>?, services: String?, signed: Boolean?,
        publishDate: Long?, latitude: Double?, longitude: Double?, placeId: Int?,
        postId: Int?, guid: Int?, markAsAds: Boolean?, adsPromotedStealth: Boolean?
    ): Flow<Int>

    @CheckResult
    fun delete(ownerId: Long?, postId: Int): Flow<Boolean>

    @CheckResult
    fun restoreComment(ownerId: Long?, commentId: Int): Flow<Boolean>

    @CheckResult
    fun deleteComment(ownerId: Long?, commentId: Int): Flow<Boolean>

    @CheckResult
    fun restore(ownerId: Long?, postId: Int): Flow<Boolean>

    @CheckResult
    fun editComment(
        ownerId: Long?,
        commentId: Int,
        message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Flow<Boolean>

    @CheckResult
    fun createComment(
        ownerId: Long?, postId: Int, fromGroup: Long?,
        message: String?, replyToComment: Int?,
        attachments: Collection<IAttachmentToken>?, stickerId: Int?,
        generatedUniqueId: Int?
    ): Flow<Int>

    @CheckResult
    operator fun get(
        ownerId: Long?, domain: String?, offset: Int?, count: Int?,
        filter: String?, extended: Boolean?, fields: String?
    ): Flow<WallResponse>

    @CheckResult
    fun getById(
        ids: Collection<IdPair>?, extended: Boolean?,
        copyHistoryDepth: Int?, fields: String?
    ): Flow<PostsResponse>

    @CheckResult
    fun getComments(
        ownerId: Long, postId: Int, needLikes: Boolean?,
        startCommentId: Int?, offset: Int?, count: Int?,
        sort: String?, extended: Boolean?, fields: String?
    ): Flow<DefaultCommentsResponse>

    @CheckResult
    fun reportPost(owner_id: Long?, post_id: Int?, reason: Int?): Flow<Int>

    @CheckResult
    fun reportComment(owner_id: Long?, post_id: Int?, reason: Int?): Flow<Int>

    @CheckResult
    fun subscribe(owner_id: Long?): Flow<Int>

    @CheckResult
    fun unsubscribe(owner_id: Long?): Flow<Int>
}