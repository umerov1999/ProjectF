package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class WallEditorAttrs : Parcelable {
    private val owner: ParcelableOwnerWrapper
    private val editor: ParcelableOwnerWrapper

    constructor(owner: Owner, editor: Owner) {
        this.owner = ParcelableOwnerWrapper(owner)
        this.editor = ParcelableOwnerWrapper(editor)
    }

    internal constructor(`in`: Parcel) {
        owner = `in`.readTypedObjectCompat(ParcelableOwnerWrapper.CREATOR)!!
        editor = `in`.readTypedObjectCompat(ParcelableOwnerWrapper.CREATOR)!!
    }

    fun getOwner(): Owner {
        return owner.get()!!
    }

    fun getEditor(): Owner {
        return editor.get()!!
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedObjectCompat(owner, flags)
        dest.writeTypedObjectCompat(editor, flags)
    }

    companion object CREATOR : Parcelable.Creator<WallEditorAttrs> {
        override fun createFromParcel(parcel: Parcel): WallEditorAttrs {
            return WallEditorAttrs(parcel)
        }

        override fun newArray(size: Int): Array<WallEditorAttrs?> {
            return arrayOfNulls(size)
        }
    }
}