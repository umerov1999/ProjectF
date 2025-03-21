package dev.ragnarok.filegallery.fragment.filemanager

import android.annotation.SuppressLint
import android.os.Environment
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager_SavedState
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.filegallery.Includes
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.fragment.base.RxSupportPresenter
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.FileType
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.model.tags.TagOwner
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.upload.IUploadManager
import dev.ragnarok.filegallery.upload.Upload
import dev.ragnarok.filegallery.upload.UploadDestination
import dev.ragnarok.filegallery.upload.UploadDestination.Companion.forRemotePlay
import dev.ragnarok.filegallery.upload.UploadIntent
import dev.ragnarok.filegallery.upload.UploadResult
import dev.ragnarok.filegallery.util.Objects.safeEquals
import dev.ragnarok.filegallery.util.Pair
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.sharedFlowToMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import java.io.File
import java.io.FilenameFilter
import java.util.Locale

class FileManagerPresenter(
    private var path: File,
    private val base: Boolean
) : RxSupportPresenter<IFileManagerView>() {
    private val fileList: ArrayList<FileItem> = ArrayList()
    private val fileListSearch: ArrayList<FileItem> = ArrayList()
    private var isLoading = false
    private val basePath = path.absolutePath
    private val directoryScrollPositions = HashMap<String, Parcelable>()
    private val remotePlay: UploadDestination = forRemotePlay()
    private val uploadManager: IUploadManager = Includes.uploadManager

    private var selectedOwner: TagOwner? = null
    private val uploadsData: MutableList<Upload> = ArrayList(0)

    fun setSelectedOwner(selectedOwner: TagOwner?) {
        this.selectedOwner = selectedOwner
        view?.updateSelectedMode(selectedOwner != null)
    }

    private val filter: FilenameFilter = FilenameFilter { dir, filename ->
        val sel = File(dir, filename)
        if (sel.absolutePath == File(
                Environment.getExternalStorageDirectory(),
                "Android"
            ).absolutePath
        ) {
            return@FilenameFilter false
        }
        var ret = !sel.isHidden && sel.canRead() && (sel.isDirectory && (sel.list()?.size
            ?: 0) > 0 || !sel.isDirectory)
        if (!sel.isDirectory && ret) {
            ret = false
            for (i in Settings.get().main().photoExt) {
                if (sel.extension.contains(i, true)) {
                    ret = true
                    break
                }
            }
            if (!ret) {
                for (i in Settings.get().main().audioExt) {
                    if (sel.extension.contains(i, true)) {
                        ret = true
                        break
                    }
                }
            }
            if (!ret) {
                for (i in Settings.get().main().videoExt) {
                    if (sel.extension.contains(i, true)) {
                        ret = true
                        break
                    }
                }
            }
        }
        ret
    }
    private var q: String? = null

    fun canRefresh(): Boolean {
        return q == null
    }

    fun doSearch(str: String?, global: Boolean) {
        if (isLoading) {
            return
        }
        val query = str?.trim()
        if (safeEquals(query, this.q) && !global) {
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
            view?.updatePathString(path.absolutePath)
        } else {
            fileListSearch.clear()
            if (!global) {
                for (i in fileList) {
                    if (i.file_name.isNullOrEmpty()) {
                        continue
                    }
                    if (i.file_name.lowercase(Locale.getDefault())
                            .contains(q?.lowercase(Locale.getDefault()).toString())
                    ) {
                        fileListSearch.add(i)
                    }
                }
                view?.resolveEmptyText(fileListSearch.isEmpty())
                view?.displayData(fileListSearch)
                view?.updatePathString(q ?: return)
            } else {
                isLoading = true
                view?.resolveEmptyText(false)
                view?.resolveLoading(isLoading)
                appendJob(rxSearchFiles().fromIOToMain({
                    fileListSearch.clear()
                    fileListSearch.addAll(it)
                    isLoading = false
                    view?.resolveEmptyText(fileListSearch.isEmpty())
                    view?.resolveLoading(isLoading)
                    view?.displayData(fileListSearch)
                    view?.updatePathString(q ?: return@fromIOToMain)
                }, {
                    view?.showThrowable(it)
                    isLoading = false
                    view?.resolveLoading(isLoading)
                }))
            }
        }
    }

    override fun onGuiCreated(viewHost: IFileManagerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(if (q == null) fileList else fileListSearch)
        (if (q == null) path.absolutePath else q)?.let { viewHost.updatePathString(it) }
        viewHost.resolveEmptyText(if (q == null) fileList.isEmpty() else fileListSearch.isEmpty())
        viewHost.resolveLoading(isLoading)
        viewHost.updateSelectedMode(selectedOwner != null)
        viewHost.displayUploads(uploadsData)
        resolveUploadDataVisibility()
    }

    private fun resolveUploadDataVisibility() {
        view?.setUploadDataVisible(uploadsData.isNotEmpty())
    }

    private fun onUploadsDataReceived(data: List<Upload>) {
        uploadsData.clear()
        uploadsData.addAll(data)
        resolveUploadDataVisibility()
    }

    private fun onUploadResults(pair: Pair<Upload, UploadResult<*>>) {
        val obj = pair.second.result as Audio
        if (obj.id == 0) {
            view?.showError(R.string.error)
        } else {
            view?.showMessage(R.string.success)
        }
    }

    private fun findIndexById(data: List<Upload>?, id: Int): Int {
        data ?: return -1
        for (i in data.indices) {
            if (data[i].id == id) {
                return i
            }
        }
        return -1
    }

    private fun onProgressUpdates(update: IUploadManager.IProgressUpdate?) {
        update?.let {
            val index = findIndexById(uploadsData, it.id)
            if (index != -1) {
                view?.notifyUploadProgressChanged(
                    index,
                    it.progress,
                    true
                )
            }
        }
    }

    private fun onUploadStatusUpdate(upload: Upload) {
        val index = findIndexById(uploadsData, upload.id)
        if (index != -1) {
            view?.notifyUploadItemChanged(
                index
            )
        }
    }

    private fun onUploadsAdded(added: List<Upload>) {
        var count = 0
        val cur = uploadsData.size
        for (u in added) {
            if (remotePlay.compareTo(u.destination)) {
                val index = findIndexById(uploadsData, u.id)
                if (index == -1) {
                    uploadsData.add(u)
                    count++
                }
            }
        }
        if (count > 0) {
            view?.notifyUploadItemsAdded(cur, count)
        }
        resolveUploadDataVisibility()
    }

    private fun onUploadDeleted(ids: IntArray) {
        for (id in ids) {
            val index = findIndexById(uploadsData, id)
            if (index != -1) {
                uploadsData.removeAt(index)
                view?.notifyUploadItemRemoved(
                    index
                )
            }
        }
        resolveUploadDataVisibility()
    }

    fun fireRemoveClick(upload: Upload) {
        uploadManager.cancel(upload.id)
    }

    private class ItemModificationComparator : Comparator<FileItem> {
        override fun compare(lhs: FileItem, rhs: FileItem): Int {
            return rhs.modification.compareTo(lhs.modification)
        }
    }

    fun backupDirectoryScroll(scroll: Parcelable) {
        directoryScrollPositions[path.absolutePath] = scroll
    }

    fun loadUp() {
        if (isLoading) {
            if (Settings.get().main().isOpen_folder_new_window && q == null) {
                val parent = path.parentFile
                if (parent != null && parent.canRead()) {
                    view?.onBusy(parent.absolutePath)
                }
            }
            return
        }
        if (q != null) {
            q = null
            view?.updatePathString(path.absolutePath)
            view?.displayData(fileList)
            view?.resolveEmptyText(fileList.isEmpty())
            return
        }
        val parent = path.parentFile
        if (parent != null && parent.canRead()) {
            path = parent
            view?.updatePathString(path.absolutePath)
            loadFiles(back = true, caches = true, fromCache = false)
        }
    }

    fun setCurrent(file: File) {
        if (isLoading) {
            if (Settings.get().main().isOpen_folder_new_window && file.canRead()) {
                view?.onBusy(file.absolutePath)
            }
            return
        }
        if (file.canRead()) {
            path = file
            view?.updatePathString(path.absolutePath)
            loadFiles(back = false, caches = true, fromCache = false)
        }
    }

    fun canLoadUp(): Boolean {
        if (q != null) {
            return true
        }
        val parent = path.parentFile
        if (base && path.absolutePath == basePath) {
            return false
        }
        return parent != null && parent.canRead()
    }

    private fun loadCache(back: Boolean) {
        isLoading = true
        view?.resolveEmptyText(false)
        view?.resolveLoading(isLoading)

        appendJob(Includes.stores.searchQueriesStore().getFiles(path.absolutePath).fromIOToMain({
            fileList.clear()
            fileList.addAll(it)
            view?.resolveEmptyText(fileList.isEmpty())
            view?.notifyAllChanged()
            directoryScrollPositions.remove(path.absolutePath)?.let { scroll ->
                view?.restoreScroll(scroll)
            } ?: view?.restoreScroll(LinearLayoutManager_SavedState())
            if (back && fileList.isEmpty() || !back) {
                loadFiles(
                    back = false, caches = false, fromCache = true
                )
            } else {
                isLoading = false
                view?.resolveLoading(isLoading)
            }
        }, {
            view?.restoreScroll(LinearLayoutManager_SavedState())
            view?.showThrowable(it)
            loadFiles(
                back = false, caches = false, fromCache = true
            )
        }))
    }

    fun loadFiles(back: Boolean, caches: Boolean, fromCache: Boolean) {
        if (isLoading && !fromCache) {
            return
        }
        if (caches) {
            loadCache(back)
            return
        }
        view?.resolveEmptyText(false)
        if (!fromCache) {
            isLoading = true
            view?.resolveLoading(isLoading)
        }
        appendJob(rxLoadFileList().fromIOToMain({
            fileList.clear()
            fileList.addAll(it)
            isLoading = false
            view?.resolveEmptyText(fileList.isEmpty())
            view?.resolveLoading(isLoading)
            view?.notifyAllChanged()
        }, {
            view?.showThrowable(it)
            isLoading = false
            view?.resolveLoading(isLoading)
        }))
    }

    private fun rxSearchFiles(): Flow<ArrayList<FileItem>> {
        return flow {
            val fileListTmp = ArrayList<FileItem>()
            searchFile(fileListTmp, path)
            val dirsList = ArrayList<FileItem>()
            val flsList = ArrayList<FileItem>()
            for (i in fileListTmp) {
                if (i.type == FileType.folder) dirsList.add(i) else flsList.add(i)
            }
            dirsList.sortWith(ItemModificationComparator())
            flsList.sortWith(ItemModificationComparator())
            fileListTmp.clear()
            fileListTmp.addAll(dirsList)
            fileListTmp.addAll(flsList)
            emit(fileListTmp)
        }
    }

    private fun searchFile(files: ArrayList<FileItem>, dir: File) {
        if (dir.exists() && dir.canRead()) {
            val fList = dir.list(filter)
            if (fList != null) {
                for (i in fList.indices) {
                    // Convert into file path
                    val file = File(dir, fList[i])
                    if (file.isDirectory) {
                        searchFile(files, file)
                    }
                    val canRead = file.canRead()
                    val mod = file.lastModified()

                    if (file.name.lowercase(Locale.getDefault())
                            .contains(q?.lowercase(Locale.getDefault()).toString())
                    ) {
                        files.add(
                            FileItem(
                                getExt(file),
                                fList[i],
                                file.absolutePath,
                                dir.name,
                                dir.absolutePath,
                                mod,
                                if (file.isDirectory) getFolderFilesCount(file) else file.length(),
                                canRead
                            ).checkTag()
                        )
                    }
                }
            }
        }
    }

    private fun getFolderFilesCount(file: File): Long {
        if (!Settings.get().main().isEnable_dirs_files_count) {
            return -1
        }
        return file.list()?.size?.toLong() ?: -1
    }

    private fun rxLoadFileList(): Flow<ArrayList<FileItem>> {
        return flow {
            val fileListTmp = ArrayList<FileItem>()
            if (path.exists() && path.canRead()) {
                val fList = path.list(filter)
                if (fList != null) {
                    for (i in fList.indices) {
                        // Convert into file path
                        val file = File(path, fList[i])
                        val canRead = file.canRead()
                        val mod = file.lastModified()

                        fileListTmp.add(
                            i,
                            FileItem(
                                getExt(file),
                                fList[i],
                                file.absolutePath,
                                path.name,
                                path.absolutePath,
                                mod,
                                if (file.isDirectory) getFolderFilesCount(file) else file.length(),
                                canRead
                            ).checkTag()
                        )
                    }
                    val dirsList = ArrayList<FileItem>()
                    val flsList = ArrayList<FileItem>()
                    for (i in fileListTmp) {
                        if (i.type == FileType.folder) dirsList.add(i) else flsList.add(i)
                    }
                    dirsList.sortWith(ItemModificationComparator())
                    flsList.sortWith(ItemModificationComparator())
                    fileListTmp.clear()
                    fileListTmp.addAll(dirsList)
                    fileListTmp.addAll(flsList)
                }
            }
            Includes.stores.searchQueriesStore().insertFiles(path.absolutePath, fileListTmp)
                .single()
            emit(fileListTmp)
        }
    }

    fun scrollTo(item: String): Boolean {
        var ret = false
        val list = if (q == null) fileList else fileListSearch
        for (i in list.indices) {
            if (!ret && list[i].file_path == item) {
                list[i].isSelected = true
                view?.notifyItemChanged(i)
                view?.onScrollTo(i)
                ret = true
            } else {
                if (list[i].isSelected) {
                    list[i].isSelected = false
                    view?.notifyItemChanged(i)
                }
            }
        }
        return ret
    }

    private fun doFixDirTime(dir: String, isRoot: Boolean) {
        val root = File(dir)
        val list: ArrayList<Long> = ArrayList()
        if (root.exists() && root.isDirectory) {
            val children = root.list()
            if (children != null) {
                for (child in children) {
                    val rem = File(root, child)
                    if (rem.isFile && !rem.isHidden && !isRoot) {
                        list.add(rem.lastModified())
                    } else if (rem.isDirectory && !rem.isHidden && rem.name != "." && rem.name != "..") {
                        doFixDirTime(rem.absolutePath, false)
                    }
                }
            }
        } else {
            return
        }
        if (isRoot) {
            return
        }

        val res = list.maxOrNull()
        res?.let {
            root.setLastModified(it)
        }
    }

    private fun fixDirTimeRx(dir: String): Flow<Boolean> {
        return flow {
            doFixDirTime(dir, true)
            emit(true)
        }
    }

    @SuppressLint("CheckResult")
    fun fireFixDirTime(dir: String) {
        if (isLoading) {
            return
        }
        isLoading = true
        view?.resolveLoading(isLoading)
        fixDirTimeRx(dir).fromIOToMain({
            view?.showMessage(R.string.success)
            isLoading = false
            loadFiles(back = false, caches = false, fromCache = false)
        }, { view?.showThrowable(it) })
    }

    fun fireToggleDirTag(item: FileItem) {
        if (selectedOwner == null) {
            return
        }
        if (item.isHasTag) {
            item.file_path?.let { op ->
                appendJob(
                    Includes.stores.searchQueriesStore().deleteTagDirByPath(op)
                        .fromIOToMain({
                            item.checkTag()
                            val list = if (q == null) fileList else fileListSearch
                            view?.notifyItemChanged(list.indexOf(item))
                        }, {
                            view?.showThrowable(it)
                        })
                )
            }
        } else {
            appendJob(
                Includes.stores.searchQueriesStore()
                    .insertTagDir((selectedOwner ?: return).id, item)
                    .fromIOToMain({
                        item.checkTag()
                        val list = if (q == null) fileList else fileListSearch
                        view?.notifyItemChanged(list.indexOf(item))
                    }, {
                        view?.showThrowable(it)
                    })
            )
        }
    }

    fun fireRemoveDirTag(item: FileItem) {
        item.file_path?.let { op ->
            appendJob(
                Includes.stores.searchQueriesStore().deleteTagDirByPath(op)
                    .fromIOToMain({
                        item.checkTag()
                        val list = if (q == null) fileList else fileListSearch
                        view?.notifyItemChanged(list.indexOf(item))
                    }, {
                        view?.showThrowable(it)
                    })
            )
        }
    }

    fun onAddTagFromDialog(item: FileItem) {
        val list = if (q == null) fileList else fileListSearch
        val s = list.indexOf(item)
        if (s >= 0) {
            list[s].checkTag()
            view?.notifyItemChanged(s)
        }
    }

    fun onClickFile(item: FileItem) {
        if (selectedOwner != null) {
            item.checkTag()
            if (item.isHasTag) {
                item.file_path?.let { op ->
                    appendJob(
                        Includes.stores.searchQueriesStore().deleteTagDirByPath(op)
                            .fromIOToMain({
                                item.checkTag()
                                val list = if (q == null) fileList else fileListSearch
                                view?.notifyItemChanged(list.indexOf(item))
                            }, {
                                view?.showThrowable(it)
                            })
                    )
                }
            } else {
                appendJob(
                    Includes.stores.searchQueriesStore()
                        .insertTagDir((selectedOwner ?: return).id, item)
                        .fromIOToMain({
                            item.checkTag()
                            val list = if (q == null) fileList else fileListSearch
                            view?.notifyItemChanged(list.indexOf(item))
                        }, {
                            view?.showThrowable(it)
                        })
                )
            }
            return
        }
        if (item.type == FileType.photo) {
            val list = if (q == null) fileList else fileListSearch
            var index = 0
            var o = 0
            val mem = ParcelNative.create(ParcelFlags.NULL_LIST)
            for (i in list) {
                if (i.type != FileType.photo && i.type != FileType.video) {
                    continue
                }
                if (i.file_path == item.file_path) {
                    index = o
                }
                val photo = Photo()
                photo.setId(i.fileNameHash)
                photo.setOwnerId(i.filePathHash)
                photo.setDate(i.modification)
                photo.setPhoto_url("file://" + i.file_path)
                photo.setPreview_url("thumb_file://" + i.file_path)
                photo.setLocal(true)
                photo.setIsAnimation(
                    i.type == FileType.video
                )
                photo.setText(i.file_name)
                mem.writeParcelable(photo)
                o++
            }
            mem.writeFirstInt(o)
            view?.displayGalleryUnSafe(mem.nativePointer, index, false)
        } else if (item.type == FileType.video) {
            val v = Video()
            v.setId(item.fileNameHash)
            v.setOwnerId(item.filePathHash)
            v.setDate(item.modification)
            v.setTitle(item.file_name)
            v.setLink("file://" + item.file_path)
            v.setDuration(item.size)
            v.setDescription(item.parent_path)
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
                if (i.file_path == item.file_path) {
                    index = o
                }
                val audio = Audio()
                audio.setId(i.fileNameHash)
                audio.setOwnerId(i.filePathHash)
                audio.setUrl("file://" + i.file_path)
                audio.setThumb_image("thumb_file://" + i.file_path)
                audio.setDuration(i.size.toInt())

                var TrackName: String =
                    i.file_name?.replace(".mp3", "") ?: ""
                val Artist: String
                val arr = TrackName.split(Regex(" - ")).toTypedArray()
                if (arr.size > 1) {
                    Artist = arr[0]
                    TrackName = TrackName.replace("$Artist - ", "")
                } else {
                    Artist = i.parent_name ?: ""
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

    private fun deleteRecursive(dir: String) {
        val fDir = File(dir)
        if (fDir.exists() && fDir.isDirectory) {
            val children = fDir.list()
            if (children != null) {
                for (child in children) {
                    val rem = File(fDir, child)
                    if (rem.isFile) {
                        rem.delete()
                    } else if (rem.isDirectory && rem.name != "." && rem.name != "..") {
                        deleteRecursive(rem.absolutePath)
                        rem.delete()
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    fun fireDelete(item: FileItem) {
        isLoading = true
        view?.resolveLoading(isLoading)
        flow {
            if (item.type != FileType.folder) {
                if (item.file_path?.let { it1 -> File(it1).delete() } == true) {
                    emit(true)
                } else {
                    throw Throwable("Can't Delete File")
                }
            } else {
                item.file_path?.let { it1 -> deleteRecursive(it1) }
                if (item.file_path?.let { it1 -> File(it1).delete() } == true) {
                    emit(true)
                } else {
                    throw Throwable("Can't Delete Folder")
                }
            }
        }.fromIOToMain({
            isLoading = false
            view?.resolveLoading(isLoading)
            view?.showMessage(R.string.success)
            loadFiles(back = false, caches = false, fromCache = false)
        }, {
            view?.showThrowable(it)
        })
    }

    fun fireFileForRemotePlaySelected(audioPath: String) {
        val intent = UploadIntent(remotePlay)
            .setAutoCommit(true)
            .setFileUri(audioPath.toUri())
        uploadManager.enqueue(listOf(intent))
    }

    @FileType
    private fun getExt(file: File): Int {
        if (file.isDirectory) {
            return FileType.folder
        }
        for (i in Settings.get().main().photoExt) {
            if (file.extension.contains(i, true)) {
                return FileType.photo
            }
        }
        for (i in Settings.get().main().videoExt) {
            if (file.extension.contains(i, true)) {
                return FileType.video
            }
        }
        for (i in Settings.get().main().audioExt) {
            if (file.extension.contains(i, true)) {
                return FileType.audio
            }
        }
        return FileType.error
    }

    init {
        loadFiles(back = false, caches = true, fromCache = false)

        appendJob(
            uploadManager[remotePlay]
                .fromIOToMain { data -> onUploadsDataReceived(data) })
        appendJob(
            uploadManager.observeAdding()
                .sharedFlowToMain {
                    onUploadsAdded(it)
                })
        appendJob(
            uploadManager.observeDeleting(true)
                .sharedFlowToMain {
                    onUploadDeleted(it)
                })
        appendJob(
            uploadManager.observeResults()
                .filter {
                    remotePlay.compareTo(
                        it.first.destination
                    ) || remotePlay.compareTo(it.first.destination)
                }
                .sharedFlowToMain {
                    onUploadResults(it)
                })

        appendJob(
            uploadManager.observeStatus()
                .sharedFlowToMain {
                    onUploadStatusUpdate(it)
                })

        appendJob(
            uploadManager.observeProgress()
                .sharedFlowToMain {
                    onProgressUpdates(it)
                })
    }
}
