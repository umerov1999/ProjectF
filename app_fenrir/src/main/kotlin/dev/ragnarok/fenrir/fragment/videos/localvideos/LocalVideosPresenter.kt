package dev.ragnarok.fenrir.fragment.videos.localvideos

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.util.Objects.safeEquals
import dev.ragnarok.fenrir.util.Utils.countOfSelection
import dev.ragnarok.fenrir.util.Utils.getSelected
import java.util.Locale

class LocalVideosPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<ILocalVideosView>(savedInstanceState) {
    private val mLocalVideos: MutableList<LocalVideo>
    private val mLocalVideos_search: MutableList<LocalVideo>
    private var mLoadingNow = false
    private var q: String? = null
    private fun loadData() {
        if (mLoadingNow) return
        changeLoadingState(true)
        appendDisposable(Stores.instance
            .localMedia()
            .videos
            .fromIOToMain()
            .subscribe({ onDataLoaded(it) }) {
                onLoadError()
            })
    }

    fun fireSearchRequestChanged(q: String?, force: Boolean) {
        val query = q?.trim { it <= ' ' }
        if (!force && safeEquals(query, this.q)) {
            return
        }
        this.q = query
        mLocalVideos_search.clear()
        if (!this.q.isNullOrEmpty()) {
            for (i in mLocalVideos) {
                if (i.getTitle().isNullOrEmpty()) {
                    continue
                }
                if (this.q?.lowercase(Locale.getDefault())
                        ?.let { i.getTitle()?.lowercase(Locale.getDefault())?.contains(it) } == true
                ) {
                    mLocalVideos_search.add(i)
                }
            }
        }
        if (!this.q.isNullOrEmpty()) view?.displayData(
            mLocalVideos_search
        )
        else view?.displayData(mLocalVideos)
    }

    private fun onLoadError() {
        changeLoadingState(false)
    }

    private fun onDataLoaded(data: List<LocalVideo>) {
        changeLoadingState(false)
        mLocalVideos.clear()
        mLocalVideos.addAll(data)
        resolveListData()
        resolveEmptyTextVisibility()
        if (!q.isNullOrEmpty()) {
            fireSearchRequestChanged(q, true)
        }
    }

    override fun onGuiCreated(viewHost: ILocalVideosView) {
        super.onGuiCreated(viewHost)
        resolveListData()
        resolveProgressView()
        resolveFabVisibility(false)
        resolveEmptyTextVisibility()
    }

    private fun resolveEmptyTextVisibility() {
        view?.setEmptyTextVisible(
            mLocalVideos
                .isEmpty()
        )
    }

    private fun resolveListData() {
        if (q.isNullOrEmpty()) {
            view?.displayData(
                mLocalVideos
            )
        } else {
            view?.displayData(
                mLocalVideos_search
            )
        }
    }

    private fun changeLoadingState(loading: Boolean) {
        mLoadingNow = loading
        resolveProgressView()
    }

    private fun resolveProgressView() {
        view?.displayProgress(mLoadingNow)
    }

    fun fireFabClick() {
        val localVideos = getSelected(mLocalVideos)
        if (localVideos.isNotEmpty()) {
            view?.returnResultToParent(
                localVideos
            )
        } else {
            view?.showError(R.string.select_attachments)
        }
    }

    fun fireVideoClick(video: LocalVideo) {
        video.setSelected(!video.isSelected)
        if (video.isSelected) {
            val single = ArrayList<LocalVideo>(1)
            single.add(video)
            view?.returnResultToParent(
                single
            )
        }
    }

    private fun resolveFabVisibility(anim: Boolean) {
        resolveFabVisibility(countOfSelection(mLocalVideos) > 0, anim)
    }

    private fun resolveFabVisibility(visible: Boolean, anim: Boolean) {
        view?.setFabVisible(
            visible,
            anim
        )
    }

    fun fireRefresh() {
        loadData()
    }

    init {
        mLocalVideos = ArrayList()
        mLocalVideos_search = ArrayList()
        loadData()
    }
}