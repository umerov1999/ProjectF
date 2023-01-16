package dev.ragnarok.fenrir.fragment.ownerarticles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.EndlessRecyclerOnScrollListener
import dev.ragnarok.fenrir.listener.OnSectionResumeCallback
import dev.ragnarok.fenrir.listener.PicassoPauseOnScrollListener
import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.place.PlaceFactory.getExternalLinkPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSimpleGalleryPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme

class OwnerArticlesFragment : BaseMvpFragment<OwnerArticlesPresenter, IOwnerArticlesView>(),
    IOwnerArticlesView, SwipeRefreshLayout.OnRefreshListener, OwnerArticlesAdapter.ClickListener {
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: OwnerArticlesAdapter? = null
    private var mEmpty: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_owner_articles, container, false)
        val recyclerView: RecyclerView = root.findViewById(android.R.id.list)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mEmpty = root.findViewById(R.id.empty)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        recyclerView.addOnScrollListener(object : EndlessRecyclerOnScrollListener() {
            override fun onScrollToLastElement() {
                presenter?.fireScrollToEnd()
            }
        })
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.setOnRefreshListener(this)
        setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = OwnerArticlesAdapter(emptyList(), requireActivity())
        mAdapter?.setClickListener(this)
        recyclerView.adapter = mAdapter
        resolveEmptyTextVisibility()
        return root
    }

    override fun onRefresh() {
        presenter?.fireRefresh()
    }

    override fun displayData(articles: List<Article>) {
        if (mAdapter != null) {
            mAdapter?.setData(articles)
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter?.notifyDataSetChanged()
            resolveEmptyTextVisibility()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (mAdapter != null) {
            mAdapter?.notifyItemRangeInserted(position, count)
            resolveEmptyTextVisibility()
        }
    }

    private fun resolveEmptyTextVisibility() {
        if (mEmpty != null && mAdapter != null) {
            mEmpty?.visibility =
                if (mAdapter?.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout?.post { mSwipeRefreshLayout?.isRefreshing = refreshing }
    }

    override fun goToArticle(accountId: Long, article: Article) {
        article.uRL?.let {
            getExternalLinkPlace(
                accountId,
                it,
                article.ownerName,
                "article_" + article.ownerId + "_" + article.id
            ).tryOpenWith(requireActivity())
        }
    }

    override fun goToPhoto(accountId: Long, photo: Photo) {
        val temp = ArrayList(listOf(photo))
        getSimpleGalleryPlace(accountId, temp, 0, false).tryOpenWith(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<OwnerArticlesPresenter> {
        return object : IPresenterFactory<OwnerArticlesPresenter> {
            override fun create(): OwnerArticlesPresenter {
                return OwnerArticlesPresenter(
                    requireArguments().getLong(
                        Extra.ACCOUNT_ID
                    ), requireArguments().getLong(Extra.OWNER_ID), saveInstanceState
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.articles)
            actionBar.subtitle = null
        }
        if (requireActivity() is OnSectionResumeCallback) {
            (requireActivity() as OnSectionResumeCallback).onClearSelection()
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun onArticleClick(article: Article) {
        presenter?.fireArticleClick(
            article
        )
    }

    override fun onPhotosOpen(photo: Photo) {
        presenter?.firePhotoClick(
            photo
        )
    }

    override fun onDelete(index: Int, article: Article) {
        presenter?.fireArticleDelete(
            index,
            article
        )
    }

    override fun onAdd(index: Int, article: Article) {
        presenter?.fireArticleAdd(
            index,
            article
        )
    }

    override fun onShare(article: Article) {
        startForSendAttachments(requireActivity(), Settings.get().accounts().current, article)
    }

    companion object {
        fun newInstance(accountId: Long, ownerId: Long): OwnerArticlesFragment {
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, accountId)
            args.putLong(Extra.OWNER_ID, ownerId)
            val fragment = OwnerArticlesFragment()
            fragment.arguments = args
            return fragment
        }
    }
}