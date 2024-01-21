package me.minetsh.imaging.core.sticker

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.view.View
import me.minetsh.imaging.view.IMGStickerView

/**
 * Created by felix on 2017/11/16 下午5:52.
 */
class IMGStickerHelper(private val mView: IMGStickerView) : IMGStickerPortrait,
    IMGStickerPortrait.Callback {
    private var mFrame: RectF? = null
    private var mCallback: IMGStickerPortrait.Callback? = null
    private var isShowing = false
    override fun show(): Boolean {
        if (!isShowing()) {
            isShowing = true
            onShowing(mView)
            return true
        }
        return false
    }

    override fun remove(): Boolean {
        return onRemove(mView)
    }

    override fun dismiss(): Boolean {
        if (isShowing()) {
            isShowing = false
            onDismiss(mView)
            return true
        }
        return false
    }

    override fun isShowing(): Boolean {
        return isShowing
    }

    override fun getFrame(): RectF {
        if (mFrame == null) {
            mFrame = RectF(0f, 0f, mView.width.toFloat(), mView.height.toFloat())
            val pivotX = mView.x + mView.pivotX
            val pivotY = mView.y + mView.pivotY
            val matrix = Matrix()
            matrix.setTranslate(mView.x, mView.y)
            matrix.postScale(mView.scaleX, mView.scaleY, pivotX, pivotY)
            matrix.mapRect(mFrame)
        }
        return mFrame ?: RectF(0f, 0f, 0f, 0f)
    }

    override fun onSticker(canvas: Canvas) {
        // empty
    }

    override fun registerCallback(callback: IMGStickerPortrait.Callback) {
        mCallback = callback
    }

    override fun unregisterCallback(callback: IMGStickerPortrait.Callback) {
        mCallback = null
    }

    override fun <V> onRemove(stickerView: V): Boolean where V : View, V : IMGSticker {
        return mCallback?.onRemove(stickerView) == true
    }

    override fun <V> onDismiss(stickerView: V) where V : View, V : IMGSticker {
        mFrame = null
        stickerView.invalidate()
        mCallback?.onDismiss(stickerView)
    }

    override fun <V> onShowing(stickerView: V) where V : View, V : IMGSticker {
        stickerView.invalidate()
        mCallback?.onShowing(stickerView)
    }
}
