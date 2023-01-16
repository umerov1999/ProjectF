package dev.ragnarok.fenrir.fragment.attachments.postedit

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
import dev.ragnarok.fenrir.fragment.attachments.abspostedit.AbsPostEditFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.WallEditorAttrs

class PostEditFragment : AbsPostEditFragment<PostEditPresenter, IPostEditView>(), IPostEditView,
    MenuProvider {

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<PostEditPresenter> {
        return object : IPresenterFactory<PostEditPresenter> {
            override fun create(): PostEditPresenter {
                val post: Post = requireArguments().getParcelableCompat(Extra.POST)!!
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val attrs: WallEditorAttrs = requireArguments().getParcelableCompat(Extra.ATTRS)!!
                return PostEditPresenter(accountId, post, attrs, saveInstanceState)
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

    override fun onResult() {
        presenter?.fireReadyClick()
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun closeAsSuccess() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun showConfirmExitDialog() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.confirmation)
            .setMessage(R.string.save_changes_question)
            .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                presenter?.fireExitWithSavingConfirmed()
            }
            .setNegativeButton(R.string.button_no) { _: DialogInterface?, _: Int ->
                presenter?.fireExitWithoutSavingClick()
            }
            .setNeutralButton(R.string.button_cancel, null)
            .show()
    }

    override fun onBackPressed(): Boolean {
        return presenter?.onBackPressed() ?: false
    }

    companion object {
        fun newInstance(args: Bundle?): PostEditFragment {
            val fragment = PostEditFragment()
            fragment.arguments = args
            return fragment
        }

        fun buildArgs(accountId: Long, post: Post, attrs: WallEditorAttrs): Bundle {
            val args = Bundle()
            args.putParcelable(Extra.POST, post)
            args.putParcelable(Extra.ATTRS, attrs)
            args.putLong(Extra.ACCOUNT_ID, accountId)
            return args
        }
    }
}