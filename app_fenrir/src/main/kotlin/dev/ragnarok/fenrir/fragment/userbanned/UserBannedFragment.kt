package dev.ragnarok.fenrir.fragment.userbanned

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.selectprofiles.SelectProfilesActivity.Companion.createIntent
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.friends.friendstabs.FriendsTabsFragment
import dev.ragnarok.fenrir.fragment.search.peoplesearch.PeopleAdapter
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.SelectProfileCriteria
import dev.ragnarok.fenrir.place.PlaceFactory.getFriendsFollowersPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomToast

class UserBannedFragment : BaseMvpFragment<UserBannedPresenter, IUserBannedView>(),
    IUserBannedView, PeopleAdapter.LongClickListener {
    private val requestSelect = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val owners: ArrayList<Owner>? =
                result.data?.getParcelableArrayListExtraCompat(Extra.OWNERS)
            lazyPresenter {
                owners?.let { u -> fireOwnersSelected(u) }
            }
        }
    }
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mRecyclerView: RecyclerView? = null
    private var mPeopleAdapter: PeopleAdapter? = null
    private var mEmptyText: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_user_banned, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mRecyclerView = root.findViewById(R.id.recycler_view)
        mRecyclerView?.layoutManager = LinearLayoutManager(requireActivity())
        mRecyclerView?.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mPeopleAdapter = PeopleAdapter(requireActivity(), emptyList())
        mPeopleAdapter?.setLongClickListener(this)
        mPeopleAdapter?.setClickListener(object : PeopleAdapter.ClickListener {
            override fun onOwnerClick(owner: Owner) {
                presenter?.fireOwnerClick(owner)
            }
        })
        mRecyclerView?.adapter = mPeopleAdapter
        mEmptyText = root.findViewById(R.id.empty_text)
        root.findViewById<View>(R.id.button_add).setOnClickListener {
            presenter?.fireButtonAddClick()
        }
        resolveEmptyTextVisibility()
        return root
    }

    private fun resolveEmptyTextVisibility() {
        if (mPeopleAdapter != null && mEmptyText != null) {
            mEmptyText?.visibility =
                if ((mPeopleAdapter?.itemCount ?: 0) > 0) View.GONE else View.VISIBLE
        }
    }

    override fun displayOwnerList(owners: List<Owner>) {
        if (mPeopleAdapter != null) {
            mPeopleAdapter?.setItems(owners)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyItemsAdded(position: Int, count: Int) {
        if (mPeopleAdapter != null) {
            mPeopleAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataSetChanged() {
        if (mPeopleAdapter != null) {
            mPeopleAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyItemRemoved(position: Int) {
        if (mPeopleAdapter != null) {
            mPeopleAdapter?.notifyItemRemoved(position)
            resolveEmptyTextVisibility()
        }
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.user_blacklist_title)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun startUserSelection(accountId: Long) {
        val place = getFriendsFollowersPlace(
            accountId,
            accountId,
            FriendsTabsFragment.TAB_ALL_FRIENDS,
            null
        )
        val criteria = SelectProfileCriteria()
        val intent = createIntent(requireActivity(), place, criteria)
        requestSelect.launch(intent)
    }

    override fun showSuccessToast() {
        CustomToast.createCustomToast(requireActivity()).setDuration(Toast.LENGTH_SHORT)
            .showToastSuccessBottom(R.string.success)
    }

    override fun scrollToPosition(position: Int) {
        mRecyclerView?.smoothScrollToPosition(position)
    }

    override fun showOwnerProfile(accountId: Long, owner: Owner) {
        getOwnerWallPlace(accountId, owner).tryOpenWith(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<UserBannedPresenter> {
        return object : IPresenterFactory<UserBannedPresenter> {
            override fun create(): UserBannedPresenter {
                return UserBannedPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun onOwnerLongClick(owner: Owner): Boolean {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(owner.fullName)
            .setItems(arrayOf(getString(R.string.delete))) { _: DialogInterface?, _: Int ->
                presenter?.fireRemoveClick(owner)
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
        return true
    }

    companion object {
        fun newInstance(accountId: Long): UserBannedFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = UserBannedFragment()
            fragment.arguments = args
            return fragment
        }
    }
}