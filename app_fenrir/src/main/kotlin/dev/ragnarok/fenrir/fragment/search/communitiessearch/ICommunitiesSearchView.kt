package dev.ragnarok.fenrir.fragment.search.communitiessearch

import dev.ragnarok.fenrir.fragment.search.abssearch.IBaseSearchView
import dev.ragnarok.fenrir.model.Community

interface ICommunitiesSearchView : IBaseSearchView<Community> {
    fun openCommunityWall(accountId: Long, community: Community)
}