package dev.ragnarok.fenrir.model.feedback

import androidx.annotation.IntDef

@IntDef(
    FeedbackType.NULL,
    FeedbackType.FOLLOW,
    FeedbackType.FRIEND_ACCEPTED,
    FeedbackType.MENTION,
    FeedbackType.MENTION_COMMENT_POST,
    FeedbackType.WALL,
    FeedbackType.WALL_PUBLISH,
    FeedbackType.COMMENT_POST,
    FeedbackType.COMMENT_PHOTO,
    FeedbackType.COMMENT_VIDEO,
    FeedbackType.REPLY_COMMENT,
    FeedbackType.REPLY_COMMENT_PHOTO,
    FeedbackType.REPLY_COMMENT_VIDEO,
    FeedbackType.REPLY_TOPIC,
    FeedbackType.LIKE_POST,
    FeedbackType.LIKE_COMMENT_POST,
    FeedbackType.LIKE_PHOTO,
    FeedbackType.LIKE_VIDEO,
    FeedbackType.LIKE_COMMENT_PHOTO,
    FeedbackType.LIKE_COMMENT_VIDEO,
    FeedbackType.LIKE_COMMENT_TOPIC,
    FeedbackType.COPY_POST,
    FeedbackType.COPY_PHOTO,
    FeedbackType.COPY_VIDEO,
    FeedbackType.MENTION_COMMENT_PHOTO,
    FeedbackType.MENTION_COMMENT_VIDEO
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class FeedbackType {
    companion object {
        const val NULL = 0

        /**
         * У пользователя появился один или несколько новых подписчиков
         */
        const val FOLLOW = 1

        /**
         * Заявка в друзья, отправленная пользователем, была принята
         */
        const val FRIEND_ACCEPTED = 2

        /**
         * Была создана запись на чужой стене, содержащая упоминание пользователя
         */
        const val MENTION = 3

        /**
         * Был оставлен комментарий, содержащий упоминание пользователя
         */
        const val MENTION_COMMENT_POST = 4

        /**
         * Была добавлена запись на стене пользователя
         */
        const val WALL = 5

        /**
         * Была опубликована новость, предложенная пользователем в публичной странице
         */
        const val WALL_PUBLISH = 6

        /**
         * Был добавлен новый комментарий к записи, созданной пользователем
         */
        const val COMMENT_POST = 7

        /**
         * Был добавлен новый комментарий к фотографии пользователя
         */
        const val COMMENT_PHOTO = 8

        /**
         * Был добавлен новый комментарий к видеозаписи пользователя
         */
        const val COMMENT_VIDEO = 9

        /**
         * Был добавлен новый ответ на комментарий пользователя
         */
        const val REPLY_COMMENT = 10

        /**
         * Был добавлен новый ответ на комментарий пользователя к фотографии
         */
        const val REPLY_COMMENT_PHOTO = 11

        /**
         * Был добавлен новый ответ на комментарий пользователя к видеозаписи
         */
        const val REPLY_COMMENT_VIDEO = 12

        /**
         * Был добавлен новый ответ пользователю в обсуждении
         */
        const val REPLY_TOPIC = 13

        /**
         * У записи пользователя появилась одна или несколько новых отметок «Мне нравится»
         */
        const val LIKE_POST = 14

        /**
         * У комментария пользователя появилась одна или несколько новых отметок «Мне нравится»
         */
        const val LIKE_COMMENT_POST = 15

        /**
         * У фотографии пользователя появилась одна или несколько новых отметок «Мне нравится»
         */
        const val LIKE_PHOTO = 16

        /**
         * У видеозаписи пользователя появилась одна или несколько новых отметок «Мне нравится»
         */
        const val LIKE_VIDEO = 17

        /**
         * У комментария пользователя к фотографии появилась одна или несколько новых отметок «Мне нравится»
         */
        const val LIKE_COMMENT_PHOTO = 18

        /**
         * У комментария пользователя к видеозаписи появилась одна или несколько новых отметок «Мне нравится»
         */
        const val LIKE_COMMENT_VIDEO = 19

        /**
         * У комментария пользователя в обсуждении появилась одна или несколько новых отметок «Мне нравится»
         */
        const val LIKE_COMMENT_TOPIC = 20

        /**
         * Один или несколько пользователей скопировали запись пользователя
         */
        const val COPY_POST = 21

        /**
         * Один или несколько пользователей скопировали фотографию пользователя
         */
        const val COPY_PHOTO = 22

        /**
         * Один или несколько пользователей скопировали видеозапись пользователя
         */
        const val COPY_VIDEO = 23

        /**
         * Под фотографией был оставлен комментарий, содержащий упоминание пользователя
         */
        const val MENTION_COMMENT_PHOTO = 24

        /**
         * Под видео был оставлен комментарий, содержащий упоминание пользователя
         */
        const val MENTION_COMMENT_VIDEO = 25
    }
}