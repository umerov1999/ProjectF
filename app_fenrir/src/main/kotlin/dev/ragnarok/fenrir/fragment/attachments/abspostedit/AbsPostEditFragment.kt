package dev.ragnarok.fenrir.fragment.attachments.abspostedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.checkbox.MaterialCheckBox
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.AbsAttachmentsEditFragment
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation

abstract class AbsPostEditFragment<P : AbsPostEditPresenter<V>, V : IBasePostEditView> :
    AbsAttachmentsEditFragment<P, V>(), IBasePostEditView {
    private var mFromGroupCheckBox: MaterialCheckBox? = null
    private var mFrindsOnlyCheckBox: MaterialCheckBox? = null
    private var mSignerRoot: View? = null
    private var mSignerAvatar: ImageView? = null
    private var mSignerName: TextView? = null
    private var mShowAuthorCheckbox: MaterialCheckBox? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        val signatureRoot =
            inflater.inflate(R.layout.content_post_edit_under_body, underBodyContainer, false)
        mFromGroupCheckBox = signatureRoot.findViewById(R.id.check_from_group)
        mFromGroupCheckBox?.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            presenter?.fireFromGroupChecked(checked)
        }
        mFrindsOnlyCheckBox = signatureRoot.findViewById(R.id.check_friends_only)
        mFrindsOnlyCheckBox?.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            presenter?.fireFriendsOnlyChecked(checked)
        }
        mSignerRoot = signatureRoot.findViewById(R.id.signer_root)
        mSignerAvatar = signatureRoot.findViewById(R.id.signer_avatar)
        mSignerName = signatureRoot.findViewById(R.id.signer_name)
        mShowAuthorCheckbox = signatureRoot.findViewById(R.id.check_show_author)
        mShowAuthorCheckbox?.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            presenter?.fireShowAuthorChecked(checked)
        }
        underBodyContainer?.addView(signatureRoot)
        return root
    }

    override fun setFromGroupChecked(checked: Boolean) {
        mFromGroupCheckBox?.isChecked = checked
    }

    override fun setFriendsOnlyChecked(checked: Boolean) {
        mFrindsOnlyCheckBox?.isChecked = checked
    }

    override fun setFriendsOnlyOptionVisible(visible: Boolean) {
        mFrindsOnlyCheckBox?.visibility =
            if (visible) View.VISIBLE else View.GONE
    }

    override fun setFromGroupOptionVisible(visible: Boolean) {
        mFromGroupCheckBox?.visibility =
            if (visible) View.VISIBLE else View.GONE
    }

    override fun displaySignerInfo(fullName: String?, photo: String?) {
        if (mSignerAvatar != null) {
            with()
                .load(photo)
                .transform(RoundTransformation())
                .into(mSignerAvatar ?: return)
        }
        mSignerName?.text = fullName
    }

    override fun setAddSignatureOptionVisible(visible: Boolean) {
        mShowAuthorCheckbox?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setShowAuthorChecked(checked: Boolean) {
        mShowAuthorCheckbox?.isChecked = checked
    }

    override fun setSignerInfoVisible(visible: Boolean) {
        mSignerRoot?.visibility = if (visible) View.VISIBLE else View.GONE
    }
}