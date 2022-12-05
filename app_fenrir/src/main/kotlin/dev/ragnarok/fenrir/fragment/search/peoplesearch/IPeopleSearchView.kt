package dev.ragnarok.fenrir.fragment.search.peoplesearch

import dev.ragnarok.fenrir.fragment.search.abssearch.IBaseSearchView
import dev.ragnarok.fenrir.model.User

interface IPeopleSearchView : IBaseSearchView<User> {
    fun openUserWall(accountId: Int, user: User)
}