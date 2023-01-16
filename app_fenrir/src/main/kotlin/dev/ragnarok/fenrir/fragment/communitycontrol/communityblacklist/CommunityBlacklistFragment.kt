package dev.ragnarok.fenrir.fragment.communitycontrol.communityblacklist

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.selectprofiles.SelectProfilesActivity.Companion.createIntent
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.communitycontrol.communityban.CommunityBannedAdapter
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.PeopleSearchCriteria
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.model.Banned
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.SelectProfileCriteria
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.place.PlaceFactory.getCommunityAddBanPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getCommunityBanEditPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleTabSearchPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class CommunityBlacklistFragment :
    BaseMvpFragment<CommunityBlacklistPresenter, ICommunityBlacklistView>(),
    ICommunityBlacklistView, CommunityBannedAdapter.ActionListener {
    private val requestSelectProfile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val users: ArrayList<Owner> = (result.data
                ?: return@registerForActivityResult).getParcelableArrayListExtraCompat(Extra.OWNERS)
                ?: return@registerForActivityResult
            lazyPresenter {
                fireAddToBanUsersSelected(users)
            }
        }
    }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: CommunityBannedAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_community_blacklist, container, false)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToBottom()
            }
        })
        mAdapter = CommunityBannedAdapter(emptyList())
        mAdapter?.setActionListener(this)
        recyclerView.adapter = mAdapter
        root.findViewById<View>(R.id.button_add).setOnClickListener {
            presenter?.fireAddClick()
        }
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunityBlacklistPresenter> {
        return object : IPresenterFactory<CommunityBlacklistPresenter> {
            override fun create(): CommunityBlacklistPresenter {
                return CommunityBlacklistPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.GROUP_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun displayRefreshing(loadingNow: Boolean) {
        if (mSwipeRefreshLayout != null) {
            (mSwipeRefreshLayout ?: return).isRefreshing = loadingNow
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            (mAdapter ?: return).notifyDataSetChanged()
        }
    }

    override fun diplayData(data: List<Banned>) {
        if (mAdapter != null) {
            (mAdapter ?: return).setData(data)
        }
    }

    override fun notifyItemRemoved(index: Int) {
        if (mAdapter != null) {
            (mAdapter ?: return).notifyItemRemoved(index)
        }
    }

    override fun openBanEditor(accountId: Long, groupId: Long, banned: Banned) {
        getCommunityBanEditPlace(accountId, groupId, banned).tryOpenWith(requireActivity())
    }

    override fun startSelectProfilesActivity(accountId: Long, groupId: Long) {
        val criteria = PeopleSearchCriteria("")
            .setGroupId(groupId)
        val c = SelectProfileCriteria()
        val place = getSingleTabSearchPlace(accountId, SearchContentType.PEOPLE, criteria)
        val intent = createIntent(requireActivity(), place, c)
        requestSelectProfile.launch(intent)
    }

    override fun addUsersToBan(accountId: Long, groupId: Long, users: ArrayList<User>) {
        getCommunityAddBanPlace(accountId, groupId, users).tryOpenWith(requireActivity())
    }

    override fun notifyItemsAdded(position: Int, size: Int) {
        mAdapter?.notifyItemRangeInserted(position, size)
    }

    override fun onBannedClick(banned: Banned) {
        presenter?.fireBannedClick(
            banned
        )
    }

    override fun onBannedLongClick(banned: Banned) {
        val items = arrayOf(getString(R.string.delete))
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(banned.banned.fullName)
            .setItems(items) { _: DialogInterface?, _: Int ->
                presenter?.fireBannedRemoveClick(
                    banned
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    companion object {

        fun newInstance(accountId: Long, groupdId: Long): CommunityBlacklistFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.GROUP_ID, groupdId)
            val fragment = CommunityBlacklistFragment()
            fragment.arguments = args
            return fragment
        }
    }
}