package com.yalantis.ucrop.model;

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 6/21/16.
 */
public class ExifInfo {

    private int mExifOrientation;
    private int mExifDegrees;
    private int mExifTranslation;

    public ExifInfo(int exifOrientation, int exifDegrees, int exifTranslation) {
        mExifOrientation = exifOrientation;
        mExifDegrees = exifDegrees;
        mExifTranslation = exifTranslation;
    }

    public int getExifOrientation() {
        return mExifOrientation;
    }

    public void setExifOrientation(int exifOrientation) {
        mExifOrientation = exifOrientation;
    }

    public int getExifDegrees() {
        return mExifDegrees;
    }

    public void setExifDegrees(int exifDegrees) {
        mExifDegrees = exifDegrees;
    }

    public int getExifTranslation() {
        return mExifTranslation;
    }

    public void setExifTranslation(int exifTranslation) {
        mExifTranslation = exifTranslation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExifInfo exifInfo = (ExifInfo) o;

        if (mExifOrientation != exifInfo.mExifOrientation) return false;
        if (mExifDegrees != exifInfo.mExifDegrees) return false;
        return mExifTranslation == exifInfo.mExifTranslation;

    }

    @Override
    public int hashCode() {
        int result = mExifOrientation;
        result = 31 * result + mExifDegrees;
        result = 31 * result + mExifTranslation;
        return result;
    }

}
