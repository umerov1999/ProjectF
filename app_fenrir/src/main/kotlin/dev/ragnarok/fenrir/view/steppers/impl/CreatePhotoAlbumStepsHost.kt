package dev.ragnarok.fenrir.view.steppers.impl

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.model.Privacy
import dev.ragnarok.fenrir.view.steppers.base.AbsStepsHost
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStepsHost.PhotoAlbumState

class CreatePhotoAlbumStepsHost : AbsStepsHost<PhotoAlbumState>(PhotoAlbumState()) {
    var isAdditionalOptionsEnable = false
    var isPrivacySettingsEnable = false
        private set
    override val stepsCount: Int
        get() = 4

    override fun readParcelState(saveInstanceState: Bundle, key: String): PhotoAlbumState? {
        return saveInstanceState.getParcelableCompat(key)
    }

    override fun getStepTitle(index: Int): Int {
        return when (index) {
            STEP_TITLE_AND_DESCRIPTION -> R.string.enter_main_album_info
            STEP_UPLOAD_AND_COMMENTS -> R.string.additional_settings
            STEP_PRIVACY_VIEW -> R.string.privacy_view
            STEP_PRIVACY_COMMENT -> R.string.privacy_comment
            else -> throw IllegalStateException("Invalid step index")
        }
    }

    override fun canMoveNext(index: Int, state: PhotoAlbumState): Boolean {
        return when (index) {
            STEP_TITLE_AND_DESCRIPTION -> !state.title.isNullOrEmpty() && (state.title?.trim { it <= ' ' }?.length
                ?: 0) > 1
            STEP_UPLOAD_AND_COMMENTS, STEP_PRIVACY_VIEW, STEP_PRIVACY_COMMENT -> true
            else -> throw IllegalStateException("Invalid step index, index: $index")
        }
    }

    fun setPrivacySettingsEnable(privacySettingsEnable: Boolean): CreatePhotoAlbumStepsHost {
        isPrivacySettingsEnable = privacySettingsEnable
        return this
    }

    override fun getNextButtonText(index: Int): Int {
        return if (index == stepsCount - 1) R.string.finish else R.string.button_continue
    }

    override fun getCancelButtonText(index: Int): Int {
        return if (index == 0) R.string.button_cancel else R.string.button_back
    }

    class PhotoAlbumState : AbsState {
        var title: String? = null
            private set
        var description: String? = null
            private set
        var privacyView: Privacy?
            private set
        var privacyComment: Privacy?
            private set
        var isUploadByAdminsOnly = false
            private set
        var isCommentsDisabled = false
            private set

        constructor(p: Parcel) : super() {
            title = p.readString()
            description = p.readString()
            privacyView = p.readTypedObjectCompat(Privacy.CREATOR)
            privacyComment = p.readTypedObjectCompat(Privacy.CREATOR)
            isUploadByAdminsOnly = p.getBoolean()
            isCommentsDisabled = p.getBoolean()
        }

        constructor() {
            privacyView = Privacy()
            privacyComment = Privacy()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(title)
            dest.writeString(description)
            dest.writeTypedObjectCompat(privacyView, flags)
            dest.writeTypedObjectCompat(privacyComment, flags)
            dest.putBoolean(isUploadByAdminsOnly)
            dest.putBoolean(isCommentsDisabled)
        }

        override fun describeContents(): Int {
            return 0
        }

        fun setTitle(title: String?): PhotoAlbumState {
            this.title = title
            return this
        }

        fun setDescription(description: String?): PhotoAlbumState {
            this.description = description
            return this
        }

        fun setPrivacyView(privacyView: Privacy?): PhotoAlbumState {
            this.privacyView = privacyView
            return this
        }

        fun setPrivacyComment(privacyComment: Privacy?): PhotoAlbumState {
            this.privacyComment = privacyComment
            return this
        }

        fun setUploadByAdminsOnly(uploadByAdminsOnly: Boolean): PhotoAlbumState {
            isUploadByAdminsOnly = uploadByAdminsOnly
            return this
        }

        fun setCommentsDisabled(commentsDisabled: Boolean): PhotoAlbumState {
            isCommentsDisabled = commentsDisabled
            return this
        }

        override fun toString(): String {
            return "PhotoAlbumState{" +
                    "title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    ", privacyView=" + privacyView +
                    ", privacyComment=" + privacyComment +
                    ", uploadByAdminsOnly=" + isUploadByAdminsOnly +
                    ", commentsDisabled=" + isCommentsDisabled +
                    '}'
        }

        companion object CREATOR : Parcelable.Creator<PhotoAlbumState> {
            override fun createFromParcel(parcel: Parcel): PhotoAlbumState {
                return PhotoAlbumState(parcel)
            }

            override fun newArray(size: Int): Array<PhotoAlbumState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        const val STEP_TITLE_AND_DESCRIPTION = 0
        const val STEP_UPLOAD_AND_COMMENTS = 1
        const val STEP_PRIVACY_VIEW = 2
        const val STEP_PRIVACY_COMMENT = 3
    }
}