package dev.ragnarok.fenrir.dialog.audioduplicate

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.core.net.toUri
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.observeServiceBinding
import dev.ragnarok.fenrir.media.music.PlayerStatus
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.util.Mp3InfoHelper.getBitrate
import dev.ragnarok.fenrir.util.Mp3InfoHelper.getLength
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.hls.M3U8
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AudioDuplicatePresenter(
    private val accountId: Long,
    private val new_audio: Audio,
    private val old_audio: Audio,
    savedInstanceState: Bundle?
) : RxSupportPresenter<IAudioDuplicateView>(savedInstanceState) {
    private val mAudioInteractor = InteractorFactory.createAudioInteractor()
    private val mPlayerDisposable = CancelableJob()
    private var audioListDisposable = CancelableJob()
    private var oldBitrate: Int? = null
    private var newBitrate: Int? = null
    private var needShowBitrateButton = true
    private val mp3AndBitrate: Unit
        get() {
            val mode = new_audio.needRefresh()
            if (mode.first) {
                audioListDisposable +=
                    mAudioInteractor.getByIdOld(accountId, listOf(new_audio), mode.second)
                        .fromIOToMain({ t -> getBitrate(t[0]) }) {
                            getBitrate(
                                new_audio
                            )
                        }
            } else {
                getBitrate(new_audio)
            }
        }

    private fun doBitrate(url: String): Flow<Int> {
        return flow {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(url, HashMap())
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (bitrate != null) {
                emit((bitrate.toLong() / 1000).toInt())
            } else {
                throw Throwable("Can't receipt bitrate!")
            }
        }
    }

    private fun getBitrate(audio: Audio) {
        val pUrl = audio.url
        if (pUrl.isNullOrEmpty()) {
            return
        }
        if (audio.isHLS) {
            audioListDisposable += M3U8(pUrl).length.fromIOToMain({ r ->
                newBitrate = getBitrate(audio.duration, r)
                view?.setNewBitrate(
                    newBitrate
                )
            }) { t -> onDataGetError(t) }
        } else if (!audio.isLocalServer) {
            audioListDisposable += getLength(pUrl).fromIOToMain({ r ->
                newBitrate = getBitrate(audio.duration, r)
                view?.setNewBitrate(
                    newBitrate
                )
            }) { t -> onDataGetError(t) }
        } else {
            audioListDisposable += doBitrate(pUrl).fromIOToMain({ r ->
                newBitrate = r
                view?.setNewBitrate(
                    newBitrate
                )
            }) { t -> onDataGetError(t) }
        }
    }

    private fun doLocalBitrate(context: Context, url: String): Flow<Int> {
        return flow {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns.DATA),
                BaseColumns._ID + "=? ",
                arrayOf(url.toUri().lastPathSegment),
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val retriever = MediaMetadataRetriever()
                val fl =
                    cursor.getString(MediaStore.MediaColumns.DATA)
                retriever.setDataSource(fl)
                cursor.close()
                val bitrate =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                if (bitrate != null) {
                    emit((bitrate.toLong() / 1000).toInt())
                } else {
                    throw Throwable("Can't receipt bitrate!")
                }
            } else {
                throw Throwable("Can't receipt bitrate!")
            }
        }
    }

    fun getBitrateAll(context: Context) {
        val pUrl = old_audio.url
        if (pUrl.isNullOrEmpty()) {
            return
        }
        needShowBitrateButton = false
        view?.updateShowBitrate(
            needShowBitrateButton
        )
        audioListDisposable += doLocalBitrate(context, pUrl).fromIOToMain({ r ->
            oldBitrate = r
            view?.setOldBitrate(
                oldBitrate
            )
            mp3AndBitrate
        }) { t -> onDataGetError(t) }
    }

    private fun onServiceBindEvent(@PlayerStatus status: Int) {
        when (status) {
            PlayerStatus.UPDATE_TRACK_INFO, PlayerStatus.SERVICE_KILLED, PlayerStatus.UPDATE_PLAY_PAUSE ->
                view?.displayData(
                    new_audio,
                    old_audio
                )

            PlayerStatus.REPEATMODE_CHANGED, PlayerStatus.SHUFFLEMODE_CHANGED, PlayerStatus.UPDATE_PLAY_LIST -> {}
        }
    }

    override fun onDestroyed() {
        mPlayerDisposable.cancel()
        audioListDisposable.cancel()
        super.onDestroyed()
    }

    override fun onGuiCreated(viewHost: IAudioDuplicateView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(new_audio, old_audio)
        viewHost.setNewBitrate(newBitrate)
        viewHost.setOldBitrate(oldBitrate)
        viewHost.updateShowBitrate(needShowBitrateButton)
    }

    private fun onDataGetError(t: Throwable) {
        view?.let {
            showError(
                it,
                Utils.getCauseIfRuntime(t)
            )
        }
    }

    init {
        mPlayerDisposable += observeServiceBinding()
            .sharedFlowToMain { onServiceBindEvent(it) }
    }
}