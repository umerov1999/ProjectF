package dev.ragnarok.fenrir.fragment.groupchats

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.model.GroupChats
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.place.PlaceFactory.getChatPlace
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper
import dev.ragnarok.fenrir.view.LoadMoreFooterHelper.Companion.createFrom

class GroupChatsFragment : BaseMvpFragment<GroupChatsPresenter, IGroupChatsView>(),
    SwipeRefreshLayout.OnRefreshListener, IGroupChatsView, GroupChatsAdapter.ActionListener {
    private var mAdapter: GroupChatsAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var helper: LoadMoreFooterHelper? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_group_chats, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val manager: RecyclerView.LayoutManager = LinearLayoutManager(requireActivity())
        recyclerView.layoutManager = manager
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mAdapter = GroupChatsAdapter(requireActivity(), mutableListOf(), this)
        val footer = inflater.inflate(R.layout.footer_load_more, recyclerView, false)
        helper = createFrom(footer, object : LoadMoreFooterHelper.Callback {
            override fun onLoadMoreClick() {
                presenter?.fireLoadMoreClick()
            }
        })
        mAdapter?.addFooter(footer)
        recyclerView.adapter = mAdapter
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        return root
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        actionBar?.setTitle(R.string.group_chats)
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

    override fun displayData(chats: MutableList<GroupChats>) {
        mAdapter?.setItems(chats)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyDataAdd(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = refreshing }
    }

    override fun setupLoadMore(@LoadMoreState state: Int) {
        helper?.switchToState(state)
    }

    override fun goToChat(accountId: Int, chat_id: Int) {
        getChatPlace(accountId, accountId, Peer(Peer.fromChatId(chat_id))).tryOpenWith(
            requireActivity()
        )
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<GroupChatsPresenter> {
        return object : IPresenterFactory<GroupChatsPresenter> {
            override fun create(): GroupChatsPresenter {
                val accountId = requireArguments().getInt(Extra.ACCOUNT_ID)
                val groupId = requireArguments().getInt(Extra.GROUP_ID)
                return GroupChatsPresenter(accountId, groupId, saveInstanceState)
            }
        }
    }

    override fun onGroupChatsClick(chat: GroupChats) {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.enter_to_group_chat)
            .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                presenter?.fireGroupChatsClick(
                    chat
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        fun buildArgs(accountId: Int, groupId: Int): Bundle {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            args.putInt(Extra.GROUP_ID, groupId)
            return args
        }

        fun newInstance(args: Bundle?): GroupChatsFragment {
            val fragment = GroupChatsFragment()
            fragment.arguments = args
            return fragment
        }

        fun newInstance(accountId: Int, ownerId: Int): GroupChatsFragment {
            return newInstance(buildArgs(accountId, ownerId))
        }
    }
}