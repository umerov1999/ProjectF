package dev.ragnarok.fenrir.view.steppers.impl

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.view.steppers.base.AbsStepHolder
import dev.ragnarok.fenrir.view.steppers.base.BaseHolderListener

class CreatePhotoAlbumStep3Holder(parent: ViewGroup, private val mActionListener: ActionListener) :
    AbsStepHolder<CreatePhotoAlbumStepsHost>(
        parent,
        R.layout.content_create_photo_album_step_3,
        CreatePhotoAlbumStepsHost.STEP_PRIVACY_VIEW
    ) {
    private var mRootView: View? = null
    private var mPrivacyViewAllowed: TextView? = null
    private var mPrivacyViewDisabled: TextView? = null
    override fun initInternalView(contentView: View) {
        mPrivacyViewAllowed = contentView.findViewById(R.id.view_allowed)
        mPrivacyViewDisabled = contentView.findViewById(R.id.view_disabled)
        mRootView = contentView.findViewById(R.id.root)
        mRootView?.setOnClickListener { mActionListener.onPrivacyViewClick() }
    }

    override fun bindViews(host: CreatePhotoAlbumStepsHost) {
        mRootView?.isEnabled = host.isPrivacySettingsEnable
        // TODO: 16-May-16 Сделать неактивным, если альбом в группе
        val context = mPrivacyViewAllowed?.context
        val text = context?.let { host.state.privacyView?.createAllowedString(it) }
        mPrivacyViewAllowed?.text = text
        mPrivacyViewDisabled?.text = host.state.privacyView?.createDisallowedString()
    }

    interface ActionListener : BaseHolderListener {
        fun onPrivacyViewClick()
    }
}