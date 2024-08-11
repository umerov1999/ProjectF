package me.minetsh.imaging

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import me.minetsh.imaging.core.IMGMode
import me.minetsh.imaging.core.IMGText
import me.minetsh.imaging.core.file.IMGAssetFileDecoder
import me.minetsh.imaging.core.file.IMGDecoder
import me.minetsh.imaging.core.file.IMGFileDecoder
import me.minetsh.imaging.core.util.IMGUtils.inSampleSize
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Created by felix on 2017/11/14 下午2:26.
 */
class IMGEditActivity : IMGEditBaseActivity() {
    override fun onCreated() {}

    @get:Suppress("deprecation")
    override val bitmap: Bitmap?
        get() {
            val intent = intent ?: return null
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    EXTRA_IMAGE_URI,
                    Uri::class.java
                )
            } else {
                intent.getParcelableExtra(EXTRA_IMAGE_URI)
            }
            if (uri == null) {
                return null
            }
            var decoder: IMGDecoder? = null
            val path = uri.path
            if (!path.isNullOrEmpty() && uri.scheme != null) {
                decoder = when (uri.scheme) {
                    "asset" -> IMGAssetFileDecoder(this, uri)
                    "content", "file" -> IMGFileDecoder(this, uri)
                    else -> null
                }
            }
            if (decoder == null) {
                return null
            }
            val options = BitmapFactory.Options()
            options.inSampleSize = 1
            options.inJustDecodeBounds = true
            decoder.decode(options)
            if (options.outWidth > MAX_WIDTH) {
                options.inSampleSize =
                    inSampleSize((1f * options.outWidth / MAX_WIDTH).roundToInt())
            }
            if (options.outHeight > MAX_HEIGHT) {
                options.inSampleSize = max(
                    options.inSampleSize,
                    inSampleSize((1f * options.outHeight / MAX_HEIGHT).roundToInt())
                )
            }
            options.inJustDecodeBounds = false
            return decoder.decode(options)
        }

    override fun onText(text: IMGText?) {
        mImgView?.addStickerText(text)
    }

    override fun onModeClick(mode: IMGMode?) {
        var modeS = mode
        val cm = mImgView?.mode
        if (cm === mode) {
            modeS = IMGMode.NONE
        }
        mImgView?.mode = modeS
        updateModeUI()
        if (modeS === IMGMode.CLIP) {
            setOpDisplay(OP_CLIP)
        }
    }

    override fun onUndoClick() {
        val mode = mImgView?.mode
        if (mode === IMGMode.DOODLE) {
            mImgView?.undoDoodle()
        } else if (mode === IMGMode.MOSAIC) {
            mImgView?.undoMosaic()
        }
    }

    override fun onCancelClick() {
        finish()
    }

    override fun onDoneClick() {
        val path = intent.getStringExtra(EXTRA_IMAGE_SAVE_PATH)
        if (!path.isNullOrEmpty()) {
            val bitmap = mImgView?.saveBitmap()
            if (bitmap != null) {
                var fout: FileOutputStream? = null
                try {
                    fout = FileOutputStream(path)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } finally {
                    if (fout != null) {
                        try {
                            fout.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
                setResult(RESULT_OK, Intent().putExtra(EXTRA_IMAGE_SAVE_PATH, path))
                finish()
                return
            }
        }
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onCancelClipClick() {
        mImgView?.cancelClip()
        setOpDisplay(if (mImgView?.mode === IMGMode.CLIP) OP_CLIP else OP_NORMAL)
    }

    override fun onDoneClipClick() {
        mImgView?.doClip()
        setOpDisplay(if (mImgView?.mode === IMGMode.CLIP) OP_CLIP else OP_NORMAL)
    }

    override fun onResetClipClick() {
        mImgView?.resetClip()
    }

    override fun onRotateClipClick() {
        mImgView?.doRotate()
    }

    override fun onColorChanged(checkedColor: Int) {
        mImgView?.setPenColor(checkedColor)
    }

    companion object {
        const val EXTRA_IMAGE_URI = "IMAGE_URI"
        const val EXTRA_IMAGE_SAVE_PATH = "IMAGE_SAVE_PATH"
        private const val MAX_WIDTH = 1024
        private const val MAX_HEIGHT = 1024
    }
}
