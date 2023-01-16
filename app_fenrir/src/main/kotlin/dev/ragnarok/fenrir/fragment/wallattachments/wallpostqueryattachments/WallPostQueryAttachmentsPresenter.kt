package dev.ragnarok.fenrir.fragment.wallattachments.wallpostqueryattachments

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.intValueIn
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.*

class WallPostQueryAttachmentsPresenter(
    accountId: Long,
    private val owner_id: Long,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<IWallPostQueryAttachmentsView>(accountId, savedInstanceState) {
    private val mPost: ArrayList<Post> = ArrayList()
    private val fInteractor: IWallsRepository = walls
    private val actualDataDisposable = CompositeDisposable()
    private var loaded = 0
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    private var Query: String? = null
    override fun onGuiCreated(viewHost: IWallPostQueryAttachmentsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mPost)
        resolveToolbar()
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(fInteractor.getWallNoCache(
            accountId,
            owner_id,
            offset,
            100,
            WallCriteria.MODE_ALL
        )
            .fromIOToMain()
            .subscribe({ data ->
                onActualDataReceived(
                    offset,
                    data
                )
            }) { t -> onActualDataGetError(t) })
    }

    fun fireSearchRequestChanged(q: String?, only_insert: Boolean) {
        Query = q?.trim { it <= ' ' }
        if (only_insert) {
            return
        }
        actualDataDisposable.clear()
        actualDataLoading = false
        resolveRefreshingView()
        resumedView?.onSetLoadingStatus(
            0
        )
        fireRefresh()
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun doCompare(data: String?, str: List<String>): Boolean {
        data ?: return false
        for (i in str) {
            if (data.lowercase(Locale.getDefault()).contains(i)) {
                return true
            }
        }
        return false
    }

    private fun checkDocs(docs: ArrayList<Document>?, str: List<String>, ids: List<Long>): Boolean {
        if (docs.isNullOrEmpty()) {
            return false
        }
        for (i in docs) {
            if (doCompare(i.title, str) || doCompare(i.ext, str) || ids.contains(i.ownerId)) {
                return true
            }
        }
        return false
    }

    private fun checkPhotos(docs: ArrayList<Photo>?, str: List<String>, ids: List<Long>): Boolean {
        if (docs.isNullOrEmpty()) {
            return false
        }
        for (i in docs) {
            if (doCompare(i.text, str) || ids.contains(i.ownerId)) {
                return true
            }
        }
        return false
    }

    private fun checkVideos(docs: ArrayList<Video>?, str: List<String>, ids: List<Long>): Boolean {
        if (docs.isNullOrEmpty()) {
            return false
        }
        for (i in docs) {
            if (doCompare(i.title, str) || doCompare(i.description, str) || ids.contains(
                    i.ownerId
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun checkAlbums(
        docs: ArrayList<PhotoAlbum>?,
        str: List<String>,
        ids: List<Long>
    ): Boolean {
        if (docs.isNullOrEmpty()) {
            return false
        }
        for (i in docs) {
            if (doCompare(i.getTitle(), str) || doCompare(i.getDescription(), str) || ids.contains(
                    i.ownerId
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun checkLinks(docs: ArrayList<Link>?, str: List<String>): Boolean {
        if (docs.isNullOrEmpty()) {
            return false
        }
        for (i in docs) {
            if (doCompare(i.title, str) || doCompare(i.description, str) || doCompare(
                    i.caption, str
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun checkArticles(
        docs: ArrayList<Article>?,
        str: List<String>,
        ids: List<Long>
    ): Boolean {
        if (docs.isNullOrEmpty()) {
            return false
        }
        for (i in docs) {
            if (doCompare(i.title, str) || doCompare(i.subTitle, str) || ids.contains(
                    i.ownerId
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun checkPoll(docs: ArrayList<Poll>?, str: List<String>, ids: List<Long>): Boolean {
        if (docs.isNullOrEmpty()) {
            return false
        }
        for (i in docs) {
            if (doCompare(i.question ?: "", str) || ids.contains(i.ownerId)) {
                return true
            }
        }
        return false
    }

    private fun update(data: List<Post>, str: List<String>, ids: List<Long>) {
        for (i in data) {
            if (i.hasText() && doCompare(
                    i.text,
                    str
                ) || ids.contains(i.ownerId) || ids.contains(i.signerId) || ids.contains(i.authorId)
            ) {
                mPost.add(i)
            } else if (i.author != null && doCompare(i.author?.fullName, str)) {
                mPost.add(i)
            } else if (i.hasAttachments() && (checkDocs(i.attachments?.docs, str, ids)
                        || checkAlbums(
                    i.attachments?.photoAlbums,
                    str,
                    ids
                ) || checkArticles(i.attachments?.articles, str, ids)
                        || checkLinks(i.attachments?.links, str) || checkPhotos(
                    i.attachments?.photos,
                    str,
                    ids
                )
                        || checkVideos(
                    i.attachments?.videos,
                    str,
                    ids
                ) || checkPoll(i.attachments?.polls, str, ids))
            ) {
                mPost.add(i)
            }
            if (i.hasCopyHierarchy()) i.getCopyHierarchy()?.let { update(it, str, ids) }
        }
    }

    private fun onActualDataReceived(offset: Int, data: List<Post>) {
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (endOfContent) resumedView?.onSetLoadingStatus(
            2
        )
        val str = (Query ?: return).split(Regex("\\|")).toTypedArray()
        for (i in str.indices) {
            str[i] = str[i].trim { it <= ' ' }.lowercase(Locale.getDefault())
        }
        val ids: MutableList<Long> = ArrayList()
        for (cc in str) {
            if (cc.contains("*id")) {
                try {
                    ids.add(cc.replace("*id", "").toLong())
                } catch (ignored: NumberFormatException) {
                }
            }
        }
        if (offset == 0) {
            loaded = data.size
            mPost.clear()
            update(data, listOf(*str), ids)
            resolveToolbar()
            view?.notifyDataSetChanged()
        } else {
            val startSize = mPost.size
            loaded += data.size
            update(data, listOf(*str), ids)
            resolveToolbar()
            view?.notifyDataAdded(
                startSize,
                mPost.size - startSize
            )
        }
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.showRefreshing(
            actualDataLoading
        )
        if (!endOfContent) resumedView?.onSetLoadingStatus(
            if (actualDataLoading) 1 else 0
        )
    }

    private fun resolveToolbar() {
        view?.let {
            it.toolbarTitle(getString(R.string.attachments_in_wall))
            it.toolbarSubtitle(
                getString(
                    R.string.query,
                    safeCountOf(mPost)
                ) + " " + getString(R.string.posts_analized, loaded)
            )
        }
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    fun fireScrollToEnd(): Boolean {
        if (Query.isNullOrEmpty()) {
            return true
        }
        if (!endOfContent && actualDataReceived && !actualDataLoading) {
            loadActualData(loaded)
            return false
        }
        return true
    }

    fun fireRefresh() {
        if (Query.isNullOrEmpty()) {
            return
        }
        actualDataDisposable.clear()
        actualDataLoading = false
        loadActualData(0)
    }

    fun firePostBodyClick(post: Post) {
        if (intValueIn(post.postType, VKApiPost.Type.SUGGEST, VKApiPost.Type.POSTPONE)) {
            view?.openPostEditor(
                accountId,
                post
            )
            return
        }
        firePostClick(post)
    }

    fun firePostRestoreClick(post: Post) {
        appendDisposable(fInteractor.restore(accountId, post.ownerId, post.vkid)
            .fromIOToMain()
            .subscribe(dummy()) { t ->
                showError(t)
            })
    }

    fun fireLikeLongClick(post: Post) {
        view?.goToLikes(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireShareLongClick(post: Post) {
        view?.goToReposts(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireLikeClick(post: Post) {
        appendDisposable(fInteractor.like(accountId, post.ownerId, post.vkid, !post.isUserLikes)
            .fromIOToMain()
            .subscribe(ignore()) { t ->
                showError(t)
            })
    }

}