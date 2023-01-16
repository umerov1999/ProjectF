package dev.ragnarok.fenrir.fragment.fave.favepages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.PlaceFactory.getMentionsPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.MySearchView

class FavePagesFragment : BaseMvpFragment<FavePagesPresenter, IFavePagesView>(), IFavePagesView,
    FavePagesAdapter.ClickListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: FavePagesAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_fave_pages, container, false)
        mEmpty = root.findViewById(R.id.empty)
        val recyclerView: RecyclerView = root.findViewById(R.id.list)
        val columns = resources.getInteger(R.integer.photos_column_count)
        val gridLayoutManager = GridLayoutManager(requireActivity(), columns)
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        val mySearchView: MySearchView = root.findViewById(R.id.searchview)
        mySearchView.setRightButtonVisibility(false)
        mySearchView.setLeftIcon(R.drawable.magnify)
        mySearchView.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    query
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                presenter?.fireSearchRequestChanged(
                    newText
                )
                return false
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = FavePagesAdapter(emptyList(), requireActivity())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyText()
        return root
    }

    private fun resolveEmptyText() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun displayData(pages: List<FavePage>) {
        if (mAdapter != null) {
            mAdapter?.setData(pages)
            resolveEmptyText()
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyText()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyText()
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun openOwnerWall(accountId: Long, owner: Owner) {
        getOwnerWallPlace(accountId, owner).tryOpenWith(requireActivity())
    }

    override fun openMention(accountId: Long, owner: Owner) {
        getMentionsPlace(accountId, owner.ownerId).tryOpenWith(requireActivity())
    }

    override fun notifyItemRemoved(index: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRemoved(index)
            resolveEmptyText()
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FavePagesPresenter> {
        return object : IPresenterFactory<FavePagesPresenter> {
            override fun create(): FavePagesPresenter {
                return FavePagesPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getBoolean(Extra.USER),
                    saveInstanceState
                )
            }
        }
    }

    override fun onPageClick(index: Int, owner: Owner) {
        presenter?.fireOwnerClick(
            owner
        )
    }

    override fun onDelete(index: Int, owner: Owner) {
        presenter?.fireOwnerDelete(
            owner
        )
    }

    override fun onPushFirst(index: Int, owner: Owner) {
        presenter?.firePushFirst(
            owner
        )
    }

    override fun onMention(owner: Owner) {
        presenter?.fireMention(
            owner
        )
    }

    companion object {

        fun newInstance(accountId: Long, isUser: Boolean): FavePagesFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putBoolean(Extra.USER, isUser)
            val fragment = FavePagesFragment()
            fragment.arguments = args
            return fragment
        }
    }
}