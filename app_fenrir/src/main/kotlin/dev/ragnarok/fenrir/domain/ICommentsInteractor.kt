package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.CommentIntent
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.CommentsBundle
import dev.ragnarok.fenrir.model.DraftComment
import dev.ragnarok.fenrir.model.Owner
import kotlinx.coroutines.flow.Flow

interface ICommentsInteractor {
    fun getAllCachedData(accountId: Long, commented: Commented): Flow<List<Comment>>
    fun getCommentsPortion(
        accountId: Long,
        commented: Commented,
        offset: Int,
        count: Int,
        startCommentId: Int?,
        threadComment: Int?,
        invalidateCache: Boolean,
        sort: String?
    ): Flow<CommentsBundle>

    fun getCommentsNoCache(
        accountId: Long,
        ownerId: Long,
        postId: Int,
        offset: Int
    ): Flow<List<Comment>>

    fun restoreDraftComment(accountId: Long, commented: Commented): Flow<DraftComment?>
    fun safeDraftComment(
        accountId: Long,
        commented: Commented,
        text: String?,
        replyToCommentId: Int,
        replyToUserId: Long
    ): Flow<Int>

    fun like(accountId: Long, commented: Commented, commentId: Int, add: Boolean): Flow<Boolean>
    fun checkAndAddLike(accountId: Long, commented: Commented, commentId: Int): Flow<Int>
    fun isLiked(accountId: Long, commented: Commented, commentId: Int): Flow<Boolean>
    fun deleteRestore(
        accountId: Long,
        commented: Commented,
        commentId: Int,
        delete: Boolean
    ): Flow<Boolean>

    fun send(
        accountId: Long,
        commented: Commented,
        commentThread: Int?,
        intent: CommentIntent
    ): Flow<Comment>

    fun getAllCommentsRange(
        accountId: Long,
        commented: Commented,
        startFromCommentId: Int,
        continueToCommentId: Int
    ): Flow<List<Comment>>

    fun getAvailableAuthors(accountId: Long): Flow<List<Owner>>
    fun edit(
        accountId: Long,
        commented: Commented,
        commentId: Int,
        text: String?,
        commentThread: Int?,
        attachments: List<AbsModel>?
    ): Flow<Comment>

    fun reportComment(accountId: Long, owner_id: Long, post_id: Int, reason: Int): Flow<Int>
}