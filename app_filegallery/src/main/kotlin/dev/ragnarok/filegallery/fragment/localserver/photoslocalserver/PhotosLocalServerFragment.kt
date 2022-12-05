package dev.ragnarok.filegallery.fragment.localserver.photoslocalserver

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.Extra
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.fragment.base.BaseMvpFragment
import dev.ragnarok.filegallery.fragment.base.core.IPresenterFactory
import dev.ragnarok.filegallery.fragment.localserver.photoslocalserver.LocalServerPhotosAdapter.PhotoSelectionListener
import dev.ragnarok.filegallery.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.filegallery.listener.PicassoPauseOnScrollListener
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.place.PlaceFactory.getPhotoLocalServerPlace
import dev.ragnarok.filegallery.util.ViewUtils
import dev.ragnarok.filegallery.view.MySearchView
import dev.ragnarok.filegallery.view.MySearchView.OnAdditionalButtonClickListener

class PhotosLocalServerFragment :
    BaseMvpFragment<PhotosLocalServerPresenter, IPhotosLocalServerView>(),
    MySearchView.OnQueryTextListener, PhotoSelectionListener, IPhotosLocalServerView {
    private val requestPhotoUpdate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && (result.data
                ?: return@registerForActivityResult)
                .extras != null
        ) {
            lazyPresenter {
                updateInfo(
                    ((result.data ?: return@lazyPresenter).extras ?: return@lazyPresenter).getInt(
                        Extra.POSITION
                    ),
                    ((result.data
                        ?: return@lazyPresenter)
                        .extras ?: return@lazyPresenter).getLong(Extra.PTR)
                )
            }
        }
    }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mPhotoRecyclerAdapter: LocalServerPhotosAdapter? = null
    private var recyclerView: RecyclerView? = null
    override fun scrollTo(position: Int) {
        mPhotoRecyclerAdapter?.updateCurrentPosition(position)
        recyclerView?.scrollToPosition(position)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_local_server_photo, container, false)
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
        recyclerView = root.findViewById(R.id.recycler_view)
        val columns = requireActivity().resources.getInteger(R.integer.photos_column_count)
        val manager = StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        recyclerView?.layoutManager = manager
        recyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mPhotoRecyclerAdapter = LocalServerPhotosAdapter(requireActivity(), emptyList())
        mPhotoRecyclerAdapter?.setPhotoSelectionListener(this)
        recyclerView?.adapter = mPhotoRecyclerAdapter
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<PhotosLocalServerPresenter> {
        return object : IPresenterFactory<PhotosLocalServerPresenter> {
            override fun create(): PhotosLocalServerPresenter {
                return PhotosLocalServerPresenter(
                    saveInstanceState
                )
            }

        }
    }

    override fun displayList(photos: List<Photo>) {
        mPhotoRecyclerAdapter?.setData(photos)
    }

    override fun notifyListChanged() {
        mPhotoRecyclerAdapter?.notifyDataSetChanged()
    }

    override fun displayLoading(loading: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loading
    }

    override fun displayGalleryUnSafe(parcelNativePointer: Long, position: Int, reversed: Boolean) {
        getPhotoLocalServerPlace(parcelNativePointer, position, reversed).setActivityResultLauncher(
            requestPhotoUpdate
        ).tryOpenWith(requireActivity())
    }

    override fun notifyItemChanged(index: Int) {
        mPhotoRecyclerAdapter?.notifyItemChanged(index)
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mPhotoRecyclerAdapter?.notifyItemRangeInserted(position, count)
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

    override fun onPhotoClicked(position: Int, photo: Photo) {
        presenter?.firePhotoClick(
            photo
        )
    }
}