package me.minetsh.imaging.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import me.minetsh.imaging.R;

/**
 * Created by felix on 2017/12/21 下午10:58.
 */

public class IMGStickerImageView extends IMGStickerView {

    public IMGStickerImageView(Context context) {
        super(context);
    }

    public IMGStickerImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IMGStickerImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public View onCreateContentView(Context context) {
        ImageView mImageView = new ImageView(context);
        mImageView.setImageResource(R.drawable.image_album_photo);
        return mImageView;
    }
}
