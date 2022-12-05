package dev.ragnarok.fenrir.fragment.videoalbumsbyvideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.videoalbums.VideoAlbumsNewAdapter
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.VideoAlbum
import dev.ragnarok.fenrir.place.PlaceFactory.getVideoAlbumPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class VideoAlbumsByVideoFragment :
    BaseMvpFragment<VideoAlbumsByVideoPresenter, IVideoAlbumsByVideoView>(),
    VideoAlbumsNewAdapter.Listener, IVideoAlbumsByVideoView {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: VideoAlbumsNewAdapter? = null
    private var mEmpty: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_video_albums_by_video, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mEmpty = root.findViewById(R.id.empty)
        val columns = requireActivity().resources.getInteger(R.integer.videos_column_count)
        val manager = StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(VideoAlbumsNewAdapter.PICASSO_TAG))
        mAdapter = VideoAlbumsNewAdapter(requireActivity(), emptyList())
        mAdapter?.setListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyTextVisibility()
        return root
    }

    override fun onClick(album: VideoAlbum) {
        presenter?.fireItemClick(
            album
        )
    }

    override fun displayData(data: List<VideoAlbum>) {
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
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = loading }
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun openAlbum(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        action: String?,
        title: String?
    ) {
        getVideoAlbumPlace(
            accountId,
            ownerId,
            albumId,
            action,
            title
        ).tryOpenWith(requireActivity())
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.videos_albums)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<VideoAlbumsByVideoPresenter> {
        return object : IPresenterFactory<VideoAlbumsByVideoPresenter> {
            override fun create(): VideoAlbumsByVideoPresenter {
                val ownerId1 = requireArguments().getInt(Extra.OWNER_ID)
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val owner = requireArguments().getInt(Extra.OWNER)
                val video = requireArguments().getInt(Extra.VIDEO)
                return VideoAlbumsByVideoPresenter(
                    accountId,
                    ownerId1,
                    owner,
                    video,
                    saveInstanceState
                )
            }
        }
    }

    companion object {
        fun newInstance(args: Bundle?): VideoAlbumsByVideoFragment {
            val fragment = VideoAlbumsByVideoFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(
            accountId: Int,
            ownerId: Int,
            video_ownerId: Int,
            video_Id: Int
        ): VideoAlbumsByVideoFragment {
            return newInstance(buildArgs(accountId, ownerId, video_ownerId, video_Id))
        }

        fun buildArgs(accountId: Int, ownerId: Int, video_ownerId: Int, video_Id: Int): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.OWNER_ID, ownerId)
            args.putInt(Extra.OWNER, video_ownerId)
            args.putInt(Extra.VIDEO, video_Id)
            return args
        }
    }
}