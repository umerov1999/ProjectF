package dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.AttachmentsActivity
import dev.ragnarok.fenrir.activity.AudioSelectActivity.Companion.createIntent
import dev.ragnarok.fenrir.activity.PhotoAlbumsActivity
import dev.ragnarok.fenrir.activity.PhotosActivity
import dev.ragnarok.fenrir.db.model.AttachmentsTypes
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.createpoll.CreatePollFragment
import dev.ragnarok.fenrir.fragment.vkphotos.IVkPhotosView
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.place.PlaceFactory.getCreatePollPlace
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.Action
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.AppTextUtils.getDateFromUnixTime
import dev.ragnarok.fenrir.util.Utils.hasScopedStorage
import dev.ragnarok.fenrir.view.DateTimePicker
import dev.ragnarok.fenrir.view.WeakRunnable
import dev.ragnarok.fenrir.view.YoutubeButton
import me.minetsh.imaging.IMGEditActivity
import java.io.File
import java.util.*

abstract class AbsAttachmentsEditFragment<P : AbsAttachmentsEditPresenter<V>, V : IBaseAttachmentsEditView> :
    BaseMvpFragment<P, V>(), IBaseAttachmentsEditView, AttchmentsEditorAdapter.Callback,
    BackPressCallback {

    private val requestCameraPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        lazyPresenter { fireCameraPermissionResolved() }
    }
    private val requestCameraPermissionScoped =
        requestPermissionsAbs(
            arrayOf(Manifest.permission.CAMERA)
        ) {
            lazyPresenter { fireCameraPermissionResolved() }
        }
    private val requestReqadPermission =
        requestPermissionsAbs(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            lazyPresenter {
                fireReadStoragePermissionResolved()
            }
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
    private val openRequestPhotoFromVK =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                val photos: ArrayList<Photo> =
                    (result.data
                        ?: return@registerForActivityResult).getParcelableArrayListExtraCompat("attachments")
                        ?: return@registerForActivityResult
                lazyPresenter {
                    fireVkPhotosSelected(photos)
                }
            }
        }
    private val openRequestPhotoFromGallery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                val photos: ArrayList<LocalPhoto> =
                    (result.data
                        ?: return@registerForActivityResult).getParcelableArrayListExtraCompat(
                        Extra.PHOTOS
                    )
                        ?: return@registerForActivityResult
                lazyPresenter {
                    firePhotosFromGallerySelected(photos)
                }
            }
        }
    private val openRequestResizePhoto =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                lazyPresenter {
                    fireFileSelected(
                        ((result.data
                            ?: return@lazyPresenter).getStringExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH))
                            ?: return@lazyPresenter
                    )
                }
            }
        }
    private var mTextBody: TextInputEditText? = null
    private var mTimerRoot: View? = null
    private var mTimerTextInfo: TextView? = null
    private var mTimerInfoRoot: View? = null
    private var mButtonsBar: View? = null
    private var mButtonPhoto: YoutubeButton? = null
    private var mButtonAudio: YoutubeButton? = null
    private var mButtonVideo: YoutubeButton? = null
    private var mButtonDoc: YoutubeButton? = null
    private var mButtonPoll: YoutubeButton? = null
    private var mButtonTimer: MaterialButton? = null
    private var mBResult: FloatingActionButton? = null
    private var mAdapter: AttchmentsEditorAdapter? = null
    var underBodyContainer: ViewGroup? = null
        private set
    private var mEmptyText: TextView? = null

    abstract fun onResult()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_attachments_manager_new, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val spancount = resources.getInteger(R.integer.attachments_editor_column_count)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        mBResult = root.findViewById(R.id.selected)
        mBResult?.setOnClickListener {
            onResult()
        }
        val manager: RecyclerView.LayoutManager = GridLayoutManager(requireActivity(), spancount)
        recyclerView.layoutManager = manager
        val headerView = inflater.inflate(R.layout.header_attachments_manager, recyclerView, false)
        mAdapter = AttchmentsEditorAdapter(requireActivity(), mutableListOf(), this)
        mAdapter?.addHeader(headerView)
        recyclerView.adapter = mAdapter
        underBodyContainer = headerView.findViewById(R.id.under_body_container)
        mTextBody = headerView.findViewById(R.id.fragment_create_post_text)
        mTextBody?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireTextChanged(s)
            }
        })
        mTimerRoot = headerView.findViewById(R.id.timer_root)
        mTimerInfoRoot = headerView.findViewById(R.id.post_schedule_info_root)
        mTimerTextInfo = headerView.findViewById(R.id.post_schedule_info)
        mButtonsBar = headerView.findViewById(R.id.buttons_bar)
        mButtonPhoto = mButtonsBar?.findViewById(R.id.fragment_create_post_photo)
        mButtonAudio = mButtonsBar?.findViewById(R.id.fragment_create_post_audio)
        mButtonVideo = mButtonsBar?.findViewById(R.id.fragment_create_post_video)
        mButtonDoc = mButtonsBar?.findViewById(R.id.fragment_create_post_doc)
        mButtonPoll = mButtonsBar?.findViewById(R.id.fragment_create_post_poll)
        mButtonTimer = headerView.findViewById(R.id.button_postpone)
        mButtonPhoto?.setOnClickListener {
            presenter?.fireButtonPhotoClick()
        }
        mButtonAudio?.setOnClickListener {
            presenter?.fireButtonAudioClick()
        }
        mButtonVideo?.setOnClickListener {
            presenter?.fireButtonVideoClick()
        }
        mButtonDoc?.setOnClickListener {
            presenter?.fireButtonDocClick()
        }
        mButtonPoll?.setOnClickListener {
            presenter?.fireButtonPollClick()
        }
        headerView.findViewById<View>(R.id.button_disable_postpone).setOnClickListener {
            presenter?.fireButtonTimerClick()
        }
        mButtonTimer?.setOnClickListener {
            presenter?.fireButtonTimerClick()
        }
        mEmptyText = headerView.findViewById(R.id.empty_text)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (mAdapter ?: return).cleanup()
    }

    override fun updateProgressAtIndex(index: Int, progress: Int) {
        mAdapter?.updateEntityProgress(index, progress)
    }

    override fun displayInitialModels(models: MutableList<AttachmentEntry>) {
        mAdapter?.setItems(models)
        resolveEmptyTextVisibility()
    }

    override fun setSupportedButtons(
        photo: Boolean, audio: Boolean, video: Boolean, doc: Boolean,
        poll: Boolean, timer: Boolean
    ) {
        mButtonPhoto?.visibility = if (photo) View.VISIBLE else View.GONE
        mButtonAudio?.visibility = if (audio) View.VISIBLE else View.GONE
        mButtonVideo?.visibility = if (video) View.VISIBLE else View.GONE
        mButtonDoc?.visibility = if (doc) View.VISIBLE else View.GONE
        mButtonPoll?.visibility = if (poll) View.VISIBLE else View.GONE
        mTimerRoot?.visibility = if (timer) View.VISIBLE else View.GONE
        mButtonsBar?.visibility =
            if (photo || video || doc || poll) View.VISIBLE else View.GONE
    }

    override fun setTextBody(text: CharSequence?) {
        mTextBody?.setText(text)
    }

    override fun openAddVkPhotosWindow(maxSelectionCount: Int, accountId: Long, ownerId: Long) {
        val intent = Intent(requireActivity(), PhotoAlbumsActivity::class.java)
        intent.putExtra(Extra.OWNER_ID, accountId)
        intent.putExtra(Extra.ACCOUNT_ID, ownerId)
        intent.putExtra(Extra.ACTION, IVkPhotosView.ACTION_SELECT_PHOTOS)
        openRequestPhotoFromVK.launch(intent)
    }

    private fun startAttachmentsActivity(accountId: Long, type: Int) {
        val intent = Intent(requireActivity(), AttachmentsActivity::class.java)
        intent.putExtra(Extra.TYPE, type)
        intent.putExtra(Extra.ACCOUNT_ID, accountId)
        openRequestAudioVideoDoc.launch(intent)
    }

    override fun openAddAudiosWindow(maxSelectionCount: Int, accountId: Long) {
        val intent = createIntent(requireActivity(), accountId)
        openRequestAudioVideoDoc.launch(intent)
    }

    override fun openAddVideosWindow(maxSelectionCount: Int, accountId: Long) {
        startAttachmentsActivity(accountId, AttachmentsTypes.VIDEO)
    }

    override fun openAddDocumentsWindow(maxSelectionCount: Int, accountId: Long) {
        startAttachmentsActivity(accountId, AttachmentsTypes.DOC)
    }

    override fun openAddPhotoFromGalleryWindow(maxSelectionCount: Int) {
        val attachPhotoIntent = Intent(requireActivity(), PhotosActivity::class.java)
        attachPhotoIntent.putExtra(PhotosActivity.EXTRA_MAX_SELECTION_COUNT, maxSelectionCount)
        openRequestPhotoFromGallery.launch(attachPhotoIntent)
    }

    override fun onRemoveClick(dataposition: Int, entry: AttachmentEntry) {
        presenter?.fireRemoveClick(dataposition, entry)
    }

    override fun requestCameraPermission() {
        if (hasScopedStorage()) requestCameraPermissionScoped.launch() else requestCameraPermission.launch()
    }

    override fun requestReadExternalStoragePermission() {
        requestReqadPermission.launch()
    }

    override fun openCamera(photoCameraUri: Uri) {
        openCameraRequest.launch(photoCameraUri)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
        resolveEmptyTextVisibility()
    }

    override fun notifyItemRangeInsert(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position + (mAdapter?.headersCount ?: 0), count)
        resolveEmptyTextVisibility()
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
            .setItems(
                R.array.array_image_sizes_names
            ) { _: DialogInterface?, index: Int ->
                presenter?.fireUploadPhotoSizeSelected(
                    photos,
                    values[index]
                )
            }
            .show()
    }

    override fun displayCropPhotoDialog(uri: Uri?) {
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

    override fun openPollCreationWindow(accountId: Long, ownerId: Long) {
        getCreatePollPlace(accountId, ownerId)
            .setFragmentListener(CreatePollFragment.REQUEST_CREATE_POLL) { _: String?, result: Bundle ->
                val poll: Poll = result.getParcelableCompat("poll") ?: return@setFragmentListener
                presenter?.firePollCreated(poll)
            }
            .tryOpenWith(requireActivity())
    }

    override fun displayChoosePhotoTypeDialog() {
        val items = arrayOf(
            getString(R.string.from_vk_albums),
            getString(R.string.from_local_albums),
            getString(R.string.from_camera)
        )
        MaterialAlertDialogBuilder(requireActivity()).setItems(items) { _: DialogInterface?, i: Int ->
            when (i) {
                0 -> presenter?.firePhotoFromVkChoose()
                1 -> presenter?.firePhotoFromLocalGalleryChoose()
                2 -> presenter?.firePhotoFromCameraChoose()
            }
        }.show()
    }

    @Suppress("DEPRECATION")
    override fun notifySystemAboutNewPhoto(uri: Uri) {
        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
        requireActivity().sendBroadcast(scanIntent)
    }

    override fun setTimerValue(time: Long?) {
        mButtonTimer?.visibility =
            if (time == null) View.VISIBLE else View.GONE
        mTimerInfoRoot?.visibility =
            if (time == null) View.GONE else View.VISIBLE
        if (mTimerTextInfo != null) {
            if (time != null) {
                val formattedTime = getDateFromUnixTime(requireActivity(), time)
                mTimerTextInfo?.text =
                    getString(
                        R.string.will_be_posted_at,
                        formattedTime.lowercase(Locale.getDefault())
                    )
                mTimerTextInfo?.visibility = View.VISIBLE
            } else {
                mTimerTextInfo?.visibility = View.GONE
            }
        }
    }

    override fun notifyItemRemoved(position: Int) {
        mAdapter?.notifyItemRemoved(position + (mAdapter?.headersCount ?: 0))
        if (mAdapter?.realItemCount == 0) {
            postResolveEmptyTextVisibility()
        }
    }

    override fun notifyItemChanged(position: Int) {
        mAdapter?.notifyItemChanged(position + (mAdapter?.headersCount ?: 0))
        if (mAdapter?.realItemCount == 0) {
            postResolveEmptyTextVisibility()
        }
    }

    private fun postResolveEmptyTextVisibility() {
        if (mEmptyText != null) {
            val action: Action<AbsAttachmentsEditFragment<P, V>> =
                object : Action<AbsAttachmentsEditFragment<P, V>> {
                    override fun call(target: AbsAttachmentsEditFragment<P, V>) {
                        target.resolveEmptyTextVisibility()
                    }
                }
            mEmptyText?.postDelayed(WeakRunnable(this, action), 1000)
        }
    }

    internal fun resolveEmptyTextVisibility() {
        mEmptyText?.visibility =
            if (mAdapter?.realItemCount == 0) View.VISIBLE else View.GONE
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

    override fun showEnterTimeDialog(initialTimeUnixtime: Long) {
        DateTimePicker.Builder(requireActivity())
            .setTime(initialTimeUnixtime)
            .setCallback(object : DateTimePicker.Callback {
                override fun onDateTimeSelected(unixtime: Long) {
                    presenter?.fireTimerTimeSelected(unixtime)
                }
            })
            .show()
    }

    override fun onBackPressed(): Boolean {
        return true
    }
}