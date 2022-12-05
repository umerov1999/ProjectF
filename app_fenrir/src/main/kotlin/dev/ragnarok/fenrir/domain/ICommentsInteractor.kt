package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.*
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

interface ICommentsInteractor {
    fun getAllCachedData(accountId: Int, commented: Commented): Single<List<Comment>>
    fun getCommentsPortion(
        accountId: Int,
        commented: Commented,
        offset: Int,
        count: Int,
        startCommentId: Int?,
        threadComment: Int?,
        invalidateCache: Boolean,
        sort: String?
    ): Single<CommentsBundle>

    fun getCommentsNoCache(
        accountId: Int,
        ownerId: Int,
        postId: Int,
        offset: Int
    ): Single<List<Comment>>

    fun restoreDraftComment(accountId: Int, commented: Commented): Maybe<DraftComment>?
    fun safeDraftComment(
        accountId: Int,
        commented: Commented,
        body: String?,
        replyToCommentId: Int,
        replyToUserId: Int
    ): Single<Int>

    fun like(accountId: Int, commented: Commented, commentId: Int, add: Boolean): Completable
    fun checkAndAddLike(accountId: Int, commented: Commented, commentId: Int): Single<Int>
    fun isLiked(accountId: Int, commented: Commented, commentId: Int): Single<Boolean>
    fun deleteRestore(
        accountId: Int,
        commented: Commented,
        commentId: Int,
        delete: Boolean
    ): Completable

    fun send(
        accountId: Int,
        commented: Commented,
        commentThread: Int?,
        intent: CommentIntent
    ): Single<Comment>

    fun getAllCommentsRange(
        accountId: Int,
        commented: Commented,
        startFromCommentId: Int,
        continueToCommentId: Int
    ): Single<List<Comment>>

    fun getAvailableAuthors(accountId: Int): Single<List<Owner>>
    fun edit(
        accountId: Int,
        commented: Commented,
        commentId: Int,
        body: String?,
        commentThread: Int?,
        attachments: List<AbsModel>?
    ): Single<Comment>

    fun reportComment(accountId: Int, owner_id: Int, post_id: Int, reason: Int): Single<Int>
}