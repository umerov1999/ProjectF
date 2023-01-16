package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.interfaces.Identificable
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.readOwner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.writeOwner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class WallReply : AbsModel, Identificable {
    private var id = 0
    var fromId = 0L
        private set
    var postId = 0
        private set
    var author: Owner? = null
        private set
    var ownerId = 0L
        private set
    var text: String? = null
        private set
    var attachments: Attachments? = null
        private set

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        fromId = parcel.readLong()
        postId = parcel.readInt()
        ownerId = parcel.readLong()
        text = parcel.readString()
        attachments = parcel.readTypedObjectCompat(Attachments.CREATOR)
        author = readOwner(parcel)
    }

    constructor()

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeLong(fromId)
        parcel.writeInt(postId)
        parcel.writeLong(ownerId)
        parcel.writeString(text)
        parcel.writeTypedObjectCompat(attachments, flags)
        writeOwner(parcel, flags, author)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_WALL_REPLY
    }

    val attachmentsCount: Int
        get() = attachments?.size().orZero()

    fun buildFromComment(comment: Comment, commented: Commented?): WallReply {
        id = comment.getObjectId()
        fromId = comment.fromId
        author = comment.author
        text = comment.text
        ownerId = commented?.sourceOwnerId ?: comment.commented.sourceOwnerId
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    fun hasAttachments(): Boolean {
        return attachments?.hasAttachments == true
    }

    fun hasStickerOnly(): Boolean {
        return attachments?.stickers.nonNullNoEmpty()
    }

    override fun getObjectId(): Int {
        return id
    }

    fun setId(id: Int): WallReply {
        this.id = id
        return this
    }

    fun setOwnerId(owner_id: Long): WallReply {
        ownerId = owner_id
        return this
    }

    fun setAuthor(author: Owner?): WallReply {
        this.author = author
        return this
    }

    val authorPhoto: String?
        get() = author?.maxSquareAvatar
    val authorName: String?
        get() = author?.fullName

    fun setPostId(post_id: Int): WallReply {
        postId = post_id
        return this
    }

    fun setFromId(from_id: Long): WallReply {
        fromId = from_id
        return this
    }

    fun setText(text: String?): WallReply {
        this.text = text
        return this
    }

    fun setAttachments(attachments: Attachments?): WallReply {
        this.attachments = attachments
        return this
    }

    companion object CREATOR : Parcelable.Creator<WallReply> {
        override fun createFromParcel(parcel: Parcel): WallReply {
            return WallReply(parcel)
        }

        override fun newArray(size: Int): Array<WallReply?> {
            return arrayOfNulls(size)
        }
    }
}