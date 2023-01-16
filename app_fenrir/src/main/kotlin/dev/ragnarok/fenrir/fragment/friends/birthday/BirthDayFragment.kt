package dev.ragnarok.fenrir.fragment.friends.birthday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.model.BirthDay
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class BirthDayFragment : BaseMvpFragment<BirthDayPresenter, IBirthDayView>(),
    SwipeRefreshLayout.OnRefreshListener, IBirthDayView, BirthDayAdapter.ClickListener {
    private var mAdapter: BirthDayAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mlinear: LinearLayoutManager? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_birth_days, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        mlinear = LinearLayoutManager(requireActivity())
        recyclerView.layoutManager = mlinear
        mAdapter = BirthDayAdapter(requireActivity(), mutableListOf())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        return root
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(R.string.birthdays_title)
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(users: List<BirthDay>) {
        mAdapter?.setItems(users)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = refreshing }
    }

    override fun goToWall(accountId: Long, user: User) {
        PlaceFactory.getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity())
    }

    override fun moveTo(pos: Int) {
        mlinear?.scrollToPosition(pos)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<BirthDayPresenter> {
        return object : IPresenterFactory<BirthDayPresenter> {
            override fun create(): BirthDayPresenter {
                val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
                val ownerId = requireArguments().getLong(Extra.OWNER_ID)
                return BirthDayPresenter(accountId, ownerId, saveInstanceState)
            }
        }
    }

    companion object {
        fun buildArgs(accountId: Long, ownerId: Long): Bundle {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            return args
        }

        fun newInstance(args: Bundle?): BirthDayFragment {
            val fragment = BirthDayFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onUserClick(user: User) {
        presenter?.fireUserClick(user)
    }
}
