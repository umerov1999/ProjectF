package dev.ragnarok.fenrir.media.record

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.Settings

class Recorder(val filePath: String, val context: Context) {
    private var mRecorder: MediaRecorder? = null

    /**
     * Общая длительность записи
     */
    private var mPreviousSectionsDuration: Long = 0
    private var mCurrentRecordingSectionStartTime: Long? = null
    var status = 0
        private set
    private var isReleased = false

    @Suppress("DEPRECATION")
    fun prepare() {
        mRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        if (isOpusSupported) {
            mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.OGG)
            mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            mRecorder?.setAudioSamplingRate(16000)
            mRecorder?.setAudioEncodingBitRate(16000)
        } else {
            mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mRecorder?.setAudioSamplingRate(44100)
            mRecorder?.setAudioEncodingBitRate(96000)
        }
        mRecorder?.setOutputFile(filePath)
        mRecorder?.prepare()
    }

    fun start() {
        assertRecorderNotNull()
        if (status == Status.PAUSED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mRecorder?.resume()
            } else {
                throw IllegalStateException("Pause does not supported")
            }
        } else {
            mRecorder?.start()
        }
        mCurrentRecordingSectionStartTime = System.currentTimeMillis()
        changeStatusTo(Status.RECORDING_NOW)
    }

    val maxAmplitude: Int
        get() {
            assertRecorderNotNull()
            return mRecorder?.maxAmplitude ?: 0
        }
    val currentRecordDuration: Long
        get() = mCurrentRecordingSectionStartTime.requireNonNull({
            mPreviousSectionsDuration + System.currentTimeMillis() - it
        }, {
            mPreviousSectionsDuration
        })

    @RequiresApi(Build.VERSION_CODES.N)
    fun pause() {
        assertRecorderNotNull()
        if (isPauseSupported) {
            resetCurrentRecordTime()
            mRecorder?.pause()
            changeStatusTo(Status.PAUSED)
        } else {
            throw UnsupportedOperationException()
        }
    }

    private fun resetCurrentRecordTime() {
        mCurrentRecordingSectionStartTime ?: return
        mPreviousSectionsDuration += (System.currentTimeMillis() - (mCurrentRecordingSectionStartTime
            ?: return))
        mCurrentRecordingSectionStartTime = null
    }

    fun stopAndRelease() {
        assertRecorderNotNull()
        resetCurrentRecordTime()
        try {
            if (isPauseSupported && status == Status.PAUSED) {
                mRecorder?.resume()
            }
            mRecorder?.stop()
            mRecorder?.release()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            //TODO show toast
        }
        changeStatusTo(Status.NO_RECORD)
        mRecorder = null
        isReleased = true
    }

    private fun assertRecorderNotNull() {
        checkNotNull(mRecorder)
    }

    private fun changeStatusTo(status: Int) {
        this.status = status
    }

    object Status {
        const val NO_RECORD = 0
        const val RECORDING_NOW = 1
        const val PAUSED = 2
    }

    companion object {
        val isPauseSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        val isOpusSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Settings.get()
                .main().isRecording_to_opus
    }
}