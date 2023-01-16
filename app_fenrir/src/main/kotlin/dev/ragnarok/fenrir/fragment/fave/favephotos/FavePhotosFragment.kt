package dev.ragnarok.fenrir.fragment.fave.favephotos

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.place.PlaceFactory.getFavePhotosGallery
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class FavePhotosFragment : BaseMvpFragment<FavePhotosPresenter, IFavePhotosView>(),
    SwipeRefreshLayout.OnRefreshListener, IFavePhotosView,
    FavePhotosAdapter.PhotoSelectionListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: FavePhotosAdapter? = null
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
        val root = inflater.inflate(R.layout.fragment_fave_photos, container, false)
        recyclerView = root.findViewById(android.R.id.list)
        mEmpty = root.findViewById(R.id.empty)
        val columns = resources.getInteger(R.integer.photos_column_count)
        val gridLayoutManager = GridLayoutManager(requireActivity(), columns)
        recyclerView?.layoutManager = gridLayoutManager
        recyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = FavePhotosAdapter(requireActivity(), emptyList())
        mAdapter?.setPhotoSelectionListener(this)
        recyclerView?.adapter = mAdapter
        resolveEmptyTextVisibility()
        return root
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun onPhotoClicked(position: Int, photo: Photo) {
        presenter?.firePhotoClick(
            position
        )
    }

    override fun displayData(photos: List<Photo>) {
        if (mAdapter != null) {
            mAdapter?.setData(photos)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyTextVisibility()
        }
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if ((mAdapter ?: return).itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = refreshing }
    }

    override fun goToGallery(accountId: Long, photos: ArrayList<Photo>, position: Int) {
        getFavePhotosGallery(accountId, photos, position).setActivityResultLauncher(
            requestPhotoUpdate
        )
            .tryOpenWith(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FavePhotosPresenter> {
        return object : IPresenterFactory<FavePhotosPresenter> {
            override fun create(): FavePhotosPresenter {
                return FavePhotosPresenter(
                    requireArguments().getLong(
                        Extra.ACCOUNT_ID
                    ), saveInstanceState
                )
            }
        }
    }

    companion object {
        fun newInstance(accountId: Long): FavePhotosFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val favePhotosFragment = FavePhotosFragment()
            favePhotosFragment.arguments = args
            return favePhotosFragment
        }
    }
}