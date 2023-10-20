package com.yalantis.ucrop.task

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import com.yalantis.ucrop.callback.BitmapLoadCallback
import com.yalantis.ucrop.io.AndroidSchedulers
import com.yalantis.ucrop.model.ExifInfo
import com.yalantis.ucrop.util.BitmapLoadUtils
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Creates and returns a Bitmap for a given Uri(String url).
 * inSampleSize is calculated based on requiredWidth property. However can be adjusted if OOM occurs.
 * If any EXIF config is found - bitmap is transformed properly.
 */
class BitmapLoadTask(
    @field:SuppressLint("StaticFieldLeak") private val mContext: Context,
    inputUri: Uri, outputUri: Uri?,
    requiredWidth: Int, requiredHeight: Int,
    loadCallback: BitmapLoadCallback
) {
    private val mOutputUri: Uri?
    private val mRequiredWidth: Int
    private val mRequiredHeight: Int
    private val mBitmapLoadCallback: BitmapLoadCallback
    private var mInputUri: Uri?

    init {
        mInputUri = inputUri
        mOutputUri = outputUri
        mRequiredWidth = requiredWidth
        mRequiredHeight = requiredHeight
        mBitmapLoadCallback = loadCallback
    }

    @SuppressLint("CheckResult")
    fun execute() {
        doInBackground().fromIOToMain().subscribe(
            { result: BitmapWorkerResult -> onPostExecute(result) }) { e: Throwable ->
            onPostExecute(
                BitmapWorkerResult(e as Exception)
            )
        }
    }

    private fun doInBackground(): Single<BitmapWorkerResult> {
        val mInputUriS = mInputUri
            ?: return Single.just(BitmapWorkerResult(NullPointerException("Input Uri cannot be null")))
        try {
            processInputUri()
        } catch (e: NullPointerException) {
            return Single.just(BitmapWorkerResult(e))
        } catch (e: IOException) {
            return Single.just(BitmapWorkerResult(e))
        }
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        options.inSampleSize =
            BitmapLoadUtils.calculateInSampleSize(options, mRequiredWidth, mRequiredHeight)
        options.inJustDecodeBounds = false
        var decodeSampledBitmap: Bitmap? = null
        var decodeAttemptSuccess = false
        while (!decodeAttemptSuccess) {
            try {
                val stream = mContext.contentResolver.openInputStream(
                    mInputUriS
                )
                try {
                    decodeSampledBitmap = BitmapFactory.decodeStream(stream, null, options)
                    if (options.outWidth == -1 || options.outHeight == -1) {
                        return Single.just(
                            BitmapWorkerResult(
                                IllegalArgumentException(
                                    "Bounds for bitmap could not be retrieved from the Uri: [$mInputUriS]"
                                )
                            )
                        )
                    }
                } finally {
                    BitmapLoadUtils.close(stream)
                }
                if (checkSize(decodeSampledBitmap, options)) continue
                decodeAttemptSuccess = true
            } catch (error: OutOfMemoryError) {
                Log.e(TAG, "doInBackground: BitmapFactory.decodeFileDescriptor: ", error)
                options.inSampleSize *= 2
            } catch (e: IOException) {
                Log.e(TAG, "doInBackground: ImageDecoder.createSource: ", e)
                return Single.just(
                    BitmapWorkerResult(
                        IllegalArgumentException(
                            "Bitmap could not be decoded from the Uri: [$mInputUriS]", e
                        )
                    )
                )
            }
        }
        if (decodeSampledBitmap == null) {
            return Single.just(
                BitmapWorkerResult(
                    IllegalArgumentException(
                        "Bitmap could not be decoded from the Uri: [$mInputUri]"
                    )
                )
            )
        }
        val exifOrientation = BitmapLoadUtils.getExifOrientation(mContext, mInputUriS)
        val exifDegrees = BitmapLoadUtils.exifToDegrees(exifOrientation)
        val exifTranslation = BitmapLoadUtils.exifToTranslation(exifOrientation)
        val exifInfo = ExifInfo(exifOrientation, exifDegrees, exifTranslation)
        val matrix = Matrix()
        if (exifDegrees != 0) {
            matrix.preRotate(exifDegrees.toFloat())
        }
        if (exifTranslation != 1) {
            matrix.postScale(exifTranslation.toFloat(), 1f)
        }
        return if (!matrix.isIdentity) {
            Single.just(
                BitmapWorkerResult(
                    BitmapLoadUtils.transformBitmap(
                        decodeSampledBitmap,
                        matrix
                    ), exifInfo
                )
            )
        } else Single.just(BitmapWorkerResult(decodeSampledBitmap, exifInfo))
    }

    @Throws(NullPointerException::class, IOException::class)
    private fun processInputUri() {
        val mInputUriS = mInputUri ?: return

        val inputUriScheme = mInputUriS.scheme
        Log.d(TAG, "Uri scheme: $inputUriScheme")
        if ("content" == inputUriScheme) {
            try {
                copyFile(mInputUriS, mOutputUri)
            } catch (e: NullPointerException) {
                Log.e(TAG, "Copying failed", e)
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Copying failed", e)
                throw e
            }
        } else if ("file" != inputUriScheme) {
            Log.e(TAG, "Invalid Uri scheme $inputUriScheme")
            throw IllegalArgumentException("Invalid Uri scheme$inputUriScheme")
        }
    }

    @Throws(NullPointerException::class, IOException::class)
    private fun copyFile(inputUri: Uri, outputUri: Uri?) {
        Log.d(TAG, "copyFile")
        if (outputUri == null) {
            throw NullPointerException("Output Uri is null - cannot copy image")
        }
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = mContext.contentResolver.openInputStream(inputUri)
            outputStream = FileOutputStream(outputUri.path)
            if (inputStream == null) {
                throw NullPointerException("InputStream for given input Uri is null")
            }
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
        } finally {
            BitmapLoadUtils.close(outputStream)
            BitmapLoadUtils.close(inputStream)

            // swap uris, because input image was copied to the output destination
            // (cropped image will override it later)
            mInputUri = mOutputUri
        }
    }

    private fun onPostExecute(result: BitmapWorkerResult) {
        result.mBitmapWorkerException?.let {
            mBitmapLoadCallback.onFailure(it)
        } ?: run {
            mInputUri?.let { k ->
                mBitmapLoadCallback.onBitmapLoaded(
                    result.mBitmapResult,
                    result.mExifInfo,
                    k.path,
                    mOutputUri?.path
                )
            } ?: mBitmapLoadCallback.onFailure(NullPointerException("mInputUri is null"))
        }
    }

    private fun checkSize(bitmap: Bitmap?, options: BitmapFactory.Options): Boolean {
        val bitmapSize = bitmap?.byteCount ?: 0
        if (bitmapSize > MAX_BITMAP_SIZE) {
            options.inSampleSize *= 2
            return true
        }
        return false
    }

    class BitmapWorkerResult {
        lateinit var mBitmapResult: Bitmap
        lateinit var mExifInfo: ExifInfo
        var mBitmapWorkerException: Exception? = null

        constructor(bitmapResult: Bitmap, exifInfo: ExifInfo) {
            mBitmapResult = bitmapResult
            mExifInfo = exifInfo
        }

        constructor(bitmapWorkerException: Exception) {
            mBitmapWorkerException = bitmapWorkerException
        }
    }

    companion object {
        private const val TAG = "BitmapWorkerTask"
        private const val MAX_BITMAP_SIZE = 100 * 1024 * 1024 // 100 MB
        inline fun <reified T : Any> Single<T>.fromIOToMain(): Single<T> =
            subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }
}
