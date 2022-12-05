package dev.ragnarok.fenrir.fragment.messages.chatusersdomain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpBottomSheetDialogFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.model.AppChatUser
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.view.MySearchView

class ChatUsersDomainFragment :
    BaseMvpBottomSheetDialogFragment<ChatUsersDomainPresenter, IChatUsersDomainView>(),
    IChatUsersDomainView, ChatMembersListDomainAdapter.ActionListener {
    private var mAdapter: ChatMembersListDomainAdapter? = null
    private var listener: Listener? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = BottomSheetDialog(requireActivity(), theme)
        val behavior = dialog.behavior
        //behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root =
            inflater.inflate(R.layout.fragment_chat_users_domain, container, false) as ViewGroup
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        val mySearchView: MySearchView = root.findViewById(R.id.searchview)
        mySearchView.setRightButtonVisibility(false)
        mySearchView.setLeftIcon(R.drawable.magnify)
        mySearchView.setOnQueryTextListener(object : MySearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                presenter?.fireQuery(
                    query
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                presenter?.fireQuery(
                    newText
                )
                return false
            }
        })
        mySearchView.activateKeyboard()
        mAdapter = ChatMembersListDomainAdapter(requireActivity(), emptyList())
        mAdapter?.setActionListener(this)
        recyclerView.adapter = mAdapter
        return root
    }

    override fun displayData(users: List<AppChatUser>) {
        mAdapter?.setData(users)
    }

    override fun notifyItemRemoved(position: Int) {
        mAdapter?.notifyItemRemoved(position)
    }

    override fun notifyDataSetChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        mAdapter?.notifyItemRangeInserted(position, count)
    }

    override fun openUserWall(accountId: Int, user: Owner) {
        getOwnerWallPlace(accountId, user).tryOpenWith(requireActivity())
    }

    override fun addDomain(accountId: Int, user: Owner) {
        listener?.onSelected(user)
    }

    override fun displayRefreshing(refreshing: Boolean) {}
    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ChatUsersDomainPresenter> {
        return object : IPresenterFactory<ChatUsersDomainPresenter> {
            override fun create(): ChatUsersDomainPresenter {
                return ChatUsersDomainPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getInt(Extra.CHAT_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun onUserClick(user: AppChatUser) {
        presenter?.fireUserClick(
            user
        )
    }

    override fun onUserLongClick(user: AppChatUser): Boolean {
        presenter?.fireUserLongClick(
            user
        )
        return true
    }

    interface Listener {
        fun onSelected(user: Owner)
    }

    companion object {
        private fun buildArgs(accountId: Int, chatId: Int): Bundle {
            val args = Bundle()
            args.putInt(Extra.CHAT_ID, chatId)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            return args
        }

        fun newInstance(accountId: Int, chatId: Int, listener: Listener?): ChatUsersDomainFragment {
            val fragment = ChatUsersDomainFragment()
            fragment.listener = listener
            fragment.arguments = buildArgs(accountId, chatId)
            return fragment
        }
    }
}
