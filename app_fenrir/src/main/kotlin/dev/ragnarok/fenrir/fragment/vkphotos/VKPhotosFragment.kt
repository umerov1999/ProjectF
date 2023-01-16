package dev.ragnarok.fenrir.fragment.vkphotos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.PhotosActivity
import dev.ragnarok.fenrir.dialog.ImageSizeAlertDialog
import dev.ragnarok.fenrir.dialog.ImageSizeAlertDialog.Companion.showUploadPhotoSizeIfNeed
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.vkphotos.BigVkPhotosAdapter.UploadActionListener
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.wrappers.SelectablePhotoWrapper
import dev.ragnarok.fenrir.place.PlaceFactory.getPhotoAlbumGalleryPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermission
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class VKPhotosFragment : BaseMvpFragment<VkPhotosPresenter, IVkPhotosView>(),
    BigVkPhotosAdapter.PhotosActionListener, UploadActionListener, IVkPhotosView, MenuProvider {
    private val requestUploadPhoto = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photos: ArrayList<LocalPhoto>? =
                result.data?.getParcelableArrayListExtraCompat(Extra.PHOTOS)
            if (photos.nonNullNoEmpty()) {
                onPhotosForUploadSelected(photos)
            }
        }
    }
    private val requestReadPermission =
        requestPermissionsAbs(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) { presenter?.fireReadStoragePermissionChanged() }
    private val requestReadPermissionForLoadDownload =
        requestPermissionsAbs(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) { presenter?.loadDownload() }
    private val requestPhotoUpdate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && (result.data
                ?: return@registerForActivityResult)
                .extras != null
        ) {
            lazyPresenter {
                updateInfo(
                    (result.data?.extras ?: return@lazyPresenter).getInt(Extra.POSITION),
                    ((result.data
                        ?: return@lazyPresenter)
                        .extras ?: return@lazyPresenter).getLong(Extra.PTR)
                )
            }
        }
    }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: BigVkPhotosAdapter? = null
    private var mEmptyText: TextView? = null
    private var mFab: FloatingActionButton? = null
    private var mAction: String? = null
    private var mRecyclerView: RecyclerView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAction = requireArguments().getString(Extra.ACTION, IVkPhotosView.ACTION_SHOW_PHOTOS)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val columnCount = resources.getInteger(R.integer.local_gallery_column_count)
        val manager: RecyclerView.LayoutManager = GridLayoutManager(requireActivity(), columnCount)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mRecyclerView = root.findViewById(R.id.list)
        mRecyclerView?.layoutManager = manager
        mRecyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(TAG))
        mRecyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mEmptyText = root.findViewById(R.id.empty)
        mFab = root.findViewById(R.id.fr_photo_gallery_attach)
        mFab?.setOnClickListener { onFabClicked() }
        mAdapter = BigVkPhotosAdapter(requireActivity(), emptyList(), emptyList(), TAG)
        mAdapter?.setPhotosActionListener(this)
        mAdapter?.setUploadActionListener(this)
        mRecyclerView?.adapter = mAdapter
        return root
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmptyText != null && mAdapter != null) {
            mEmptyText?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    private fun resolveFabVisibility(show: Boolean) {
        if (!isAdded || mFab == null) return
        if (mFab?.isShown == true && !show) {
            mFab?.hide()
        }
        if (mFab?.isShown == false && show) {
            mFab?.show()
        }
    }

    private fun onFabClicked() {
        if (isSelectionMode) {
            presenter?.fireSelectionCommitClick()
        } else {
            presenter?.fireAddPhotosClick()
        }
    }

    private val isSelectionMode: Boolean
        get() = IVkPhotosView.ACTION_SELECT_PHOTOS == mAction

    private fun onPhotosForUploadSelected(photos: List<LocalPhoto>) {
        showUploadPhotoSizeIfNeed(
            requireActivity(),
            object : ImageSizeAlertDialog.Callback {
                override fun onSizeSelected(size: Int) {
                    doUploadPhotosToAlbum(photos, size)
                }
            })
    }

    internal fun doUploadPhotosToAlbum(photos: List<LocalPhoto>, size: Int) {
        presenter?.firePhotosForUploadSelected(
            photos,
            size
        )
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onDestroyView() {
        mAdapter?.cleanup()
        super.onDestroyView()
    }

    override fun onPhotoClick(
        holder: BigVkPhotosAdapter.PhotoViewHolder,
        photoWrapper: SelectablePhotoWrapper
    ) {
        if (isSelectionMode) {
            presenter?.firePhotoSelectionChanged(
                photoWrapper
            )
            mAdapter?.updatePhotoHoldersSelectionAndIndexes()
        } else {
            presenter?.firePhotoClick(
                photoWrapper
            )
        }
    }

    override fun onUploadRemoveClicked(upload: Upload) {
        presenter?.fireUploadRemoveClick(
            upload
        )
    }

    override fun displayData(photos: List<SelectablePhotoWrapper>, uploads: List<Upload>) {
        if (mAdapter != null) {
            mAdapter?.setData(BigVkPhotosAdapter.DATA_TYPE_UPLOAD, uploads)
            mAdapter?.setData(BigVkPhotosAdapter.DATA_TYPE_PHOTO, photos)
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyPhotosAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count, BigVkPhotosAdapter.DATA_TYPE_PHOTO)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyPhotosChanged(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeChanged(position, count)
            resolveEmptyTextVisibility()
        }
    }

    override fun displayRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun notifyUploadAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count, BigVkPhotosAdapter.DATA_TYPE_UPLOAD)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyUploadRemoved(index: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRemoved(index, BigVkPhotosAdapter.DATA_TYPE_UPLOAD)
            resolveEmptyTextVisibility()
        }
    }

    override fun setButtonAddVisible(visible: Boolean, anim: Boolean) {
        if (mFab != null) {
            resolveFabVisibility(visible)
        }
    }

    override fun notifyUploadItemChanged(index: Int) {
        mAdapter?.notifyItemChanged(index, BigVkPhotosAdapter.DATA_TYPE_UPLOAD)
    }

    override fun notifyUploadProgressChanged(id: Int, progress: Int) {
        mAdapter?.updateUploadHoldersProgress(id, true, progress)
    }

    override fun displayGallery(
        accountId: Long,
        albumId: Int,
        ownerId: Long,
        source: TmpSource,
        position: Int
    ) {
        getPhotoAlbumGalleryPlace(
            accountId,
            albumId,
            ownerId,
            source,
            position,
            false,
            Settings.get().other().isInvertPhotoRev
        ).tryOpenWith(requireActivity())
    }

    override fun displayGalleryUnSafe(
        accountId: Long,
        albumId: Int,
        ownerId: Long,
        parcelNativePointer: Long,
        position: Int
    ) {
        getPhotoAlbumGalleryPlace(
            accountId,
            albumId,
            ownerId,
            parcelNativePointer,
            position,
            false,
            Settings.get().other().isInvertPhotoRev
        ).setActivityResultLauncher(requestPhotoUpdate).tryOpenWith(requireActivity())
    }

    override fun displayDefaultToolbarTitle() {
        setToolbarTitle(getString(R.string.photos))
    }

    override fun displayToolbarSubtitle(album: PhotoAlbum?, text: String) {
        if (album != null) {
            setToolbarSubtitle(album.getDisplayTitle(requireActivity()) + " " + text)
        } else {
            setToolbarSubtitle(text)
        }
    }

    override fun scrollTo(position: Int) {
        mRecyclerView?.scrollToPosition(position)
    }

    override fun setDrawerPhotosSelected(selected: Boolean) {
        if (requireActivity() is OnSectionResumeCallback) {
            if (selected) {
                (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_PHOTOS)
            } else {
                (requireActivity() as OnSectionResumeCallback).onClearSelection()
            }
        }
    }

    override fun returnSelectionToParent(selected: List<Photo>) {
        val intent = Intent()
        intent.putParcelableArrayListExtra(Extra.ATTACHMENTS, ArrayList(selected))
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun showSelectPhotosToast() {
        CustomToast.createCustomToast(requireActivity()).setDuration(Toast.LENGTH_LONG)
            .showToastInfo(R.string.select_attachments)
    }

    override fun startLocalPhotosSelection() {
        if (!hasReadStoragePermission(requireActivity())) {
            requestReadPermission.launch()
            return
        }
        startLocalPhotosSelectionActivity()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_photos, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(R.id.action_toggle_rev).setTitle(
            if (Settings.get()
                    .other().isInvertPhotoRev
            ) R.string.sort_new_to_old else R.string.sort_old_to_new
        )
        menu.findItem(R.id.action_show_date).isVisible =
            !(presenter?.isShowBDate ?: false)
    }

    override fun onToggleShowDate(isShow: Boolean) {
        mAdapter?.setIsShowDate(isShow)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_get_downloaded -> {
                if (!hasReadWriteStoragePermission(requireActivity())) {
                    requestReadPermissionForLoadDownload.launch()
                    return true
                }
                presenter?.loadDownload()
                return true
            }
            R.id.action_show_date -> {
                presenter?.doToggleDate()
                requireActivity().invalidateOptionsMenu()
                return true
            }
            R.id.action_toggle_rev -> {
                presenter?.togglePhotoInvert()
                requireActivity().invalidateOptionsMenu()
                return true
            }
        }
        return false
    }

    private fun startLocalPhotosSelectionActivity() {
        val intent = Intent(requireActivity(), PhotosActivity::class.java)
        intent.putExtra(PhotosActivity.EXTRA_MAX_SELECTION_COUNT, Int.MAX_VALUE)
        requestUploadPhoto.launch(intent)
    }

    override fun startLocalPhotosSelectionIfHasPermission() {
        if (hasReadStoragePermission(requireActivity())) {
            startLocalPhotosSelectionActivity()
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<VkPhotosPresenter> {
        return object : IPresenterFactory<VkPhotosPresenter> {
            override fun create(): VkPhotosPresenter {
                val ownerWrapper: ParcelableOwnerWrapper? =
                    requireArguments().getParcelableCompat(Extra.OWNER)
                val owner = ownerWrapper?.get()
                val album: PhotoAlbum? = requireArguments().getParcelableCompat(Extra.ALBUM)
                return VkPhotosPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
                    requireArguments().getInt(Extra.ALBUM_ID),
                    requireArguments().getString(Extra.ACTION, IVkPhotosView.ACTION_SHOW_PHOTOS),
                    owner,
                    album,
                    requireArguments().getInt(Extra.SELECTED),
                    saveInstanceState
                )
            }
        }
    }

    companion object {
        private val TAG = VKPhotosFragment::class.java.simpleName
        fun buildArgs(
            accountId: Long,
            ownerId: Long,
            albumId: Int,
            action: String?,
            selected: Int
        ): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putInt(Extra.ALBUM_ID, albumId)
            args.putInt(Extra.SELECTED, selected)
            args.putString(Extra.ACTION, action)
            return args
        }

        fun newInstance(args: Bundle?): VKPhotosFragment {
            val fragment = VKPhotosFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(
            accountId: Long,
            ownerId: Long,
            albumId: Int,
            action: String?
        ): VKPhotosFragment {
            return newInstance(buildArgs(accountId, ownerId, albumId, action, -1))
        }
    }
}