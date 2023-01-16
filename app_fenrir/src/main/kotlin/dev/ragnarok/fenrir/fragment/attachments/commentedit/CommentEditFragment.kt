package dev.ragnarok.fenrir.fragment.attachments.commentedit

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit.AbsAttachmentsEditFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Comment

class CommentEditFragment : AbsAttachmentsEditFragment<CommentEditPresenter, ICommentEditView>(),
    ICommentEditView, MenuProvider {
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommentEditPresenter> {
        return object : IPresenterFactory<CommentEditPresenter> {
            override fun create(): CommentEditPresenter {
                val aid = requireArguments().getLong(Extra.ACCOUNT_ID)
                val CommentThread: Int? =
                    if (requireArguments().containsKey(Extra.COMMENT_ID)) requireArguments().getInt(
                        Extra.COMMENT_ID
                    ) else null
                val comment: Comment = requireArguments().getParcelableCompat(Extra.COMMENT)!!
                return CommentEditPresenter(comment, aid, CommentThread, saveInstanceState)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_attchments, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.ready) {
            presenter?.fireReadyClick()
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(this, R.string.comment_editing_title)
        setToolbarSubtitle(this, null)
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onBackPressed(): Boolean {
        return presenter?.onBackPressed() ?: false
    }

    override fun goBackWithResult(comment: Comment) {
        val data = Bundle()
        data.putParcelable(Extra.COMMENT, comment)
        parentFragmentManager.setFragmentResult(REQUEST_COMMENT_EDIT, data)
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun showConfirmWithoutSavingDialog() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.confirmation)
            .setMessage(R.string.save_changes_question)
            .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                presenter?.fireReadyClick()
            }
            .setNegativeButton(R.string.button_no) { _: DialogInterface?, _: Int ->
                presenter?.fireSavingCancelClick()
            }
            .setNeutralButton(R.string.button_cancel, null)
            .show()
    }

    override fun goBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onResult() {
        presenter?.fireReadyClick()
    }

    companion object {
        const val REQUEST_COMMENT_EDIT = "request_comment_edit"
        fun newInstance(
            accountId: Long,
            comment: Comment?,
            CommentThread: Int?
        ): CommentEditFragment {
            val args = Bundle()
            args.putParcelable(Extra.COMMENT, comment)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            if (CommentThread != null) {
                args.putInt(Extra.COMMENT_ID, CommentThread)
            }
            val fragment = CommentEditFragment()
            fragment.arguments = args
            return fragment
        }
    }
}