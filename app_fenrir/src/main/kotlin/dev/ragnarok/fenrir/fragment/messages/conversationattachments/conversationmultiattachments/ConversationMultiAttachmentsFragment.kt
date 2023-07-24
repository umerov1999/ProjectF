package dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationmultiattachments

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.audio.audios.AudioRecyclerAdapter
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.docs.DocsAdapter
import dev.ragnarok.fenrir.fragment.fave.favephotos.FavePhotosAdapter
import dev.ragnarok.fenrir.fragment.videos.VideosAdapter
import dev.ragnarok.fenrir.fragment.wall.WallAdapter
import dev.ragnarok.fenrir.fragment.wall.wallattachments.wallmultiattachments.LinksAdapter
import dev.ragnarok.fenrir.fragment.wall.wallattachments.wallmultiattachments.PhotoAlbumsAdapter
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.module.StringHash
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostEditor
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.view.RecyclerViewSavedScroll

class ConversationMultiAttachmentsFragment :
    PlaceSupportMvpFragment<ConversationMultiAttachmentsPresenter, IConversationMultiAttachmentsView>(),
    IConversationMultiAttachmentsView, WallAdapter.ClickListener, DocsAdapter.ActionListener,
    LinksAdapter.ActionListener, PhotoAlbumsAdapter.ClickListener,
    FavePhotosAdapter.PhotoSelectionListener, VideosAdapter.VideoOnClickListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mAudiosAdapter: AudioRecyclerAdapter? = null
    private var mDocsAdapter: DocsAdapter? = null
    private var mLinksAdapter: LinksAdapter? = null
    private var mPhotoAlbumAdapter: PhotoAlbumsAdapter? = null
    private var mPhotoAdapter: FavePhotosAdapter? = null
    private var mVideoAdapter: VideosAdapter? = null
    private var mPostsAdapter: WallAdapter? = null

    private var mLoadMore: FloatingActionButton? = null
    private var frameAdapter: FrameAdapter? = null

    private val requestWritePermission: AppPerms.DoRequestPermissions = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        CustomToast.createCustomToast(requireActivity())
            .showToast(R.string.permission_all_granted_text)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(
            R.layout.fragment_wall_conversation_attachments_multi,
            container,
            false
        )
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))

        val viewpager: ViewPager2 = root.findViewById(R.id.viewpager)
        viewpager.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().viewpager_page_transform
            )
        )
        frameAdapter = FrameAdapter(
            listOf(
                AttachmentConversationType.PHOTO,
                AttachmentConversationType.PHOTO_ALBUM,
                AttachmentConversationType.VIDEO,
                AttachmentConversationType.DOC,
                AttachmentConversationType.LINK,
                AttachmentConversationType.POST,
                AttachmentConversationType.AUDIO
            )
        )
        viewpager.adapter = frameAdapter
        TabLayoutMediator(
            root.findViewById(R.id.tablayout),
            viewpager
        ) { tab: TabLayout.Tab, position: Int ->
            frameAdapter?.getTittle(position)
                ?.let { tab.setText(it) }
        }.attach()
        viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                frameAdapter?.getItem(position)?.let { presenter?.fireUpdateCurrentPage(it) }
            }
        })

        mEmpty = root.findViewById(R.id.empty)
        mLoadMore = root.findViewById(R.id.goto_button)

        mLoadMore?.setOnClickListener {
            presenter?.fireScrollToEnd(true)
        }
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            mSwipeRefreshLayout?.isRefreshing = false
            CustomSnackbars.createCustomSnackbars(view, null, true)
                ?.setDurationSnack(Snackbar.LENGTH_LONG)?.defaultSnack(R.string.do_update)
                ?.setAction(R.string.button_yes) {
                    presenter?.fireRefresh()
                }?.show()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)

        mAudiosAdapter = AudioRecyclerAdapter(
            requireActivity(), mutableListOf(),
            not_show_my = false,
            iSSelectMode = false,
            playlist_id = null
        )
        mAudiosAdapter?.setClickListener(object : AudioRecyclerAdapter.ClickListener {
            override fun onClick(position: Int, audio: Audio) {
                presenter?.playAudio(
                    requireActivity(),
                    position
                )
            }

            override fun onEdit(position: Int, audio: Audio) {}
            override fun onDelete(position: Int) {}
            override fun onUrlPhotoOpen(url: String, prefix: String, photo_prefix: String) {
                PlaceFactory.getSingleURLPhotoPlace(url, prefix, photo_prefix)
                    .tryOpenWith(requireActivity())
            }

            override fun onRequestWritePermissions() {
                requestWritePermission.launch()
            }
        })

        mDocsAdapter = DocsAdapter(mutableListOf())
        mDocsAdapter?.setActionListener(this)

        mLinksAdapter = LinksAdapter(mutableListOf())
        mLinksAdapter?.setActionListener(this)

        mPhotoAlbumAdapter = PhotoAlbumsAdapter(emptyList(), requireActivity())
        mPhotoAlbumAdapter?.setClickListener(this)

        mPhotoAdapter = FavePhotosAdapter(requireActivity(), emptyList())
        mPhotoAdapter?.setPhotoSelectionListener(this)

        mVideoAdapter = VideosAdapter(requireActivity(), emptyList())
        mVideoAdapter?.setVideoOnClickListener(this)

        mPostsAdapter = WallAdapter(requireActivity(), mutableListOf(), this, this)

        return root
    }

    private val requestPhotoUpdate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && (result.data
                ?: return@registerForActivityResult)
                .extras != null
        ) {
            val ps = ((result.data ?: return@registerForActivityResult).extras
                ?: return@registerForActivityResult).getInt(Extra.POSITION)
            mPhotoAdapter?.updateCurrentPosition(ps)
            mPhotoAdapter?.attachedRecyclerView?.scrollToPosition(ps)
        }
    }

    override fun onPhotoClicked(position: Int, photo: Photo) {
        presenter?.firePhotoClick(
            position
        )
    }

    override fun goToTempPhotosGallery(accountId: Long, source: TmpSource, index: Int) {
        PlaceFactory.getTmpSourceGalleryPlace(accountId, source, index).setActivityResultLauncher(
            requestPhotoUpdate
        ).tryOpenWith(requireActivity())
    }

    override fun goToTempPhotosGallery(accountId: Long, ptr: Long, index: Int) {
        PlaceFactory.getTmpSourceGalleryPlace(
            accountId,
            ptr,
            index
        ).setActivityResultLauncher(requestPhotoUpdate).tryOpenWith(requireActivity())
    }

    override fun onAlbumClick(index: Int, album: PhotoAlbum) {
        presenter?.firePhotoAlbumClick(
            album
        )
    }

    override fun onDocClick(index: Int, doc: Document) {
        presenter?.fireDocClick(
            doc
        )
    }

    override fun onLinkClick(index: Int, doc: Link) {
        presenter?.fireLinkClick(
            doc
        )
    }

    override fun onDocLongClick(index: Int, doc: Document): Boolean {
        return false
    }

    override fun onVideoClick(position: Int, video: Video) {
        presenter?.fireVideoClick(
            video
        )
    }

    override fun onVideoLongClick(position: Int, video: Video): Boolean {
        return false
    }

    override fun resolveEmptyText(@AttachmentConversationType type: Int) {
        when (type) {
            AttachmentConversationType.AUDIO -> {
                mEmpty?.visibility =
                    if (mAudiosAdapter?.itemCount == 0) View.VISIBLE else View.GONE
            }

            AttachmentConversationType.DOC -> {
                mEmpty?.visibility = if (mDocsAdapter?.itemCount == 0) View.VISIBLE else View.GONE
            }

            AttachmentConversationType.LINK -> {
                mEmpty?.visibility = if (mLinksAdapter?.itemCount == 0) View.VISIBLE else View.GONE
            }

            AttachmentConversationType.PHOTO -> {
                mEmpty?.visibility = if (mPhotoAdapter?.itemCount == 0) View.VISIBLE else View.GONE
            }

            AttachmentConversationType.PHOTO_ALBUM -> {
                mEmpty?.visibility =
                    if (mPhotoAlbumAdapter?.itemCount == 0) View.VISIBLE else View.GONE
            }

            AttachmentConversationType.POST -> {
                mEmpty?.visibility =
                    if (mPostsAdapter?.itemCount == 0) View.VISIBLE else View.GONE
            }

            AttachmentConversationType.VIDEO -> {
                mEmpty?.visibility = if (mVideoAdapter?.itemCount == 0) View.VISIBLE else View.GONE
            }
        }
    }

    override fun displayAudioData(posts: MutableList<Audio>) {
        mAudiosAdapter?.setItems(posts)
    }

    override fun displayDocsData(docs: MutableList<Document>) {
        mDocsAdapter?.setItems(docs)
    }

    override fun displayLinksData(links: MutableList<Link>) {
        mLinksAdapter?.setItems(links)
    }

    override fun displayPhotoAlbumsData(photoAlbums: MutableList<PhotoAlbum>) {
        mPhotoAlbumAdapter?.setData(photoAlbums)
    }

    override fun displayPhotoData(photos: MutableList<Photo>) {
        mPhotoAdapter?.setData(photos)
    }

    override fun displayVideoData(videos: MutableList<Video>) {
        mVideoAdapter?.setData(videos)
    }

    override fun displayPostsData(posts: MutableList<Post>) {
        mPostsAdapter?.setItems(posts)
    }

    override fun notifyDataRemoved(
        @AttachmentConversationType type: Int,
        position: Int,
        count: Int
    ) {
        when (type) {
            AttachmentConversationType.AUDIO -> {
                mAudiosAdapter?.notifyItemBindableRangeRemoved(position, count)
            }

            AttachmentConversationType.DOC -> {
                mDocsAdapter?.notifyItemBindableRangeRemoved(position, count)
            }

            AttachmentConversationType.LINK -> {
                mLinksAdapter?.notifyItemBindableRangeRemoved(position, count)
            }

            AttachmentConversationType.PHOTO -> {
                mPhotoAdapter?.notifyItemRangeRemoved(position, count)
            }

            AttachmentConversationType.PHOTO_ALBUM -> {
                mPhotoAlbumAdapter?.notifyItemRangeRemoved(position, count)
            }

            AttachmentConversationType.POST -> {
                mPostsAdapter?.notifyItemBindableRangeRemoved(position, count)
            }

            AttachmentConversationType.VIDEO -> {
                mVideoAdapter?.notifyItemRangeRemoved(position, count)
            }
        }
    }

    override fun notifyDataAdded(@AttachmentConversationType type: Int, position: Int, count: Int) {
        when (type) {
            AttachmentConversationType.AUDIO -> {
                mAudiosAdapter?.notifyItemBindableRangeInserted(position, count)
            }

            AttachmentConversationType.DOC -> {
                mDocsAdapter?.notifyItemBindableRangeInserted(position, count)
            }

            AttachmentConversationType.LINK -> {
                mLinksAdapter?.notifyItemBindableRangeInserted(position, count)
            }

            AttachmentConversationType.PHOTO -> {
                mPhotoAdapter?.notifyItemRangeInserted(position, count)
            }

            AttachmentConversationType.PHOTO_ALBUM -> {
                mPhotoAlbumAdapter?.notifyItemRangeInserted(position, count)
            }

            AttachmentConversationType.POST -> {
                mPostsAdapter?.notifyItemBindableRangeInserted(position, count)
            }

            AttachmentConversationType.VIDEO -> {
                mVideoAdapter?.notifyItemRangeInserted(position, count)
            }
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ConversationMultiAttachmentsPresenter> {
        return object : IPresenterFactory<ConversationMultiAttachmentsPresenter> {
            override fun create(): ConversationMultiAttachmentsPresenter {
                return ConversationMultiAttachmentsPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.PEER_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun toolbarTitle(title: String) {
        supportToolbarFor(this)?.title = title
    }

    override fun toolbarSubtitle(subtitle: String) {
        supportToolbarFor(this)?.subtitle = subtitle
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

    override fun onSetLoadingStatus(isLoad: Int) {
        when (isLoad) {
            1 -> mLoadMore?.setImageResource(R.drawable.audio_died)
            2 -> mLoadMore?.setImageResource(R.drawable.view)
            else -> mLoadMore?.setImageResource(R.drawable.ic_arrow_down)
        }
    }

    override fun onAvatarClick(ownerId: Long) {
        onOwnerClick(ownerId)
    }

    override fun onShareClick(post: Post) {
        presenter?.fireShareClick(
            post
        )
    }

    override fun onPostClick(post: Post) {
        presenter?.firePostBodyClick(
            post
        )
    }

    override fun onRestoreClick(post: Post) {
        presenter?.firePostRestoreClick(
            post
        )
    }

    override fun onCommentsClick(post: Post) {
        presenter?.fireCommentsClick(
            post
        )
    }

    override fun onLikeLongClick(post: Post) {
        presenter?.fireLikeLongClick(
            post
        )
    }

    override fun onShareLongClick(post: Post) {
        presenter?.fireShareLongClick(
            post
        )
    }

    override fun onLikeClick(post: Post) {
        presenter?.fireLikeClick(
            post
        )
    }

    override fun openPostEditor(accountId: Long, post: Post) {
        goToPostEditor(requireActivity(), accountId, post)
    }

    inner class FrameAdapter(@AttachmentConversationType private var data: List<Int>) :
        RecyclerView.Adapter<FrameAdapter.Holder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(
                LayoutInflater.from(requireActivity())
                    .inflate(R.layout.item_wall_attachments_recycler, parent, false)
            )
        }

        @AttachmentConversationType
        fun getItem(position: Int): Int {
            return data[position]
        }

        @StringRes
        fun getTittle(position: Int): Int {
            return when (data[position]) {
                AttachmentConversationType.AUDIO -> {
                    R.string.audios
                }

                AttachmentConversationType.DOC -> {
                    R.string.documents
                }

                AttachmentConversationType.LINK -> {
                    R.string.links
                }

                AttachmentConversationType.PHOTO -> {
                    R.string.photos
                }

                AttachmentConversationType.PHOTO_ALBUM -> {
                    R.string.photo_album
                }

                AttachmentConversationType.POST -> {
                    R.string.posts
                }

                AttachmentConversationType.VIDEO -> {
                    R.string.videos
                }

                else -> {
                    R.string.unknown_error
                }
            }
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = data[position]
            holder.recyclerView.clearOnScrollListeners()
            when (item) {
                AttachmentConversationType.AUDIO -> {
                    holder.recyclerView.adapter = null
                    holder.recyclerView.layoutManager =
                        LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
                    holder.recyclerView.adapter = mAudiosAdapter
                }

                AttachmentConversationType.DOC -> {
                    holder.recyclerView.adapter = null
                    holder.recyclerView.layoutManager =
                        LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
                    holder.recyclerView.adapter = mDocsAdapter
                }

                AttachmentConversationType.LINK -> {
                    holder.recyclerView.adapter = null
                    holder.recyclerView.layoutManager =
                        LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
                    holder.recyclerView.adapter = mLinksAdapter
                }

                AttachmentConversationType.PHOTO -> {
                    holder.recyclerView.adapter = null
                    val columns = resources.getInteger(R.integer.photos_column_count)
                    holder.recyclerView.layoutManager =
                        GridLayoutManager(requireActivity(), columns)
                    holder.recyclerView.adapter = mPhotoAdapter
                }

                AttachmentConversationType.PHOTO_ALBUM -> {
                    holder.recyclerView.adapter = null
                    val columnCount = resources.getInteger(R.integer.photos_albums_column_count)
                    holder.recyclerView.layoutManager =
                        GridLayoutManager(requireActivity(), columnCount)
                    holder.recyclerView.adapter = mPhotoAlbumAdapter
                }

                AttachmentConversationType.POST -> {
                    holder.recyclerView.adapter = null
                    holder.recyclerView.layoutManager =
                        LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
                    holder.recyclerView.adapter = mPostsAdapter
                }

                AttachmentConversationType.VIDEO -> {
                    holder.recyclerView.adapter = null
                    val columns = resources.getInteger(R.integer.videos_column_count)
                    holder.recyclerView.layoutManager =
                        GridLayoutManager(requireActivity(), columns)
                    holder.recyclerView.adapter = mVideoAdapter
                }
            }
            holder.recyclerView.updateUid(
                StringHash.calculateCRC32(TAG + "_" + item)
            )
            PicassoPauseOnScrollListener.addListener(holder.recyclerView)
            holder.recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
                override fun onScrollToLastElement() {
                    presenter?.fireScrollToEnd(false)
                }
            })
        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val recyclerView: RecyclerViewSavedScroll =
                itemView.findViewById(R.id.wall_attachments_recycle)
        }
    }

    companion object {
        private val TAG = ConversationMultiAttachmentsFragment::class.java.simpleName
    }
}
