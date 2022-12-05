package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.TopicsResponse
import io.reactivex.rxjava3.core.Single

interface IBoardApi {
    @CheckResult
    fun getComments(
        groupId: Int, topicId: Int, needLikes: Boolean?, startCommentId: Int?,
        offset: Int?, count: Int?, extended: Boolean?,
        sort: String?, fields: String?
    ): Single<DefaultCommentsResponse>

    @CheckResult
    fun restoreComment(groupId: Int, topicId: Int, commentId: Int): Single<Boolean>

    @CheckResult
    fun deleteComment(groupId: Int, topicId: Int, commentId: Int): Single<Boolean>

    @CheckResult
    fun getTopics(
        groupId: Int, topicIds: Collection<Int>?, order: Int?,
        offset: Int?, count: Int?, extended: Boolean?,
        preview: Int?, previewLength: Int?, fields: String?
    ): Single<TopicsResponse>

    @CheckResult
    fun editComment(
        groupId: Int, topicId: Int, commentId: Int,
        message: String?, attachments: Collection<IAttachmentToken>?
    ): Single<Boolean>

    @CheckResult
    fun addComment(
        groupId: Int?, topicId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?, fromGroup: Boolean?,
        stickerId: Int?, generatedUniqueId: Int?
    ): Single<Int>
}