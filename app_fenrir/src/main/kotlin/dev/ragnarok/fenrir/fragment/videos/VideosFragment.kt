package dev.ragnarok.fenrir.fragment.videos

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.DualTabPhotoActivity.Companion.createIntent
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.docs.DocsUploadAdapter
import dev.ragnarok.fenrir.getParcelableExtraCompat
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.selection.FileManagerSelectableSource
import dev.ragnarok.fenrir.model.selection.LocalVideosSelectableSource
import dev.ragnarok.fenrir.model.selection.Sources
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.PlaceFactory.getAlbumsByVideoPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVideoPreviewPlace
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostCreation
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils.shareLink
import dev.ragnarok.fenrir.util.Utils.singletonArrayList
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.MySearchView

class VideosFragment : BaseMvpFragment<VideosListPresenter, IVideosListView>(), IVideosListView,
    DocsUploadAdapter.ActionListener, VideosAdapter.VideoOnClickListener {
    private val requestFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = result.data?.getStringExtra(Extra.PATH)
            val vid: LocalVideo? = result.data?.getParcelableExtraCompat(Extra.VIDEO)
            if (file.nonNullNoEmpty()) {
                lazyPresenter {
                    fireFileForUploadSelected(file)
                }
            } else if (vid != null) {
                lazyPresenter {
                    fireFileForUploadSelected(vid.getData().toString())
                }
            }
        }
    }
    private val requestReadPermission =
        requestPermissionsAbs(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) { lazyPresenter { fireReadPermissionResolved() } }

    /**
     * True - если фрагмент находится внутри TabLayout
     */
    private var inTabsContainer = false
    private var mAdapter: VideosAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mUploadAdapter: DocsUploadAdapter? = null
    private var mUploadRoot: View? = null
    private var mEmpty: TextView? = null
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<VideosListPresenter> {
        return object : IPresenterFactory<VideosListPresenter> {
            override fun create(): VideosListPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val albumId = requireArguments().getInt(Extra.ALBUM_ID)
                val ownerId = requireArguments().getLong(Extra.OWNER_ID)
                val optAlbumTitle = requireArguments().getString(EXTRA_ALBUM_TITLE)
                val action = requireArguments().getString(Extra.ACTION)
                return VideosListPresenter(
                    accountId,
                    ownerId,
                    albumId,
                    action,
                    optAlbumTitle,
                    requireActivity(),
                    saveInstanceState
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inTabsContainer = requireArguments().getBoolean(EXTRA_IN_TABS_CONTAINER)
    }

    override fun setToolbarTitle(title: String?) {
        if (!inTabsContainer) {
            super.setToolbarTitle(title)
        }
    }

    override fun setToolbarSubtitle(subtitle: String?) {
        if (!inTabsContainer) {
            super.setToolbarSubtitle(subtitle)
        }
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(getString(R.string.videos))
        if (!inTabsContainer) {
            if (requireActivity() is OnSectionResumeCallback) {
                (requireActivity() as OnSectionResumeCallback).onClearSelection()
            }
            ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity())
        }
    }

    override fun requestReadExternalStoragePermission() {
        requestReadPermission.launch()
    }

    override fun startSelectUploadFileActivity(accountId: Long) {
        val sources = Sources()
            .with(LocalVideosSelectableSource())
            .with(FileManagerSelectableSource())
        val intent = createIntent(requireActivity(), 1, sources)
        requestFile.launch(intent)
    }

    override fun setUploadDataVisible(visible: Boolean) {
        mUploadRoot?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun displayUploads(data: List<Upload>) {
        mUploadAdapter?.setData(data)
    }

    override fun notifyUploadDataChanged() {
        mUploadAdapter?.notifyDataSetChanged()
    }

    override fun notifyUploadItemsAdded(position: Int, count: Int) {
        mUploadAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun notifyUploadItemChanged(position: Int) {
        mUploadAdapter?.notifyItemChanged(position)
    }

    override fun notifyUploadItemRemoved(position: Int) {
        mUploadAdapter?.notifyItemRemoved(position)
    }

    override fun notifyUploadProgressChanged(position: Int, progress: Int, smoothly: Boolean) {
        mUploadAdapter?.changeUploadProgress(position, progress, smoothly)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_videos, container, false)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val toolbar: Toolbar = root.findViewById(R.id.toolbar)
        if (!inTabsContainer) {
            toolbar.visibility = View.VISIBLE
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        } else {
            toolbar.visibility = View.GONE
        }
        val mySearchView: MySearchView = root.findViewById(R.id.searchview)
        mySearchView.setRightButtonVisibility(false)
        mySearchView.setLeftIcon(R.drawable.magnify)
        mySearchView.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    query
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    newText
                )
                return false
            }
        })
        val Add: FloatingActionButton = root.findViewById(R.id.add_button)
        var isNotMy = true
        presenter?.let {
            isNotMy = it.accountId != it.ownerId
        }
        if (isNotMy) Add.visibility = View.GONE else {
            Add.visibility = View.VISIBLE
            Add.setOnClickListener {
                presenter?.doUpload()
            }
        }
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mEmpty = root.findViewById(R.id.empty)
        val uploadRecyclerView: RecyclerView = root.findViewById(R.id.uploads_recycler_view)
        uploadRecyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        val columns = requireActivity().resources.getInteger(R.integer.videos_column_count)
        val manager = StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mAdapter = VideosAdapter(requireActivity(), emptyList())
        mAdapter?.setVideoOnClickListener(this)
        mUploadAdapter = DocsUploadAdapter(emptyList(), this)
        uploadRecyclerView.adapter = mUploadAdapter
        mUploadRoot = root.findViewById(R.id.uploads_root)
        recyclerView.adapter = mAdapter
        resolveEmptyTextVisibility()
        return root
    }

    override fun onVideoClick(position: Int, video: Video) {
        presenter?.fireVideoClick(
            video
        )
    }

    override fun onVideoLongClick(position: Int, video: Video): Boolean {
        presenter?.fireOnVideoLongClick(
            position,
            video
        )
        return true
    }

    override fun displayData(data: List<Video>) {
        if (mAdapter != null) {
            mAdapter?.setData(data)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyTextVisibility()
        }
    }

    override fun displayLoading(loading: Boolean) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout?.isRefreshing = loading
        }
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyItemRemoved(position: Int) {
        mAdapter?.notifyItemRemoved(position)
    }

    override fun notifyItemChanged(position: Int) {
        mAdapter?.notifyItemChanged(position)
    }

    override fun returnSelectionToParent(video: Video) {
        val intent = Intent()
        intent.putParcelableArrayListExtra(Extra.ATTACHMENTS, singletonArrayList(video))
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun showVideoPreview(accountId: Long, video: Video) {
        getVideoPreviewPlace(accountId, video).tryOpenWith(requireActivity())
    }

    override fun onRemoveClick(upload: Upload) {
        presenter?.fireRemoveClick(
            upload
        )
    }

    override fun onUploaded(upload: Video) {
        val intent = Intent()
        intent.putParcelableArrayListExtra(Extra.ATTACHMENTS, singletonArrayList(upload))
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun doVideoLongClick(
        accountId: Long,
        ownerId: Long,
        isMy: Boolean,
        position: Int,
        video: Video
    ) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        if (!isMy) {
            if (video.isCanAdd) {
                menus.add(
                    OptionRequest(
                        R.id.action_add_to_my_videos,
                        getString(R.string.add_to_my_videos),
                        R.drawable.plus,
                        false
                    )
                )
            }
        } else {
            menus.add(
                OptionRequest(
                    R.id.action_delete_from_my_videos,
                    getString(R.string.delete),
                    R.drawable.ic_outline_delete,
                    true
                )
            )
        }
        if (video.isCanEdit) {
            menus.add(
                OptionRequest(
                    R.id.action_edit,
                    getString(R.string.edit),
                    R.drawable.pencil,
                    true
                )
            )
        }
        menus.add(
            OptionRequest(
                R.id.action_copy_url,
                getString(R.string.copy_url),
                R.drawable.content_copy,
                false
            )
        )
        menus.add(
            OptionRequest(
                R.id.share_button,
                getString(R.string.share),
                R.drawable.share,
                true
            )
        )
        menus.add(
            OptionRequest(
                R.id.check_show_author,
                getString(R.string.author),
                R.drawable.person,
                true
            )
        )
        menus.add(
            OptionRequest(
                R.id.album_container,
                getString(R.string.videos_albums),
                R.drawable.album_photo,
                true
            )
        )
        menus.header(video.title, R.drawable.video, video.image)
        menus.columns(2)
        menus.show(
            childFragmentManager,
            "video_options"
        ) { _, option ->
            when (option.id) {
                R.id.action_copy_url -> {
                    val clipboard =
                        requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText(
                        getString(R.string.link),
                        "https://vk.com/video" + video.ownerId + "_" + video.id
                    )
                    clipboard?.setPrimaryClip(clip)
                    createCustomToast(requireActivity()).showToast(R.string.copied_url)
                }

                R.id.check_show_author -> {
                    getOwnerWallPlace(accountId, video.ownerId, null).tryOpenWith(requireActivity())
                }

                R.id.album_container -> {
                    getAlbumsByVideoPlace(accountId, ownerId, video.ownerId, video.id).tryOpenWith(
                        requireActivity()
                    )
                }

                else -> {
                    presenter?.fireVideoOption(
                        option.id,
                        video,
                        position,
                        requireActivity()
                    )
                }
            }
        }
    }

    override fun displayShareDialog(accountId: Long, video: Video, canPostToMyWall: Boolean) {
        val items: Array<String> = if (canPostToMyWall) {
            if (!video.private) {
                arrayOf(
                    getString(R.string.share_link),
                    getString(R.string.repost_send_message),
                    getString(R.string.repost_to_wall)
                )
            } else {
                arrayOf(getString(R.string.repost_send_message), getString(R.string.repost_to_wall))
            }
        } else {
            if (!video.private) {
                arrayOf(getString(R.string.share_link), getString(R.string.repost_send_message))
            } else {
                arrayOf(getString(R.string.repost_send_message))
            }
        }
        MaterialAlertDialogBuilder(requireActivity())
            .setItems(items) { _: DialogInterface?, i: Int ->
                if (video.private) {
                    when (i) {
                        0 -> startForSendAttachments(requireActivity(), accountId, video)
                        1 -> goToPostCreation(
                            requireActivity(),
                            accountId,
                            accountId,
                            EditingPostType.TEMP,
                            listOf(video)
                        )
                    }
                } else {
                    when (i) {
                        0 -> shareLink(
                            requireActivity(),
                            "https://vk.com/video" + video.ownerId + "_" + video.id,
                            video.title
                        )

                        1 -> startForSendAttachments(requireActivity(), accountId, video)
                        2 -> goToPostCreation(
                            requireActivity(),
                            accountId,
                            accountId,
                            EditingPostType.TEMP,
                            listOf(video)
                        )
                    }
                }
            }
            .setCancelable(true)
            .setTitle(R.string.repost_title)
            .show()
    }

    override fun showSuccessToast() {
        createCustomToast(requireActivity()).setDuration(Toast.LENGTH_SHORT)
            .showToastSuccessBottom(R.string.success)
    }

    companion object {
        const val EXTRA_IN_TABS_CONTAINER = "in_tabs_container"
        const val EXTRA_ALBUM_TITLE = "album_title"
        fun buildArgs(
            accountId: Long,
            ownerId: Long,
            albumId: Int,
            action: String?,
            albumTitle: String?
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.ALBUM_ID, albumId)
            args.putLong(Extra.OWNER_ID, ownerId)
            if (albumTitle != null) {
                args.putString(EXTRA_ALBUM_TITLE, albumTitle)
            }
            args.putString(Extra.ACTION, action)
            return args
        }

        fun newInstance(
            accountId: Long,
            ownerId: Long,
            albumId: Int,
            action: String?,
            albumTitle: String?
        ): VideosFragment {
            return newInstance(buildArgs(accountId, ownerId, albumId, action, albumTitle))
        }

        fun newInstance(args: Bundle?): VideosFragment {
            val fragment = VideosFragment()
            fragment.arguments = args
            return fragment
        }
    }
}