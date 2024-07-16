package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.TopicsResponse
import kotlinx.coroutines.flow.Flow

interface IBoardApi {
    @CheckResult
    fun getComments(
        groupId: Long, topicId: Int, needLikes: Boolean?, startCommentId: Int?,
        offset: Int?, count: Int?, extended: Boolean?,
        sort: String?, fields: String?
    ): Flow<DefaultCommentsResponse>

    @CheckResult
    fun restoreComment(groupId: Long, topicId: Int, commentId: Int): Flow<Boolean>

    @CheckResult
    fun deleteComment(groupId: Long, topicId: Int, commentId: Int): Flow<Boolean>

    @CheckResult
    fun getTopics(
        groupId: Long, topicIds: Collection<Int>?, order: Int?,
        offset: Int?, count: Int?, extended: Boolean?,
        preview: Int?, previewLength: Int?, fields: String?
    ): Flow<TopicsResponse>

    @CheckResult
    fun editComment(
        groupId: Long, topicId: Int, commentId: Int,
        message: String?, attachments: Collection<IAttachmentToken>?
    ): Flow<Boolean>

    @CheckResult
    fun addComment(
        groupId: Long?, topicId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?, fromGroup: Boolean?,
        stickerId: Int?, generatedUniqueId: Int?
    ): Flow<Int>
}