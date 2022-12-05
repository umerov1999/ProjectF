package dev.ragnarok.filegallery.db.column

import android.provider.BaseColumns

object FilesColumns : BaseColumns {
    const val TABLENAME = "files"
    const val PARENT_DIR = "parent"
    const val TYPE = "type"
    const val IS_DIR = "is_dir"
    const val FILE_NAME = "file_name"
    const val FILE_PATH = "file_path"
    const val PARENT_NAME = "parent_name"
    const val PARENT_PATH = "parent_path"
    const val MODIFICATIONS = "modification"
    const val SIZE = "size"
    const val CAN_READ = "can_read"
}