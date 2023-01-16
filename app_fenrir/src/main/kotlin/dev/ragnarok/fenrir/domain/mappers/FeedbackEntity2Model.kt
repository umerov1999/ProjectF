package dev.ragnarok.fenrir.domain.mappers

import dev.ragnarok.fenrir.db.model.entity.feedback.*
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildAttachmentFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildCommentFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildPostFromDbo
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.IOwnersBundle
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.feedback.*
import java.util.*

object FeedbackEntity2Model {
    fun buildFeedback(entity: FeedbackEntity, owners: IOwnersBundle): Feedback {
        val feedback: Feedback = when (entity.type) {
            FeedbackType.FOLLOW, FeedbackType.FRIEND_ACCEPTED -> buildUsersFeedback(
                entity as UsersEntity,
                owners
            )
            FeedbackType.MENTION -> buildMentionFeedback(
                entity as MentionEntity,
                owners
            )
            FeedbackType.MENTION_COMMENT_POST, FeedbackType.MENTION_COMMENT_PHOTO, FeedbackType.MENTION_COMMENT_VIDEO -> buildMentionCommentFeedback(
                entity as MentionCommentEntity,
                owners
            )
            FeedbackType.WALL, FeedbackType.WALL_PUBLISH -> buildPostPublishFeedback(
                entity as PostFeedbackEntity,
                owners
            )
            FeedbackType.COMMENT_POST, FeedbackType.COMMENT_PHOTO, FeedbackType.COMMENT_VIDEO -> buildCommentFeedback(
                entity as NewCommentEntity,
                owners
            )
            FeedbackType.REPLY_COMMENT, FeedbackType.REPLY_COMMENT_PHOTO, FeedbackType.REPLY_COMMENT_VIDEO, FeedbackType.REPLY_TOPIC -> buildReplyCommentFeedback(
                entity as ReplyCommentEntity,
                owners
            )
            FeedbackType.LIKE_POST, FeedbackType.LIKE_PHOTO, FeedbackType.LIKE_VIDEO -> buildLikeFeedback(
                entity as LikeEntity,
                owners
            )
            FeedbackType.LIKE_COMMENT_POST, FeedbackType.LIKE_COMMENT_PHOTO, FeedbackType.LIKE_COMMENT_VIDEO, FeedbackType.LIKE_COMMENT_TOPIC -> buildLikeCommentFeedback(
                entity as LikeCommentEntity,
                owners
            )
            FeedbackType.COPY_POST, FeedbackType.COPY_PHOTO, FeedbackType.COPY_VIDEO -> buildCopyFeedback(
                entity as CopyEntity,
                owners
            )
            FeedbackType.NULL -> throw UnsupportedOperationException("Unsupported feedback type, type: " + entity.type)
            else -> throw UnsupportedOperationException("Unsupported feedback type, type: " + entity.type)
        }
        if (entity.reply != null) {
            feedback.setReply(buildCommentFromDbo(entity.reply, owners))
        }
        feedback.setDate(entity.date)
        return feedback
    }

    private fun buildUsersFeedback(entity: UsersEntity, owners: IOwnersBundle): UsersFeedback {
        return UsersFeedback(entity.type)
            .setOwners(buildUserArray(entity.owners, owners))
    }

    private fun buildUserArray(usersIds: LongArray?, owners: IOwnersBundle): List<Owner> {
        usersIds ?: return emptyList()
        val data: MutableList<Owner> = ArrayList(usersIds.size)
        for (id in usersIds) {
            data.add(owners.getById(id))
        }
        return data
    }

    private fun buildCommentFeedback(
        entity: NewCommentEntity,
        owners: IOwnersBundle
    ): CommentFeedback {
        return CommentFeedback(entity.type)
            .setCommentOf(entity.getCommented()?.let { buildAttachmentFromDbo(it, owners) })
            .setComment(buildCommentFromDbo(entity.comment, owners))
    }

    private fun buildCopyFeedback(entity: CopyEntity, owners: IOwnersBundle): CopyFeedback {
        val feedback = CopyFeedback(entity.type)
        feedback.setWhat(entity.getCopied()?.let { buildAttachmentFromDbo(it, owners) })
        val copyOwners: MutableList<Owner> = LinkedList()
        for (pair in entity.copies?.pairDbos.orEmpty()) {
            copyOwners.add(owners.getById(pair.ownerId))
        }
        feedback.setOwners(copyOwners)
        return feedback
    }

    private fun buildLikeCommentFeedback(
        entity: LikeCommentEntity,
        owners: IOwnersBundle
    ): LikeCommentFeedback {
        val feedback = LikeCommentFeedback(entity.type)
        feedback.setOwners(buildUserArray(entity.likesOwnerIds, owners))
        feedback.setLiked(buildCommentFromDbo(entity.liked, owners))
        feedback.setCommented(entity.getCommented()?.let { buildAttachmentFromDbo(it, owners) })
        return feedback
    }

    private fun buildLikeFeedback(entity: LikeEntity, owners: IOwnersBundle): LikeFeedback {
        return LikeFeedback(entity.type)
            .setOwners(buildUserArray(entity.likesOwnerIds, owners))
            .setLiked(entity.getLiked()?.let { buildAttachmentFromDbo(it, owners) })
    }

    private fun buildMentionCommentFeedback(
        entity: MentionCommentEntity,
        owners: IOwnersBundle
    ): MentionCommentFeedback {
        return MentionCommentFeedback(entity.type)
            .setWhere(buildCommentFromDbo(entity.where, owners))
            .setCommentOf(entity.getCommented()?.let { buildAttachmentFromDbo(it, owners) })
    }

    private fun buildMentionFeedback(
        entity: MentionEntity,
        owners: IOwnersBundle
    ): MentionFeedback {
        return MentionFeedback(entity.type)
            .setWhere(entity.getWhere()?.let { buildAttachmentFromDbo(it, owners) })
    }

    private fun buildPostPublishFeedback(
        entity: PostFeedbackEntity,
        owners: IOwnersBundle
    ): PostPublishFeedback {
        return PostPublishFeedback(entity.type)
            .setPost(entity.post?.let { buildPostFromDbo(it, owners) })
    }

    private fun buildReplyCommentFeedback(
        entity: ReplyCommentEntity,
        owners: IOwnersBundle
    ): ReplyCommentFeedback {
        var ownComment: Comment? = null
        if (entity.ownComment != null) {
            ownComment = buildCommentFromDbo(entity.ownComment, owners)
        }
        return ReplyCommentFeedback(entity.type)
            .setCommentsOf(entity.getCommented()?.let { buildAttachmentFromDbo(it, owners) })
            .setFeedbackComment(buildCommentFromDbo(entity.feedbackComment, owners))
            .setOwnComment(ownComment)
    }

    @FeedbackType
    fun transformType(apitype: String?): Int {
        return when (apitype) {
            "follow" -> FeedbackType.FOLLOW
            "friend_accepted" -> FeedbackType.FRIEND_ACCEPTED
            "mention" -> FeedbackType.MENTION
            "mention_comments" -> FeedbackType.MENTION_COMMENT_POST
            "wall" -> FeedbackType.WALL
            "wall_publish" -> FeedbackType.WALL_PUBLISH
            "comment_post" -> FeedbackType.COMMENT_POST
            "comment_photo" -> FeedbackType.COMMENT_PHOTO
            "comment_video" -> FeedbackType.COMMENT_VIDEO
            "reply_comment" -> FeedbackType.REPLY_COMMENT
            "reply_comment_photo" -> FeedbackType.REPLY_COMMENT_PHOTO
            "reply_comment_video" -> FeedbackType.REPLY_COMMENT_VIDEO
            "reply_topic" -> FeedbackType.REPLY_TOPIC
            "like_post" -> FeedbackType.LIKE_POST
            "like_comment" -> FeedbackType.LIKE_COMMENT_POST
            "like_photo" -> FeedbackType.LIKE_PHOTO
            "like_video" -> FeedbackType.LIKE_VIDEO
            "like_comment_photo" -> FeedbackType.LIKE_COMMENT_PHOTO
            "like_comment_video" -> FeedbackType.LIKE_COMMENT_VIDEO
            "like_comment_topic" -> FeedbackType.LIKE_COMMENT_TOPIC
            "copy_post" -> FeedbackType.COPY_POST
            "copy_photo" -> FeedbackType.COPY_PHOTO
            "copy_video" -> FeedbackType.COPY_VIDEO
            "mention_comment_photo" -> FeedbackType.MENTION_COMMENT_PHOTO
            "mention_comment_video" -> FeedbackType.MENTION_COMMENT_VIDEO
            else -> throw UnsupportedOperationException()
        }
    }
}