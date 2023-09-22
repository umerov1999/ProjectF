package dev.ragnarok.fenrir.fragment.audio.audioplaylists

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.FindAtWithContent
import dev.ragnarok.fenrir.util.Utils.SafeCallCheckInt
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.isValueAssigned
import dev.ragnarok.fenrir.util.Utils.safeCheck
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.Locale
import java.util.concurrent.TimeUnit

class AudioPlaylistsPresenter(accountId: Long, val owner_id: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IAudioPlaylistsView>(accountId, savedInstanceState) {
    private val addon: MutableList<AudioPlaylist>
    private val playlists: MutableList<AudioPlaylist>
    private val fInteractor: IAudioInteractor
    private val searcher: FindPlaylist
    private var actualDataDisposable = Disposable.disposed()
    private var pending_to_add: AudioPlaylist? = null
    private var Foffset = 0
    private var endOfContent = false
    private var actualDataLoading = false
    private var doAudioLoadTabs = false
    private var actualDataReceived = false
    override fun onGuiCreated(viewHost: IAudioPlaylistsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(playlists)
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        appendDisposable(fInteractor.getPlaylists(accountId, owner_id, offset, GET_COUNT)
            .fromIOToMain()
            .subscribe({ data ->
                onActualDataReceived(
                    offset,
                    data
                )
            }) { t -> onActualDataGetError(t) })
    }

    internal fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(
            getCauseIfRuntime(t)
        )
        resolveRefreshingView()
    }

    private fun onActualDataReceived(offset: Int, data: List<AudioPlaylist>) {
        actualDataReceived = true
        Foffset = offset + GET_COUNT
        actualDataLoading = false
        endOfContent = data.isEmpty()
        if (offset == 0) {
            playlists.clear()
            playlists.addAll(addon)
            playlists.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val startSize = playlists.size
            playlists.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doAudioLoadTabs = if (doAudioLoadTabs) {
            return
        } else {
            true
        }
        resumedView?.showHelper()
        if (accountId == owner_id) {
            val ids = Settings.get().main().servicePlaylist
            if (ids.nonNullNoEmpty()) {
                actualDataLoading = true
                resolveRefreshingView()
                if (ids.size == 1) {
                    appendDisposable(fInteractor.getPlaylistById(
                        accountId,
                        ids[0],
                        owner_id,
                        null
                    )
                        .fromIOToMain()
                        .subscribe({
                            addon.clear()
                            addon.add(it)
                            loadActualData(0)
                        }) { loadActualData(0) })
                } else {
                    val code = StringBuilder()
                    val code_addon = StringBuilder("return [")
                    var code_first = true
                    for ((tick, i) in ids.withIndex()) {
                        code.append("var playlist_").append(tick)
                            .append(" = API.audio.getPlaylistById({\"v\":\"" + Constants.API_VERSION + "\", \"owner_id\":")
                            .append(owner_id).append(", \"playlist_id\": ").append(i).append("});")
                        if (code_first) {
                            code_first = false
                        } else {
                            code_addon.append(", ")
                        }
                        code_addon.append("playlist_").append(tick)
                    }
                    code_addon.append("];")
                    code.append(code_addon)
                    appendDisposable(fInteractor.getPlaylistsCustom(accountId, code.toString())
                        .fromIOToMain()
                        .subscribe({
                            addon.clear()
                            addon.addAll(it)
                            loadActualData(0)
                        }) { loadActualData(0) })
                }
            } else {
                loadActualData(0)
            }
        } else {
            loadActualData(0)
        }
    }

    internal fun resolveRefreshingView() {
        resumedView?.showRefreshing(
            actualDataLoading
        )
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    fun fireScrollToEnd(): Boolean {
        if (playlists.nonNullNoEmpty() && actualDataReceived && !actualDataLoading) {
            if (searcher.isSearchMode) {
                searcher.do_search()
            } else if (!endOfContent) {
                loadActualData(Foffset)
            }
            return false
        }
        return true
    }

    private fun sleep_search(q: String?) {
        if (actualDataLoading) return
        actualDataDisposable.dispose()
        if (q.isNullOrEmpty()) {
            searcher.cancel()
        } else {
            actualDataDisposable = Single.just(Any())
                .delay(WEB_SEARCH_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .fromIOToMain()
                .subscribe({ searcher.do_search(q) }) { t ->
                    onActualDataGetError(
                        t
                    )
                }
        }
    }

    fun fireSearchRequestChanged(q: String?) {
        sleep_search(q?.trim { it <= ' ' })
    }

    fun onDelete(index: Int, album: AudioPlaylist) {
        appendDisposable(fInteractor.deletePlaylist(accountId, album.getId(), album.getOwnerId())
            .fromIOToMain()
            .subscribe({
                playlists.removeAt(index)
                view?.notifyItemRemoved(
                    index
                )
                view?.customToast?.showToast(
                    R.string.success
                )
            }) { throwable ->
                showError(
                    throwable
                )
            })
    }

    fun onEdit(context: Context, album: AudioPlaylist) {
        val root = View.inflate(context, R.layout.entry_playlist_info, null)
        (root.findViewById<View>(R.id.edit_title) as TextInputEditText).setText(album.getTitle())
        (root.findViewById<View>(R.id.edit_description) as TextInputEditText).setText(album.getDescription())
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.edit)
            .setCancelable(true)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                appendDisposable(fInteractor.editPlaylist(
                    accountId, album.getOwnerId(), album.getId(),
                    (root.findViewById<View>(R.id.edit_title) as TextInputEditText).text.toString(),
                    (root.findViewById<View>(R.id.edit_description) as TextInputEditText).text.toString()
                ).fromIOToMain()
                    .subscribe({ fireRefresh() }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun doInsertPlaylist(playlist: AudioPlaylist) {
        val offset = addon.size
        playlists.add(offset, playlist)
        view?.notifyDataAdded(
            offset,
            1
        )
    }

    fun fireCreatePlaylist(context: Context) {
        val root = View.inflate(context, R.layout.entry_playlist_info, null)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.create_playlist)
            .setCancelable(true)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                appendDisposable(fInteractor.createPlaylist(
                    accountId, owner_id,
                    (root.findViewById<View>(R.id.edit_title) as TextInputEditText).text.toString(),
                    (root.findViewById<View>(R.id.edit_description) as TextInputEditText).text.toString()
                ).fromIOToMain()
                    .subscribe({ playlist -> doInsertPlaylist(playlist) }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    fun onAdd(album: AudioPlaylist, clone: Boolean) {
        appendDisposable((if (clone) fInteractor.clonePlaylist(
            accountId,
            album.getId(),
            album.getOwnerId()
        ) else fInteractor.followPlaylist(
            accountId,
            album.getId(),
            album.getOwnerId(),
            album.getAccess_key()
        ))
            .fromIOToMain()
            .subscribe({
                view?.customToast?.showToast(
                    R.string.success
                )
                if (clone && isValueAssigned(
                        album.getId(),
                        Settings.get().main().servicePlaylist
                    )
                ) {
                    fireRefresh()
                }
            }) { throwable ->
                showError(throwable)
            })
    }

    fun fireAudiosSelected(audios: List<Audio>) {
        val targets: MutableList<AccessIdPair> = ArrayList(audios.size)
        for (i in audios) {
            targets.add(AccessIdPair(i.id, i.ownerId, i.accessKey))
        }
        pending_to_add?.let { o ->
            appendDisposable(fInteractor.addToPlaylist(
                accountId,
                o.getOwnerId(),
                o.getId(),
                targets
            )
                .fromIOToMain()
                .subscribe({
                    view?.customToast?.showToast(
                        R.string.success
                    )
                }) { throwable ->
                    showError(
                        throwable
                    )
                })
        }
        pending_to_add = null
    }

    fun onPlaceToPending(album: AudioPlaylist) {
        pending_to_add = album
        view?.doAddAudios(accountId)
    }

    fun fireRefresh() {
        if (actualDataLoading) {
            return
        }
        if (searcher.isSearchMode) {
            searcher.reset()
        } else {
            loadActualData(0)
        }
    }

    private inner class FindPlaylist(disposable: CompositeDisposable) :
        FindAtWithContent<AudioPlaylist>(
            disposable, SEARCH_VIEW_COUNT, SEARCH_COUNT
        ) {
        override fun search(offset: Int, count: Int): Single<List<AudioPlaylist>> {
            return fInteractor.getPlaylists(accountId, owner_id, offset, count)
        }

        override fun onError(e: Throwable) {
            onActualDataGetError(e)
        }

        override fun onResult(data: MutableList<AudioPlaylist>) {
            actualDataReceived = true
            val startSize = playlists.size
            playlists.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }

        override fun updateLoading(loading: Boolean) {
            actualDataLoading = loading
            resolveRefreshingView()
        }

        override fun clean() {
            playlists.clear()
            view?.notifyDataSetChanged()
        }

        override fun compare(data: AudioPlaylist, q: String): Boolean {
            return (safeCheck(
                data.getTitle(),
                object : SafeCallCheckInt {
                    override fun check(): Boolean {
                        return data.getTitle()?.lowercase(Locale.getDefault())?.contains(
                            q.lowercase(
                                Locale.getDefault()
                            )
                        ) == true
                    }
                })
                    || safeCheck(
                data.getArtist_name(),
                object : SafeCallCheckInt {
                    override fun check(): Boolean {
                        return data.getArtist_name()?.lowercase(Locale.getDefault())?.contains(
                            q.lowercase(
                                Locale.getDefault()
                            )
                        ) == true
                    }
                })
                    || safeCheck(
                data.getDescription(),
                object : SafeCallCheckInt {
                    override fun check(): Boolean {
                        return data.getDescription()?.lowercase(Locale.getDefault())?.contains(
                            q.lowercase(
                                Locale.getDefault()
                            )
                        ) == true
                    }
                }))
        }

        override fun onReset(
            data: MutableList<AudioPlaylist>,
            offset: Int,
            isEnd: Boolean
        ) {
            if (playlists.isEmpty()) {
                fireRefresh()
            } else {
                Foffset = offset
                endOfContent = isEnd
                playlists.clear()
                playlists.addAll(addon)
                playlists.addAll(data)
                view?.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val SEARCH_COUNT = 50
        private const val SEARCH_VIEW_COUNT = 10
        private const val GET_COUNT = 50
        private const val WEB_SEARCH_DELAY = 1000
    }

    init {
        playlists = ArrayList()
        addon = ArrayList()
        fInteractor = InteractorFactory.createAudioInteractor()
        searcher = FindPlaylist(compositeDisposable)
    }
}