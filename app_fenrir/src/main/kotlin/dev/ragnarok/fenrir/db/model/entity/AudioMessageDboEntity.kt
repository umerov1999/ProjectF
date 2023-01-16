package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("audio_message")
class AudioMessageDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0L
        private set
    var duration = 0
        private set
    var waveform: ByteArray? = null
        private set
    var linkOgg: String? = null
        private set
    var linkMp3: String? = null
        private set
    var accessKey: String? = null
        private set
    var transcript: String? = null
        private set
    var was_listened = false
        private set

    operator fun set(id: Int, ownerId: Long): AudioMessageDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setWasListened(listened: Boolean): AudioMessageDboEntity {
        this.was_listened = listened
        return this
    }

    fun setDuration(duration: Int): AudioMessageDboEntity {
        this.duration = duration
        return this
    }

    fun setWaveform(waveform: ByteArray?): AudioMessageDboEntity {
        this.waveform = waveform
        return this
    }

    fun setLinkOgg(linkOgg: String?): AudioMessageDboEntity {
        this.linkOgg = linkOgg
        return this
    }

    fun setLinkMp3(linkMp3: String?): AudioMessageDboEntity {
        this.linkMp3 = linkMp3
        return this
    }

    fun setTranscript(transcript: String?): AudioMessageDboEntity {
        this.transcript = transcript
        return this
    }

    fun setAccessKey(accessKey: String?): AudioMessageDboEntity {
        this.accessKey = accessKey
        return this
    }
}