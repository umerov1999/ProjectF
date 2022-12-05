package dev.ragnarok.fenrir.fragment.fave.favearticles

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.Photo

interface IFaveArticlesView : IMvpView, IErrorView {
    fun displayData(articles: List<Article>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun goToArticle(accountId: Int, article: Article)
    fun goToPhoto(accountId: Int, photo: Photo)
}