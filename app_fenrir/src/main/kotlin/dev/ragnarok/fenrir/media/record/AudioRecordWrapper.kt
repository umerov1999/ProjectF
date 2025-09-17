package dev.ragnarok.fenrir.media.record

import android.content.Context
import android.os.Environment
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import java.io.File
import java.io.IOException

class AudioRecordWrapper internal constructor(builder: Builder) {
    private val mContext: Context = builder.mContext
    private val mFileExt: String = builder.mFileExt
    private var mRecorder: Recorder? = null

    @Throws(AudioRecordException::class)
    fun doRecord() {
        if (mRecorder == null) {
            val file = tmpRecordFile
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    throw AudioRecordException(AudioRecordException.Codes.UNABLE_TO_REMOVE_TMP_FILE)
                }
            }
            mRecorder = Recorder(file.absolutePath, mContext)
            try {
                mRecorder?.prepare()
            } catch (_: IOException) {
                mRecorder = null
                throw AudioRecordException(AudioRecordException.Codes.UNABLE_TO_PREPARE_RECORDER)
            }
        }
        if (mRecorder?.status == Recorder.Status.RECORDING_NOW) {
            mRecorder?.pause()
        } else {
            try {
                mRecorder?.start()
            } catch (e: IllegalStateException) {
                createCustomToast(mContext, null)?.showToastError(e.localizedMessage)
            }
        }
    }

    val currentRecordDuration: Long
        get() = if (mRecorder == null) 0 else mRecorder?.currentRecordDuration.orZero()
    val currentMaxAmplitude: Int
        get() = if (mRecorder == null) 0 else mRecorder?.maxAmplitude.orZero()

    fun pause() {
        if (mRecorder == null) {
            Logger.wtf(TAG, "Recorder in NULL")
            return
        }
        if (mRecorder?.status == Recorder.Status.RECORDING_NOW) {
            try {
                mRecorder?.pause()
            } catch (e: IllegalStateException) {
                createCustomToast(mContext, null)?.showToastError(e.localizedMessage)
            }
        } else {
            Logger.wtf(TAG, "Recorder status is not RECORDING_NOW")
        }
    }

    fun stopRecording() {
        checkNotNull(mRecorder) { "Recorder in NULL" }
        try {
            mRecorder?.stopAndRelease()
        } catch (e: IllegalStateException) {
            createCustomToast(mContext, null)?.showToastError(e.localizedMessage)
        }
        mRecorder = null
    }

    @Throws(AudioRecordException::class)
    fun stopRecordingAndReceiveFile(): File {
        checkNotNull(mRecorder) { "Recorder in NULL" }
        val status = mRecorder?.status
        return if (status == Recorder.Status.RECORDING_NOW || status == Recorder.Status.PAUSED) {
            val filePath = mRecorder?.filePath
            filePath
                ?: throw AudioRecordException(AudioRecordException.Codes.INVALID_RECORDER_STATUS)
            mRecorder?.stopAndRelease()
            mRecorder = null
            val currentTime = System.currentTimeMillis()
            val destFileName = "record_$currentTime.$mFileExt"
            val destFile =
                File(getRecordingDirectory(mContext), destFileName)
            val file = File(filePath)
            val renamed = file.renameTo(destFile)
            if (!renamed) {
                throw AudioRecordException(AudioRecordException.Codes.UNABLE_TO_RENAME_TMP_FILE)
            }
            destFile
        } else {
            throw AudioRecordException(AudioRecordException.Codes.INVALID_RECORDER_STATUS)
        }
    }

    val recorderStatus: Int
        get() = mRecorder?.status ?: Recorder.Status.NO_RECORD
    private val tmpRecordFile: File
        get() = File(getRecordingDirectory(mContext), "$TEMP_FILE_NAME.$mFileExt")

    class Builder(val mContext: Context) {
        val mFileExt = if (Recorder.isOpusSupported) "ogg" else "mp3"
        fun build(): AudioRecordWrapper {
            return AudioRecordWrapper(this)
        }
    }

    companion object {
        private const val TEMP_FILE_NAME = "temp_recording"
        private val TAG = AudioRecordWrapper::class.simpleName.orEmpty()
        fun getRecordingDirectory(context: Context): File? {
            return context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES)
        }
    }

}