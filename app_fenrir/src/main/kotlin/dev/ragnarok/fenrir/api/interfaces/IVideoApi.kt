package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.api.model.VKApiVideoAlbum
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.SearchVideoResponse
import io.reactivex.rxjava3.core.Single

interface IVideoApi {
    @CheckResult
    fun getComments(
        ownerId: Long?, videoId: Int, needLikes: Boolean?,
        startCommentId: Int?, offset: Int?, count: Int?, sort: String?,
        extended: Boolean?, fields: String?
    ): Single<DefaultCommentsResponse>

    @CheckResult
    fun addVideo(targetId: Long?, videoId: Int?, ownerId: Long?): Single<Int>

    @CheckResult
    fun deleteVideo(videoId: Int?, ownerId: Long?, targetId: Long?): Single<Int>

    @CheckResult
    fun getAlbums(
        ownerId: Long?,
        offset: Int?,
        count: Int?,
        needSystem: Boolean?
    ): Single<Items<VKApiVideoAlbum>>

    @CheckResult
    fun getAlbumsByVideo(
        target_id: Long?,
        owner_id: Long?,
        video_id: Int?
    ): Single<Items<VKApiVideoAlbum>>

    @CheckResult
    fun search(
        query: String?, sort: Int?, hd: Boolean?, adult: Boolean?, filters: String?,
        searchOwn: Boolean?, offset: Int?, longer: Int?, shorter: Int?,
        count: Int?, extended: Boolean?
    ): Single<SearchVideoResponse>

    @CheckResult
    fun restoreComment(ownerId: Long?, commentId: Int): Single<Boolean>

    @CheckResult
    fun deleteComment(ownerId: Long?, commentId: Int): Single<Boolean>

    @CheckResult
    operator fun get(
        ownerId: Long?, ids: Collection<AccessIdPair>?, albumId: Int?,
        count: Int?, offset: Int?, extended: Boolean?
    ): Single<Items<VKApiVideo>>

    @CheckResult
    fun createComment(
        ownerId: Long, videoId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?, fromGroup: Boolean?,
        replyToComment: Int?, stickerId: Int?, uniqueGeneratedId: Int?
    ): Single<Int>

    @CheckResult
    fun editComment(
        ownerId: Long,
        commentId: Int,
        message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Single<Boolean>

    @CheckResult
    fun edit(ownerId: Long, video_id: Int, name: String?, desc: String?): Single<Boolean>
}