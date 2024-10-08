package dev.ragnarok.fenrir.dialog

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.Upload

class ImageSizeAlertDialog internal constructor(builder: Builder) {
    private val mActivity: Activity = builder.mActivity
    private val mOnSelectedCallback: OnSelectedCallback?
    private val mOnCancelCallback: OnCancelCallback?
    fun show() {
        MaterialAlertDialogBuilder(mActivity)
            .setTitle(mActivity.getString(R.string.select_image_size_title))
            .setItems(R.array.array_image_sizes_names) { _, j ->
                val selectedSize = when (j) {
                    0 -> Upload.IMAGE_SIZE_800
                    1 -> Upload.IMAGE_SIZE_1200
                    2 -> Upload.IMAGE_SIZE_FULL
                    3 -> Upload.IMAGE_SIZE_CROPPING
                    else -> Upload.IMAGE_SIZE_FULL
                }
                mOnSelectedCallback?.onSizeSelected(selectedSize)
            }
            .setCancelable(false)
            .setNegativeButton(R.string.button_cancel) { _, _ -> mOnCancelCallback?.onCancel() }
            .show()
    }

    interface OnSelectedCallback {
        fun onSizeSelected(size: Int)
    }

    interface OnCancelCallback {
        fun onCancel()
    }

    interface Callback {
        fun onSizeSelected(size: Int)
    }

    class Builder(val mActivity: Activity) {
        var mOnSelectedCallback: OnSelectedCallback? = null
        var mOnCancelCallback: OnCancelCallback? = null
        fun setOnSelectedCallback(onSelectedCallback: OnSelectedCallback?): Builder {
            mOnSelectedCallback = onSelectedCallback
            return this
        }

        fun setOnCancelCallback(onCancelCallback: OnCancelCallback?): Builder {
            mOnCancelCallback = onCancelCallback
            return this
        }

        fun build(): ImageSizeAlertDialog {
            return ImageSizeAlertDialog(this)
        }

        fun show() {
            build().show()
        }
    }

    companion object {

        fun showUploadPhotoSizeIfNeed(activity: Activity, callback: Callback) {
            val size = Settings.get()
                .main()
                .uploadImageSize
            if (size == null) {
                val dialog = MaterialAlertDialogBuilder(activity)
                    .setTitle(activity.getString(R.string.select_image_size_title))
                    .setItems(R.array.array_image_sizes_names) { _, j ->
                        val selectedSize = when (j) {
                            0 -> Upload.IMAGE_SIZE_800
                            1 -> Upload.IMAGE_SIZE_1200
                            2 -> Upload.IMAGE_SIZE_FULL
                            3 -> Upload.IMAGE_SIZE_CROPPING
                            else -> Upload.IMAGE_SIZE_FULL
                        }
                        callback.onSizeSelected(selectedSize)
                    }.setCancelable(true).create()
                dialog.show()
            } else {
                callback.onSizeSelected(size)
            }
        }
    }

    init {
        mOnCancelCallback = builder.mOnCancelCallback
        mOnSelectedCallback = builder.mOnSelectedCallback
    }
}