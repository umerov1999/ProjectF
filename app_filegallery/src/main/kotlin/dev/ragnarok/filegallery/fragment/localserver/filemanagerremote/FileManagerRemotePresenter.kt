package dev.ragnarok.filegallery.fragment.localserver.filemanagerremote

import android.os.Bundle
import android.os.Parcelable
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.filegallery.Includes
import dev.ragnarok.filegallery.fragment.base.RxSupportPresenter
import dev.ragnarok.filegallery.fromIOToMain
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.FileRemote
import dev.ragnarok.filegallery.model.FileType
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.util.Objects.safeEquals
import java.util.Locale

class FileManagerRemotePresenter(
    savedInstanceState: Bundle?
) : RxSupportPresenter<IFileManagerRemoteView>(savedInstanceState) {
    private val fileList: ArrayList<FileRemote> = ArrayList()
    private val fileListSearch: ArrayList<FileRemote> = ArrayList()
    private var isLoading = false
    private val directoryScrollPositions = HashMap<String?, Parcelable>()
    private var q: String? = null
    private var path: ArrayList<String> = ArrayList()

    private inline fun <reified T> join(
        tokens: Iterable<T>?,
        delimiter: String?
    ): String? {
        if (tokens == null) {
            return null
        }
        val sb = StringBuilder()
        var firstTime = true
        for (token in tokens) {
            if (firstTime) {
                firstTime = false
            } else {
                sb.append(delimiter)
            }
            sb.append(token)
        }
        return sb.toString()
    }

    fun canRefresh(): Boolean {
        return q == null
    }

    private fun buildPath(): String? {
        return join(path, "/")
    }

    fun doSearch(str: String?) {
        if (isLoading) {
            return
        }
        val query = str?.trim { it <= ' ' }
        if (safeEquals(query, this.q)) {
            return
        }
        q = if (query.isNullOrEmpty()) {
            null
        } else {
            query
        }
        if (q == null) {
            fileListSearch.clear()
            view?.resolveEmptyText(fileList.isEmpty())
            view?.displayData(fileList)
            view?.updatePathString(buildPath())
        } else {
            fileListSearch.clear()
            for (i in fileList) {
                if (i.file_name.isNullOrEmpty()) {
                    continue
                }
                if (i.file_name?.lowercase(Locale.getDefault())
                        ?.contains(q?.lowercase(Locale.getDefault()).toString()) == true
                ) {
                    fileListSearch.add(i)
                }
            }
            view?.resolveEmptyText(fileListSearch.isEmpty())
            view?.displayData(fileListSearch)
            view?.updatePathString(q ?: return)
        }
    }

    override fun onGuiCreated(viewHost: IFileManagerRemoteView) {
        super.onGuiCreated(viewHost)
        (if (q == null) buildPath() else q)?.let { viewHost.updatePathString(it) }
        viewHost.displayData(if (q == null) fileList else fileListSearch)
        viewHost.updatePathString(buildPath())
        viewHost.resolveEmptyText(fileList.isEmpty())
        viewHost.resolveLoading(isLoading)
    }

    fun backupDirectoryScroll(scroll: Parcelable) {
        directoryScrollPositions[buildPath()] = scroll
    }

    fun loadUp() {
        if (q != null) {
            q = null
            view?.updatePathString(buildPath())
            view?.displayData(fileList)
            view?.resolveEmptyText(fileList.isEmpty())
            return
        }
        path.removeLast()
        view?.updatePathString(buildPath())
        loadFiles()
    }

    fun canLoadUp(): Boolean {
        if (q != null) {
            return true
        }
        return path.nonNullNoEmpty()
    }

    fun onClickFile(item: FileRemote) {
        if (item.type == FileType.photo) {
            val list = if (q == null) fileList else fileListSearch
            var index = 0
            var o = 0
            val mem = ParcelNative.create(ParcelFlags.NULL_LIST)
            for (i in list) {
                if (i.type != FileType.photo && i.type != FileType.video) {
                    continue
                }
                if (i.id == item.id && i.owner_Id == item.owner_Id) {
                    index = o
                }
                val photo = Photo()
                photo.setId(i.id)
                photo.setOwnerId(i.owner_Id)
                photo.setDate(i.modification_time)
                photo.setPhoto_url(i.url)
                photo.setPreview_url(i.preview_url)
                photo.setLocal(true)
                photo.setGif(
                    i.type == FileType.video || i.file_name.toString().endsWith("gif", true)
                )
                photo.setText(i.file_name)
                mem.writeParcelable(photo)
                o++
            }
            mem.writeFirstInt(o)
            view?.displayGalleryUnSafe(mem.nativePointer, index, false)
        } else if (item.type == FileType.video) {
            val v = Video()
            v.setId(item.id)
            v.setOwnerId(item.owner_Id)
            v.setDate(item.modification_time)
            v.setTitle(item.file_name)
            v.setLink(item.url)
            v.setDuration(item.size.toInt())
            view?.displayVideo(v)
        } else if (item.type == FileType.audio) {
            val list = if (q == null) fileList else fileListSearch
            var index = 0
            var o = 0
            val mAudios: ArrayList<Audio> = ArrayList()
            for (i in list) {
                if (i.type != FileType.audio) {
                    continue
                }
                if (i.id == item.id && i.owner_Id == item.owner_Id) {
                    index = o
                }
                val audio = Audio()
                audio.setId(i.id)
                audio.setOwnerId(i.owner_Id)
                audio.setUrl(i.url)
                audio.setThumb_image(i.preview_url)
                audio.setDuration(i.size.toInt())

                var TrackName: String =
                    i.file_name?.replace(".mp3", "") ?: ""
                val Artist: String
                val arr = TrackName.split(Regex(" - ")).toTypedArray()
                if (arr.size > 1) {
                    Artist = arr[0]
                    TrackName = TrackName.replace("$Artist - ", "")
                } else {
                    Artist = ""
                }
                audio.setIsLocal()
                audio.setArtist(Artist)
                audio.setTitle(TrackName)

                mAudios.add(audio)
                o++
            }
            view?.startPlayAudios(mAudios, index)
        }
    }

    fun goFolder(item: FileRemote) {
        if (isLoading) {
            return
        }
        if (q != null) {
            q = null
            view?.updatePathString(buildPath())
            view?.displayData(fileList)
            view?.resolveEmptyText(fileList.isEmpty())
        }
        item.file_name?.let { path.add(it) }
        view?.updatePathString(buildPath())
        loadFiles()
    }

    fun scrollTo(id: Int, ownerId: Long): Boolean {
        var ret = false
        val list = if (q == null) fileList else fileListSearch
        for (i in list.indices) {
            if (!ret && list[i].id == id && list[i].owner_Id == ownerId) {
                list[i].setSelected(true)
                view?.notifyItemChanged(i)
                view?.onScrollTo(i)
                ret = true
            } else {
                if (list[i].isSelected) {
                    list[i].setSelected(false)
                    view?.notifyItemChanged(i)
                }
            }
        }
        return ret
    }

    fun loadFiles() {
        if (isLoading) {
            return
        }
        isLoading = true
        view?.resolveEmptyText(false)
        view?.resolveLoading(isLoading)
        appendDisposable(
            Includes.networkInterfaces.localServerApi().fsGet(buildPath()).fromIOToMain()
                .subscribe({
                    fileList.clear()
                    fileList.addAll(it)
                    isLoading = false
                    view?.resolveEmptyText(fileList.isEmpty())
                    view?.resolveLoading(isLoading)
                    view?.notifyAllChanged()
                    directoryScrollPositions.remove(buildPath())?.let { scroll ->
                        view?.restoreScroll(scroll)
                    }
                }, {
                    view?.showThrowable(it)
                    isLoading = false
                    view?.resolveLoading(isLoading)
                })
        )
    }

    init {
        loadFiles()
    }
}
