package dev.ragnarok.filegallery.fragment.tagdir

import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.model.tags.TagDir

interface ITagDirView : IMvpView, IErrorView {
    fun displayData(data: List<TagDir>)
    fun notifyChanges()
    fun notifyRemove(index: Int)
    fun onScrollTo(pos: Int)
    fun notifyItemChanged(pos: Int)

    fun displayGalleryUnSafe(parcelNativePointer: Long, position: Int, reversed: Boolean)
    fun displayVideo(video: Video)
    fun startPlayAudios(audios: ArrayList<Audio>, position: Int)
}
