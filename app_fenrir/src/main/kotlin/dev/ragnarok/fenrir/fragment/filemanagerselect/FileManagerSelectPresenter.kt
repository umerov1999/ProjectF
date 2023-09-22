package dev.ragnarok.fenrir.fragment.filemanagerselect

import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.FileItem
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Objects.safeEquals
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.FilenameFilter
import java.util.Locale

class FileManagerSelectPresenter(
    private var path: File,
    private val ext: String?,
    savedInstanceState: Bundle?
) : RxSupportPresenter<IFileManagerSelectView>(savedInstanceState) {
    private val fileList: ArrayList<FileItem> = ArrayList()
    private val fileListSearch: ArrayList<FileItem> = ArrayList()
    private var isLoading = false
    private val directoryScrollPositions = HashMap<String, Parcelable>()
    private var q: String? = null

    @Suppress("DEPRECATION")
    private val filter: FilenameFilter = FilenameFilter { dir: File, filename: String ->
        val sel = File(dir, filename)
        if (sel.absolutePath == File(
                Environment.getExternalStorageDirectory(),
                "Android"
            ).absolutePath
        ) {
            return@FilenameFilter false
        }
        val ret = !sel.isHidden && sel.canRead()
        // Filters based on whether the file is hidden or not
        if ("dirs" == ext && sel.isDirectory && ret) {
            return@FilenameFilter true
        } else if (ext == null && ret) {
            return@FilenameFilter true
        } else if ((!sel.isDirectory && ext?.let {
                sel.extension.contains(
                    it,
                    true
                )
            } == true || sel.isDirectory) && ret) {
            return@FilenameFilter true
        }
        return@FilenameFilter false
    }

    fun getCurrentDir(): String {
        return path.absolutePath
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
            view?.updatePathString(path.absolutePath)
        } else {
            fileListSearch.clear()
            for (i in fileList) {
                if (i.file_name.isEmpty()) {
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
        }
    }

    override fun onGuiCreated(viewHost: IFileManagerSelectView) {
        super.onGuiCreated(viewHost)
        (if (q == null) path.absolutePath else q)?.let { viewHost.updatePathString(it) }
        viewHost.displayData(if (q == null) fileList else fileListSearch)
        viewHost.updatePathString(path.absolutePath)
        viewHost.resolveEmptyText(fileList.isEmpty())
        viewHost.resolveLoading(isLoading)
        viewHost.updateSelectVisibility(ext == "dirs")
        viewHost.updateHeader(ext)
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
            return
        }
        val parent = path.parentFile
        if (parent != null && parent.canRead()) {
            path = parent
            view?.updatePathString(path.absolutePath)
            loadFiles()
        }
    }

    fun setCurrent(file: File) {
        if (isLoading) {
            return
        }
        if (file.canRead()) {
            view?.displayData(fileList)
            q = null
            path = file
            view?.updatePathString(path.absolutePath)
            loadFiles()
        }
    }

    fun canLoadUp(): Boolean {
        val parent = path.parentFile
        return parent != null && parent.canRead()
    }

    private fun loadFiles() {
        if (isLoading) {
            return
        }
        isLoading = true
        view?.resolveEmptyText(false)
        view?.resolveLoading(isLoading)
        appendDisposable(rxLoadFileList().fromIOToMain().subscribe({
            fileList.clear()
            fileList.addAll(it)
            isLoading = false
            view?.resolveEmptyText(fileList.isEmpty())
            view?.resolveLoading(isLoading)
            view?.notifyAllChanged()
            directoryScrollPositions.remove(path.absolutePath)?.let { scroll ->
                view?.restoreScroll(scroll)
            }
        }, {
            view?.onError(it)
            isLoading = false
            view?.resolveLoading(isLoading)
        }))
    }

    private fun getFolderFilesCount(file: File): Long {
        if (!Settings.get().main().isEnable_dirs_files_count) {
            return -1
        }
        return file.listFiles()?.size?.toLong() ?: -1
    }

    private fun rxLoadFileList(): Single<ArrayList<FileItem>> {
        return Single.create {
            val fileListTmp = ArrayList<FileItem>()
            if (path.exists() && path.canRead()) {
                val fList = path.list(filter)
                if (fList != null) {
                    for (i in fList.indices) {
                        // Convert into file path
                        val file = File(path, fList[i])
                        val mod = file.lastModified()

                        fileListTmp.add(
                            i,
                            FileItem(
                                file.isDirectory,
                                fList[i],
                                file.absolutePath,
                                path.name,
                                path.absolutePath,
                                mod,
                                if (file.isDirectory) getFolderFilesCount(file) else file.length(),
                            )
                        )
                    }
                    val dirsList = ArrayList<FileItem>()
                    val flsList = ArrayList<FileItem>()
                    for (i in fileListTmp) {
                        if (i.isDir) dirsList.add(i) else flsList.add(i)
                    }
                    dirsList.sortWith(ItemModificationComparator())
                    flsList.sortWith(ItemModificationComparator())
                    fileListTmp.clear()
                    fileListTmp.addAll(dirsList)
                    fileListTmp.addAll(flsList)
                }
            }
            it.onSuccess(fileListTmp)
        }
    }

    init {
        loadFiles()
    }
}
