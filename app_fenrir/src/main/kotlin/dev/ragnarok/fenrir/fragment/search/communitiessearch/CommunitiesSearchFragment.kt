package dev.ragnarok.fenrir.fragment.search.communitiessearch

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.GroupSearchCriteria
import dev.ragnarok.fenrir.fragment.search.peoplesearch.PeopleAdapter
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace

class CommunitiesSearchFragment :
    AbsSearchFragment<CommunitiesSearchPresenter, ICommunitiesSearchView, Community, PeopleAdapter>(),
    ICommunitiesSearchView, PeopleAdapter.ClickListener {
    override fun setAdapterData(adapter: PeopleAdapter, data: MutableList<Community>) {
        adapter.setItems(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Community>): PeopleAdapter {
        val adapter = PeopleAdapter(requireActivity(), data)
        adapter.setClickListener(this)
        return adapter
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CommunitiesSearchPresenter> {
        return object : IPresenterFactory<CommunitiesSearchPresenter> {
            override fun create(): CommunitiesSearchPresenter {
                return CommunitiesSearchPresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.CRITERIA),
                    saveInstanceState
                )
            }
        }
    }

    override fun onOwnerClick(owner: Owner) {
        presenter?.fireCommunityClick(
            owner as Community
        )
    }

    override fun openCommunityWall(accountId: Long, community: Community) {
        getOwnerWallPlace(accountId, community).tryOpenWith(requireActivity())
    }

    companion object {

        fun newInstance(
            accountId: Long,
            initialCriteria: GroupSearchCriteria?
        ): CommunitiesSearchFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            val fragment = CommunitiesSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
