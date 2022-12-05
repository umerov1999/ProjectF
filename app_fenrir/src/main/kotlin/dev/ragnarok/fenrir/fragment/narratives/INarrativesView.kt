package dev.ragnarok.fenrir.fragment.narratives

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Narratives
import dev.ragnarok.fenrir.model.Story

interface INarrativesView : IMvpView, IErrorView {
    fun displayData(narratives: List<Narratives>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun onNarrativesOpen(accountId: Int, stories: ArrayList<Story>)
}
