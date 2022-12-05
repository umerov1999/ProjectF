package dev.ragnarok.fenrir.fragment.attachments.postcreate

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.dialog.ImageSizeAlertDialog
import dev.ragnarok.fenrir.fragment.attachments.abspostedit.AbsPostEditFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableArrayListCompat
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.ModelsBundle
import dev.ragnarok.fenrir.model.WallEditorAttrs

class PostCreateFragment : AbsPostEditFragment<PostCreatePresenter, IPostCreateView>(),
    IPostCreateView, MenuProvider {

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<PostCreatePresenter> {
        return object : IPresenterFactory<PostCreatePresenter> {
            override fun create(): PostCreatePresenter {
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val ownerId = requireArguments().getInt(Extra.OWNER_ID)
                @EditingPostType val type = requireArguments().getInt(EXTRA_EDITING_TYPE)
                val bundle: ModelsBundle? = requireArguments().getParcelableCompat(Extra.BUNDLE)
                val attrs: WallEditorAttrs = requireArguments().getParcelableCompat(Extra.ATTRS)!!
                val links = requireArguments().getString(Extra.BODY)
                val mime = requireArguments().getString(Extra.TYPE)
                val streams = requireArguments().getParcelableArrayListCompat<Uri>(EXTRA_STREAMS)
                requireArguments().remove(EXTRA_STREAMS) // only first start
                requireArguments().remove(Extra.BODY)
                return PostCreatePresenter(
                    accountId,
                    ownerId,
                    type,
                    bundle,
                    attrs,
                    streams,
                    links,
                    mime,
                    saveInstanceState
                )
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
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(true)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayUploadUriSizeDialog(uris: List<Uri>) {
        ImageSizeAlertDialog.Builder(requireActivity())
            .setOnSelectedCallback(object : ImageSizeAlertDialog.OnSelectedCallback {
                override fun onSizeSelected(size: Int) {
                    presenter?.fireUriUploadSizeSelected(
                        uris,
                        size
                    )
                }
            })
            .setOnCancelCallback(object : ImageSizeAlertDialog.OnCancelCallback {
                override fun onCancel() {
                    presenter?.fireUriUploadCancelClick()
                }
            })
            .show()
    }

    override fun goBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onResult() {
        presenter?.fireReadyClick()
    }

    override fun onBackPressed(): Boolean {
        return presenter?.onBackPresed() ?: false
    }

    companion object {
        private const val EXTRA_EDITING_TYPE = "editing_type"
        private const val EXTRA_STREAMS = "streams"
        fun newInstance(args: Bundle?): PostCreateFragment {
            val fragment = PostCreateFragment()
            fragment.arguments = args
            return fragment
        }

        fun buildArgs(
            accountId: Int, ownerId: Int, @EditingPostType editingType: Int,
            bundle: ModelsBundle?, attrs: WallEditorAttrs,
            streams: ArrayList<Uri>?, body: String?, mime: String?
        ): Bundle {
            val args = Bundle()
            args.putInt(EXTRA_EDITING_TYPE, editingType)
            args.putParcelableArrayList(EXTRA_STREAMS, streams)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.OWNER_ID, ownerId)
            args.putString(Extra.BODY, body)
            args.putParcelable(Extra.BUNDLE, bundle)
            args.putParcelable(Extra.ATTRS, attrs)
            args.putString(Extra.TYPE, mime)
            return args
        }
    }
}