package dev.ragnarok.fenrir.fragment.search.photosearch

import dev.ragnarok.fenrir.fragment.search.abssearch.IBaseSearchView
import dev.ragnarok.fenrir.model.Photo

interface IPhotoSearchView : IBaseSearchView<Photo> {
    fun displayGallery(accountId: Long, photos: ArrayList<Photo>, position: Int)
}