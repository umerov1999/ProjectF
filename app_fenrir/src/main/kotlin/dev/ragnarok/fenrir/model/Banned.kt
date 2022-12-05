package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Banned : Parcelable {
    val banned: Owner
    val admin: User
    val info: Info

    constructor(banned: Owner, admin: User, info: Info) {
        this.banned = banned
        this.admin = admin
        this.info = info
    }

    internal constructor(`in`: Parcel) {
        val wrapper = `in`.readTypedObjectCompat(ParcelableOwnerWrapper.CREATOR)
        banned = wrapper?.get()!!
        admin = `in`.readTypedObjectCompat(User.CREATOR)!!
        info = `in`.readTypedObjectCompat(Info.CREATOR)!!
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedObjectCompat(ParcelableOwnerWrapper(banned), flags)
        dest.writeTypedObjectCompat(admin, flags)
        dest.writeTypedObjectCompat(info, flags)
    }

    class Info : Parcelable {
        var date: Long = 0
            private set
        var reason = 0
            private set
        var comment: String? = null
            private set
        var endDate: Long = 0
            private set
        var isCommentVisible = false
            private set

        internal constructor(`in`: Parcel) {
            date = `in`.readLong()
            reason = `in`.readInt()
            comment = `in`.readString()
            endDate = `in`.readLong()
            isCommentVisible = `in`.getBoolean()
        }

        constructor()

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(parcel: Parcel, i: Int) {
            parcel.writeLong(date)
            parcel.writeInt(reason)
            parcel.writeString(comment)
            parcel.writeLong(endDate)
            parcel.putBoolean(isCommentVisible)
        }

        fun setDate(date: Long): Info {
            this.date = date
            return this
        }

        fun setReason(reason: Int): Info {
            this.reason = reason
            return this
        }

        fun setComment(comment: String?): Info {
            this.comment = comment
            return this
        }

        fun setEndDate(endDate: Long): Info {
            this.endDate = endDate
            return this
        }

        fun setCommentVisible(commentVisible: Boolean): Info {
            isCommentVisible = commentVisible
            return this
        }

        companion object CREATOR : Parcelable.Creator<Info> {
            override fun createFromParcel(parcel: Parcel): Info {
                return Info(parcel)
            }

            override fun newArray(size: Int): Array<Info?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<Banned> {
        override fun createFromParcel(parcel: Parcel): Banned {
            return Banned(parcel)
        }

        override fun newArray(size: Int): Array<Banned?> {
            return arrayOfNulls(size)
        }
    }
}