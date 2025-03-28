package dev.ragnarok.fenrir.fragment

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.domain.IDocsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.BaseFragment
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostCreation
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.AppTextUtils.getSizeString
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doDownloadDoc
import dev.ragnarok.fenrir.util.Utils.shareLink
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.TouchImageView

class DocPreviewFragment : BaseFragment(), MenuProvider {
    private var accountId = 0L
    private var rootView: View? = null
    private var ownerId = 0L
    private var documentId = 0
    private var documentAccessKey: String? = null
    private var document: Document? = null
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        doDownloadDoc()
    }
    private var preview: TouchImageView? = null
    private var ivDocIcon: ImageView? = null
    private var tvTitle: TextView? = null
    private var tvSubtitle: TextView? = null
    private var mLoadingNow = false
    private var deleted = false
    private val docsInteractor: IDocsInteractor = InteractorFactory.createDocsInteractor()
    private fun doDownloadDoc() {
        document?.let { dc ->
            if (doDownloadDoc(requireActivity(), dc, false) == 1) {
                CustomSnackbars.createCustomSnackbars(view)
                    ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                    ?.themedSnack(R.string.audio_force_download)
                    ?.setAction(
                        R.string.button_yes
                    ) { doDownloadDoc(requireActivity(), dc, true) }
                    ?.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        savedInstanceState?.let { restoreFromInstanceState(it) }
        ownerId = requireArguments().getLong(Extra.OWNER_ID)
        documentId = requireArguments().getInt(Extra.DOC_ID)
        if (requireArguments().containsKey(Extra.DOC)) {
            document = requireArguments().getParcelableCompat(Extra.DOC)
        }
        if (requireArguments().containsKey(Extra.ACCESS_KEY)) {
            documentAccessKey = requireArguments().getString(Extra.ACCESS_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_document_preview, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(rootView?.findViewById(R.id.toolbar))
        preview = rootView?.findViewById(R.id.fragment_document_preview)
        ivDocIcon = rootView?.findViewById(R.id.no_preview_icon)
        tvTitle = rootView?.findViewById(R.id.fragment_document_title)
        tvSubtitle = rootView?.findViewById(R.id.fragment_document_subtitle)
        val deleteOrAddButton: CircleCounterButton? =
            rootView?.findViewById(R.id.add_or_delete_button)
        deleteOrAddButton?.setOnClickListener {
            if (isMy) {
                remove()
            } else {
                addYourSelf()
            }
        }
        rootView?.findViewById<CircleCounterButton>(R.id.download_button)?.setOnClickListener {
            download()
        }
        rootView?.findViewById<CircleCounterButton>(R.id.share_button)?.setOnClickListener {
            share()
        }
        deleteOrAddButton?.setIcon(if (isMy) R.drawable.ic_outline_delete else R.drawable.plus)
        return rootView
    }

    private val isMy: Boolean
        get() = accountId == ownerId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        if (document == null && !mLoadingNow) {
            requestVideoInfo()
        }
        resolveAllViews()
    }

    private fun resolveAllViews() {
        if (!isAdded) return
        val pRoot = rootView ?: return
        if (document == null) {
            pRoot.findViewById<View>(R.id.content_root).visibility = View.GONE
            pRoot.findViewById<View>(R.id.loading_root).visibility = View.VISIBLE
            pRoot.findViewById<View>(R.id.progressBar).visibility =
                if (mLoadingNow) View.VISIBLE else View.GONE
            pRoot.findViewById<View>(R.id.post_loading_text).visibility =
                if (mLoadingNow) View.VISIBLE else View.GONE
            pRoot.findViewById<View>(R.id.try_again_button).visibility =
                if (mLoadingNow) View.GONE else View.VISIBLE
            return
        }
        pRoot.findViewById<View>(R.id.content_root).visibility =
            View.VISIBLE
        pRoot.findViewById<View>(R.id.loading_root).visibility =
            View.GONE
        if (document?.graffiti != null) {
            ivDocIcon?.visibility = View.GONE
            preview?.visibility = View.VISIBLE
            val graffitiUrl = document?.graffiti?.src
            preview?.let { p ->
                if (graffitiUrl.nonNullNoEmpty()) {
                    with()
                        .load(graffitiUrl)
                        .into(p)
                }
            }
        } else if (document?.type == 4 && document?.url.nonNullNoEmpty()) {
            ivDocIcon?.visibility = View.GONE
            preview?.visibility = View.VISIBLE
            val previewUrl = document?.url
            if (previewUrl.nonNullNoEmpty()) {
                preview?.let { p ->
                    with()
                        .load(previewUrl)
                        .into(p)
                }
            }
        } else if (document?.photoPreview != null) {
            ivDocIcon?.visibility = View.GONE
            preview?.visibility = View.VISIBLE
            val previewUrl = document?.photoPreview?.getUrlForSize(PhotoSize.X, true)
            preview?.let {
                if (previewUrl.nonNullNoEmpty()) {
                    with()
                        .load(previewUrl)
                        .into(it)
                }
            }
        } else {
            preview?.visibility = View.GONE
            ivDocIcon?.visibility = View.VISIBLE
        }
        tvTitle?.text = document?.title
        tvSubtitle?.text = document?.size?.let { getSizeString(it) }
        resolveButtons()
    }

    private fun resolveButtons() {
        if (!isAdded) {
            return
        }
        rootView?.findViewById<View>(R.id.add_or_delete_button)?.visibility =
            if (deleted) View.INVISIBLE else View.VISIBLE
        rootView?.findViewById<View>(R.id.share_button)?.visibility =
            if (deleted) View.INVISIBLE else View.VISIBLE
    }

    private fun requestVideoInfo() {
        mLoadingNow = true
        appendJob(
            docsInteractor.findById(accountId, ownerId, documentId, documentAccessKey)
                .fromIOToMain({ document -> onDocumentInfoReceived(document) }) {
                    onDocumentInfoGetError()
                })
    }

    private fun onDocumentInfoGetError() {
        mLoadingNow = false
    }

    private fun onDocumentInfoReceived(document: Document) {
        mLoadingNow = false
        this.document = document
        requireArguments().putParcelable(Extra.DOC, document)
        resolveAllViews()
        resolveActionBar()
    }

    override fun onResume() {
        super.onResume()
        resolveActionBar()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    private fun resolveActionBar() {
        if (!isAdded) return
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.attachment_document)
            actionBar.subtitle = if (document == null) null else document?.title
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVE_DELETED, deleted)
    }

    private fun restoreFromInstanceState(state: Bundle) {
        deleted = state.getBoolean(SAVE_DELETED)
    }

    private fun doRemove() {
        appendJob(
            docsInteractor.delete(accountId, documentId, ownerId)
                .fromIOToMain({ onDeleteSuccess() }) { })
    }

    private fun onDeleteSuccess() {
        CustomSnackbars.createCustomSnackbars(rootView)
            ?.setDurationSnack(Snackbar.LENGTH_LONG)
            ?.coloredSnack(R.string.deleted, "#AA48BE2D".toColor())?.show()
        deleted = true
        resolveButtons()
    }

    private fun remove() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.remove_confirm)
            .setMessage(R.string.doc_remove_confirm_message)
            .setPositiveButton(R.string.button_yes) { _, _ -> doRemove() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun share() {
        val items = arrayOf(
            getString(R.string.share_link),
            getString(R.string.repost_send_message),
            getString(R.string.repost_to_wall)
        )
        MaterialAlertDialogBuilder(requireActivity())
            .setItems(items) { _, i ->
                when (i) {
                    0 -> shareLink(requireActivity(), genLink(), document?.title)
                    1 -> document?.let { startForSendAttachments(requireActivity(), accountId, it) }
                    2 -> postToMyWall()
                }
            }
            .setCancelable(true)
            .setTitle(R.string.share_document_title)
            .show()
    }

    private fun postToMyWall() {
        document?.let {
            val models: List<AbsModel> = listOf(it)
            goToPostCreation(requireActivity(), accountId, accountId, EditingPostType.TEMP, models)
        }
    }

    private fun genLink(): String {
        return String.format("vk.com/doc%s_%s", ownerId, documentId)
    }

    private fun download() {
        if (!hasReadWriteStoragePermission(requireActivity())) {
            requestWritePermission.launch()
            return
        }
        doDownloadDoc()
    }

    private fun openOwnerWall() {
        getOwnerWallPlace(accountId, ownerId, null).tryOpenWith(requireActivity())
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.add(R.string.goto_user).setOnMenuItemClickListener {
            openOwnerWall()
            true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return true
    }

    private fun doAddYourSelf() {
        val docsInteractor = InteractorFactory.createDocsInteractor()
        val accessKey = document?.accessKey
        appendJob(
            docsInteractor.add(accountId, documentId, ownerId, accessKey)
                .fromIOToMain({ onDocumentAdded() }) { t -> onDocAddError(t) })
    }

    private fun onDocAddError(t: Throwable) {
        t.printStackTrace()
    }

    private fun onDocumentAdded() {
        CustomSnackbars.createCustomSnackbars(rootView)
            ?.setDurationSnack(Snackbar.LENGTH_LONG)
            ?.coloredSnack(R.string.added, "#AA48BE2D".toColor())?.show()
        deleted = false
        resolveButtons()
    }

    private fun addYourSelf() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.confirmation)
            .setMessage(R.string.add_document_to_yourself_commit)
            .setPositiveButton(R.string.button_yes) { _, _ -> doAddYourSelf() }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    companion object {
        private const val SAVE_DELETED = "deleted"
        fun buildArgs(
            accountId: Long,
            docId: Int,
            docOwnerId: Long,
            accessKey: String?,
            document: Document?
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.DOC_ID, docId)
            args.putLong(Extra.OWNER_ID, docOwnerId)
            if (!accessKey.isNullOrEmpty()) {
                args.putString(Extra.ACCESS_KEY, accessKey)
            }
            if (document != null) {
                args.putParcelable(Extra.DOC, document)
            }
            return args
        }

        fun newInstance(arsg: Bundle?): DocPreviewFragment {
            val fragment = DocPreviewFragment()
            fragment.arguments = arsg
            return fragment
        }
    }
}