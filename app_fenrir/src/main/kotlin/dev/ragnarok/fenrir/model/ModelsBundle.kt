package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.wrap

class ModelsBundle : Parcelable, Iterable<AbsModel> {
    private val wrappers: MutableList<ParcelableModelWrapper>

    constructor() {
        wrappers = ArrayList()
    }

    constructor(capacity: Int) {
        wrappers = ArrayList(capacity)
    }

    internal constructor(parcel: Parcel) {
        wrappers = parcel.createTypedArrayList(ParcelableModelWrapper.CREATOR)!!
    }

    fun size(): Int {
        return wrappers.size
    }

    fun clear() {
        wrappers.clear()
    }

    fun append(model: AbsModel): ModelsBundle {
        wrappers.add(wrap(model))
        return this
    }

    fun remove(model: AbsModel) {
        wrappers.remove(wrap(model))
    }

    fun append(data: Collection<AbsModel>): ModelsBundle {
        for (model in data) {
            wrappers.add(wrap(model))
        }
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(wrappers)
    }

    override fun iterator(): MutableIterator<AbsModel> {
        return BundleIterator(wrappers.iterator())
    }

    private class BundleIterator(val internal: MutableIterator<ParcelableModelWrapper>) :
        MutableIterator<AbsModel> {
        override fun hasNext(): Boolean {
            return internal.hasNext()
        }

        override fun next(): AbsModel {
            return internal.next().get()
        }

        override fun remove() {
            internal.remove()
        }
    }

    companion object CREATOR : Parcelable.Creator<ModelsBundle> {
        override fun createFromParcel(parcel: Parcel): ModelsBundle {
            return ModelsBundle(parcel)
        }

        override fun newArray(size: Int): Array<ModelsBundle?> {
            return arrayOfNulls(size)
        }
    }
}