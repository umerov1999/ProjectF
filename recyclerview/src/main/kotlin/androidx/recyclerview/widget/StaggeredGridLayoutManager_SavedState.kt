package androidx.recyclerview.widget

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

/**
 * @hide
 */
@Serializable
class StaggeredGridLayoutManager_SavedState : Parcelable {
    @JvmField
    var mAnchorPosition = 0

    @JvmField
    var mVisibleAnchorPosition = 0

    @JvmField
    var mSpanOffsetsSize = 0

    @JvmField
    var mSpanOffsets: IntArray? = null

    @JvmField
    var mSpanLookupSize = 0

    @JvmField
    var mSpanLookup: IntArray? = null

    @JvmField
    var mFullSpanItems: List<FullSpanItem>? = null

    @JvmField
    var mReverseLayout = false

    @JvmField
    var mAnchorLayoutFromEnd = false

    @JvmField
    var mLastLayoutRTL = false

    constructor()
    internal constructor(parcel: Parcel) {
        mAnchorPosition = parcel.readInt()
        mVisibleAnchorPosition = parcel.readInt()
        mSpanOffsetsSize = parcel.readInt()
        if (mSpanOffsetsSize > 0) {
            mSpanOffsets = IntArray(mSpanOffsetsSize)
            parcel.readIntArray(mSpanOffsets!!)
        }
        mSpanLookupSize = parcel.readInt()
        if (mSpanLookupSize > 0) {
            mSpanLookup = IntArray(mSpanLookupSize)
            parcel.readIntArray(mSpanLookup!!)
        }
        mReverseLayout = parcel.readInt() != 0
        mAnchorLayoutFromEnd = parcel.readInt() != 0
        mLastLayoutRTL = parcel.readInt() != 0
        val fullSpanItems: ArrayList<FullSpanItem>? =
            parcel.createTypedArrayList(FullSpanItem.CREATOR)
        mFullSpanItems = fullSpanItems
    }

    constructor(other: StaggeredGridLayoutManager_SavedState) {
        mSpanOffsetsSize = other.mSpanOffsetsSize
        mAnchorPosition = other.mAnchorPosition
        mVisibleAnchorPosition = other.mVisibleAnchorPosition
        mSpanOffsets = other.mSpanOffsets
        mSpanLookupSize = other.mSpanLookupSize
        mSpanLookup = other.mSpanLookup
        mReverseLayout = other.mReverseLayout
        mAnchorLayoutFromEnd = other.mAnchorLayoutFromEnd
        mLastLayoutRTL = other.mLastLayoutRTL
        mFullSpanItems = other.mFullSpanItems
    }

    fun invalidateSpanInfo() {
        mSpanOffsets = null
        mSpanOffsetsSize = 0
        mSpanLookupSize = 0
        mSpanLookup = null
        mFullSpanItems = null
    }

    fun invalidateAnchorPositionInfo() {
        mSpanOffsets = null
        mSpanOffsetsSize = 0
        mAnchorPosition = RecyclerView.NO_POSITION
        mVisibleAnchorPosition = RecyclerView.NO_POSITION
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(mAnchorPosition)
        dest.writeInt(mVisibleAnchorPosition)
        dest.writeInt(mSpanOffsetsSize)
        if (mSpanOffsetsSize > 0) {
            dest.writeIntArray(mSpanOffsets)
        }
        dest.writeInt(mSpanLookupSize)
        if (mSpanLookupSize > 0) {
            dest.writeIntArray(mSpanLookup)
        }
        dest.writeInt(if (mReverseLayout) 1 else 0)
        dest.writeInt(if (mAnchorLayoutFromEnd) 1 else 0)
        dest.writeInt(if (mLastLayoutRTL) 1 else 0)
        dest.writeList(mFullSpanItems)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<StaggeredGridLayoutManager_SavedState> =
            object : Parcelable.Creator<StaggeredGridLayoutManager_SavedState> {
                override fun createFromParcel(parcel: Parcel): StaggeredGridLayoutManager_SavedState {
                    return StaggeredGridLayoutManager_SavedState(parcel)
                }

                override fun newArray(size: Int): Array<StaggeredGridLayoutManager_SavedState?> {
                    return arrayOfNulls(size)
                }
            }
    }
}

/**
 * We keep information about full span items because they may create gaps in the UI.
 */
@Serializable
class FullSpanItem : Parcelable {
    @JvmField
    var mPosition = 0

    @JvmField
    var mGapDir = 0

    @JvmField
    var mGapPerSpan: IntArray? = null

    // A full span may be laid out in primary direction but may have gaps due to
    // invalidation of views after it. This is recorded during a reverse scroll and if
    // view is still on the screen after scroll stops, we have to recalculate layout
    @JvmField
    var mHasUnwantedGapAfter = false

    constructor(parcel: Parcel) {
        mPosition = parcel.readInt()
        mGapDir = parcel.readInt()
        mHasUnwantedGapAfter = parcel.readInt() != 0
        val spanCount = parcel.readInt()
        if (spanCount > 0) {
            mGapPerSpan = IntArray(spanCount)
            parcel.readIntArray(mGapPerSpan!!)
        }
    }

    constructor()

    fun getGapForSpan(spanIndex: Int): Int {
        return if (mGapPerSpan == null) 0 else mGapPerSpan!![spanIndex]
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(mPosition)
        dest.writeInt(mGapDir)
        dest.writeInt(if (mHasUnwantedGapAfter) 1 else 0)
        if (mGapPerSpan?.isNotEmpty() == true) {
            dest.writeInt(mGapPerSpan?.size ?: 0)
            dest.writeIntArray(mGapPerSpan)
        } else {
            dest.writeInt(0)
        }
    }

    override fun toString(): String {
        return ("FullSpanItem{"
                + "mPosition=" + mPosition
                + ", mGapDir=" + mGapDir
                + ", mHasUnwantedGapAfter=" + mHasUnwantedGapAfter
                + ", mGapPerSpan=" + mGapPerSpan.contentToString()
                + '}')
    }

    companion object CREATOR : Parcelable.Creator<FullSpanItem> {
        override fun createFromParcel(parcel: Parcel): FullSpanItem {
            return FullSpanItem(parcel)
        }

        override fun newArray(size: Int): Array<FullSpanItem?> {
            return arrayOfNulls(size)
        }
    }
}
