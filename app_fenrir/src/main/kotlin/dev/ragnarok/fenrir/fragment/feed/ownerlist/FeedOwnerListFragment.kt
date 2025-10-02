package dev.ragnarok.fenrir.fragment.feed.ownerlist

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.fenrir.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.fenrir.activity.selectprofiles.SelectProfilesActivity.Companion.startFaveSelection
import dev.ragnarok.fenrir.db.model.entity.FeedOwnersEntity
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.getParcelableArrayListExtraCompat
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.InputTextDialog
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class FeedOwnerListFragment :
    BaseMvpFragment<FeedOwnerListPresenter, IFeedOwnerListView>(),
    IFeedOwnerListView, FeedOwnerListAdapter.ClickListener {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: FeedOwnerListAdapter? = null
    private var mAdd: FloatingActionButton? = null

    private val requestProfileSelect = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val owners: ArrayList<Owner>? =
                result.data?.getParcelableArrayListExtraCompat(Extra.OWNERS)
            lazyPresenter {
                presenter?.fireAddToFaveOwnerList(requireActivity(), owners)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_feed_owner_list, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val manager = LinearLayoutManager(requireActivity())
        recyclerView.layoutManager = manager
        mAdapter = FeedOwnerListAdapter(emptyList(), requireActivity())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter

        mAdd = root.findViewById(R.id.add_button)
        mAdd?.setOnClickListener {
            requestProfileSelect.launch(startFaveSelection(requireActivity()))
        }
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): FeedOwnerListPresenter {
        val accountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        return FeedOwnerListPresenter(accountId, saveInstanceState)
    }

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.PREFERENCES)
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_SETTINGS)
        }
        setToolbarTitle(this, R.string.feed_owner_list)
        setToolbarSubtitle(this, null)
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayData(data: List<FeedOwnersEntity>) {
        mAdapter?.setData(data)
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun notifyDataRemoved(position: Int, count: Int) {
        mAdapter?.notifyItemRangeRemoved(position, count)
    }

    override fun notifyDataChanged(position: Int, count: Int) {
        mAdapter?.notifyItemRangeChanged(position, count)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun showLoading(loading: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loading
    }

    override fun feedOwnerListOpen(accountId: Long, listId: Long) {
        PlaceFactory.getFeedOwnersPlace(accountId, listId).tryOpenWith(requireActivity())
    }

    override fun onFeedOwnerListClick(owner: FeedOwnersEntity) {
        presenter?.fireFeedOwnerListClick(owner.id)
    }

    override fun onFeedOwnerListDelete(index: Int, owner: FeedOwnersEntity) {
        CustomSnackbars.createCustomSnackbars(view)
            ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)?.themedSnack(R.string.do_delete)
            ?.setAction(
                R.string.button_yes
            ) {
                presenter?.fireFeedOwnerListDelete(index, owner.id)
            }?.show()
    }

    override fun onFeedOwnerListRename(index: Int, owner: FeedOwnersEntity) {
        InputTextDialog.Builder(requireActivity())
            .setTitleRes(R.string.set_news_list_title)
            .setAllowEmpty(false)
            .setInputType(InputType.TYPE_CLASS_TEXT)
            .setCallback(object : InputTextDialog.Callback {
                override fun onChanged(newValue: String?) {
                    presenter?.fireFeedOwnerListRename(index, owner.id, newValue)
                }

                override fun onCanceled() {

                }
            })
            .show()
    }

    companion object {
        fun newInstance(accountId: Long): FeedOwnerListFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = FeedOwnerListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
