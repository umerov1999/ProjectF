package dev.ragnarok.filegallery.fragment.tagowner

import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.fragment.base.core.IToastView
import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.tags.TagOwner

interface ITagOwnerView : IMvpView, IErrorView, IToastView {
    fun displayData(data: List<TagOwner>)
    fun notifyChanges()
    fun notifyAdd(index: Int)
    fun notifyRemove(index: Int)
    fun successAdd(owner: TagOwner, item: FileItem)
}