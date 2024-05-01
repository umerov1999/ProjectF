package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

class VoiceMessage : AbsModel {
    val id: Int
    val ownerId: Long
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
    var showTranscript = false
        private set
    var transcript: String? = null
        private set
    var was_listened = false
        private set

    constructor(id: Int, ownerId: Long) {
        this.id = id
        this.ownerId = ownerId
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
        duration = parcel.readInt()
        waveform = parcel.createByteArray()
        linkOgg = parcel.readString()
        linkMp3 = parcel.readString()
        accessKey = parcel.readString()
        transcript = parcel.readString()
        showTranscript = parcel.getBoolean()
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_VOICE_MESSAGE
    }

    fun setWasListened(listened: Boolean): VoiceMessage {
        this.was_listened = listened
        return this
    }

    fun setShowTranscript(showTranscript: Boolean): VoiceMessage {
        this.showTranscript = showTranscript
        return this
    }

    fun setAccessKey(accessKey: String?): VoiceMessage {
        this.accessKey = accessKey
        return this
    }

    fun setDuration(duration: Int): VoiceMessage {
        this.duration = duration
        return this
    }

    fun setWaveform(waveform: ByteArray?): VoiceMessage {
        this.waveform = waveform
        return this
    }

    fun setLinkOgg(linkOgg: String?): VoiceMessage {
        this.linkOgg = linkOgg
        return this
    }

    fun setLinkMp3(linkMp3: String?): VoiceMessage {
        this.linkMp3 = linkMp3
        return this
    }

    fun setTranscript(transcript: String?): VoiceMessage {
        this.transcript = transcript
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeLong(ownerId)
        parcel.writeInt(duration)
        parcel.writeByteArray(waveform)
        parcel.writeString(linkOgg)
        parcel.writeString(linkMp3)
        parcel.writeString(accessKey)
        parcel.writeString(transcript)
        parcel.putBoolean(showTranscript)
    }

    companion object CREATOR : Parcelable.Creator<VoiceMessage> {
        override fun createFromParcel(parcel: Parcel): VoiceMessage {
            return VoiceMessage(parcel)
        }

        override fun newArray(size: Int): Array<VoiceMessage?> {
            return arrayOfNulls(size)
        }
    }
}