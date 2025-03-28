package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AbsModelType
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Block
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2RecommendationPlaylist
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Section
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class CatalogV2SectionPresenter(
    accountId: Long,
    private val section_id: String,
    savedInstanceState: Bundle?
) :
    AccountDependencyPresenter<ICatalogV2SectionView>(accountId, savedInstanceState) {
    private val pages: MutableList<AbsModel> = ArrayList()
    private val fInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    private var doAudioLoadTabs = false
    private var actualDataLoading = false
    private var actualBlockLoading = false
    private var nextFrom: String? = null
    private var listContentType: String? = null

    private fun resolveLoadMoreFooterView() {
        if (pages.nonNullNoEmpty() && nextFrom.isNullOrEmpty()) {
            view?.setupLoadMoreFooter(LoadMoreState.END_OF_LIST)
        } else if (actualDataLoading) {
            view?.setupLoadMoreFooter(LoadMoreState.LOADING)
        } else if (nextFrom.nonNullNoEmpty()) {
            view?.setupLoadMoreFooter(LoadMoreState.CAN_LOAD_MORE)
        } else {
            view?.setupLoadMoreFooter(LoadMoreState.END_OF_LIST)
        }
    }

    override fun onGuiCreated(viewHost: ICatalogV2SectionView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(pages)
        listContentType.nonNullNoEmpty {
            viewHost.updateLayoutManager(it)
        }
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doAudioLoadTabs = if (doAudioLoadTabs) {
            return
        } else {
            true
        }
        loadActualData()
    }

    fun fireSearchRequestSubmitted(q: String?) {
        if (q.isNullOrEmpty()) {
            return
        }
        view?.search(accountId, q)
    }

    fun getAudioPos(dt: MutableList<AbsModel>?, audio: Audio?): Int {
        val op = dt ?: pages
        if (op.isNotEmpty() && audio != null) {
            for ((pos, i) in op.withIndex()) {
                if (i.getModelType() == AbsModelType.MODEL_CATALOG_V2_BLOCK) {
                    val u = i as CatalogV2Block
                    if (u.items.isNullOrEmpty()) {
                        continue
                    }
                    val sd = getAudioPos(u.items, audio)
                    if (sd >= 0) {
                        i.setScroll()
                        view?.notifyDataChanged(
                            pos, 1
                        )
                        return pos
                    }
                    continue
                } else if (i.getModelType() == AbsModelType.MODEL_CATALOG_V2_RECOMMENDATION_PLAYLIST) {
                    val u = i as CatalogV2RecommendationPlaylist
                    if (u.audios.isNullOrEmpty()) {
                        continue
                    }
                    val sd = getAudioPos(u.getAudiosModels(), audio)
                    if (sd >= 0) {
                        i.setScroll()
                        view?.notifyDataChanged(
                            pos, 1
                        )
                        return pos
                    }
                    continue
                } else if (i.getModelType() != AbsModelType.MODEL_AUDIO) {
                    continue
                }
                val u = i as Audio
                if (u.id == audio.id && u.ownerId == audio.ownerId) {
                    u.isAnimationNow = true
                    if (dt == null) {
                        view?.notifyDataChanged(
                            pos, 1
                        )
                    }
                    return pos
                }
            }
        }
        return -1
    }

    fun changeActualBlockLoading(state: Boolean) {
        actualBlockLoading = state
        resolveRefreshingView()
    }

    private fun loadActualData() {
        actualDataLoading = true
        resolveRefreshingView()
        appendJob(
            fInteractor.getCatalogV2Section(accountId, section_id, null)
                .fromIOToMain({ data -> onActualDataReceived(false, data) }) { t ->
                    onActualDataGetError(
                        t
                    )
                })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(
            getCauseIfRuntime(t)
        )
        resolveRefreshingView()
    }

    fun onNext() {
        if (actualDataLoading || nextFrom.isNullOrEmpty()) {
            return
        }
        actualDataLoading = true
        resolveRefreshingView()
        appendJob(
            fInteractor.getCatalogV2Section(accountId, section_id, nextFrom)
                .fromIOToMain({ data -> onActualDataReceived(true, data) }) { t ->
                    onActualDataGetError(
                        t
                    )
                })
    }

    private fun onActualDataReceived(isNext: Boolean, data: CatalogV2Section) {
        if (!isNext) {
            pages.clear()
        }
        data.listContentType.nonNullNoEmpty {
            if (listContentType != it) {
                listContentType = it
                view?.updateLayoutManager(it)
            }
        }
        nextFrom = data.next_from
        actualDataLoading = false
        val sz = pages.size
        pages.addAll(data.blocks.orEmpty())
        if (!isNext) {
            view?.notifyDataSetChanged()
        } else {
            view?.notifyDataAdded(sz, data.blocks.orEmpty().size)
        }
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resolveLoadMoreFooterView()
        resumedView?.showRefreshing(
            actualBlockLoading
        )
    }

    fun onAdd(album: AudioPlaylist) {
        appendJob(
            fInteractor.followPlaylist(
                accountId,
                album.id,
                album.owner_id,
                album.access_key
            )
                .fromIOToMain({
                    view?.customToast?.showToast(
                        R.string.success
                    )
                }) {
                    showError(
                        it
                    )
                })
    }

    fun fireRefresh() {
        if (actualDataLoading || actualBlockLoading) {
            return
        }
        loadActualData()
    }
}
