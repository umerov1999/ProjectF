package dev.ragnarok.filegallery.db.impl

import android.content.Context
import android.content.ContextWrapper
import dev.ragnarok.filegallery.db.interfaces.ISearchRequestHelperStorage
import dev.ragnarok.filegallery.db.interfaces.IStorages

class AppStorages(base: Context) : ContextWrapper(base), IStorages {
    private val searchQueries = SearchRequestHelperStorage(this)
    override fun searchQueriesStore(): ISearchRequestHelperStorage {
        return searchQueries
    }
}