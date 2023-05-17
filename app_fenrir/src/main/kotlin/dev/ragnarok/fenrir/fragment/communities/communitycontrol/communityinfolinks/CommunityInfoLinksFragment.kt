package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communityinfolinks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class CommunityInfoLinksFragment :
    BaseMvpFragment<CommunityInfoLinksPresenter, ICommunityInfoLinksView>(),
    ICommunityInfoLinksView, CommunityInfoLinksAdapter.ActionListener {
    private var mLinksAdapter: CommunityInfoLinksAdapter? = null
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
        mLinksAdapter = CommunityInfoLinksAdapter(emptyList())
        mLinksAdapter?.setActionListener(this)
        recyclerView.adapter = mLinksAdapter
        root.findViewById<View>(R.id.button_add).visibility = View.INVISIBLE
        return root
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunityInfoLinksPresenter> {
        return object : IPresenterFactory<CommunityInfoLinksPresenter> {
            override fun create(): CommunityInfoLinksPresenter {
                return CommunityInfoLinksPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.GROUP_ID)!!,
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

    companion object {
        fun newInstance(accountId: Long, groupId: Community?): CommunityInfoLinksFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.GROUP_ID, groupId)
            val fragment = CommunityInfoLinksFragment()
            fragment.arguments = args
            return fragment
        }
    }
}