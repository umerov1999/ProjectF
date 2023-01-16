package dev.ragnarok.fenrir.fragment.localserver.photoslocalserver

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getPhotoAlbumGalleryPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.MySearchView
import dev.ragnarok.fenrir.view.MySearchView.OnAdditionalButtonClickListener
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class PhotosLocalServerFragment :
    BaseMvpFragment<PhotosLocalServerPresenter, IPhotosLocalServerView>(),
    MySearchView.OnQueryTextListener, LocalServerPhotosAdapter.PhotoSelectionListener,
    IPhotosLocalServerView {
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
                    ), ((result.data
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
        (mPhotoRecyclerAdapter ?: return).updateCurrentPosition(position)
        (recyclerView ?: return).scrollToPosition(position)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_local_server_photo, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
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
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun displayList(photos: List<Photo>) {
        mPhotoRecyclerAdapter?.setData(photos)
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.AUDIOS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.on_server)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_PHOTOS)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun notifyListChanged() {
        mPhotoRecyclerAdapter?.notifyDataSetChanged()
    }

    override fun displayLoading(loading: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loading
    }

    override fun displayGallery(
        accountId: Long,
        albumId: Int,
        ownerId: Long,
        source: TmpSource,
        position: Int,
        reversed: Boolean
    ) {
        getPhotoAlbumGalleryPlace(
            accountId,
            albumId,
            ownerId,
            source,
            position,
            true,
            reversed
        ).tryOpenWith(requireActivity())
    }

    override fun displayGalleryUnSafe(
        accountId: Long,
        albumId: Int,
        ownerId: Long,
        parcelNativePointer: Long,
        position: Int,
        reversed: Boolean
    ) {
        getPhotoAlbumGalleryPlace(
            accountId,
            albumId,
            ownerId,
            parcelNativePointer,
            position,
            true,
            reversed
        ).setActivityResultLauncher(requestPhotoUpdate).tryOpenWith(requireActivity())
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

    companion object {
        fun newInstance(accountId: Long): PhotosLocalServerFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = PhotosLocalServerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}