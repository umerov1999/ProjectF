package me.minetsh.imaging.core.sticker;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * Created by felix on 2017/12/11 下午2:48.
 */

public class IMGStickerX {

    private final static float SIZE_ANCHOR = 60;
    private final static float STROKE_FRAME = 6f;
    protected final float[] mPivotXY = {0, 0};
    /**
     * isActivated 为true时，其坐标相对于屏幕左上角
     * isActivated 为false时，其坐标相对Image，切为单位坐标
     */
    protected final RectF mFrame = new RectF();
    private final RectF mRemoveFrame = new RectF();
    private final RectF mAdjustFrame = new RectF();
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mRotate;
    private float mX, mY;
    private StickerEvent mTouchEvent;
    private boolean isActivated = true;

    {
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(STROKE_FRAME);
        mPaint.setStyle(Paint.Style.STROKE);

        mFrame.set(0, 0, SIZE_ANCHOR * 2, SIZE_ANCHOR * 2);
        mRemoveFrame.set(0, 0, SIZE_ANCHOR, SIZE_ANCHOR);
        mAdjustFrame.set(0, 0, SIZE_ANCHOR, SIZE_ANCHOR);
    }

    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        isActivated = activated;

    }

    public void onMeasure(float width, float height) {
        mFrame.set(0, 0, width, height);
        mFrame.offset(mPivotXY[0] - mFrame.centerX(), mPivotXY[1] - mFrame.centerY());
    }

    public void onDraw(Canvas canvas) {
        if (isActivated) {
            canvas.save();

            canvas.rotate(mRotate, mPivotXY[0], mPivotXY[1]);

            canvas.drawRect(mFrame, mPaint);

            canvas.translate(mFrame.left, mFrame.top);

            canvas.drawRect(mRemoveFrame, mPaint);

            canvas.translate(mFrame.width() - mAdjustFrame.width(), mFrame.height() - mAdjustFrame.height());

            canvas.drawRect(mAdjustFrame, mPaint);

            canvas.restore();
        }

        canvas.rotate(mRotate, mPivotXY[0], mPivotXY[1]);

//        canvas.scale(mBaseScale * mScale, mBaseScale * mScale, mPivotXY[0], mPivotXY[1]);
    }

    public void setScale(float scale) {
    }

    public void setRotate(float rotate) {
        mRotate = rotate;
    }

    public void setBaseScale(float baseScale) {
    }

    public void setBaseRotate(float baseRotate) {
    }

    public void offset(float dx, float dy) {
        mPivotXY[0] += dx;
        mPivotXY[1] += dy;
        mFrame.offset(mPivotXY[0] - mFrame.centerX(), mPivotXY[1] - mFrame.centerY());
    }

    public StickerEvent onTouch(MotionEvent event) {
        int action = event.getActionMasked();

        if (mTouchEvent == null && action != MotionEvent.ACTION_DOWN) {
            return null;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mX = event.getX();
                mY = event.getY();
                mTouchEvent = getTouchEvent(mX, mY);

                return mTouchEvent;
            case MotionEvent.ACTION_MOVE:
                if (mTouchEvent == StickerEvent.BODY) {
                    offset(event.getX() - mX, event.getY() - mY);
                    mX = event.getX();
                    mY = event.getY();
                }
            default:
                return mTouchEvent;
        }
    }

    private StickerEvent getTouchEvent(float x, float y) {
        float[] xy = {x, y};
        Matrix matrix = new Matrix();
        matrix.setRotate(mRotate, mFrame.centerX(), mFrame.centerY());
        matrix.mapPoints(xy);

        if (mFrame.contains(xy[0], xy[1])) {
            if (isInsideRemove(xy[0], xy[1])) {
                // 触摸到删除按钮
                return mTouchEvent = StickerEvent.REMOVE;
            } else if (isInsideAdjust(xy[0], xy[1])) {
                // 触摸到调整按钮
                return mTouchEvent = StickerEvent.ADJUST;
            }
            return StickerEvent.BODY;
        }
        return null;
    }

    public void setTouchEvent(StickerEvent touchEvent) {
        mTouchEvent = touchEvent;
    }

    public boolean isInsideRemove(float x, float y) {
        return mRemoveFrame.contains(x - mFrame.left, y - mFrame.top);
    }

    public boolean isInsideAdjust(float x, float y) {
        return mAdjustFrame.contains(
                x - mFrame.right + mAdjustFrame.width(),
                y - mFrame.bottom + mAdjustFrame.height()
        );
    }

    public boolean isInside(float x, float y) {
        float[] xy = {x, y};
        Matrix matrix = new Matrix();
        matrix.setRotate(mRotate, mFrame.centerX(), mFrame.centerY());
        matrix.mapPoints(xy);

        return mFrame.contains(xy[0], xy[1]);
    }

    public void transform(Matrix matrix) {
        matrix.mapPoints(mPivotXY);
        mFrame.offset(mPivotXY[0] - mFrame.centerX(), mPivotXY[1] - mFrame.centerY());
    }

    public enum StickerEvent {
        REMOVE,
        BODY,
        ADJUST
    }
}
