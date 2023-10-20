package com.yalantis.ucrop.task

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.io.AndroidSchedulers.mainThread
import com.yalantis.ucrop.model.CropParameters
import com.yalantis.ucrop.model.ImageState
import com.yalantis.ucrop.util.BitmapLoadUtils
import com.yalantis.ucrop.util.FileUtils
import com.yalantis.ucrop.util.ImageHeaderParser
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Crops part of image that fills the crop bounds.
 *
 *
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
class BitmapCropTask(
    context: Context,
    private var mViewBitmap: Bitmap?,
    imageState: ImageState,
    cropParameters: CropParameters,
    private val mCropCallback: BitmapCropCallback?
) {
    private val mContext: WeakReference<Context>
    private val mCropRect: RectF
    private val mCurrentImageRect: RectF
    private val mCurrentAngle: Float
    private val mMaxResultImageSizeX: Int
    private val mMaxResultImageSizeY: Int
    private val mCompressFormat: CompressFormat
    private val mCompressQuality: Int
    private val mImageInputPath: String?
    private val mImageOutputPath: String?
    private var mCurrentScale: Float
    private var mCroppedImageWidth = 0
    private var mCroppedImageHeight = 0
    private var cropOffsetX = 0
    private var cropOffsetY = 0

    init {
        mContext = WeakReference(context)
        mCropRect = imageState.cropRect
        mCurrentImageRect = imageState.currentImageRect
        mCurrentScale = imageState.currentScale
        mCurrentAngle = imageState.currentAngle
        mMaxResultImageSizeX = cropParameters.maxResultImageSizeX
        mMaxResultImageSizeY = cropParameters.maxResultImageSizeY
        mCompressFormat = cropParameters.compressFormat
        mCompressQuality = cropParameters.compressQuality
        mImageInputPath = cropParameters.imageInputPath
        mImageOutputPath = cropParameters.imageOutputPath
    }

    @SuppressLint("CheckResult")
    fun execute() {
        doInBackground().fromIOToMain()
            .subscribe({
                val uri = Uri.fromFile(mImageOutputPath?.let { File(it) })
                mCropCallback?.onBitmapCropped(
                    uri,
                    cropOffsetX,
                    cropOffsetY,
                    mCroppedImageWidth,
                    mCroppedImageHeight
                )
            }) {
                mCropCallback?.onCropFailure(
                    it
                )
            }
    }

    private fun doInBackground(): Completable {
        if (mViewBitmap == null) {
            return Completable.error(NullPointerException("ViewBitmap is null"))
        } else if (mViewBitmap?.isRecycled == true) {
            return Completable.error(NullPointerException("ViewBitmap is recycled"))
        } else if (mCurrentImageRect.isEmpty) {
            return Completable.error(NullPointerException("CurrentImageRect is empty"))
        }
        mViewBitmap = try {
            crop()
            null
        } catch (throwable: Throwable) {
            return Completable.error(throwable)
        }
        return Completable.complete()
    }

    @Throws(IOException::class)
    private fun crop(): Boolean {
        val mImageInputPathS = mImageInputPath ?: return false
        val mImageOutputPathS = mImageOutputPath ?: return false
        // Downsize if needed
        if (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0) {
            val cropWidth = mCropRect.width() / mCurrentScale
            val cropHeight = mCropRect.height() / mCurrentScale
            if (cropWidth > mMaxResultImageSizeX || cropHeight > mMaxResultImageSizeY) {
                val scaleX = mMaxResultImageSizeX / cropWidth
                val scaleY = mMaxResultImageSizeY / cropHeight
                val resizeScale = min(scaleX, scaleY)

                val mViewBitmapS = mViewBitmap ?: return false
                val resizedBitmap = Bitmap.createScaledBitmap(
                    mViewBitmapS,
                    (mViewBitmapS.width * resizeScale).roundToInt(),
                    (mViewBitmapS.height * resizeScale).roundToInt(), false
                )
                if (mViewBitmap != resizedBitmap) {
                    mViewBitmap?.recycle()
                }
                mViewBitmap = resizedBitmap
                mCurrentScale /= resizeScale
            }
        }

        // Rotate if needed
        if (mCurrentAngle != 0f) {
            val tempMatrix = Matrix()

            val mViewBitmapS = mViewBitmap ?: return false
            tempMatrix.setRotate(
                mCurrentAngle,
                mViewBitmapS.width.toFloat() / 2,
                mViewBitmapS.height.toFloat() / 2
            )
            val rotatedBitmap = Bitmap.createBitmap(
                mViewBitmapS, 0, 0, mViewBitmapS.width, mViewBitmapS.height,
                tempMatrix, true
            )
            if (mViewBitmap != rotatedBitmap) {
                mViewBitmap?.recycle()
            }
            mViewBitmap = rotatedBitmap
        }
        cropOffsetX = ((mCropRect.left - mCurrentImageRect.left) / mCurrentScale).roundToInt()
        cropOffsetY = ((mCropRect.top - mCurrentImageRect.top) / mCurrentScale).roundToInt()
        mCroppedImageWidth = (mCropRect.width() / mCurrentScale).roundToInt()
        mCroppedImageHeight = (mCropRect.height() / mCurrentScale).roundToInt()
        val shouldCrop = shouldCrop(mCroppedImageWidth, mCroppedImageHeight)
        Log.i(TAG, "Should crop: $shouldCrop")
        return if (shouldCrop) {
            val originalExif = ExifInterface(
                mImageInputPathS
            )
            val mViewBitmapS = mViewBitmap ?: return false
            saveImage(
                Bitmap.createBitmap(
                    mViewBitmapS,
                    cropOffsetX,
                    cropOffsetY,
                    mCroppedImageWidth,
                    mCroppedImageHeight
                )
            )
            if (mCompressFormat == CompressFormat.JPEG) {
                ImageHeaderParser.copyExif(
                    originalExif,
                    mCroppedImageWidth,
                    mCroppedImageHeight,
                    mImageOutputPath
                )
            }
            true
        } else {
            FileUtils.copyFile(mImageInputPathS, mImageOutputPathS)
            false
        }
    }

    private fun saveImage(croppedBitmap: Bitmap) {
        var outputStream: OutputStream? = null
        var outStream: ByteArrayOutputStream? = null
        try {
            outputStream = FileOutputStream(mImageOutputPath, false)
            outStream = ByteArrayOutputStream()
            croppedBitmap.compress(mCompressFormat, mCompressQuality, outStream)
            outputStream.write(outStream.toByteArray())
            croppedBitmap.recycle()
        } catch (exc: IOException) {
            Log.e(TAG, exc.localizedMessage.orEmpty())
        } finally {
            BitmapLoadUtils.close(outputStream)
            BitmapLoadUtils.close(outStream)
        }
    }

    /**
     * Check whether an image should be cropped at all or just file can be copied to the destination path.
     * For each 1000 pixels there is one pixel of error due to matrix calculations etc.
     *
     * @param width  - crop area width
     * @param height - crop area height
     * @return - true if image must be cropped, false - if original image fits requirements
     */
    private fun shouldCrop(width: Int, height: Int): Boolean {
        var pixelError = 1
        pixelError += (max(width, height) / 1000f).roundToInt()
        return mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0 || abs(mCropRect.left - mCurrentImageRect.left) > pixelError || abs(
            mCropRect.top - mCurrentImageRect.top
        ) > pixelError || abs(mCropRect.bottom - mCurrentImageRect.bottom) > pixelError || abs(
            mCropRect.right - mCurrentImageRect.right
        ) > pixelError || mCurrentAngle != 0f
    }

    companion object {
        private const val TAG = "BitmapCropTask"
        private fun Completable.fromIOToMain(): Completable =
            subscribeOn(Schedulers.io()).observeOn(mainThread())
    }
}