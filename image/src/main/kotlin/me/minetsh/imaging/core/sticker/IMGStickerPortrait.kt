package me.minetsh.imaging.core.sticker

import android.graphics.Canvas
import android.graphics.RectF
import android.view.View

/**
 * Created by felix on 2017/11/16 下午5:54.
 */
interface IMGStickerPortrait {
    fun show(): Boolean
    fun remove(): Boolean
    fun dismiss(): Boolean

    fun isShowing(): Boolean

    fun getFrame(): RectF

    //    RectF getAdjustFrame();
    //
    //    RectF getDeleteFrame();
    fun onSticker(canvas: Canvas)
    fun registerCallback(callback: Callback)
    fun unregisterCallback(callback: Callback)
    interface Callback {
        fun <V> onDismiss(stickerView: V) where V : View, V : IMGSticker
        fun <V> onShowing(stickerView: V) where V : View, V : IMGSticker
        fun <V> onRemove(stickerView: V): Boolean where V : View, V : IMGSticker
    }
}
