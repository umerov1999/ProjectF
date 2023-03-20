package dev.ragnarok.fenrir.upload

import android.os.Parcel
import android.os.Parcelable

open class UploadDestination : Parcelable {
    val id: Int
    val ownerId: Long
    val ref: String?

    @Method
    val method: Int

    @MessageMethod
    var messageMethod: Int

    constructor(id: Int, ownerId: Long, method: Int, messageMethod: Int, ref: String?) {
        this.id = id
        this.ownerId = ownerId
        this.method = method
        this.messageMethod = messageMethod
        this.ref = ref
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
        method = parcel.readInt()
        messageMethod = parcel.readInt()
        ref = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeLong(ownerId)
        dest.writeInt(method)
        dest.writeInt(messageMethod)
        dest.writeString(ref)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as UploadDestination
        return id == that.id && ownerId == that.ownerId && method == that.method
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + ownerId.hashCode()
        result = 31 * result + method
        return result
    }

    fun compareTo(destination: UploadDestination?): Boolean {
        destination ?: return false
        return compareTo(destination.id, destination.ownerId, destination.method)
    }

    fun compareTo(id: Int, ownerId: Long, type: Int): Boolean {
        return this.id == id && this.ownerId == ownerId && method == type
    }

    override fun toString(): String {
        return "UploadDestination{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", method=" + method +
                ", message_method=" + messageMethod +
                ", ref=" + ref.orEmpty() +
                '}'
    }

    companion object {
        const val WITHOUT_OWNER = 0L
        private const val NO_ID = 0
        private const val NO_ID_L = 0L

        @JvmField
        val CREATOR: Parcelable.Creator<UploadDestination> =
            object : Parcelable.Creator<UploadDestination> {
                override fun createFromParcel(parcel: Parcel): UploadDestination {
                    return UploadDestination(parcel)
                }

                override fun newArray(size: Int): Array<UploadDestination?> {
                    return arrayOfNulls(size)
                }
            }


        fun forProfilePhoto(ownerId: Long): UploadDestination {
            return UploadDestination(
                NO_ID,
                ownerId,
                Method.PHOTO_TO_PROFILE,
                MessageMethod.NULL,
                null
            )
        }


        fun forChatPhoto(chat_id: Long): UploadDestination {
            return UploadDestination(
                NO_ID,
                chat_id,
                Method.PHOTO_TO_CHAT,
                MessageMethod.NULL,
                null
            )
        }


        fun forDocuments(ownerId: Long): UploadDestination {
            return UploadDestination(
                NO_ID,
                ownerId,
                Method.DOCUMENT,
                MessageMethod.NULL,
                null
            )
        }


        fun forAudio(ownerId: Long): UploadDestination {
            return UploadDestination(
                NO_ID,
                ownerId,
                Method.AUDIO,
                MessageMethod.NULL,
                null
            )
        }


        fun forRemotePlay(): UploadDestination {
            return UploadDestination(
                NO_ID,
                NO_ID_L,
                Method.REMOTE_PLAY_AUDIO,
                MessageMethod.NULL,
                null
            )
        }


        fun forStory(@MessageMethod msg_method: Int, ref: String?): UploadDestination {
            return UploadDestination(NO_ID, NO_ID_L, Method.STORY, msg_method, ref)
        }


        fun forVideo(is_public: Int, ownerId: Long): UploadDestination {
            return UploadDestination(
                is_public,
                ownerId,
                Method.VIDEO,
                MessageMethod.NULL,
                null
            )
        }


        fun forMessage(mdbid: Int): UploadDestination {
            return UploadDestination(
                mdbid, WITHOUT_OWNER,
                Method.TO_MESSAGE, MessageMethod.PHOTO,
                null
            )
        }


        fun forMessage(mdbid: Int, @MessageMethod msg_method: Int): UploadDestination {
            return UploadDestination(
                mdbid, WITHOUT_OWNER,
                Method.TO_MESSAGE, msg_method,
                null
            )
        }


        fun forPhotoAlbum(albumId: Int, ownerId: Long): UploadDestination {
            return UploadDestination(
                albumId,
                ownerId,
                Method.PHOTO_TO_ALBUM,
                MessageMethod.NULL,
                null
            )
        }


        fun forPost(dbid: Int, ownerId: Long, @MessageMethod msg_method: Int): UploadDestination {
            return UploadDestination(dbid, ownerId, Method.TO_WALL, msg_method, null)
        }


        fun forPost(dbid: Int, ownerId: Long): UploadDestination {
            return UploadDestination(
                dbid,
                ownerId,
                Method.TO_WALL,
                MessageMethod.PHOTO,
                null
            )
        }


        fun forComment(dbid: Int, sourceOwnerId: Long): UploadDestination {
            return UploadDestination(
                dbid,
                sourceOwnerId,
                Method.TO_COMMENT,
                MessageMethod.PHOTO,
                null
            )
        }
    }
}