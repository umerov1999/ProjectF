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
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import io.reactivex.rxjava3.core.Single

class PhotoSearchPresenter(
    accountId: Int,
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
        accountId: Int,
        criteria: PhotoSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<Photo>, IntNextFrom>> {
        val offset = startFrom.offset
        val nextFrom = IntNextFrom(50 + offset)
        return photoInteractor.search(accountId, criteria, offset, 50)
            .map { photos -> create(photos, nextFrom) }
    }

    override fun instantiateEmptyCriteria(): PhotoSearchCriteria {
        return PhotoSearchCriteria("")
    }

    override fun canSearch(criteria: PhotoSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty() || (criteria?.findOptionByKey(
            PhotoSearchCriteria.KEY_GPS
        ) as SimpleGPSOption?)?.has() == true
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
        view?.displayGallery(
            accountId,
            photos_ret,
            finalIndex
        )
    }
}