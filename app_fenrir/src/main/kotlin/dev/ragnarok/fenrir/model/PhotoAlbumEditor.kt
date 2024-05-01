package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class PhotoAlbumEditor : Parcelable {
    var title: String? = null
        private set
    var description: String? = null
        private set
    var privacyView: Privacy? = null
        private set
    var privacyComment: Privacy? = null
        private set
    var commentsDisabled = false
        private set
    var uploadByAdminsOnly = false
        private set

    private constructor()
    internal constructor(parcel: Parcel) {
        title = parcel.readString()
        description = parcel.readString()
        privacyView = parcel.readTypedObjectCompat(Privacy.CREATOR)
        privacyComment = parcel.readTypedObjectCompat(Privacy.CREATOR)
        commentsDisabled = parcel.getBoolean()
        uploadByAdminsOnly = parcel.getBoolean()
    }

    fun setTitle(title: String?): PhotoAlbumEditor {
        this.title = title
        return this
    }

    fun setDescription(description: String?): PhotoAlbumEditor {
        this.description = description
        return this
    }

    fun setPrivacyView(privacyView: Privacy?): PhotoAlbumEditor {
        this.privacyView = privacyView
        return this
    }

    fun setPrivacyComment(privacyComment: Privacy?): PhotoAlbumEditor {
        this.privacyComment = privacyComment
        return this
    }

    fun setCommentsDisabled(commentsDisabled: Boolean): PhotoAlbumEditor {
        this.commentsDisabled = commentsDisabled
        return this
    }

    fun setUploadByAdminsOnly(uploadByAdminsOnly: Boolean): PhotoAlbumEditor {
        this.uploadByAdminsOnly = uploadByAdminsOnly
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeTypedObjectCompat(privacyView, i)
        parcel.writeTypedObjectCompat(privacyComment, i)
        parcel.putBoolean(commentsDisabled)
        parcel.putBoolean(uploadByAdminsOnly)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PhotoAlbumEditor> =
            object : Parcelable.Creator<PhotoAlbumEditor> {
                override fun createFromParcel(parcel: Parcel): PhotoAlbumEditor {
                    return PhotoAlbumEditor(parcel)
                }

                override fun newArray(size: Int): Array<PhotoAlbumEditor?> {
                    return arrayOfNulls(size)
                }
            }

        fun create(): PhotoAlbumEditor {
            val editor = PhotoAlbumEditor()
            editor.setPrivacyComment(Privacy())
            editor.setPrivacyView(Privacy())
            return editor
        }
    }
}