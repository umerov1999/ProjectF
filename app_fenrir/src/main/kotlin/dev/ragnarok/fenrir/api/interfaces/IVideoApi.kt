package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.SearchVideoResponse
import io.reactivex.rxjava3.core.Single

interface IVideoApi {
    @CheckResult
    fun getComments(
        ownerId: Int?, videoId: Int, needLikes: Boolean?,
        startCommentId: Int?, offset: Int?, count: Int?, sort: String?,
        extended: Boolean?, fields: String?
    ): Single<DefaultCommentsResponse>

    @CheckResult
    fun addVideo(targetId: Int?, videoId: Int?, ownerId: Int?): Single<Int>

    @CheckResult
    fun deleteVideo(videoId: Int?, ownerId: Int?, targetId: Int?): Single<Int>

    @CheckResult
    fun getAlbums(
        ownerId: Int?,
        offset: Int?,
        count: Int?,
        needSystem: Boolean?
    ): Single<Items<VKApiVideoAlbum>>

    @CheckResult
    fun getAlbumsByVideo(
        target_id: Int?,
        owner_id: Int?,
        video_id: Int?
    ): Single<Items<VKApiVideoAlbum>>

    @CheckResult
    fun search(
        query: String?, sort: Int?, hd: Boolean?, adult: Boolean?, filters: String?,
        searchOwn: Boolean?, offset: Int?, longer: Int?, shorter: Int?,
        count: Int?, extended: Boolean?
    ): Single<SearchVideoResponse>

    @CheckResult
    fun restoreComment(ownerId: Int?, commentId: Int): Single<Boolean>

    @CheckResult
    fun deleteComment(ownerId: Int?, commentId: Int): Single<Boolean>

    @CheckResult
    operator fun get(
        ownerId: Int?, ids: Collection<AccessIdPair>?, albumId: Int?,
        count: Int?, offset: Int?, extended: Boolean?
    ): Single<Items<VKApiVideo>>

    @CheckResult
    fun createComment(
        ownerId: Int, videoId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?, fromGroup: Boolean?,
        replyToComment: Int?, stickerId: Int?, uniqueGeneratedId: Int?
    ): Single<Int>

    @CheckResult
    fun editComment(
        ownerId: Int,
        commentId: Int,
        message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Single<Boolean>

    @CheckResult
    fun edit(ownerId: Int, video_id: Int, name: String?, desc: String?): Single<Boolean>
}