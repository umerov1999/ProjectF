package dev.ragnarok.fenrir.api.model.response

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import org.json.JSONObject

@Serializable
class ResolveDomailResponse : Parcelable {
    var type: String? = null
    var object_id: String? = null

    @Suppress("UNUSED")
    constructor()
    internal constructor(`in`: Parcel) {
        type = `in`.readString()
        object_id = `in`.readString()
    }

    fun parse(jsonObject: JSONObject): ResolveDomailResponse {
        type = jsonObject.optString("type")
        object_id = jsonObject.optString("object_id")
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(type)
        dest.writeString(object_id)
    }

    override fun toString(): String {
        return "ResolveDomailResponse{" +
                "type='" + type + '\'' +
                ", object_id='" + object_id + '\'' +
                '}'
    }

    companion object CREATOR : Parcelable.Creator<ResolveDomailResponse> {
        override fun createFromParcel(parcel: Parcel): ResolveDomailResponse {
            return ResolveDomailResponse(parcel)
        }

        override fun newArray(size: Int): Array<ResolveDomailResponse?> {
            return arrayOfNulls(size)
        }
    }
}