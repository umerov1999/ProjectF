package dev.ragnarok.fenrir.view.steppers.impl

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.view.steppers.base.AbsStepHolder
import dev.ragnarok.fenrir.view.steppers.base.BaseHolderListener

class CreatePhotoAlbumStep4Holder(parent: ViewGroup, private val mActionListener: ActionListener) :
    AbsStepHolder<CreatePhotoAlbumStepsHost>(
        parent,
        R.layout.content_create_photo_album_step_4,
        CreatePhotoAlbumStepsHost.STEP_PRIVACY_VIEW
    ) {
    private var mRootView: View? = null
    private var mPrivacyCommentAllowed: TextView? = null
    private var mPrivacyComemntDisallowed: TextView? = null
    override fun initInternalView(contentView: View) {
        mPrivacyCommentAllowed = contentView.findViewById(R.id.commenting_allowed)
        mPrivacyComemntDisallowed = contentView.findViewById(R.id.commenting_disabled)
        mRootView = contentView.findViewById(R.id.root)
        mRootView?.setOnClickListener { mActionListener.onPrivacyCommentClick() }
    }

    override fun bindViews(host: CreatePhotoAlbumStepsHost) {
        mRootView?.isEnabled = host.isPrivacySettingsEnable
        val context = mPrivacyCommentAllowed?.context
        val text = context?.let { host.state.privacyComment?.createAllowedString(it) }
        mPrivacyCommentAllowed?.text = text
        mPrivacyComemntDisallowed?.text = host.state.privacyComment?.createDisallowedString()
    }

    interface ActionListener : BaseHolderListener {
        fun onPrivacyCommentClick()
    }
}