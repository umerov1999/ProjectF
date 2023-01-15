package dev.ragnarok.fenrir.model.feedback

import androidx.annotation.IntDef

@IntDef(
    FeedbackModelType.MODEL_NULL_FEEDBACK,
    FeedbackModelType.MODEL_COMMENT_FEEDBACK,
    FeedbackModelType.MODEL_COPY_FEEDBACK,
    FeedbackModelType.MODEL_LIKECOMMENT_FEEDBACK,
    FeedbackModelType.MODEL_LIKE_FEEDBACK,
    FeedbackModelType.MODEL_MENTIONCOMMENT_FEEDBACK,
    FeedbackModelType.MODEL_MENTION_FEEDBACK,
    FeedbackModelType.MODEL_POSTPUBLISH_FEEDBACK,
    FeedbackModelType.MODEL_REPLYCOMMENT_FEEDBACK,
    FeedbackModelType.MODEL_USERS_FEEDBACK
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class FeedbackModelType {
    companion object {
        const val MODEL_NULL_FEEDBACK = 0
        const val MODEL_COMMENT_FEEDBACK = 1
        const val MODEL_COPY_FEEDBACK = 2
        const val MODEL_LIKECOMMENT_FEEDBACK = 3
        const val MODEL_LIKE_FEEDBACK = 4
        const val MODEL_MENTIONCOMMENT_FEEDBACK = 5
        const val MODEL_MENTION_FEEDBACK = 6
        const val MODEL_POSTPUBLISH_FEEDBACK = 7
        const val MODEL_REPLYCOMMENT_FEEDBACK = 8
        const val MODEL_USERS_FEEDBACK = 9
    }
}
