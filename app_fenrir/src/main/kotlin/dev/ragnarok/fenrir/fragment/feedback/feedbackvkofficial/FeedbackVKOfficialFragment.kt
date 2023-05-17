package dev.ragnarok.fenrir.fragment.feedback.feedbackvkofficial

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.FeedbackVKOfficial
import dev.ragnarok.fenrir.model.FeedbackVKOfficial.ActionMessage
import dev.ragnarok.fenrir.model.FeedbackVKOfficial.ActionURL
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getMessagesLookupPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.MessagesReplyItemCallback
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.view.navigation.AbsNavigationView

class FeedbackVKOfficialFragment :
    BaseMvpFragment<FeedbackVKOfficialPresenter, IFeedbackVKOfficialView>(),
    SwipeRefreshLayout.OnRefreshListener, IFeedbackVKOfficialView,
    FeedbackVKOfficialAdapter.ClickListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: FeedbackVKOfficialAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_feedback, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mEmpty = root.findViewById(R.id.fragment_feedback_empty_text)
        val manager: RecyclerView.LayoutManager = LinearLayoutManager(requireActivity())
        val recyclerView: RecyclerView = root.findViewById(R.id.recycleView)
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        ItemTouchHelper(MessagesReplyItemCallback { o: Int ->
            if (mAdapter?.checkPosition(o) == true) {
                val notification = mAdapter?.getByPosition(o)
                if (notification != null && !notification.hide_query.isNullOrEmpty()) {
                    presenter?.hideNotification(
                        o,
                        notification.hide_query
                    )
                } else {
                    CustomSnackbars.createCustomSnackbars(recyclerView)
                        ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                        ?.coloredSnack(R.string.error_hiding, Color.RED)
                        ?.show()
                }
            }
        }).attachToRecyclerView(recyclerView)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = FeedbackVKOfficialAdapter(null, requireActivity())
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

    override fun onResume() {
        super.onResume()
        Settings.get().ui().notifyPlaceResumed(Place.NOTIFICATIONS)
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.drawer_feedback)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onSectionResume(AbsNavigationView.SECTION_ITEM_FEEDBACK)
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun displayData(pages: FeedbackVKOfficialList) {
        if (mAdapter != null) {
            mAdapter?.setData(pages)
            resolveEmptyText()
        }
    }

    override fun notifyFirstListReceived() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyText()
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).readAllNotifications()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyText()
        }
    }

    override fun notifyItemRemoved(position: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRemoved(position)
            resolveEmptyText()
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = refreshing
    }

    override fun openOwnerWall(owner_id: Long) {
        getOwnerWallPlace(Settings.get().accounts().current, owner_id, null).tryOpenWith(
            requireActivity()
        )
    }

    override fun openAction(action: FeedbackVKOfficial.Action) {
        if (action.getActionType() == FeedbackVKOfficial.Action_Types.URL) {
            LinkHelper.openLinkInBrowser(requireActivity(), (action as ActionURL).getUrl())
        } else if (action.getActionType() == FeedbackVKOfficial.Action_Types.MESSAGE) {
            val msg = action as ActionMessage
            getMessagesLookupPlace(
                Settings.get().accounts().current,
                msg.getPeerId(),
                msg.getMessageId(),
                null
            ).tryOpenWith(requireActivity())
        }
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<FeedbackVKOfficialPresenter> {
        return object : IPresenterFactory<FeedbackVKOfficialPresenter> {
            override fun create(): FeedbackVKOfficialPresenter {
                return FeedbackVKOfficialPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    saveInstanceState
                )
            }
        }
    }

    companion object {
        fun newInstance(accountId: Long): FeedbackVKOfficialFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            val fragment = FeedbackVKOfficialFragment()
            fragment.arguments = args
            return fragment
        }
    }
}