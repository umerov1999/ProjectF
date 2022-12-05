package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.api.model.Identificable
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.readOwner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.writeOwner

class Comment : AbsModel, Identificable {
    val commented: Commented

    /**
     * идентификатор комментария.
     */
    private var id = 0

    /**
     * идентификатор автора комментария.
     */
    var fromId = 0
        private set

    /**
     * дата создания комментария в формате unixtime.
     */
    var date: Long = 0
        private set

    /**
     * текст комментария
     */
    var text: String? = null
        private set

    /**
     * идентификатор пользователя или сообщества, в ответ которому оставлен текущий комментарий (если применимо).
     */
    var replyToUser = 0
        private set

    /**
     * идентификатор комментария, в ответ на который оставлен текущий (если применимо).
     */
    var replyToComment = 0
        private set

    /**
     * Number of likes on the comment.
     */
    var likesCount = 0
        private set

    /**
     * Information whether the current user liked the comment.
     */
    var isUserLikes = false
        private set

    /**
     * Whether the current user can like on the comment.
     */
    var isCanLike = false
        private set
    var isCanEdit = false
        private set

    /**
     * объект, содержащий информацию о медиавложениях в комментарии
     */
    var attachments: Attachments? = null
        private set
    var author: Owner? = null
        private set
    private var dbid = 0
    var isDeleted = false
        private set

    //not parcelable
    var isAnimationNow = false
        private set
    var threadsCount = 0
        private set
    var pid = 0
        private set
    var threads: List<Comment>? = null
        private set

    constructor(commented: Commented) {
        this.commented = commented
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        fromId = `in`.readInt()
        date = `in`.readLong()
        text = `in`.readString()
        replyToUser = `in`.readInt()
        replyToComment = `in`.readInt()
        likesCount = `in`.readInt()
        isUserLikes = `in`.getBoolean()
        isCanLike = `in`.getBoolean()
        isCanEdit = `in`.getBoolean()
        attachments = `in`.readTypedObjectCompat(Attachments.CREATOR)
        commented = `in`.readTypedObjectCompat(Commented.CREATOR)!!
        author = readOwner(`in`)
        dbid = `in`.readInt()
        isDeleted = `in`.getBoolean()
        threadsCount = `in`.readInt()
        threads = `in`.createTypedArrayList(CREATOR)
        pid = `in`.readInt()
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_COMMENT
    }

    override fun getObjectId(): Int {
        return id
    }

    fun setId(id: Int): Comment {
        this.id = id
        return this
    }

    fun setThreads(threads: List<Comment>?): Comment {
        this.threads = threads
        return this
    }

    fun setThreadsCount(threads_count: Int): Comment {
        threadsCount = threads_count
        return this
    }

    fun setFromId(fromId: Int): Comment {
        this.fromId = fromId
        return this
    }

    fun setDate(date: Long): Comment {
        this.date = date
        return this
    }

    fun setText(text: String?): Comment {
        this.text = text
        return this
    }

    fun setReplyToUser(replyToUser: Int): Comment {
        this.replyToUser = replyToUser
        return this
    }

    fun setReplyToComment(replyToComment: Int): Comment {
        this.replyToComment = replyToComment
        return this
    }

    fun setLikesCount(likesCount: Int): Comment {
        this.likesCount = likesCount
        return this
    }

    fun setUserLikes(userLikes: Boolean): Comment {
        isUserLikes = userLikes
        return this
    }

    fun setCanLike(canLike: Boolean): Comment {
        isCanLike = canLike
        return this
    }

    fun setAttachments(attachments: Attachments?): Comment {
        this.attachments = attachments
        return this
    }

    fun setPid(pid: Int): Comment {
        this.pid = pid
        return this
    }

    fun setAuthor(author: Owner?): Comment {
        this.author = author
        return this
    }

    fun setDeleted(deleted: Boolean): Comment {
        isDeleted = deleted
        return this
    }

    fun setAnimationNow(animationNow: Boolean): Comment {
        isAnimationNow = animationNow
        return this
    }

    fun hasAttachments(): Boolean {
        return attachments?.hasAttachments == true
    }

    fun hasStickerOnly(): Boolean {
        return attachments?.stickers.nonNullNoEmpty()
    }

    val fullAuthorName: String?
        get() = author?.fullName
    val maxAuthorAvaUrl: String?
        get() = author?.maxSquareAvatar

    override fun describeContents(): Int {
        return 0
    }

    fun setCanEdit(canEdit: Boolean): Comment {
        isCanEdit = canEdit
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(fromId)
        parcel.writeLong(date)
        parcel.writeString(text)
        parcel.writeInt(replyToUser)
        parcel.writeInt(replyToComment)
        parcel.writeInt(likesCount)
        parcel.putBoolean(isUserLikes)
        parcel.putBoolean(isCanLike)
        parcel.putBoolean(isCanEdit)
        parcel.writeTypedObjectCompat(attachments, flags)
        parcel.writeTypedObjectCompat(commented, flags)
        writeOwner(parcel, flags, author)
        parcel.writeInt(dbid)
        parcel.putBoolean(isDeleted)
        parcel.writeInt(threadsCount)
        parcel.writeTypedList(threads)
        parcel.writeInt(pid)
    }

    val attachmentsCount: Int
        get() = attachments?.size().orZero()

    fun hasThreads(): Boolean {
        return threads.nonNullNoEmpty()
    }

    fun receivedThreadsCount(): Int {
        return threads?.size.orZero()
    }

    companion object CREATOR : Parcelable.Creator<Comment> {
        override fun createFromParcel(parcel: Parcel): Comment {
            return Comment(parcel)
        }

        override fun newArray(size: Int): Array<Comment?> {
            return arrayOfNulls(size)
        }
    }
}