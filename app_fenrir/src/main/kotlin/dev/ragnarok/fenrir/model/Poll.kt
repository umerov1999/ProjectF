package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

class Poll : AbsModel {
    val id: Int
    val ownerId: Int
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
    var authorId = 0
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

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        creationTime = `in`.readLong()
        question = `in`.readString()
        voteCount = `in`.readInt()
        myAnswerIds = `in`.createLongArray()
        isAnonymous = `in`.getBoolean()
        answers = `in`.createTypedArrayList(Answer.CREATOR)
        isBoard = `in`.getBoolean()
        isClosed = `in`.getBoolean()
        authorId = `in`.readInt()
        isCanVote = `in`.getBoolean()
        isCanEdit = `in`.getBoolean()
        isCanReport = `in`.getBoolean()
        isCanShare = `in`.getBoolean()
        endDate = `in`.readLong()
        isMultiple = `in`.getBoolean()
        photo = `in`.readString()
    }

    constructor(id: Int, ownerId: Int) {
        this.id = id
        this.ownerId = ownerId
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeLong(creationTime)
        parcel.writeString(question)
        parcel.writeInt(voteCount)
        parcel.writeLongArray(myAnswerIds)
        parcel.putBoolean(isAnonymous)
        parcel.writeTypedList(answers)
        parcel.putBoolean(isBoard)
        parcel.putBoolean(isClosed)
        parcel.writeInt(authorId)
        parcel.putBoolean(isCanVote)
        parcel.putBoolean(isCanEdit)
        parcel.putBoolean(isCanReport)
        parcel.putBoolean(isCanShare)
        parcel.writeLong(endDate)
        parcel.putBoolean(isMultiple)
        parcel.writeString(photo)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_POLL
    }

    fun setClosed(closed: Boolean): Poll {
        isClosed = closed
        return this
    }

    fun setAuthorId(authorId: Int): Poll {
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

        internal constructor(`in`: Parcel) {
            id = `in`.readLong()
            text = `in`.readString()
            voteCount = `in`.readInt()
            rate = `in`.readDouble()
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

    companion object CREATOR : Parcelable.Creator<Poll> {
        override fun createFromParcel(parcel: Parcel): Poll {
            return Poll(parcel)
        }

        override fun newArray(size: Int): Array<Poll?> {
            return arrayOfNulls(size)
        }
    }
}