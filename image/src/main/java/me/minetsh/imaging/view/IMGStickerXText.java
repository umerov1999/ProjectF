package me.minetsh.imaging.view;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;

import me.minetsh.imaging.core.IMGText;
import me.minetsh.imaging.core.sticker.IMGStickerX;

/**
 * Created by felix on 2017/12/11 下午2:49.
 */

public class IMGStickerXText extends IMGStickerX {
    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private StaticLayout mTextLayout;

    public IMGStickerXText(IMGText text) {
        // 字体大小 22sp
        mTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 22, Resources.getSystem().getDisplayMetrics()));

        setText(text);
    }

    @SuppressWarnings("deprecation")
    public void setText(IMGText text) {
        mTextPaint.setColor(text.getColor());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mTextLayout = new StaticLayout(text.getText(), mTextPaint,
                    Math.round(Resources.getSystem().getDisplayMetrics().widthPixels * 0.8f),
                    Layout.Alignment.ALIGN_NORMAL, 1f, 0, false);
        } else {
            StaticLayout.Builder builder =
                    StaticLayout.Builder.obtain(text.getText(), 0, text.length(), mTextPaint, Math.round(Resources.getSystem().getDisplayMetrics().widthPixels * 0.8f))
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(0.f, 1.f)
                            .setIncludePad(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setUseLineSpacingFromFallbacks(true);
            }
            mTextLayout = builder.build();
        }

        float width = 0f;
        for (int i = 0; i < mTextLayout.getLineCount(); i++) {
            width = Math.max(width, mTextLayout.getLineWidth(i));
        }

        onMeasure(width, mTextLayout.getHeight());
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(mFrame.left, mFrame.top);
        mTextLayout.draw(canvas);
        canvas.restore();
    }
}
