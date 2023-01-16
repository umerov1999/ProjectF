package dev.ragnarok.fenrir.fragment.productalbums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.place.PlaceFactory.getMarketPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class ProductAlbumsFragment : BaseMvpFragment<ProductAlbumsPresenter, IProductAlbumsView>(),
    IProductAlbumsView, SwipeRefreshLayout.OnRefreshListener, MarketAlbumAdapter.ClickListener {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: MarketAlbumAdapter? = null
    private var mEmpty: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_products, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(android.R.id.list)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mEmpty = root.findViewById(R.id.empty)
        val columnCount = resources.getInteger(R.integer.photos_albums_column_count)
        recyclerView.layoutManager = GridLayoutManager(requireActivity(), columnCount)
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = MarketAlbumAdapter(emptyList(), requireActivity())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyTextVisibility()
        return root
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(market_albums: List<MarketAlbum>) {
        if (mAdapter != null) {
            mAdapter?.setData(market_albums)
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
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = refreshing }
    }

    override fun onMarketAlbumOpen(accountId: Long, market_album: MarketAlbum) {
        getMarketPlace(
            accountId,
            market_album.getOwner_id(),
            market_album.getId(),
            false
        ).tryOpenWith(
            requireActivity()
        )
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ProductAlbumsPresenter> {
        return object : IPresenterFactory<ProductAlbumsPresenter> {
            override fun create(): ProductAlbumsPresenter {
                return ProductAlbumsPresenter(
                    requireArguments().getLong(
                        Extra.ACCOUNT_ID
                    ),
                    requireArguments().getLong(Extra.OWNER_ID),
                    requireActivity(),
                    saveInstanceState
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.markets_albums)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onOpenClick(index: Int, market_album: MarketAlbum) {
        presenter?.fireAlbumOpen(
            market_album
        )
    }

    companion object {
        fun newInstance(accountId: Long, ownerId: Long): ProductAlbumsFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            val fragment = ProductAlbumsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}