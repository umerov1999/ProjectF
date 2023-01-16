package dev.ragnarok.fenrir.fragment.localserver.videoslocalserver

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.InternalVideoSize
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.place.PlaceFactory.getVkInternalPlayerPlace
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.MySearchView.OnAdditionalButtonClickListener

class VideosLocalServerFragment :
    BaseMvpFragment<VideosLocalServerPresenter, IVideosLocalServerView>(),
    MySearchView.OnQueryTextListener, LocalServerVideosAdapter.VideoOnClickListener,
    IVideosLocalServerView {
    private val requestWritePermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }
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
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
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
                    requireArguments().getLong(Extra.ACCOUNT_ID),
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
        getVkInternalPlayerPlace(video, InternalVideoSize.SIZE_720, true).tryOpenWith(
            requireActivity()
        )
    }

    override fun onRequestWritePermissions() {
        requestWritePermission.launch()
    }

    companion object {
        fun newInstance(accountId: Long): VideosLocalServerFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = VideosLocalServerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}