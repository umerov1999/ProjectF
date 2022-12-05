package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.util.ParcelUtils.readObjectInteger
import dev.ragnarok.fenrir.util.ParcelUtils.writeObjectInteger

class MessageSearchCriteria : BaseSearchCriteria {
    var peerId: Int? = null
        private set

    constructor(query: String?) : super(query) {
        // for test
        //appendOption(new SimpleBooleanOption(1, R.string.photo, true));
    }

    internal constructor(`in`: Parcel) : super(`in`) {
        peerId = readObjectInteger(`in`)
    }

    fun setPeerId(peerId: Int?): MessageSearchCriteria {
        this.peerId = peerId
        return this
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        writeObjectInteger(dest, peerId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MessageSearchCriteria> {
        override fun createFromParcel(parcel: Parcel): MessageSearchCriteria {
            return MessageSearchCriteria(parcel)
        }

        override fun newArray(size: Int): Array<MessageSearchCriteria?> {
            return arrayOfNulls(size)
        }
    }
}