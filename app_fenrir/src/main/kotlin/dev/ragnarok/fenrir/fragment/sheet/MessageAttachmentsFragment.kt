package dev.ragnarok.fenrir.fragment.sheet

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.activity.AttachmentsActivity
import dev.ragnarok.fenrir.activity.AudioSelectActivity.Companion.createIntent
import dev.ragnarok.fenrir.activity.DualTabPhotoActivity.Companion.createIntent
import dev.ragnarok.fenrir.activity.VideoSelectActivity
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.db.model.AttachmentsTypes
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.selection.*
import dev.ragnarok.fenrir.service.ErrorLocalizer.localizeThrowable
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils.hasScopedStorage
import dev.ragnarok.fenrir.util.Utils.isLandscape
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import me.minetsh.imaging.IMGEditActivity
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MessageAttachmentsFragment :
    AbsPresenterBottomSheetFragment<MessageAttachmentsPresenter, IMessageAttachmentsView>(),
    IMessageAttachmentsView, AttachmentsBottomSheetAdapter.ActionListener {
    private val requestCameraPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        presenter?.fireCameraPermissionResolved()
    }
    private val requestCameraPermissionScoped =
        requestPermissionsAbs(
            arrayOf(Manifest.permission.CAMERA)
        ) {
            presenter?.fireCameraPermissionResolved()
        }
    private val openCameraRequest =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { result: Boolean ->
            if (result) {
                lazyPresenter {
                    firePhotoMaked()
                }
            }
        }
    private val openRequestAudioVideoDoc =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                val attachments: ArrayList<AbsModel> =
                    (result.data
                        ?: return@registerForActivityResult).getParcelableArrayListExtraCompat(
                        Extra.ATTACHMENTS
                    )
                        ?: return@registerForActivityResult
                lazyPresenter {
                    fireAttachmentsSelected(attachments)
                }
            }
        }
    private val openRequestPhoto =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                val vkphotos: ArrayList<Photo>? =
                    result.data?.getParcelableArrayListExtraCompat(Extra.ATTACHMENTS)
                val localPhotos: ArrayList<LocalPhoto>? =
                    result.data?.getParcelableArrayListExtraCompat(Extra.PHOTOS)
                val file = result.data?.getStringExtra(Extra.PATH)
                val video: LocalVideo? = result.data?.getParcelableExtraCompat(Extra.VIDEO)
                lazyPresenter {
                    firePhotosSelected(vkphotos, localPhotos, file, video)
                }
            }
        }
    private val openRequestResizePhoto =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                assert(result.data != null)
                lazyPresenter {
                    doUploadFile(
                        (result.data
                            ?: return@lazyPresenter).getStringExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH)
                            ?: return@lazyPresenter,
                        Upload.IMAGE_SIZE_FULL,
                        1
                    )
                }
            }
        }
    private var mAdapter: AttachmentsBottomSheetAdapter? = null
    private var mRecyclerView: RecyclerView? = null
    private var mEmptyView: View? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return if (isLandscape(requireActivity())) {
            val dialog = BottomSheetDialog(requireActivity(), theme)
            val behavior: BottomSheetBehavior<*> = dialog.behavior
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            dialog
        } else {
            super.onCreateDialog(savedInstanceState)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val view = View.inflate(requireActivity(), R.layout.bottom_sheet_attachments, null)
        mRecyclerView = view.findViewById(R.id.recycler_view)
        mRecyclerView?.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        mEmptyView = view.findViewById(R.id.no_attachments_text)
        view.findViewById<View>(R.id.button_send).setOnClickListener {
            parentFragmentManager.setFragmentResult(MESSAGE_CLOSE_ONLY, Bundle())
            (getDialog() ?: return@setOnClickListener).dismiss()
        }
        view.findViewById<View>(R.id.button_hide)
            .setOnClickListener { (getDialog() ?: return@setOnClickListener).dismiss() }
        view.findViewById<View>(R.id.button_video).setOnClickListener {
            presenter?.fireButtonVideoClick()
        }
        view.findViewById<View>(R.id.button_audio).setOnClickListener {
            presenter?.fireButtonAudioClick()
        }
        view.findViewById<View>(R.id.button_doc).setOnClickListener {
            presenter?.fireButtonDocClick()
        }
        view.findViewById<View>(R.id.button_camera).setOnClickListener {
            presenter?.fireButtonCameraClick()
        }
        view.findViewById<View>(R.id.button_photo_settings).setOnClickListener {
            presenter?.fireCompressSettings(
                requireActivity()
            )
        }
        view.findViewById<View>(R.id.button_photo_settings).visibility =
            if (Settings.get().other().isChange_upload_size) View.VISIBLE else View.GONE
        dialog.setContentView(view)
        fireViewCreated()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<MessageAttachmentsPresenter> {
        return object : IPresenterFactory<MessageAttachmentsPresenter> {
            override fun create(): MessageAttachmentsPresenter {
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val messageId = requireArguments().getInt(Extra.MESSAGE_ID)
                val messageOwnerId = requireArguments().getInt(Extra.OWNER_ID)
                val bundle: ModelsBundle = requireArguments().getParcelableCompat(Extra.BUNDLE)!!
                return MessageAttachmentsPresenter(
                    accountId,
                    messageOwnerId,
                    messageId,
                    requireActivity(),
                    bundle,
                    saveInstanceState
                )
            }
        }
    }

    override fun displayAttachments(entries: List<AttachmentEntry>) {
        if (mRecyclerView != null) {
            mAdapter = AttachmentsBottomSheetAdapter(entries, this)
            (mRecyclerView ?: return).adapter = mAdapter
        }
    }

    override fun notifyDataAdded(positionStart: Int, count: Int) {
        if (mAdapter != null) {
            (mAdapter ?: return).notifyItemRangeInserted(positionStart + 1, count)
        }
    }

    override fun addPhoto(accountId: Int, ownerId: Int) {
        val sources = Sources()
            .with(LocalPhotosSelectableSource())
            .with(LocalGallerySelectableSource())
            .with(LocalVideosSelectableSource())
            .with(VkPhotosSelectableSource(accountId, ownerId))
            .with(FileManagerSelectableSource())
        val intent = createIntent(requireActivity(), 10, sources)
        openRequestPhoto.launch(intent)
    }

    override fun notifyEntryRemoved(index: Int) {
        if (mAdapter != null) {
            (mAdapter ?: return).notifyItemRemoved(index + 1)
        }
    }

    override fun displaySelectUploadPhotoSizeDialog(photos: List<LocalPhoto>) {
        val values = intArrayOf(
            Upload.IMAGE_SIZE_800,
            Upload.IMAGE_SIZE_1200,
            Upload.IMAGE_SIZE_FULL,
            Upload.IMAGE_SIZE_CROPPING
        )
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.select_image_size_title)
            .setItems(R.array.array_image_sizes_names) { _: DialogInterface?, j: Int ->
                presenter?.fireUploadPhotoSizeSelected(
                    photos,
                    values[j]
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun displayCropPhotoDialog(uri: Uri) {
        try {
            openRequestResizePhoto.launch(
                Intent(requireContext(), IMGEditActivity::class.java)
                    .putExtra(IMGEditActivity.EXTRA_IMAGE_URI, uri)
                    .putExtra(
                        IMGEditActivity.EXTRA_IMAGE_SAVE_PATH,
                        File(requireActivity().externalCacheDir.toString() + File.separator + "scale.jpg").absolutePath
                    )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun displaySelectUploadFileSizeDialog(file: String) {
        val values = intArrayOf(
            Upload.IMAGE_SIZE_800,
            Upload.IMAGE_SIZE_1200,
            Upload.IMAGE_SIZE_FULL,
            Upload.IMAGE_SIZE_CROPPING
        )
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.select_image_size_title)
            .setItems(R.array.array_image_sizes_names) { _: DialogInterface?, j: Int ->
                presenter?.fireUploadFileSizeSelected(
                    file,
                    values[j]
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    override fun changePercentageSmoothly(id: Int, progress: Int) {
        if (mAdapter != null) {
            (mAdapter ?: return).changeUploadProgress(id, progress, true)
        }
    }

    override fun notifyItemChanged(index: Int) {
        if (mAdapter != null) {
            (mAdapter ?: return).notifyItemChanged(index + 1)
        }
    }

    override fun setEmptyViewVisible(visible: Boolean) {
        if (mEmptyView != null) {
            (mEmptyView ?: return).visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    override fun requestCameraPermission() {
        if (hasScopedStorage()) requestCameraPermissionScoped.launch() else requestCameraPermission.launch()
    }

    override fun startCamera(fileUri: Uri) {
        openCameraRequest.launch(fileUri)
    }

    override fun syncAccompanyingWithParent(accompanying: ModelsBundle) {
        val data = Bundle()
        data.putParcelable(Extra.BUNDLE, accompanying)
        parentFragmentManager.setFragmentResult(MESSAGE_SYNC_ATTACHMENTS, data)
    }

    override fun startAddDocumentActivity(accountId: Int) {
        val intent =
            AttachmentsActivity.createIntent(requireActivity(), accountId, AttachmentsTypes.DOC)
        openRequestAudioVideoDoc.launch(intent)
    }

    override fun startAddVideoActivity(accountId: Int, ownerId: Int) {
        val intent = VideoSelectActivity.createIntent(requireActivity(), accountId, ownerId)
        openRequestAudioVideoDoc.launch(intent)
    }

    override fun startAddAudioActivity(accountId: Int) {
        val intent = createIntent(requireActivity(), accountId)
        openRequestAudioVideoDoc.launch(intent)
    }

    override fun onAddPhotoButtonClick() {
        presenter?.fireAddPhotoButtonClick()
    }

    override fun onButtonRemoveClick(entry: AttachmentEntry) {
        presenter?.fireRemoveClick(
            entry
        )
    }

    override fun onButtonRetryClick(entry: AttachmentEntry) {
        presenter?.fireRetryClick(
            entry
        )
    }

    override fun showError(errorText: String?) {
        if (isAdded) {
            customToast.showToastError(errorText)
        }
    }

    override fun showThrowable(throwable: Throwable?) {
        if (isAdded) {
            CustomSnackbars.createCustomSnackbars(view)?.let {
                val snack = it.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG).coloredSnack(
                    localizeThrowable(provideApplicationContext(), throwable),
                    Color.parseColor("#eeff0000")
                )
                if (throwable !is ApiException && throwable !is SocketTimeoutException && throwable !is UnknownHostException) {
                    snack.setAction(R.string.more_info) {
                        val text = StringBuilder()
                        text.append(
                            localizeThrowable(
                                provideApplicationContext(),
                                throwable
                            )
                        )
                        text.append("\r\n")
                        for (stackTraceElement in (throwable ?: return@setAction).stackTrace) {
                            text.append("    ")
                            text.append(stackTraceElement)
                            text.append("\r\n")
                        }
                        MaterialAlertDialogBuilder(requireActivity())
                            .setIcon(R.drawable.ic_error)
                            .setMessage(text)
                            .setTitle(R.string.more_info)
                            .setPositiveButton(R.string.button_ok, null)
                            .setCancelable(true)
                            .show()
                    }
                }
                snack.show()
            } ?: showError(localizeThrowable(provideApplicationContext(), throwable))
        }
    }

    override fun showError(titleTes: Int, vararg params: Any?) {
        if (isAdded) {
            showError(getString(titleTes, *params))
        }
    }

    override val customToast: CustomToast
        get() = if (isAdded) {
            createCustomToast(requireActivity())
        } else createCustomToast(null)

    companion object {
        const val MESSAGE_CLOSE_ONLY = "message_attachments_close_only"
        const val MESSAGE_SYNC_ATTACHMENTS = "message_attachments_sync"
        fun newInstance(
            accountId: Int,
            messageOwnerId: Int,
            messageId: Int,
            bundle: ModelsBundle?
        ): MessageAttachmentsFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.MESSAGE_ID, messageId)
            args.putInt(Extra.OWNER_ID, messageOwnerId)
            args.putParcelable(Extra.BUNDLE, bundle)
            val fragment = MessageAttachmentsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}