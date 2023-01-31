package dev.ragnarok.fenrir.model.selection

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Sources : Parcelable {
    val sources: ArrayList<AbsSelectableSource>

    constructor() {
        sources = ArrayList(2)
    }

    internal constructor(parcel: Parcel) {
        val size = parcel.readInt()
        sources = ArrayList(size)
        for (i in 0 until size) {
            when (@Types val type = parcel.readInt()) {
                Types.FILES -> sources.add(
                    parcel.readTypedObjectCompat(
                        FileManagerSelectableSource.CREATOR
                    )!!
                )

                Types.LOCAL_PHOTOS -> sources.add(
                    parcel.readTypedObjectCompat(
                        LocalPhotosSelectableSource.CREATOR
                    )!!
                )

                Types.LOCAL_GALLERY -> sources.add(
                    parcel.readTypedObjectCompat(
                        LocalGallerySelectableSource.CREATOR
                    )!!
                )

                Types.VIDEOS -> sources.add(
                    parcel.readTypedObjectCompat(
                        LocalVideosSelectableSource.CREATOR
                    )!!
                )

                Types.VK_PHOTOS -> sources.add(
                    parcel.readTypedObjectCompat(
                        VKPhotosSelectableSource.CREATOR
                    )!!
                )

                else -> throw UnsupportedOperationException("Invalid type $type")
            }
        }
    }

    fun with(source: AbsSelectableSource): Sources {
        sources.add(source)
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    fun count(): Int {
        return sources.size
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(sources.size)
        for (source in sources) {
            parcel.writeInt(source.type)
            parcel.writeTypedObjectCompat(source, flags)
        }
    }

    operator fun get(position: Int): AbsSelectableSource {
        return sources[position]
    }

    companion object CREATOR : Parcelable.Creator<Sources> {
        override fun createFromParcel(parcel: Parcel): Sources {
            return Sources(parcel)
        }

        override fun newArray(size: Int): Array<Sources?> {
            return arrayOfNulls(size)
        }
    }
}