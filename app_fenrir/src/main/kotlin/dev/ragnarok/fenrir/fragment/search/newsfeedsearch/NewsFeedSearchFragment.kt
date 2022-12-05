package dev.ragnarok.fenrir.fragment.search.newsfeedsearch

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.domain.ILikesInteractor
import dev.ragnarok.fenrir.fragment.abswall.WallAdapter
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchFragment
import dev.ragnarok.fenrir.fragment.search.criteria.NewsFeedCriteria
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.util.Utils.is600dp
import dev.ragnarok.fenrir.util.Utils.isLandscape

class NewsFeedSearchFragment :
    AbsSearchFragment<NewsFeedSearchPresenter, INewsFeedSearchView, Post, WallAdapter>(),
    WallAdapter.ClickListener, INewsFeedSearchView {
    override fun setAdapterData(adapter: WallAdapter, data: MutableList<Post>) {
        adapter.setItems(data)
    }

    override fun postCreate(root: View) {}
    override fun createAdapter(data: MutableList<Post>): WallAdapter {
        return WallAdapter(requireActivity(), data, this, this)
    }

    override fun createLayoutManager(): RecyclerView.LayoutManager {
        return if (is600dp(requireActivity())) {
            val land = isLandscape(requireActivity())
            StaggeredGridLayoutManager(if (land) 2 else 1, StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        }
    }

    override fun onAvatarClick(ownerId: Int) {
        presenter?.fireOwnerClick(
            ownerId
        )
    }

    override fun onShareClick(post: Post) {
        presenter?.fireShareClick(
            post
        )
    }

    override fun onPostClick(post: Post) {
        presenter?.firePostClick(
            post
        )
    }

    override fun onRestoreClick(post: Post) {
        // not supported
    }

    override fun onCommentsClick(post: Post) {
        presenter?.fireCommentsClick(
            post
        )
    }

    override fun onLikeLongClick(post: Post) {
        presenter?.fireCopiesLikesClick(
            "post",
            post.ownerId,
            post.vkid,
            ILikesInteractor.FILTER_LIKES
        )
    }

    override fun onShareLongClick(post: Post) {
        presenter?.fireCopiesLikesClick(
            "post",
            post.ownerId,
            post.vkid,
            ILikesInteractor.FILTER_COPIES
        )
    }

    override fun onLikeClick(post: Post) {
        presenter?.fireLikeClick(
            post
        )
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<NewsFeedSearchPresenter> {
        return object : IPresenterFactory<NewsFeedSearchPresenter> {
            override fun create(): NewsFeedSearchPresenter {
                return NewsFeedSearchPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.CRITERIA),
                    saveInstanceState
                )
            }
        }
    }

    companion object {

        fun newInstance(
            accountId: Int,
            initialCriteria: NewsFeedCriteria?
        ): NewsFeedSearchFragment {
            val args = Bundle()
            args.putParcelable(Extra.CRITERIA, initialCriteria)
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val fragment = NewsFeedSearchFragment()
            fragment.arguments = args
            return fragment
        }
    }
}