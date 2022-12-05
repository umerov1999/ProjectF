package dev.ragnarok.fenrir.view.steppers.impl

import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.google.android.material.checkbox.MaterialCheckBox
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.view.steppers.base.AbsStepHolder
import dev.ragnarok.fenrir.view.steppers.base.BaseHolderListener

class CreatePhotoAlbumStep2Holder(parent: ViewGroup, private val mActionListener: ActionListener) :
    AbsStepHolder<CreatePhotoAlbumStepsHost>(
        parent,
        R.layout.content_create_photo_album_step_2,
        CreatePhotoAlbumStepsHost.STEP_UPLOAD_AND_COMMENTS
    ) {
    private var mUploadByAdminsOnly: MaterialCheckBox? = null
    private var mDisableComments: MaterialCheckBox? = null
    override fun initInternalView(contentView: View) {
        mUploadByAdminsOnly = contentView.findViewById(R.id.upload_only_admins)
        mDisableComments = contentView.findViewById(R.id.disable_comments)
        val uploadByAdminsOnlyListener =
            CompoundButton.OnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
                mActionListener.onUploadByAdminsOnlyChecked(b)
            }
        val disableCommentsListener =
            CompoundButton.OnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
                mActionListener.onCommentsDisableChecked(b)
            }
        mUploadByAdminsOnly?.setOnCheckedChangeListener(uploadByAdminsOnlyListener)
        mDisableComments?.setOnCheckedChangeListener(disableCommentsListener)
    }

    override fun bindViews(host: CreatePhotoAlbumStepsHost) {
        mDisableComments?.isChecked = host.state.isCommentsDisabled
        mUploadByAdminsOnly?.isChecked = host.state.isUploadByAdminsOnly
        mDisableComments?.isEnabled = host.isAdditionalOptionsEnable
        mUploadByAdminsOnly?.isEnabled = host.isAdditionalOptionsEnable
    }

    interface ActionListener : BaseHolderListener {
        fun onUploadByAdminsOnlyChecked(checked: Boolean)
        fun onCommentsDisableChecked(checked: Boolean)
    }
}