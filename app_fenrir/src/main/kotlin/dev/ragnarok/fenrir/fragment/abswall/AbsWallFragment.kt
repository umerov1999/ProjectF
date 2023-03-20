package dev.ragnarok.fenrir.fragment.abswall

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.DualTabPhotoActivity
import dev.ragnarok.fenrir.api.impl.AbsApi
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.fragment.abswall.WallAdapter.NonPublishedPostActionListener
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.horizontal.HorizontalStoryAdapter
import dev.ragnarok.fenrir.fragment.docs.DocsUploadAdapter
import dev.ragnarok.fenrir.fragment.groupwall.GroupWallFragment
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.WallSearchCriteria
import dev.ragnarok.fenrir.fragment.userwall.UserWallFragment
import dev.ragnarok.fenrir.fragment.videos.IVideosListView
import dev.ragnarok.fenrir.fragment.vkphotos.IVKPhotosView
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.selection.FileManagerSelectableSource
import dev.ragnarok.fenrir.model.selection.LocalGallerySelectableSource
import dev.ragnarok.fenrir.model.selection.LocalPhotosSelectableSource
import dev.ragnarok.fenrir.model.selection.LocalVideosSelectableSource
import dev.ragnarok.fenrir.model.selection.Sources
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.thorvg.ThorVGRender
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceFactory.getAudiosPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getNarrativesPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerArticles
import dev.ragnarok.fenrir.place.PlaceFactory.getPhotoAlbumGalleryPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getShortVideoPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleTabSearchPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVKPhotoAlbumsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getVideosPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getWallAttachmentsPlace
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostCreation
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostEditor
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.util.*
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.AppTextUtils.getCounterWithK
import dev.ragnarok.fenrir.util.Utils.dp
import dev.ragnarok.fenrir.util.Utils.is600dp
import dev.ragnarok.fenrir.util.Utils.isLandscape
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper.Companion.createFrom
import dev.ragnarok.fenrir.view.UpEditFab
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import me.minetsh.imaging.IMGEditActivity
import java.io.File

abstract class AbsWallFragment<V : IWallView, P : AbsWallPresenter<V>> :
    PlaceSupportMvpFragment<P, V>(), IWallView, WallAdapter.ClickListener,
    NonPublishedPostActionListener, MenuProvider, BackPressCallback,
    DocsUploadAdapter.ActionListener {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mWallAdapter: WallAdapter? = null
    private var mLoadMoreFooterHelper: LoadMoreFooterHelper? = null
    private var mStoryAdapter: HorizontalStoryAdapter? = null
    private var fabCreate: UpEditFab? = null

    private var mUploadAdapter: DocsUploadAdapter? = null
    private var mUploadRoot: View? = null
    protected fun setupPaganContent(Runes: View?, paganSymbol: RLottieImageView?) {
        Runes?.visibility = if (Settings.get()
                .other().isRunes_show
        ) View.VISIBLE else View.GONE
        if (!FenrirNative.isNativeLoaded) {
            paganSymbol?.visibility = View.GONE
            return
        }
        val symbol = Settings.get().other().paganSymbol
        paganSymbol?.visibility = if (symbol != 0) View.VISIBLE else View.GONE
        if (symbol == 0) {
            return
        }
        val pic = Common.requirePaganSymbol(symbol, requireActivity())
        if (pic.isAnimation) {
            paganSymbol?.fromRes(
                pic.lottieRes,
                dp(pic.lottie_widthHeight),
                dp(pic.lottie_widthHeight),
                pic.lottie_replacement, pic.lottie_useMoveColor
            )
            paganSymbol?.playAnimation()
        } else {
            paganSymbol?.setImageBitmap(
                ThorVGRender.createBitmap(
                    pic.iconRes,
                    dp(pic.icon_width),
                    dp(pic.icon_height)
                )
            )
        }
    }

    override fun onRequestSkipOffset(
        accountId: Long,
        ownerId: Long,
        wallFilter: Int,
        currentPos: Int
    ) {
        InputWallOffsetDialog.Builder(requireActivity(), accountId, ownerId, wallFilter)
            .setTitleRes(R.string.action_jump_offset).setValue(currentPos)
            .setCallback(object : InputWallOffsetDialog.Callback {
                override fun onChanged(newValue: Int) {
                    presenter?.fireSkipOffset(newValue)
                }

                override fun onCanceled() {

                }
            }).show()
    }

    override fun onBackPressed(): Boolean {
        if (presenter?.canLoadUp() == true) {
            presenter?.fireSkipOffset(0)
            return false
        }
        return true
    }

    private val openRequestStory =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                val localPhotos: ArrayList<LocalPhoto>? =
                    result.data?.getParcelableArrayListExtraCompat(Extra.PHOTOS)
                val file = result.data?.getStringExtra(Extra.PATH)
                val video: LocalVideo? = result.data?.getParcelableExtraCompat(Extra.VIDEO)
                lazyPresenter {
                    fireStorySelected(requireActivity(), localPhotos, file, video)
                }
            }
        }

    private val openRequestResizeStoryPhoto =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                lazyPresenter {
                    doUploadStoryFile(
                        (result.data
                            ?: return@lazyPresenter).getStringExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH)
                            ?: return@lazyPresenter,
                        Upload.IMAGE_SIZE_FULL,
                        false
                    )
                }
            }
        }

    override fun doEditStoryPhoto(uri: Uri) {
        try {
            openRequestResizeStoryPhoto.launch(
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

    override fun showAvatarUploadedMessage(accountId: Long, post: Post) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.success)
            .setMessage(R.string.avatar_was_changed_successfully)
            .setPositiveButton(R.string.button_show) { _: DialogInterface?, _: Int ->
                PlaceFactory.getPostPreviewPlace(
                    accountId,
                    post.vkid,
                    post.ownerId,
                    post
                ).tryOpenWith(requireActivity())
            }
            .setNegativeButton(R.string.button_ok, null)
            .show()
    }

    fun requestUploadStory() {
        val sources = Sources()
            .with(LocalPhotosSelectableSource())
            .with(LocalGallerySelectableSource())
            .with(LocalVideosSelectableSource())
            .with(FileManagerSelectableSource())
        val intent = DualTabPhotoActivity.createIntent(requireActivity(), 1, sources)
        openRequestStory.launch(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
    }

    override fun goNarratives(accountId: Long, ownerId: Long) {
        getNarrativesPlace(accountId, ownerId).tryOpenWith(requireActivity())
    }

    override fun goClips(accountId: Long, ownerId: Long) {
        getShortVideoPlace(accountId, ownerId).tryOpenWith(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_wall, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))

        val uploadRecyclerView: RecyclerView = root.findViewById(R.id.uploads_recycler_view)
        uploadRecyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        mUploadAdapter = DocsUploadAdapter(emptyList(), this)
        uploadRecyclerView.adapter = mUploadAdapter
        mUploadRoot = root.findViewById(R.id.uploads_root)

        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val manager: RecyclerView.LayoutManager = if (is600dp(requireActivity())) {
            val land = isLandscape(requireActivity())
            StaggeredGridLayoutManager(if (land) 2 else 1, StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(requireActivity())
        }
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = manager
        fabCreate = root.findViewById(R.id.fragment_user_profile_fab)
        fabCreate?.setOnClickListener {
            if (fabCreate?.isEdit == true) {
                presenter?.fireCreateClick()
            } else {
                recyclerView.scrollToPosition(0)
            }
        }
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        fabCreate?.getRecyclerObserver(7)?.let { recyclerView.addOnScrollListener(it) }
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        val headerView = inflater.inflate(headerLayout(), recyclerView, false)
        onHeaderInflated(headerView)
        val footerView = inflater.inflate(R.layout.footer_load_more, recyclerView, false)
        mLoadMoreFooterHelper = createFrom(footerView, object : LoadMoreFooterHelper.Callback {
            override fun onLoadMoreClick() {
                presenter?.fireLoadMoreClick()
            }
        })
        val headerStory = inflater.inflate(R.layout.header_story, recyclerView, false)
        val headerStoryRecyclerView: RecyclerView = headerStory.findViewById(R.id.header_story)
        headerStoryRecyclerView.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        mStoryAdapter = HorizontalStoryAdapter(mutableListOf())
        mStoryAdapter?.setListener(object : HorizontalStoryAdapter.Listener {
            override fun onStoryClick(item: Story, pos: Int) {
                openHistoryVideo(
                    Settings.get().accounts().current, ArrayList(
                        presenter?.stories ?: emptyList()
                    ), pos
                )
            }

            override fun onStoryLongClick(item: Story, pos: Int): Boolean {
                val isMy = item.ownerId == Settings.get().accounts().current
                val items: Array<String> = if (isMy) {
                    arrayOf(
                        getString(R.string.delete),
                        getString(R.string.views)
                    )
                } else {
                    arrayOf(getString(R.string.answer), getString(R.string.views))
                }
                MaterialAlertDialogBuilder(requireActivity()).setItems(items) { _: DialogInterface?, i: Int ->
                    when (i) {
                        0 -> {
                            if (!isMy) {
                                presenter?.updateToStory(AbsApi.join(
                                    listOf(AccessIdPair(item.id, item.ownerId, item.accessKey)),
                                    ","
                                ) { AccessIdPair.format(it) })
                                requestUploadStory()
                            } else {
                                MaterialAlertDialogBuilder(
                                    requireActivity()
                                ).setMessage(R.string.do_delete)
                                    .setTitle(R.string.confirmation)
                                    .setCancelable(true)
                                    .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                                        presenter?.fireRemoveStoryClick(item.ownerId, item.id)
                                    }.setNegativeButton(R.string.button_cancel, null)
                                    .show()
                            }
                        }

                        1 -> {
                            PlaceFactory.getStoriesViewPlace(
                                Settings.get().accounts().current,
                                item.ownerId,
                                item.id
                            ).tryOpenWith(requireActivity())
                        }
                    }
                }.setCancelable(true).show()
                return true
            }
        })
        headerStoryRecyclerView.adapter = mStoryAdapter
        mWallAdapter = WallAdapter(requireActivity(), mutableListOf(), this, this)
        mWallAdapter?.addHeader(headerView)
        mWallAdapter?.addHeader(headerStoryRecyclerView)
        mWallAdapter?.addFooter(footerView)
        mWallAdapter?.setNonPublishedPostActionListener(this)
        recyclerView.adapter = mWallAdapter
        return root
    }


    override fun onAvatarClick(ownerId: Long) {
        super.onOwnerClick(ownerId)
    }

    override fun showSnackbar(res: Int, isLong: Boolean) {
        CustomSnackbars.createCustomSnackbars(view)
            ?.setDurationSnack(if (isLong) BaseTransientBottomBar.LENGTH_LONG else BaseTransientBottomBar.LENGTH_SHORT)
            ?.defaultSnack(res)?.show()
    }

    override fun openPhotoAlbum(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        photos: ArrayList<Photo>,
        position: Int
    ) {
        getPhotoAlbumGalleryPlace(
            accountId,
            albumId,
            ownerId,
            photos,
            position,
            false,
            Settings.get().other().isInvertPhotoRev
        )
            .tryOpenWith(requireActivity())
    }

    override fun goToWallSearch(accountId: Long, ownerId: Long) {
        val criteria = WallSearchCriteria("", ownerId)
        getSingleTabSearchPlace(accountId, SearchContentType.WALL, criteria).tryOpenWith(
            requireActivity()
        )
    }

    override fun goToConversationAttachments(accountId: Long, ownerId: Long) {
        val types = arrayOf(
            FindAttachmentType.TYPE_PHOTO,
            FindAttachmentType.TYPE_VIDEO,
            FindAttachmentType.TYPE_DOC,
            FindAttachmentType.TYPE_AUDIO,
            FindAttachmentType.TYPE_LINK,
            FindAttachmentType.TYPE_ALBUM,
            FindAttachmentType.TYPE_POST_WITH_COMMENT,
            FindAttachmentType.TYPE_POST_WITH_QUERY
        )
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(OptionRequest(0, getString(R.string.photos), R.drawable.photo_album, true))
        menus.add(OptionRequest(1, getString(R.string.videos), R.drawable.video, true))
        menus.add(OptionRequest(2, getString(R.string.documents), R.drawable.book, true))
        menus.add(OptionRequest(3, getString(R.string.music), R.drawable.song, true))
        menus.add(OptionRequest(4, getString(R.string.links), R.drawable.web, true))
        menus.add(OptionRequest(5, getString(R.string.photo_album), R.drawable.album_photo, false))
        menus.add(
            OptionRequest(
                6,
                getString(R.string.posts_with_comment),
                R.drawable.comment,
                false
            )
        )
        menus.add(OptionRequest(7, getString(R.string.posts_with_query), R.drawable.magnify, false))
        menus.show(
            childFragmentManager,
            "attachments_select",
            object : ModalBottomSheetDialogFragment.Listener {
                override fun onModalOptionSelected(option: Option) {
                    getWallAttachmentsPlace(
                        accountId,
                        ownerId,
                        types[option.id]
                    ).tryOpenWith(requireActivity())
                }
            })
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_wall, menu)
    }

    private val requestWriteQRPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        lazyPresenter {
            fireShowQR(requireActivity())
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_toggle_monitor -> {
                if (HelperSimple.needHelp(HelperSimple.MONITOR_CHANGES, 2)) {
                    showSnackbar(R.string.toggle_monitor_info, true)
                }
                presenter?.fireToggleMonitor()
                return true
            }

            R.id.action_refresh -> {
                presenter?.fireRefresh()
                return true
            }

            R.id.action_show_qr -> {
                if (!AppPerms.hasReadWriteStoragePermission(requireActivity())) {
                    requestWriteQRPermission.launch()
                } else {
                    presenter?.fireShowQR(
                        requireActivity()
                    )
                }
                return true
            }

            R.id.action_edit -> {
                presenter?.fireEdit(requireActivity())
                return true
            }

            R.id.action_copy_url -> {
                presenter?.fireCopyUrlClick()
                return true
            }

            R.id.action_copy_id -> {
                presenter?.fireCopyIdClick()
                return true
            }

            R.id.action_add_to_shortcut -> {
                presenter?.fireAddToShortcutClick()
                return true
            }

            R.id.action_search -> {
                presenter?.fireSearchClick()
                return true
            }

            R.id.wall_attachments -> {
                presenter?.openConversationAttachments()
                return true
            }

            R.id.search_stories -> {
                val menus = ModalBottomSheetDialogFragment.Builder()
                menus.add(
                    OptionRequest(
                        R.id.button_ok,
                        getString(R.string.by_name),
                        R.drawable.pencil,
                        false
                    )
                )
                menus.add(
                    OptionRequest(
                        R.id.button_cancel,
                        getString(R.string.by_owner),
                        R.drawable.person,
                        false
                    )
                )
                menus.show(
                    requireActivity().supportFragmentManager,
                    "search_story_options",
                    object : ModalBottomSheetDialogFragment.Listener {
                        override fun onModalOptionSelected(option: Option) {
                            if (menuItem.itemId == R.id.button_ok) {
                                presenter?.searchStory(true)
                            } else if (menuItem.itemId == R.id.button_cancel) {
                                presenter?.searchStory(false)
                            }
                        }
                    })
                return true
            }

            R.id.action_open_url -> {
                val clipBoard =
                    requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                if (clipBoard?.primaryClip?.itemCount.orZero() > 0) {
                    val temp = clipBoard?.primaryClip?.getItemAt(0)?.text.toString()
                    LinkHelper.openUrl(
                        requireActivity(),
                        presenter?.accountId ?: Settings.get().accounts().current,
                        temp, false
                    )
                }
                return true
            }

            R.id.action_jump_offset -> {
                presenter?.fireRequestSkipOffset()
                return true
            }

            else -> return false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        val view = OptionView()
        presenter?.fireOptionViewCreated(view)
        val isDebug = Settings.get().other().isDeveloper_mode
        menu.findItem(R.id.action_open_url).isVisible = view.isMy
        menu.findItem(R.id.search_stories).isVisible = view.isMy && isDebug
        menu.findItem(R.id.action_edit).isVisible = view.isMy
        menu.findItem(R.id.action_add_to_shortcut).isVisible = !view.isMy
        menu.findItem(R.id.action_toggle_monitor).setTitle(
            if (Settings.get().other()
                    .isOwnerInChangesMonitor(view.ownerId)
            ) R.string.toggle_monitor_off else R.string.toggle_monitor_on
        )
    }

    override fun copyToClipboard(label: String?, body: String?) {
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText(label, body)
        clipboard?.setPrimaryClip(clip)
        customToast.showToast(R.string.copied)
    }

    override fun goToPostCreation(accountId: Long, ownerId: Long, @EditingPostType postType: Int) {
        goToPostCreation(requireActivity(), accountId, ownerId, postType, null)
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    @LayoutRes
    protected abstract fun headerLayout(): Int
    protected abstract fun onHeaderInflated(headerRootView: View)
    override fun displayWallData(data: MutableList<Post>) {
        mWallAdapter?.setItems(data)
    }

    override fun notifyWallDataSetChanged() {
        mWallAdapter?.notifyDataSetChanged()
    }

    override fun updateStory(stories: MutableList<Story>?) {
        mStoryAdapter?.setItems(stories ?: mutableListOf())
        mStoryAdapter?.notifyDataSetChanged()
    }

    override fun notifyWallItemChanged(position: Int) {
        mWallAdapter?.notifyItemChanged(position + (mWallAdapter?.headersCount ?: 0))
    }

    override fun notifyWallDataAdded(position: Int, count: Int) {
        mWallAdapter?.notifyItemRangeInserted(position + (mWallAdapter?.headersCount ?: 0), count)
    }

    override fun notifyWallItemRemoved(index: Int) {
        mWallAdapter?.notifyItemRemoved(index + (mWallAdapter?.headersCount ?: 0))
    }

    override fun onOwnerClick(ownerId: Long) {
        onOpenOwner(ownerId)
    }

    override fun onShareClick(post: Post) {
        presenter?.fireShareClick(post)
    }

    override fun onPostClick(post: Post) {
        presenter?.firePostBodyClick(post)
    }

    override fun onRestoreClick(post: Post) {
        presenter?.firePostRestoreClick(post)
    }

    override fun onCommentsClick(post: Post) {
        if (!post.isCanPostComment) {
            CustomToast.createCustomToast(requireActivity())
                .showToastError(R.string.comments_disabled_post)
        }
        presenter?.fireCommentsClick(post)
    }

    override fun onLikeLongClick(post: Post) {
        presenter?.fireLikeLongClick(post)
    }

    override fun onShareLongClick(post: Post) {
        presenter?.fireShareLongClick(post)
    }

    override fun onLikeClick(post: Post) {
        presenter?.fireLikeClick(post)
    }

    override fun openPostEditor(accountId: Long, post: Post) {
        goToPostEditor(requireActivity(), accountId, post)
    }

    override fun setupLoadMoreFooter(@LoadMoreState state: Int) {
        mLoadMoreFooterHelper?.switchToState(state)
    }

    override fun openPhotoAlbums(accountId: Long, ownerId: Long, owner: Owner?) {
        getVKPhotoAlbumsPlace(
            accountId,
            ownerId,
            IVKPhotosView.ACTION_SHOW_PHOTOS,
            ParcelableOwnerWrapper.wrap(owner)
        )
            .tryOpenWith(requireActivity())
    }

    override fun openVideosLibrary(accountId: Long, ownerId: Long, owner: Owner?) {
        getVideosPlace(accountId, ownerId, IVideosListView.ACTION_SHOW)
            .withParcelableExtra(Extra.OWNER, owner)
            .tryOpenWith(requireActivity())
    }

    override fun openAudios(accountId: Long, ownerId: Long, owner: Owner?) {
        getAudiosPlace(accountId, ownerId)
            .withParcelableExtra(Extra.OWNER, owner)
            .tryOpenWith(requireActivity())
    }

    override fun openArticles(accountId: Long, ownerId: Long, owner: Owner?) {
        getOwnerArticles(accountId, ownerId)
            .withParcelableExtra(Extra.OWNER, owner)
            .tryOpenWith(requireActivity())
    }

    override fun onResume() {
        super.onResume()
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

    override fun onButtonRemoveClick(post: Post) {
        presenter?.fireButtonRemoveClick(post)
    }

    override fun onRemoveClick(upload: Upload) {
        presenter?.fireRemoveClick(
            upload
        )
    }

    override fun setUploadDataVisible(visible: Boolean) {
        mUploadRoot?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun displayUploads(data: List<Upload>) {
        mUploadAdapter?.setData(data)
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

    protected class OptionView : IWallView.IOptionView {
        var isMy = false
        var isBlacklistedByMe = false
        var isFavorite = false
        var isSubscribed = false
        var ownerId = 0L
        override fun setIsBlacklistedByMe(blocked: Boolean) {
            isBlacklistedByMe = blocked
        }

        override fun typeOwnerId(id: Long) {
            ownerId = id
        }

        override fun setIsMy(my: Boolean) {
            isMy = my
        }

        override fun setIsFavorite(favorite: Boolean) {
            isFavorite = favorite
        }

        override fun setIsSubscribed(subscribed: Boolean) {
            isSubscribed = subscribed
        }
    }

    companion object {
        fun buildArgs(accountId: Long, ownerId: Long, owner: Owner?): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            args.putParcelable(Extra.OWNER, ParcelableOwnerWrapper(owner))
            return args
        }

        fun newInstance(args: Bundle): Fragment {
            val fragment: Fragment = if (args.getLong(Extra.OWNER_ID) > 0) {
                UserWallFragment()
            } else {
                GroupWallFragment()
            }
            fragment.arguments = args
            return fragment
        }


        fun setupCounter(view: TextView?, count: Int) {
            view?.text = if (count > 0) getCounterWithK(count) else "-"
            view?.isEnabled = count > 0
        }


        fun setupCounterFlow(view: TextView?, container: ViewGroup?, count: Int) {
            view?.text = if (count > 0) getCounterWithK(count) else "-"
            view?.isEnabled = count > 0
            container?.visibility = if (count > 0) View.VISIBLE else View.GONE
        }


        @SuppressLint("SetTextI18n")
        fun setupCounterWith(view: TextView?, count: Int, with: Int) {
            view?.text =
                (if (count > 0) getCounterWithK(count) else "-") + if (with > 0) "/" + getCounterWithK(
                    with
                ) else ""
            view?.isEnabled = count > 0
        }
    }
}