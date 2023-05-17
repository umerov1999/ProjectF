package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Poll : AbsModel {
    val id: Int
    val ownerId: Long
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
    var answers: MutableList<Answer>? = null
        private set
    var isBoard = false
        private set
    var isClosed = false
        private set
    var authorId = 0L
        private set
    var isCanVote = false
        private set
    var isCanEdit = false
        private set
    var isCanReport = false
        private set
    var isCanShare = false
        private set
    var endDate: Long = 0
        private set
    var isMultiple = false
        private set
    var photo: String? = null
        private set
    var background: PollBackground? = null
        private set

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
        creationTime = parcel.readLong()
        question = parcel.readString()
        voteCount = parcel.readInt()
        myAnswerIds = parcel.createLongArray()
        isAnonymous = parcel.getBoolean()
        answers = parcel.createTypedArrayList(Answer.CREATOR)
        isBoard = parcel.getBoolean()
        isClosed = parcel.getBoolean()
        authorId = parcel.readLong()
        isCanVote = parcel.getBoolean()
        isCanEdit = parcel.getBoolean()
        isCanReport = parcel.getBoolean()
        isCanShare = parcel.getBoolean()
        endDate = parcel.readLong()
        isMultiple = parcel.getBoolean()
        photo = parcel.readString()
        background = parcel.readTypedObjectCompat(PollBackground.CREATOR)
    }

    constructor(id: Int, ownerId: Long) {
        this.id = id
        this.ownerId = ownerId
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeLong(ownerId)
        parcel.writeLong(creationTime)
        parcel.writeString(question)
        parcel.writeInt(voteCount)
        parcel.writeLongArray(myAnswerIds)
        parcel.putBoolean(isAnonymous)
        parcel.writeTypedList(answers)
        parcel.putBoolean(isBoard)
        parcel.putBoolean(isClosed)
        parcel.writeLong(authorId)
        parcel.putBoolean(isCanVote)
        parcel.putBoolean(isCanEdit)
        parcel.putBoolean(isCanReport)
        parcel.putBoolean(isCanShare)
        parcel.writeLong(endDate)
        parcel.putBoolean(isMultiple)
        parcel.writeString(photo)
        parcel.writeTypedObjectCompat(background, flags)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_POLL
    }

    fun setClosed(closed: Boolean): Poll {
        isClosed = closed
        return this
    }

    fun setAuthorId(authorId: Long): Poll {
        this.authorId = authorId
        return this
    }

    fun setCanVote(canVote: Boolean): Poll {
        isCanVote = canVote
        return this
    }

    fun setCanEdit(canEdit: Boolean): Poll {
        isCanEdit = canEdit
        return this
    }

    fun setCanReport(canReport: Boolean): Poll {
        isCanReport = canReport
        return this
    }

    fun setCanShare(canShare: Boolean): Poll {
        isCanShare = canShare
        return this
    }

    fun setEndDate(endDate: Long): Poll {
        this.endDate = endDate
        return this
    }

    fun setMultiple(multiple: Boolean): Poll {
        isMultiple = multiple
        return this
    }

    fun setCreationTime(creationTime: Long): Poll {
        this.creationTime = creationTime
        return this
    }

    fun setQuestion(question: String?): Poll {
        this.question = question
        return this
    }

    fun setPhoto(photo: String?): Poll {
        this.photo = photo
        return this
    }

    fun setBackground(background: PollBackground?): Poll {
        this.background = background
        return this
    }

    fun setVoteCount(voteCount: Int): Poll {
        this.voteCount = voteCount
        return this
    }

    fun setMyAnswerIds(myAnswerIds: LongArray?): Poll {
        this.myAnswerIds = myAnswerIds
        return this
    }

    fun setAnonymous(anonymous: Boolean): Poll {
        isAnonymous = anonymous
        return this
    }

    fun setAnswers(answers: MutableList<Answer>?): Poll {
        this.answers = answers
        return this
    }

    fun setBoard(board: Boolean): Poll {
        isBoard = board
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    class Answer : AbsModel {
        val id: Long
        var text: String? = null
            private set
        var voteCount = 0
            private set
        var rate = 0.0
            private set

        constructor(id: Long) {
            this.id = id
        }

        internal constructor(parcel: Parcel) {
            id = parcel.readLong()
            text = parcel.readString()
            voteCount = parcel.readInt()
            rate = parcel.readDouble()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(id)
            parcel.writeString(text)
            parcel.writeInt(voteCount)
            parcel.writeDouble(rate)
        }

        @AbsModelType
        override fun getModelType(): Int {
            return AbsModelType.MODEL_POLL_ANSWER
        }

        fun setText(text: String?): Answer {
            this.text = text
            return this
        }

        fun setVoteCount(voteCount: Int): Answer {
            this.voteCount = voteCount
            return this
        }

        fun setRate(rate: Double): Answer {
            this.rate = rate
            return this
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Answer> {
            override fun createFromParcel(parcel: Parcel): Answer {
                return Answer(parcel)
            }

            override fun newArray(size: Int): Array<Answer?> {
                return arrayOfNulls(size)
            }
        }
    }

    class PollBackground : AbsModel {
        val id: Int
        var angle: Int = 0
            private set
        var name: String? = null
            private set
        var points: List<PollBackgroundPoint>? = null
            private set

        constructor(id: Int) {
            this.id = id
        }

        internal constructor(parcel: Parcel) {
            id = parcel.readInt()
            angle = parcel.readInt()
            points = parcel.createTypedArrayList(PollBackgroundPoint.CREATOR)
            name = parcel.readString()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(id)
            parcel.writeInt(angle)
            parcel.writeTypedList(points)
            parcel.writeString(name)
        }

        @AbsModelType
        override fun getModelType(): Int {
            return AbsModelType.MODEL_POLL_BACKGROUND
        }

        fun getColors(): IntArray {
            val tmpPoints = points
            tmpPoints ?: return IntArray(0)
            val arr = IntArray(tmpPoints.size)
            for (i in tmpPoints.indices) {
                arr[i] = tmpPoints[i].color
            }
            return arr
        }

        fun getPositions(): FloatArray {
            val tmpPoints = points
            tmpPoints ?: return FloatArray(0)
            val arr = FloatArray(tmpPoints.size)
            for (i in tmpPoints.indices) {
                arr[i] = tmpPoints[i].position
            }
            return arr
        }

        fun setName(name: String?): PollBackground {
            this.name = name
            return this
        }

        fun setAngle(angle: Int): PollBackground {
            this.angle = angle
            return this
        }

        fun setPoints(points: List<PollBackgroundPoint>?): PollBackground {
            this.points = points
            return this
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<PollBackground> {
            override fun createFromParcel(parcel: Parcel): PollBackground {
                return PollBackground(parcel)
            }

            override fun newArray(size: Int): Array<PollBackground?> {
                return arrayOfNulls(size)
            }
        }
    }

    class PollBackgroundPoint : AbsModel {
        @ColorInt
        var color: Int = 0
            private set
        var position: Float = 0f
            private set

        constructor()

        internal constructor(parcel: Parcel) {
            color = parcel.readInt()
            position = parcel.readFloat()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(color)
            parcel.writeFloat(position)
        }

        @AbsModelType
        override fun getModelType(): Int {
            return AbsModelType.MODEL_POLL_BACKGROUND_POINT
        }

        fun setColor(@ColorInt color: Int): PollBackgroundPoint {
            this.color = color
            return this
        }

        fun setPosition(position: Float): PollBackgroundPoint {
            this.position = position
            return this
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<PollBackgroundPoint> {
            override fun createFromParcel(parcel: Parcel): PollBackgroundPoint {
                return PollBackgroundPoint(parcel)
            }

            override fun newArray(size: Int): Array<PollBackgroundPoint?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<Poll> {
        override fun createFromParcel(parcel: Parcel): Poll {
            return Poll(parcel)
        }

        override fun newArray(size: Int): Array<Poll?> {
            return arrayOfNulls(size)
        }
    }
}