package dev.ragnarok.fenrir.fragment.ownerarticles

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.Photo

interface IOwnerArticlesView : IMvpView, IErrorView {
    fun displayData(articles: List<Article>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun goToArticle(accountId: Long, article: Article)
    fun goToPhoto(accountId: Long, photo: Photo)
}