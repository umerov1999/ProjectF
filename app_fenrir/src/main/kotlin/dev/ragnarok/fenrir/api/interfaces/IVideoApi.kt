package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.api.model.VKApiVideoAlbum
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.SearchVideoResponse
import dev.ragnarok.fenrir.api.model.server.VKApiVideosUploadServer
import kotlinx.coroutines.flow.Flow

interface IVideoApi {
    @CheckResult
    fun getComments(
        ownerId: Long?, videoId: Int, needLikes: Boolean?,
        startCommentId: Int?, offset: Int?, count: Int?, sort: String?,
        extended: Boolean?, fields: String?
    ): Flow<DefaultCommentsResponse>

    @CheckResult
    fun addVideo(targetId: Long?, videoId: Int?, ownerId: Long?): Flow<Int>

    @CheckResult
    fun deleteVideo(videoId: Int?, ownerId: Long?, targetId: Long?): Flow<Int>

    @CheckResult
    fun getAlbums(
        ownerId: Long?,
        offset: Int?,
        count: Int?,
        needSystem: Boolean?
    ): Flow<Items<VKApiVideoAlbum>>

    @CheckResult
    fun getAlbumsByVideo(
        target_id: Long?,
        owner_id: Long?,
        video_id: Int?
    ): Flow<Items<VKApiVideoAlbum>>

    @CheckResult
    fun search(
        query: String?, sort: Int?, hd: Boolean?, adult: Boolean?, filters: String?,
        searchOwn: Boolean?, offset: Int?, longer: Int?, shorter: Int?,
        count: Int?, extended: Boolean?
    ): Flow<SearchVideoResponse>

    @CheckResult
    fun restoreComment(ownerId: Long?, commentId: Int): Flow<Boolean>

    @CheckResult
    fun deleteComment(ownerId: Long?, commentId: Int): Flow<Boolean>

    @CheckResult
    operator fun get(
        ownerId: Long?, ids: Collection<AccessIdPair>?, albumId: Int?,
        count: Int?, offset: Int?, extended: Boolean?
    ): Flow<Items<VKApiVideo>>

    @CheckResult
    fun createComment(
        ownerId: Long, videoId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?, fromGroup: Boolean?,
        replyToComment: Int?, stickerId: Int?, uniqueGeneratedId: Int?
    ): Flow<Int>

    @CheckResult
    fun editComment(
        ownerId: Long,
        commentId: Int,
        message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Flow<Boolean>

    @CheckResult
    fun edit(ownerId: Long, video_id: Int, name: String?, desc: String?): Flow<Boolean>

    @CheckResult
    fun getVideoServer(
        isPrivate: Int?,
        group_id: Long?,
        name: String?
    ): Flow<VKApiVideosUploadServer>
}