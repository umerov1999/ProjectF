package dev.ragnarok.fenrir.view.steppers.impl

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.view.steppers.base.AbsStepHolder
import dev.ragnarok.fenrir.view.steppers.base.BaseHolderListener

class CreatePhotoAlbumStep1Holder(parent: ViewGroup, private val mActionListener: ActionListener) :
    AbsStepHolder<CreatePhotoAlbumStepsHost>(
        parent,
        R.layout.step_create_photo_album_1,
        CreatePhotoAlbumStepsHost.STEP_TITLE_AND_DESCRIPTION
    ) {
    private var mTitle: TextView? = null
    private var mDescription: TextView? = null
    override fun initInternalView(contentView: View) {
        mTitle = contentView.findViewById(R.id.title)
        mDescription = contentView.findViewById(R.id.description)
        mTitle?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mActionListener.onTitleEdited(s)
            }
        })
        mTitle?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mActionListener.onDescriptionEdited(s)
            }
        })
    }

    override fun bindViews(host: CreatePhotoAlbumStepsHost) {
        mTitle?.text = host.state.title
        mDescription?.text = host.state.description
    }

    interface ActionListener : BaseHolderListener {
        fun onTitleEdited(text: CharSequence?)
        fun onDescriptionEdited(text: CharSequence?)
    }
}