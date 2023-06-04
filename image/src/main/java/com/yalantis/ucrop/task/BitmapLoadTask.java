package com.yalantis.ucrop.task;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yalantis.ucrop.callback.BitmapLoadCallback;
import com.yalantis.ucrop.io.AndroidSchedulers;
import com.yalantis.ucrop.model.ExifInfo;
import com.yalantis.ucrop.util.BitmapLoadUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Creates and returns a Bitmap for a given Uri(String url).
 * inSampleSize is calculated based on requiredWidth property. However can be adjusted if OOM occurs.
 * If any EXIF config is found - bitmap is transformed properly.
 */
public class BitmapLoadTask {

    private static final String TAG = "BitmapWorkerTask";

    private static final int MAX_BITMAP_SIZE = 100 * 1024 * 1024;   // 100 MB

    @SuppressLint("StaticFieldLeak")
    private final Context mContext;
    private final Uri mOutputUri;
    private final int mRequiredWidth;
    private final int mRequiredHeight;
    private final BitmapLoadCallback mBitmapLoadCallback;
    private Uri mInputUri;

    public BitmapLoadTask(@NonNull Context context,
                          @NonNull Uri inputUri, @Nullable Uri outputUri,
                          int requiredWidth, int requiredHeight,
                          BitmapLoadCallback loadCallback) {
        mContext = context;
        mInputUri = inputUri;
        mOutputUri = outputUri;
        mRequiredWidth = requiredWidth;
        mRequiredHeight = requiredHeight;
        mBitmapLoadCallback = loadCallback;
    }

    private static <T> SingleTransformer<T, T> applySingleIOToMainSchedulers() {
        return upstream -> upstream
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressLint("CheckResult")
    public void execute() {
        doInBackground().compose(applySingleIOToMainSchedulers()).subscribe(this::onPostExecute, e -> onPostExecute(new BitmapWorkerResult((Exception) e)));
    }

    private Single<BitmapWorkerResult> doInBackground() {
        if (mInputUri == null) {
            return Single.just(new BitmapWorkerResult(new NullPointerException("Input Uri cannot be null")));
        }

        try {
            processInputUri();
        } catch (NullPointerException | IOException e) {
            return Single.just(new BitmapWorkerResult(e));
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = BitmapLoadUtils.calculateInSampleSize(options, mRequiredWidth, mRequiredHeight);
        options.inJustDecodeBounds = false;

        Bitmap decodeSampledBitmap = null;

        boolean decodeAttemptSuccess = false;
        while (!decodeAttemptSuccess) {
            try {
                InputStream stream = mContext.getContentResolver().openInputStream(mInputUri);
                try {
                    decodeSampledBitmap = BitmapFactory.decodeStream(stream, null, options);
                    if (options.outWidth == -1 || options.outHeight == -1) {
                        return Single.just(new BitmapWorkerResult(new IllegalArgumentException("Bounds for bitmap could not be retrieved from the Uri: [" + mInputUri + "]")));
                    }
                } finally {
                    BitmapLoadUtils.close(stream);
                }
                if (checkSize(decodeSampledBitmap, options)) continue;
                decodeAttemptSuccess = true;
            } catch (OutOfMemoryError error) {
                Log.e(TAG, "doInBackground: BitmapFactory.decodeFileDescriptor: ", error);
                options.inSampleSize *= 2;
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: ImageDecoder.createSource: ", e);
                return Single.just(new BitmapWorkerResult(new IllegalArgumentException("Bitmap could not be decoded from the Uri: [" + mInputUri + "]", e)));
            }
        }

        if (decodeSampledBitmap == null) {
            return Single.just(new BitmapWorkerResult(new IllegalArgumentException("Bitmap could not be decoded from the Uri: [" + mInputUri + "]")));
        }

        int exifOrientation = BitmapLoadUtils.getExifOrientation(mContext, mInputUri);
        int exifDegrees = BitmapLoadUtils.exifToDegrees(exifOrientation);
        int exifTranslation = BitmapLoadUtils.exifToTranslation(exifOrientation);

        ExifInfo exifInfo = new ExifInfo(exifOrientation, exifDegrees, exifTranslation);

        Matrix matrix = new Matrix();
        if (exifDegrees != 0) {
            matrix.preRotate(exifDegrees);
        }
        if (exifTranslation != 1) {
            matrix.postScale(exifTranslation, 1);
        }
        if (!matrix.isIdentity()) {
            return Single.just(new BitmapWorkerResult(BitmapLoadUtils.transformBitmap(decodeSampledBitmap, matrix), exifInfo));
        }

        return Single.just(new BitmapWorkerResult(decodeSampledBitmap, exifInfo));
    }

    private void processInputUri() throws NullPointerException, IOException {
        String inputUriScheme = mInputUri.getScheme();
        Log.d(TAG, "Uri scheme: " + inputUriScheme);
        if ("content".equals(inputUriScheme)) {
            try {
                copyFile(mInputUri, mOutputUri);
            } catch (NullPointerException | IOException e) {
                Log.e(TAG, "Copying failed", e);
                throw e;
            }
        } else if (!"file".equals(inputUriScheme)) {
            Log.e(TAG, "Invalid Uri scheme " + inputUriScheme);
            throw new IllegalArgumentException("Invalid Uri scheme" + inputUriScheme);
        }
    }

    private void copyFile(@NonNull Uri inputUri, @Nullable Uri outputUri) throws NullPointerException, IOException {
        Log.d(TAG, "copyFile");

        if (outputUri == null) {
            throw new NullPointerException("Output Uri is null - cannot copy image");
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = mContext.getContentResolver().openInputStream(inputUri);
            outputStream = new FileOutputStream(outputUri.getPath());
            if (inputStream == null) {
                throw new NullPointerException("InputStream for given input Uri is null");
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } finally {
            BitmapLoadUtils.close(outputStream);
            BitmapLoadUtils.close(inputStream);

            // swap uris, because input image was copied to the output destination
            // (cropped image will override it later)
            mInputUri = mOutputUri;
        }
    }

    private void onPostExecute(@NonNull BitmapWorkerResult result) {
        if (result.mBitmapWorkerException == null) {
            mBitmapLoadCallback.onBitmapLoaded(result.mBitmapResult, result.mExifInfo, mInputUri.getPath(), (mOutputUri == null) ? null : mOutputUri.getPath());
        } else {
            mBitmapLoadCallback.onFailure(result.mBitmapWorkerException);
        }
    }

    private boolean checkSize(Bitmap bitmap, BitmapFactory.Options options) {
        int bitmapSize = bitmap != null ? bitmap.getByteCount() : 0;
        if (bitmapSize > MAX_BITMAP_SIZE) {
            options.inSampleSize *= 2;
            return true;
        }
        return false;
    }

    public static class BitmapWorkerResult {

        Bitmap mBitmapResult;
        ExifInfo mExifInfo;
        Exception mBitmapWorkerException;

        public BitmapWorkerResult(@NonNull Bitmap bitmapResult, @NonNull ExifInfo exifInfo) {
            mBitmapResult = bitmapResult;
            mExifInfo = exifInfo;
        }

        public BitmapWorkerResult(@NonNull Exception bitmapWorkerException) {
            mBitmapWorkerException = bitmapWorkerException;
        }

    }
}
