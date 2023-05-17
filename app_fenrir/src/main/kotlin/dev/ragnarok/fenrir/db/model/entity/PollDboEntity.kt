package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.ColorInt
import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("poll")
class PollDboEntity : DboEntity() {
    var isClosed = false
    var authorId = 0L
    var isCanVote = false
    var isCanEdit = false
    var isCanReport = false
    var isCanShare = false
    var endDate: Long = 0
    var isMultiple = false
    var id = 0
        private set
    var ownerId = 0L
        private set
    var creationTime: Long = 0
        private set
    var question: String? = null
        private set
    var voteCount = 0
        private set
    var myAnswerIds: LongArray? = null
        private set
    var isAnonymous = false
        private set
    var answers: List<Answer>? = null
        private set
    var isBoard = false
        private set
    var photo: String? = null
        private set
    var background: BackgroundEntity? = null
        private set

    operator fun set(id: Int, ownerId: Long): PollDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setMultiple(multiple: Boolean): PollDboEntity {
        isMultiple = multiple
        return this
    }

    fun setClosed(closed: Boolean): PollDboEntity {
        isClosed = closed
        return this
    }

    fun setAuthorId(authorId: Long): PollDboEntity {
        this.authorId = authorId
        return this
    }

    fun setCanVote(canVote: Boolean): PollDboEntity {
        isCanVote = canVote
        return this
    }

    fun setCanEdit(canEdit: Boolean): PollDboEntity {
        isCanEdit = canEdit
        return this
    }

    fun setCanReport(canReport: Boolean): PollDboEntity {
        isCanReport = canReport
        return this
    }

    fun setCanShare(canShare: Boolean): PollDboEntity {
        isCanShare = canShare
        return this
    }

    fun setEndDate(endDate: Long): PollDboEntity {
        this.endDate = endDate
        return this
    }

    fun setCreationTime(creationTime: Long): PollDboEntity {
        this.creationTime = creationTime
        return this
    }

    fun setQuestion(question: String?): PollDboEntity {
        this.question = question
        return this
    }

    fun setPhoto(photo: String?): PollDboEntity {
        this.photo = photo
        return this
    }

    fun setBackground(background: BackgroundEntity?): PollDboEntity {
        this.background = background
        return this
    }

    fun setVoteCount(voteCount: Int): PollDboEntity {
        this.voteCount = voteCount
        return this
    }

    fun setMyAnswerIds(myAnswerIds: LongArray?): PollDboEntity {
        this.myAnswerIds = myAnswerIds
        return this
    }

    fun setAnonymous(anonymous: Boolean): PollDboEntity {
        isAnonymous = anonymous
        return this
    }

    fun setAnswers(answers: List<Answer>?): PollDboEntity {
        this.answers = answers
        return this
    }

    fun setBoard(board: Boolean): PollDboEntity {
        isBoard = board
        return this
    }

    @Keep
    @Serializable
    class BackgroundEntity(
        var id: Int,
        var angle: Int,
        var name: String?,
        var points: List<BackgroundPointEntity>
    )

    @Keep
    @Serializable
    class BackgroundPointEntity(@ColorInt var color: Int, var position: Float)

    @Keep
    @Serializable
    class Answer {
        var id = 0L
            private set
        var text: String? = null
            private set
        var voteCount = 0
            private set
        var rate = 0.0
            private set

        operator fun set(id: Long, text: String?, voteCount: Int, rate: Double): Answer {
            this.id = id
            this.text = text
            this.voteCount = voteCount
            this.rate = rate
            return this
        }
    }
}