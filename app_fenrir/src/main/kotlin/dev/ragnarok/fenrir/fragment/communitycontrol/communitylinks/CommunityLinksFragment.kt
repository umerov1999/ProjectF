package dev.ragnarok.fenrir.fragment.communitycontrol.communitylinks

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class CommunityLinksFragment : BaseMvpFragment<CommunityLinksPresenter, ICommunityLinksView>(),
    ICommunityLinksView, CommunityLinksAdapter.ActionListener {
    private var mLinksAdapter: CommunityLinksAdapter? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_community_links, container, false)
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener {
            presenter?.fireRefresh()
        }
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        mLinksAdapter = CommunityLinksAdapter(emptyList())
        mLinksAdapter?.setActionListener(this)
        recyclerView.adapter = mLinksAdapter
        root.findViewById<View>(R.id.button_add).setOnClickListener {
            presenter?.fireButtonAddClick()
        }
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunityLinksPresenter> {
        return object : IPresenterFactory<CommunityLinksPresenter> {
            override fun create(): CommunityLinksPresenter {
                return CommunityLinksPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getLong(Extra.GROUP_ID),
                    saveInstanceState
                )
            }
        }
    }

    override fun displayRefreshing(loadingNow: Boolean) {
        mSwipeRefreshLayout?.isRefreshing = loadingNow
    }

    override fun notifyDataSetChanged() {
        mLinksAdapter?.notifyDataSetChanged()
    }

    override fun displayData(links: List<VKApiCommunity.Link>) {
        mLinksAdapter?.setData(links)
    }

    override fun openLink(link: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(intent)
    }

    override fun onClick(link: VKApiCommunity.Link) {
        presenter?.fireLinkClick(
            link
        )
    }

    override fun onLongClick(link: VKApiCommunity.Link) {
        val items = arrayOf(getString(R.string.edit), getString(R.string.delete))
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(link.name)
            .setItems(items) { _: DialogInterface?, which: Int ->
                when (which) {
                    0 -> presenter?.fireLinkEditClick()
                    1 -> presenter?.fireLinkDeleteClick()
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    companion object {
        fun newInstance(accountId: Long, groupId: Long): CommunityLinksFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.GROUP_ID, groupId)
            val fragment = CommunityLinksFragment()
            fragment.arguments = args
            return fragment
        }
    }
}