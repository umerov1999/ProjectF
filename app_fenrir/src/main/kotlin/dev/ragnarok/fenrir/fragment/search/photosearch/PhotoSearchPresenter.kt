package dev.ragnarok.fenrir.fragment.search.photosearch

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IPhotosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.PhotoSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.fragment.search.options.SimpleGPSOption
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class PhotoSearchPresenter(
    accountId: Long,
    criteria: PhotoSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IPhotoSearchView, PhotoSearchCriteria, Photo, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val photoInteractor: IPhotosInteractor = InteractorFactory.createPhotosInteractor()
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun readParcelSaved(savedInstanceState: Bundle, key: String): PhotoSearchCriteria? {
        return savedInstanceState.getParcelableCompat(key)
    }

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun doSearch(
        accountId: Long,
        criteria: PhotoSearchCriteria,
        startFrom: IntNextFrom
    ): Flow<Pair<List<Photo>, IntNextFrom>> {
        val offset = startFrom.offset
        val nextFrom = IntNextFrom(50 + offset)
        return photoInteractor.search(accountId, criteria, offset, 50)
            .map { photos -> create(photos, nextFrom) }
    }

    override fun instantiateEmptyCriteria(): PhotoSearchCriteria {
        return PhotoSearchCriteria("")
    }

    override fun canSearch(criteria: PhotoSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty() || (criteria?.findOptionByKey<SimpleGPSOption>(
            PhotoSearchCriteria.KEY_GPS
        ))?.has() == true
    }

    fun firePhotoClick(wrapper: Photo) {
        var Index = 0
        var trig = false
        val photos_ret = ArrayList<Photo>(data.size)
        for (i in data.indices) {
            val photo = data[i]
            photos_ret.add(photo)
            if (!trig && photo.getObjectId() == wrapper.getObjectId() && photo.ownerId == wrapper.ownerId) {
                Index = i
                trig = true
            }
        }
        val finalIndex = Index
        if (FenrirNative.isNativeLoaded && Settings.get().main().isNative_parcel_photo) {
            appendJob(
                flow {
                    val mem = ParcelNative.create(ParcelFlags.NULL_LIST)
                    mem.writeInt(photos_ret.size)
                    for (i in photos_ret.indices) {
                        if (!isActive()) {
                            mem.forceDestroy()
                            return@flow
                        }
                        val photo = photos_ret[i]
                        mem.writeParcelable(photo)
                    }
                    if (!isActive()) {
                        mem.forceDestroy()
                    } else {
                        emit(mem.nativePointer)
                    }
                }.fromIOToMain({
                    view?.displayGalleryNative(
                        accountId,
                        it,
                        finalIndex
                    )
                }) { obj -> obj.printStackTrace() })
        } else {
            view?.displayGallery(
                accountId,
                photos_ret,
                finalIndex
            )
        }
    }
}