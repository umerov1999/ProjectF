package me.minetsh.imaging;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AppCompatActivity;

import me.minetsh.imaging.core.IMGMode;
import me.minetsh.imaging.core.IMGText;
import me.minetsh.imaging.view.IMGColorGroup;
import me.minetsh.imaging.view.IMGView;

/**
 * Created by felix on 2017/12/5 下午3:08.
 */

abstract class IMGEditBaseActivity extends AppCompatActivity implements IMGTextEditDialog.Callback, RadioGroup.OnCheckedChangeListener,
        DialogInterface.OnShowListener, DialogInterface.OnDismissListener {

    public static final int OP_HIDE = -1;
    public static final int OP_NORMAL = 0;
    public static final int OP_CLIP = 1;
    public static final int OP_SUB_DOODLE = 0;
    public static final int OP_SUB_MOSAIC = 1;
    protected IMGView mImgView;
    private RadioGroup mModeGroup;
    private IMGColorGroup mColorGroup;
    private IMGTextEditDialog mTextDialog;
    private View mLayoutOpSub;
    private ViewSwitcher mOpSwitcher, mOpSubSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            setContentView(R.layout.image_edit_activity);
            initViews();
            mImgView.setImageBitmap(bitmap);
            onCreated();
        } else finish();
    }

    public void onCreated() {

    }

    private void initViews() {
        mImgView = findViewById(R.id.image_canvas);
        mModeGroup = findViewById(R.id.rg_modes);

        mOpSwitcher = findViewById(R.id.vs_op);
        mOpSubSwitcher = findViewById(R.id.vs_op_sub);

        mColorGroup = findViewById(R.id.cg_colors);
        mColorGroup.setOnCheckedChangeListener(this);

        mLayoutOpSub = findViewById(R.id.layout_op_sub);

        findViewById(R.id.ib_clip_rotate).setOnClickListener(v -> onRotateClipClick());
        findViewById(R.id.ib_clip_cancel).setOnClickListener(v -> onCancelClipClick());
        findViewById(R.id.tv_clip_reset).setOnClickListener(v -> onResetClipClick());
        findViewById(R.id.ib_clip_done).setOnClickListener(v -> onDoneClipClick());

        findViewById(R.id.rb_doodle).setOnClickListener(v -> onModeClick(IMGMode.DOODLE));
        findViewById(R.id.btn_text).setOnClickListener(v -> onTextModeClick());
        findViewById(R.id.rb_mosaic).setOnClickListener(v -> onModeClick(IMGMode.MOSAIC));
        findViewById(R.id.btn_clip).setOnClickListener(v -> onModeClick(IMGMode.CLIP));
        findViewById(R.id.btn_undo).setOnClickListener(v -> onUndoClick());
        findViewById(R.id.tv_done).setOnClickListener(v -> onDoneClick());
        findViewById(R.id.tv_cancel).setOnClickListener(v -> onCancelClick());
    }

    public void updateModeUI() {
        IMGMode mode = mImgView.getMode();
        switch (mode) {
            case DOODLE:
                mModeGroup.check(R.id.rb_doodle);
                setOpSubDisplay(OP_SUB_DOODLE);
                break;
            case MOSAIC:
                mModeGroup.check(R.id.rb_mosaic);
                setOpSubDisplay(OP_SUB_MOSAIC);
                break;
            case NONE:
                mModeGroup.clearCheck();
                setOpSubDisplay(OP_HIDE);
                break;
        }
    }

    public void onTextModeClick() {
        if (mTextDialog == null) {
            mTextDialog = new IMGTextEditDialog(this, this);
            mTextDialog.setOnShowListener(this);
            mTextDialog.setOnDismissListener(this);
        }
        mTextDialog.show();
    }

    @Override
    public final void onCheckedChanged(RadioGroup group, int checkedId) {
        onColorChanged(mColorGroup.getCheckColor());
    }

    public void setOpDisplay(int op) {
        if (op >= 0) {
            mOpSwitcher.setDisplayedChild(op);
        }
    }

    public void setOpSubDisplay(int opSub) {
        if (opSub < 0) {
            mLayoutOpSub.setVisibility(View.GONE);
        } else {
            mOpSubSwitcher.setDisplayedChild(opSub);
            mLayoutOpSub.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mOpSwitcher.setVisibility(View.GONE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mOpSwitcher.setVisibility(View.VISIBLE);
    }

    public abstract Bitmap getBitmap();

    public abstract void onModeClick(IMGMode mode);

    public abstract void onUndoClick();

    public abstract void onCancelClick();

    public abstract void onDoneClick();

    public abstract void onCancelClipClick();

    public abstract void onDoneClipClick();

    public abstract void onResetClipClick();

    public abstract void onRotateClipClick();

    public abstract void onColorChanged(int checkedColor);

    @Override
    public abstract void onText(IMGText text);
}
