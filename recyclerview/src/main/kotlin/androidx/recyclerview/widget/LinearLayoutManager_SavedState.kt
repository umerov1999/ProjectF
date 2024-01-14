package androidx.recyclerview.widget

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

/**
 * @hide
 */
@Serializable
class LinearLayoutManager_SavedState : Parcelable {
    @JvmField
    var mAnchorPosition = 0

    @JvmField
    var mAnchorOffset = 0

    @JvmField
    var mAnchorLayoutFromEnd = false

    constructor()
    internal constructor(parcel: Parcel) {
        mAnchorPosition = parcel.readInt()
        mAnchorOffset = parcel.readInt()
        mAnchorLayoutFromEnd = parcel.readInt() != 0
    }

    constructor(other: LinearLayoutManager_SavedState) {
        mAnchorPosition = other.mAnchorPosition
        mAnchorOffset = other.mAnchorOffset
        mAnchorLayoutFromEnd = other.mAnchorLayoutFromEnd
    }

    fun hasValidAnchor(): Boolean {
        return mAnchorPosition >= 0
    }

    fun invalidateAnchor() {
        mAnchorPosition = RecyclerView.NO_POSITION
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(mAnchorPosition)
        dest.writeInt(mAnchorOffset)
        dest.writeInt(if (mAnchorLayoutFromEnd) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<LinearLayoutManager_SavedState> {
        override fun createFromParcel(parcel: Parcel): LinearLayoutManager_SavedState {
            return LinearLayoutManager_SavedState(parcel)
        }

        override fun newArray(size: Int): Array<LinearLayoutManager_SavedState?> {
            return arrayOfNulls(size)
        }
    }
}
