package dev.ragnarok.fenrir.activity.storypager

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.media.story.IStoryPlayer
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.Video

interface IStoryPagerView : IMvpView, IErrorView, IToastView {
    fun displayData(pageCount: Int, selectedIndex: Int)
    fun setAspectRatioAt(position: Int, w: Int, h: Int)
    fun setPreparingProgressVisible(position: Int, preparing: Boolean)
    fun attachDisplayToPlayer(adapterPosition: Int, storyPlayer: IStoryPlayer?)
    fun setToolbarTitle(@StringRes titleRes: Int, vararg params: Any?)
    fun setToolbarSubtitle(story: Story, account_id: Long, isPlaySpeed: Boolean)
    fun onShare(story: Story, account_id: Long)
    fun configHolder(adapterPosition: Int, progress: Boolean, aspectRatioW: Int, aspectRatioH: Int)
    fun onNext()
    fun requestWriteExternalStoragePermission()

    fun downloadPhoto(url: String, dir: String, file: String)
    fun downloadVideo(video: Video, url: String, Res: String)
}