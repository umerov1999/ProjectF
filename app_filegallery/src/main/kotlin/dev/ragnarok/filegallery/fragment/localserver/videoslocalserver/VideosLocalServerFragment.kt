package dev.ragnarok.filegallery.fragment.localserver.videoslocalserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.fragment.base.BaseMvpFragment
import dev.ragnarok.filegallery.fragment.base.core.IPresenterFactory
import dev.ragnarok.filegallery.fragment.localserver.videoslocalserver.LocalServerVideosAdapter.VideoOnClickListener
import dev.ragnarok.filegallery.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.filegallery.listener.PicassoPauseOnScrollListener
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.place.PlaceFactory.getInternalPlayerPlace
import dev.ragnarok.filegallery.util.ViewUtils
import dev.ragnarok.filegallery.view.MySearchView
import dev.ragnarok.filegallery.view.MySearchView.OnAdditionalButtonClickListener

class VideosLocalServerFragment :
    BaseMvpFragment<VideosLocalServerPresenter, IVideosLocalServerView>(),
    MySearchView.OnQueryTextListener, VideoOnClickListener, IVideosLocalServerView {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mVideoRecyclerAdapter: LocalServerVideosAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_local_server_video, container, false)
        val searchView: MySearchView = root.findViewById(R.id.searchview)
        searchView.setOnQueryTextListener(this)
        searchView.setRightButtonVisibility(true)
        searchView.setRightIcon(R.drawable.ic_recent)
        searchView.setLeftIcon(R.drawable.magnify)
        searchView.setOnAdditionalButtonClickListener(object : OnAdditionalButtonClickListener {
            override fun onAdditionalButtonClick() {
                presenter?.toggleReverse()
            }

        })
        searchView.setQuery("", true)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh(
                false
            )
        }
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val columns = requireActivity().resources.getInteger(R.integer.videos_column_count)
        val manager = StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mVideoRecyclerAdapter = LocalServerVideosAdapter(requireActivity(), emptyList())
        mVideoRecyclerAdapter?.setVideoOnClickListener(this)
        recyclerView.adapter = mVideoRecyclerAdapter
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<VideosLocalServerPresenter> {
        return object : IPresenterFactory<VideosLocalServerPresenter> {
            override fun create(): VideosLocalServerPresenter {
                return VideosLocalServerPresenter(
                    saveInstanceState
                )
            }
        }
    }

    override fun displayList(videos: List<Video>) {
        mVideoRecyclerAdapter?.setData(videos)
    }

    override fun notifyListChanged() {
        mVideoRecyclerAdapter?.notifyDataSetChanged()
    }

    override fun displayLoading(loading: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loading
    }

    override fun notifyItemChanged(index: Int) {
        mVideoRecyclerAdapter?.notifyItemChanged(index)
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mVideoRecyclerAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        presenter?.fireSearchRequestChanged(
            query
        )
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        presenter?.fireSearchRequestChanged(
            newText
        )
        return false
    }

    override fun onVideoClick(position: Int, video: Video) {
        getInternalPlayerPlace(video).tryOpenWith(requireActivity())
    }
}