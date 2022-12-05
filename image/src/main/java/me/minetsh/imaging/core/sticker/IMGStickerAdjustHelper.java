package me.minetsh.imaging.core.sticker;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import me.minetsh.imaging.view.IMGStickerView;

/**
 * Created by felix on 2017/11/15 下午5:44.
 */

public class IMGStickerAdjustHelper implements View.OnTouchListener {

    private static final String TAG = "IMGStickerAdjustHelper";

    private final View mView;

    private final IMGStickerView mContainer;
    private final Matrix M = new Matrix();
    private double mRadius, mDegrees;

    public IMGStickerAdjustHelper(IMGStickerView container, View view) {
        mView = view;
        mContainer = container;
        mView.setOnTouchListener(this);
    }

    private static double toDegrees(float v, float v1) {
        return Math.toDegrees(Math.atan2(v, v1));
    }

    private static double toLength(float x1, float y1, float x2, float y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float mCenterY;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                float x = event.getX();

                float y = event.getY();

                float mCenterX = mCenterY = 0;

                float pointX = mView.getX() + x - mContainer.getPivotX();

                float pointY = mView.getY() + y - mContainer.getPivotY();

                Log.d(TAG, String.format("X=%f,Y=%f", pointX, pointY));

                mRadius = toLength(0, 0, pointX, pointY);

                mDegrees = toDegrees(pointY, pointX);

                M.setTranslate(pointX - x, pointY - y);

                Log.d(TAG, String.format("degrees=%f", toDegrees(pointY, pointX)));

                M.postRotate((float) -toDegrees(pointY, pointX), mCenterX, mCenterY);

                return true;

            case MotionEvent.ACTION_MOVE:

                float[] xy = {event.getX(), event.getY()};

                pointX = mView.getX() + xy[0] - mContainer.getPivotX();

                pointY = mView.getY() + xy[1] - mContainer.getPivotY();

                Log.d(TAG, String.format("X=%f,Y=%f", pointX, pointY));

                double radius = toLength(0, 0, pointX, pointY);

                double degrees = toDegrees(pointY, pointX);

                float scale = (float) (radius / mRadius);


                mContainer.addScale(scale);

                Log.d(TAG, "    D   = " + (degrees - mDegrees));

                mContainer.setRotation((float) (mContainer.getRotation() + degrees - mDegrees));

                mRadius = radius;

                return true;
        }
        return false;
    }
}
