package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

/**
 * Describes poll on the wall on board.
 */
@Serializable
class VKApiPoll
/**
 * Creates empty Country instance.
 */
    : VKApiAttachment {
    /**
     * Poll ID to get information about it using polls.getById method;
     */
    var id = 0

    /**
     * ID of the user or community that owns this poll.
     */
    var owner_id = 0

    /**
     * Date (in Unix time) the poll was created.
     */
    var created: Long = 0

    /**
     * Question in the poll.
     */
    var question: String? = null

    /**
     * The total number of users answered.
     */
    var votes = 0

    /**
     * Response ID of the current user(if the current user has not yet posted in this poll, it contains 0)
     */
    var answer_ids: LongArray? = null

    /* возвращется для анонимных опросов. */
    var anonymous = false

    /**
     * Array of answers for this question.
     */
    var answers: List<Answer>? = null

    /**
     * true – опрос находится в обсуждении,
     * false – опрос прикреплен к стене.
     */
    var is_board = false
    var closed = false
    var author_id = 0
    var can_vote = false
    var can_edit = false
    var can_report = false
    var can_share = false
    var end_date: Long = 0
    var multiple = false
    var photo: Photo? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_POLL
    }

    /**
     * Represents answer for the poll
     */
    @Serializable
    class Answer {
        /**
         * ID of the answer for the question
         */
        var id = 0L

        /**
         * Text of the answer
         */
        var text: String? = null

        /**
         * Number of users that voted for this answer
         */
        var votes = 0

        /**
         * Rate of this answer in percent
         */
        var rate = 0.0
    }

    @Serializable
    class Photo {
        var color: String? = null
        var id = 0
        var images: List<Image>? = null
    }

    @Serializable
    class Image {
        var width = 0
        var height = 0
        var url: String? = null
    }
}