package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Topic : AbsModel {
    val id: Int
    val ownerId: Int
    var title: String? = null
        private set
    var creationTime: Long = 0
        private set
    var createdByOwnerId = 0
        private set
    var lastUpdateTime: Long = 0
        private set
    var updatedByOwnerId = 0
        private set
    var isClosed = false
        private set
    var isFixed = false
        private set
    var commentsCount = 0
        private set
    var firstCommentBody: String? = null
        private set
    var lastCommentBody: String? = null
        private set
    var creator: Owner? = null
        private set
    var updater: Owner? = null
        private set

    constructor(id: Int, ownerId: Int) {
        this.id = id
        this.ownerId = ownerId
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        title = `in`.readString()
        creationTime = `in`.readLong()
        createdByOwnerId = `in`.readInt()
        lastUpdateTime = `in`.readLong()
        updatedByOwnerId = `in`.readInt()
        isClosed = `in`.getBoolean()
        isFixed = `in`.getBoolean()
        commentsCount = `in`.readInt()
        firstCommentBody = `in`.readString()
        lastCommentBody = `in`.readString()
        creator = `in`.readTypedObjectCompat(ParcelableOwnerWrapper.CREATOR)?.get()
        updater = `in`.readTypedObjectCompat(ParcelableOwnerWrapper.CREATOR)?.get()
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_TOPIC
    }

    fun setTitle(title: String?): Topic {
        this.title = title
        return this
    }

    fun setCreationTime(creationTime: Long): Topic {
        this.creationTime = creationTime
        return this
    }

    fun setCreatedByOwnerId(createdByOwnerId: Int): Topic {
        this.createdByOwnerId = createdByOwnerId
        return this
    }

    fun setLastUpdateTime(lastUpdateTime: Long): Topic {
        this.lastUpdateTime = lastUpdateTime
        return this
    }

    fun setUpdatedByOwnerId(updatedByOwnerId: Int): Topic {
        this.updatedByOwnerId = updatedByOwnerId
        return this
    }

    fun setClosed(closed: Boolean): Topic {
        isClosed = closed
        return this
    }

    fun setFixed(fixed: Boolean): Topic {
        isFixed = fixed
        return this
    }

    fun setCommentsCount(commentsCount: Int): Topic {
        this.commentsCount = commentsCount
        return this
    }

    fun setFirstCommentBody(firstCommentBody: String?): Topic {
        this.firstCommentBody = firstCommentBody
        return this
    }

    fun setLastCommentBody(lastCommentBody: String?): Topic {
        this.lastCommentBody = lastCommentBody
        return this
    }

    fun setCreator(creator: Owner?): Topic {
        this.creator = creator
        return this
    }

    fun setUpdater(updater: Owner?): Topic {
        this.updater = updater
        return this
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeString(title)
        parcel.writeLong(creationTime)
        parcel.writeInt(createdByOwnerId)
        parcel.writeLong(lastUpdateTime)
        parcel.writeInt(updatedByOwnerId)
        parcel.putBoolean(isClosed)
        parcel.putBoolean(isFixed)
        parcel.writeInt(commentsCount)
        parcel.writeString(firstCommentBody)
        parcel.writeString(lastCommentBody)
        parcel.writeTypedObjectCompat(ParcelableOwnerWrapper(creator), flags)
        parcel.writeTypedObjectCompat(ParcelableOwnerWrapper(updater), flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Topic> {
        override fun createFromParcel(parcel: Parcel): Topic {
            return Topic(parcel)
        }

        override fun newArray(size: Int): Array<Topic?> {
            return arrayOfNulls(size)
        }
    }
}