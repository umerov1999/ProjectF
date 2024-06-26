package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.CommentIntent
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.CommentsBundle
import dev.ragnarok.fenrir.model.DraftComment
import dev.ragnarok.fenrir.model.Owner
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

interface ICommentsInteractor {
    fun getAllCachedData(accountId: Long, commented: Commented): Single<List<Comment>>
    fun getCommentsPortion(
        accountId: Long,
        commented: Commented,
        offset: Int,
        count: Int,
        startCommentId: Int?,
        threadComment: Int?,
        invalidateCache: Boolean,
        sort: String?
    ): Single<CommentsBundle>

    fun getCommentsNoCache(
        accountId: Long,
        ownerId: Long,
        postId: Int,
        offset: Int
    ): Single<List<Comment>>

    fun restoreDraftComment(accountId: Long, commented: Commented): Maybe<DraftComment>?
    fun safeDraftComment(
        accountId: Long,
        commented: Commented,
        text: String?,
        replyToCommentId: Int,
        replyToUserId: Long
    ): Single<Int>

    fun like(accountId: Long, commented: Commented, commentId: Int, add: Boolean): Completable
    fun checkAndAddLike(accountId: Long, commented: Commented, commentId: Int): Single<Int>
    fun isLiked(accountId: Long, commented: Commented, commentId: Int): Single<Boolean>
    fun deleteRestore(
        accountId: Long,
        commented: Commented,
        commentId: Int,
        delete: Boolean
    ): Completable

    fun send(
        accountId: Long,
        commented: Commented,
        commentThread: Int?,
        intent: CommentIntent
    ): Single<Comment>

    fun getAllCommentsRange(
        accountId: Long,
        commented: Commented,
        startFromCommentId: Int,
        continueToCommentId: Int
    ): Single<List<Comment>>

    fun getAvailableAuthors(accountId: Long): Single<List<Owner>>
    fun edit(
        accountId: Long,
        commented: Commented,
        commentId: Int,
        text: String?,
        commentThread: Int?,
        attachments: List<AbsModel>?
    ): Single<Comment>

    fun reportComment(accountId: Long, owner_id: Long, post_id: Int, reason: Int): Single<Int>
}