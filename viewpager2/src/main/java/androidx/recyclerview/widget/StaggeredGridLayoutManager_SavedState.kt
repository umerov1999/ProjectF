package androidx.recyclerview.widget

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import java.util.*

/**
 * @hide
 */
@Serializable
class StaggeredGridLayoutManager_SavedState : Parcelable {
    var mAnchorPosition = 0
    var mVisibleAnchorPosition // Replacement for span info when spans are invalidated
            = 0
    var mSpanOffsetsSize = 0
    var mSpanOffsets: IntArray? = null
    var mSpanLookupSize = 0
    var mSpanLookup: IntArray? = null
    var mFullSpanItems: ArrayList<FullSpanItem>? = null
    var mReverseLayout = false
    var mAnchorLayoutFromEnd = false
    var mLastLayoutRTL = false

    constructor()
    internal constructor(`in`: Parcel) {
        mAnchorPosition = `in`.readInt()
        mVisibleAnchorPosition = `in`.readInt()
        mSpanOffsetsSize = `in`.readInt()
        if (mSpanOffsetsSize > 0) {
            mSpanOffsets = IntArray(mSpanOffsetsSize)
            `in`.readIntArray(mSpanOffsets!!)
        }
        mSpanLookupSize = `in`.readInt()
        if (mSpanLookupSize > 0) {
            mSpanLookup = IntArray(mSpanLookupSize)
            `in`.readIntArray(mSpanLookup!!)
        }
        mReverseLayout = `in`.readInt() != 0
        mAnchorLayoutFromEnd = `in`.readInt() != 0
        mLastLayoutRTL = `in`.readInt() != 0
        val fullSpanItems: ArrayList<FullSpanItem>? =
            `in`.createTypedArrayList(FullSpanItem.CREATOR)
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
                override fun createFromParcel(`in`: Parcel): StaggeredGridLayoutManager_SavedState {
                    return StaggeredGridLayoutManager_SavedState(`in`)
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
    var mPosition = 0
    var mGapDir = 0
    var mGapPerSpan: IntArray? = null

    // A full span may be laid out in primary direction but may have gaps due to
    // invalidation of views after it. This is recorded during a reverse scroll and if
    // view is still on the screen after scroll stops, we have to recalculate layout
    var mHasUnwantedGapAfter = false

    constructor(`in`: Parcel) {
        mPosition = `in`.readInt()
        mGapDir = `in`.readInt()
        mHasUnwantedGapAfter = `in`.readInt() != 0
        val spanCount = `in`.readInt()
        if (spanCount > 0) {
            mGapPerSpan = IntArray(spanCount)
            `in`.readIntArray(mGapPerSpan!!)
        }
    }

    @Suppress("unused")
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
                + ", mGapPerSpan=" + Arrays.toString(mGapPerSpan)
                + '}')
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FullSpanItem> =
            object : Parcelable.Creator<FullSpanItem> {
                override fun createFromParcel(`in`: Parcel): FullSpanItem {
                    return FullSpanItem(`in`)
                }

                override fun newArray(size: Int): Array<FullSpanItem?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
