package dev.ragnarok.fenrir.fragment.docs

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.DocFilter
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.upload.Upload

interface IDocListView : IMvpView, IErrorView {
    fun displayData(documents: MutableList<Document>, asImages: Boolean)
    fun showRefreshing(refreshing: Boolean)
    fun notifyDataSetChanged()
    fun notifyDataAdd(position: Int, count: Int)
    fun notifyDataRemoved(position: Int)
    fun openDocument(accountId: Int, document: Document)
    fun returnSelection(docs: ArrayList<Document>)
    fun goToGifPlayer(accountId: Int, gifs: ArrayList<Document>, selected: Int)
    fun requestReadExternalStoragePermission()
    fun startSelectUploadFileActivity(accountId: Int)
    fun setUploadDataVisible(visible: Boolean)
    fun displayUploads(data: List<Upload>)
    fun notifyUploadItemsAdded(position: Int, count: Int)
    fun notifyUploadItemChanged(position: Int)
    fun notifyUploadItemRemoved(position: Int)
    fun notifyUploadProgressChanged(position: Int, progress: Int, smoothly: Boolean)
    fun displayFilterData(filters: MutableList<DocFilter>)
    fun notifyFiltersChanged()
    fun setAdapterType(imagesOnly: Boolean)
}