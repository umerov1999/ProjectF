package dev.ragnarok.fenrir.model.catalog_v2_audio

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2Button
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class CatalogV2Button : Parcelable {
    var action: CatalogV2Action? = null
        private set
    var name: String? = null
        private set
    var section_id: String? = null
        private set
    var title: String? = null
        private set
    var owner_id: Long = 0
        private set
    var target_block_ids: List<String>? = null
        private set
    var ref_items_count = 0
        private set
    var ref_layout_name: String? = null
        private set
    var ref_data_type: String? = null
        private set

    constructor(object_v: VKApiCatalogV2Button) {
        action = object_v.action?.let { CatalogV2Action(it) }
        name = object_v.name
        section_id = object_v.section_id
        title = object_v.title
        owner_id = object_v.owner_id
        target_block_ids = object_v.target_block_ids
        ref_items_count = object_v.ref_items_count
        ref_layout_name = object_v.ref_layout_name
        ref_data_type = object_v.ref_data_type
    }

    constructor(parcel: Parcel) {
        action = parcel.readTypedObjectCompat(CatalogV2Action.CREATOR)
        name = parcel.readString()
        section_id = parcel.readString()
        title = parcel.readString()
        owner_id = parcel.readLong()
        target_block_ids = parcel.createStringArrayList()
        ref_items_count = parcel.readInt()
        ref_layout_name = parcel.readString()
        ref_data_type = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedObjectCompat(action, flags)
        parcel.writeString(name)
        parcel.writeString(section_id)
        parcel.writeString(title)
        parcel.writeLong(owner_id)
        parcel.writeStringList(target_block_ids)
        parcel.writeInt(ref_items_count)
        parcel.writeString(ref_layout_name)
        parcel.writeString(ref_data_type)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CatalogV2Button> {
        override fun createFromParcel(parcel: Parcel): CatalogV2Button {
            return CatalogV2Button(parcel)
        }

        override fun newArray(size: Int): Array<CatalogV2Button?> {
            return arrayOfNulls(size)
        }
    }
}
