package dev.ragnarok.fenrir.fragment.audio.catalog_v2.lists

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.model.AudioArtist
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2List
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_CATALOG
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_LOCAL_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_LOCAL_SERVER_AUDIO
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_PLAYLIST
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2SortListCategory.Companion.TYPE_RECOMMENDATIONS
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.settings.Settings
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CatalogV2ListPresenter(
    accountId: Long,
    private val owner_id: Long,
    private val artist_id: String?,
    private val query: String?,
    private val url: String?,
    context: Context,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<ICatalogV2ListView>(accountId, savedInstanceState) {
    private val audioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    private val mSections: ArrayList<CatalogV2List.CatalogV2ListItem> =
        ArrayList()
    private val netDisposable = CompositeDisposable()
    private var netLoadingNow = false

    private val resolv = HashMap<Int, String>()

    fun resolveLoading() {
        view?.resolveLoading(netLoadingNow)
    }

    fun fireRepost(context: Context) {
        if (artist_id.isNullOrEmpty()) {
            return
        }
        netDisposable.add(
            audioInteractor.getArtistById(accountId, artist_id).fromIOToMain()
                .subscribe({ artistInfo ->
                    artistInfo.id.ifNonNullNoEmpty({
                        startForSendAttachments(context, accountId, AudioArtist(it))
                    }, {
                        startForSendAttachments(context, accountId, AudioArtist(artist_id))
                    })
                }, {
                    startForSendAttachments(context, accountId, AudioArtist(artist_id))
                })
        )
    }

    override fun onDestroyed() {
        netDisposable.dispose()
        super.onDestroyed()
    }

    private fun request() {
        netLoadingNow = true
        resolveLoading()
        netDisposable.add(audioInteractor.getCatalogV2Sections(
            accountId,
            owner_id,
            artist_id,
            url,
            query,
            null
        )
            .fromIOToMain()
            .subscribe({ sections ->
                onNetDataReceived(
                    sections
                )
            }) { t -> onNetDataGetError(t) })
    }

    private fun onNetDataGetError(t: Throwable) {
        mSections.clear()
        val srt = Settings.get().other().catalogV2ListSort
        for (i in srt) {
            makeByUid(i, null)
        }
        view?.notifyDataSetChanged()
        netLoadingNow = false
        resolveLoading()
        showError(t)
    }

    private fun makeByUid(
        @CatalogV2SortListCategory n: Int,
        catalogSections: List<CatalogV2List.CatalogV2ListItem>?
    ) {
        if ((artist_id.nonNullNoEmpty() || query.nonNullNoEmpty() || url.nonNullNoEmpty()) && n != TYPE_CATALOG) {
            return
        }
        when (n) {
            TYPE_AUDIO -> {
                mSections.add(
                    CatalogV2List.CatalogV2ListItem(
                        TYPE_AUDIO,
                        resolv[R.string.my_saved].orEmpty()
                    )
                )
            }

            TYPE_CATALOG -> {
                if (catalogSections != null) {
                    mSections.addAll(catalogSections)
                }
            }

            TYPE_LOCAL_AUDIO -> {
                if (accountId == owner_id) {
                    mSections.add(
                        CatalogV2List.CatalogV2ListItem(
                            TYPE_LOCAL_AUDIO,
                            resolv[R.string.local_audios].orEmpty()
                        )
                    )
                }
            }

            TYPE_LOCAL_SERVER_AUDIO -> {
                if (accountId == owner_id && accountId >= 0 && Settings.get()
                        .other().localServer.enabled
                ) {
                    mSections.add(
                        CatalogV2List.CatalogV2ListItem(
                            TYPE_LOCAL_SERVER_AUDIO,
                            resolv[R.string.on_server].orEmpty()
                        )
                    )
                }
            }

            TYPE_PLAYLIST -> {
                mSections.add(
                    CatalogV2List.CatalogV2ListItem(
                        TYPE_PLAYLIST,
                        resolv[R.string.playlists].orEmpty()
                    )
                )
            }

            TYPE_RECOMMENDATIONS -> {
                mSections.add(
                    CatalogV2List.CatalogV2ListItem(
                        TYPE_RECOMMENDATIONS,
                        resolv[R.string.recommendation].orEmpty()
                    )
                )
            }
        }
    }

    private fun onNetDataReceived(data: CatalogV2List) {
        netLoadingNow = false
        resolveLoading()
        mSections.clear()
        val srt = Settings.get().other().catalogV2ListSort
        for (i in srt) {
            makeByUid(i, data.sections)
        }
        var pos = 0
        for (i in 0 until data.sections?.size.orZero()) {
            if (query.nonNullNoEmpty() && data.sections?.get(i)?.title.isNullOrEmpty()) {
                data.sections?.get(i)?.updateTitle(query)
            }
            if (data.sections?.get(i)?.id == data.default_section) {
                pos = i
                break
            }
        }
        view?.notifyDataSetChanged()
        if (srt[0] == TYPE_CATALOG) {
            view?.setSection(pos)
        }
    }

    override fun onGuiCreated(viewHost: ICatalogV2ListView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mSections)
        resolveLoading()
    }

    fun fireRefresh() {
        netDisposable.clear()
        netLoadingNow = false
        resolveLoading()
        request()
    }

    init {
        resolv[R.string.my_saved] = context.getString(R.string.my_saved)
        resolv[R.string.local_audios] = context.getString(R.string.local_audios)
        resolv[R.string.on_server] = context.getString(R.string.on_server)
        resolv[R.string.playlists] = context.getString(R.string.playlists)
        resolv[R.string.recommendation] = context.getString(R.string.recommendation)

        request()
    }
}
