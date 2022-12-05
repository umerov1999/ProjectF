package me.minetsh.imaging.core.file;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by felix on 2017/12/26 下午3:07.
 */

public class IMGFileDecoder extends IMGDecoder {

    private final Context mContext;

    public IMGFileDecoder(Context context, Uri uri) {
        super(uri);
        mContext = context;
    }

    @Override
    public Bitmap decode(BitmapFactory.Options options) {
        Uri uri = getUri();
        if (uri == null) {
            return null;
        }

        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        try {
            InputStream originalStream;
            File filef = new File(path);
            if (filef.isFile()) {
                originalStream = new FileInputStream(filef);
            } else {
                originalStream = mContext.getContentResolver().openInputStream(uri);
            }
            return BitmapFactory.decodeStream(originalStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
