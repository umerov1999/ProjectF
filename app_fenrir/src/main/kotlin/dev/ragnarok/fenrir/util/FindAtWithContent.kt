package dev.ragnarok.fenrir.util

import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.nonNullNoEmpty
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class FindAtWithContent<T>(
    disposable: CompositeDisposable,
    visibleCount: Int,
    searchCount: Int
) {
    private val cached: MutableList<T>
    private val disposable: CompositeDisposable
    private val visibleCount: Int
    private val searchCount: Int
    private var q: String? = null
    private var ended = false
    private var needSearchInCache: Boolean
    private var offset = 0
    protected abstract fun search(offset: Int, count: Int): Single<List<T>>
    protected abstract fun onError(e: Throwable)
    protected abstract fun onResult(data: MutableList<T>)
    protected abstract fun updateLoading(loading: Boolean)
    protected abstract fun clean()
    protected abstract fun compare(data: T, q: String): Boolean
    protected abstract fun onReset(data: MutableList<T>, offset: Int, isEnd: Boolean)
    fun cancel() {
        if (q != null) {
            q = null
            onReset(cached, offset, ended)
        }
    }

    @JvmOverloads
    fun do_search(q: String? = this.q) {
        if (q.isNullOrEmpty()) {
            this.q = q
            return
        } else if (!q.equals(this.q, ignoreCase = true)) {
            needSearchInCache = true
            this.q = q
            clean()
        }
        if (needSearchInCache) {
            needSearchInCache = false
            val result: MutableList<T> = ArrayList()
            for (i in cached) {
                if (compare(i, q)) {
                    result.add(i)
                }
            }
            if (result.isNotEmpty()) {
                onResult(result)
            }
        }
        if (!ended) {
            updateLoading(true)
            progress(0)
        }
    }

    private fun progress(searched: Int) {
        disposable.add(
            search(offset, searchCount).fromIOToMain()
                .subscribe({
                    offset += searchCount
                    if (it.isEmpty()) {
                        ended = true
                        updateLoading(false)
                    } else {
                        cached.addAll(it)
                        val result: MutableList<T> = ArrayList()
                        for (i in it) {
                            if (compare(i, q ?: return@subscribe)) {
                                result.add(i)
                            }
                        }
                        if (result.isNotEmpty()) {
                            onResult(result)
                        }
                        if (searched + result.size >= visibleCount) {
                            updateLoading(false)
                        } else {
                            progress(searched + result.size)
                        }
                    }
                }, { e -> onError(e) })
        )
    }

    fun reset() {
        ended = false
        cached.clear()
        needSearchInCache = true
        val tmp = q
        q = null
        offset = 0
        do_search(tmp)
    }

    fun insertCache(data: List<T>, offset: Int) {
        if (data.isEmpty() || cached.isNotEmpty()) {
            return
        }
        this.offset = offset
        cached.addAll(data)
    }

    val isSearchMode: Boolean
        get() = q.nonNullNoEmpty()

    init {
        cached = ArrayList()
        this.disposable = disposable
        this.visibleCount = visibleCount
        this.searchCount = searchCount
        needSearchInCache = true
    }
}