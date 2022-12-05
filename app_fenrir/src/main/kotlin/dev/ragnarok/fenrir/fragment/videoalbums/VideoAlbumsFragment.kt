package dev.ragnarok.fenrir.fragment.videoalbums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.VideoAlbum
import dev.ragnarok.fenrir.place.PlaceFactory.getVideoAlbumPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class VideoAlbumsFragment : BaseMvpFragment<VideoAlbumsPresenter, IVideoAlbumsView>(),
    VideoAlbumsNewAdapter.Listener, IVideoAlbumsView {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: VideoAlbumsNewAdapter? = null
    private var mEmpty: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_video_albums, container, false)
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
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToLast()
            }
        })
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
            (mAdapter ?: return).notifyItemRangeInserted(position, count)
            resolveEmptyTextVisibility()
        }
    }

    override fun displayLoading(loading: Boolean) {
        if (mSwipeRefreshLayout != null) {
            (mSwipeRefreshLayout ?: return).post {
                (mSwipeRefreshLayout ?: return@post).isRefreshing = loading
            }
        }
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmpty != null && mAdapter != null) {
            (mEmpty ?: return).visibility =
                if ((mAdapter ?: return).itemCount == 0) View.VISIBLE else View.GONE
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

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<VideoAlbumsPresenter> {
        return object : IPresenterFactory<VideoAlbumsPresenter> {
            override fun create(): VideoAlbumsPresenter {
                val ownerId1 = requireArguments().getInt(Extra.OWNER_ID)
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val action = requireArguments().getString(Extra.ACTION)
                return VideoAlbumsPresenter(accountId, ownerId1, action, saveInstanceState)
            }
        }
    }

    companion object {
        fun newInstance(args: Bundle?): VideoAlbumsFragment {
            val fragment = VideoAlbumsFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(accountId: Int, ownerId: Int, action: String?): VideoAlbumsFragment {
            return newInstance(buildArgs(accountId, ownerId, action))
        }

        fun buildArgs(aid: Int, ownerId: Int, action: String?): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, aid)
            args.putInt(Extra.OWNER_ID, ownerId)
            args.putString(Extra.ACTION, action)
            return args
        }
    }
}