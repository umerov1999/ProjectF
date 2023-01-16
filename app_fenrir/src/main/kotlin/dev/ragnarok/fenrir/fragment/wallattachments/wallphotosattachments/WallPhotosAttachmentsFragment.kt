package dev.ragnarok.fenrir.fragment.wallattachments.wallphotosattachments

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.PlaceSupportMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.fave.favephotos.FavePhotosAdapter
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.place.PlaceFactory.getTmpSourceGalleryPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class WallPhotosAttachmentsFragment :
    PlaceSupportMvpFragment<WallPhotosAttachmentsPresenter, IWallPhotosAttachmentsView>(),
    IWallPhotosAttachmentsView, FavePhotosAdapter.PhotoSelectionListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: FavePhotosAdapter? = null
    private var mLoadMore: FloatingActionButton? = null
    private var recyclerView: RecyclerView? = null
    private val requestPhotoUpdate = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && (result.data
                ?: return@registerForActivityResult)
                .extras != null
        ) {
            val ps = ((result.data ?: return@registerForActivityResult).extras
                ?: return@registerForActivityResult).getInt(Extra.POSITION)
            mAdapter?.updateCurrentPosition(ps)
            recyclerView?.scrollToPosition(ps)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_wall_attachments, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mEmpty = root.findViewById(R.id.empty)
        mLoadMore = root.findViewById(R.id.goto_button)
        val columns = resources.getInteger(R.integer.photos_column_count)
        val gridLayoutManager = GridLayoutManager(requireActivity(), columns)
        recyclerView = root.findViewById(android.R.id.list)
        recyclerView?.layoutManager = gridLayoutManager
        recyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mLoadMore?.setOnClickListener {
            presenter?.fireScrollToEnd()
        }
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = FavePhotosAdapter(requireActivity(), emptyList())
        mAdapter?.setPhotoSelectionListener(this)
        recyclerView?.adapter = mAdapter
        resolveEmptyText()
        return root
    }

    private fun resolveEmptyText() {
        mEmpty?.visibility =
            if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun displayData(photos: MutableList<Photo>) {
        mAdapter?.setData(photos)
        resolveEmptyText()
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
        resolveEmptyText()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
        resolveEmptyText()
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<WallPhotosAttachmentsPresenter> {
        return object : IPresenterFactory<WallPhotosAttachmentsPresenter> {
            override fun create(): WallPhotosAttachmentsPresenter {
                return WallPhotosAttachmentsPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.OWNER_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun toolbarTitle(title: String) {
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.title = title
        }
    }

    override fun toolbarSubtitle(subtitle: String) {
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.subtitle = subtitle
        }
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

    override fun onPhotoClicked(position: Int, photo: Photo) {
        presenter?.firePhotoClick(
            position
        )
    }

    override fun goToTempPhotosGallery(accountId: Long, source: TmpSource, index: Int) {
        getTmpSourceGalleryPlace(accountId, source, index).setActivityResultLauncher(
            requestPhotoUpdate
        ).tryOpenWith(requireActivity())
    }

    override fun goToTempPhotosGallery(accountId: Long, ptr: Long, index: Int) {
        getTmpSourceGalleryPlace(
            accountId,
            ptr,
            index
        ).setActivityResultLauncher(requestPhotoUpdate).tryOpenWith(requireActivity())
    }

    override fun onSetLoadingStatus(isLoad: Int) {
        when (isLoad) {
            1 -> mLoadMore?.setImageResource(R.drawable.audio_died)
            2 -> mLoadMore?.setImageResource(R.drawable.view)
            else -> mLoadMore?.setImageResource(R.drawable.ic_arrow_down)
        }
    }

    companion object {
        fun newInstance(accountId: Long, ownerId: Long): WallPhotosAttachmentsFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            val fragment = WallPhotosAttachmentsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}