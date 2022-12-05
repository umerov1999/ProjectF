package dev.ragnarok.fenrir.fragment.videos

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.fragment.base.core.IToolbarView
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.upload.Upload

interface IVideosListView : IMvpView, IToolbarView, IErrorView, IToastView {
    fun displayData(data: List<Video>)
    fun notifyDataAdded(position: Int, count: Int)
    fun displayLoading(loading: Boolean)
    fun notifyDataSetChanged()
    fun notifyItemRemoved(position: Int)
    fun notifyItemChanged(position: Int)
    fun returnSelectionToParent(video: Video)
    fun showVideoPreview(accountId: Int, video: Video)
    fun notifyUploadItemsAdded(position: Int, count: Int)
    fun notifyUploadItemRemoved(position: Int)
    fun notifyUploadItemChanged(position: Int)
    fun notifyUploadProgressChanged(position: Int, progress: Int, smoothly: Boolean)
    fun setUploadDataVisible(visible: Boolean)
    fun startSelectUploadFileActivity(accountId: Int)
    fun requestReadExternalStoragePermission()
    fun displayUploads(data: List<Upload>)
    fun notifyUploadDataChanged()
    fun onUploaded(upload: Video)
    fun doVideoLongClick(accountId: Int, ownerId: Int, isMy: Boolean, position: Int, video: Video)
    fun displayShareDialog(accountId: Int, video: Video, canPostToMyWall: Boolean)
    fun showSuccessToast()

    companion object {
        const val ACTION_SELECT = "VideosFragment.ACTION_SELECT"
        const val ACTION_SHOW = "VideosFragment.ACTION_SHOW"
    }
}