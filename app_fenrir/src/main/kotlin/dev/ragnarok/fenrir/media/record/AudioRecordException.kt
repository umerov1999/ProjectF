package dev.ragnarok.fenrir.media.record

class AudioRecordException : Exception {
    val code: Int

    constructor(code: Int) {
        this.code = code
    }

    constructor(code: Int, message: String?) : super(message) {
        this.code = code
    }

    object Codes {
        const val UNABLE_TO_REMOVE_TMP_FILE = 10
        const val UNABLE_TO_RENAME_TMP_FILE = 12
        const val UNABLE_TO_PREPARE_RECORDER = 11
        const val INVALID_RECORDER_STATUS = 13
    }
}