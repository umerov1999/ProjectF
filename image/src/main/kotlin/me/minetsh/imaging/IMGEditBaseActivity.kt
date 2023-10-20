package me.minetsh.imaging

import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.ViewSwitcher
import androidx.appcompat.app.AppCompatActivity
import me.minetsh.imaging.core.IMGMode
import me.minetsh.imaging.core.IMGText
import me.minetsh.imaging.view.IMGColorGroup
import me.minetsh.imaging.view.IMGView

/**
 * Created by felix on 2017/12/5 下午3:08.
 */
abstract class IMGEditBaseActivity : AppCompatActivity(), IMGTextEditDialog.Callback,
    RadioGroup.OnCheckedChangeListener, OnShowListener, DialogInterface.OnDismissListener {
    protected var mImgView: IMGView? = null
    private var mModeGroup: RadioGroup? = null
    private var mColorGroup: IMGColorGroup? = null
    private var mTextDialog: IMGTextEditDialog? = null
    private var mLayoutOpSub: View? = null
    private var mOpSwitcher: ViewSwitcher? = null
    private var mOpSubSwitcher: ViewSwitcher? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bitmap = bitmap
        if (bitmap != null) {
            setContentView(R.layout.image_edit_activity)
            initViews()
            mImgView?.setImageBitmap(bitmap)
            onCreated()
        } else finish()
    }

    open fun onCreated() {}
    private fun initViews() {
        mImgView = findViewById(R.id.image_canvas)
        mModeGroup = findViewById(R.id.rg_modes)
        mOpSwitcher = findViewById(R.id.vs_op)
        mOpSubSwitcher = findViewById(R.id.vs_op_sub)
        mColorGroup = findViewById(R.id.cg_colors)
        mColorGroup?.setOnCheckedChangeListener(this)
        mLayoutOpSub = findViewById(R.id.layout_op_sub)
        findViewById<View>(R.id.ib_clip_rotate).setOnClickListener { onRotateClipClick() }
        findViewById<View>(R.id.ib_clip_cancel).setOnClickListener { onCancelClipClick() }
        findViewById<View>(R.id.tv_clip_reset).setOnClickListener { onResetClipClick() }
        findViewById<View>(R.id.ib_clip_done).setOnClickListener { onDoneClipClick() }
        findViewById<View>(R.id.rb_doodle).setOnClickListener { onModeClick(IMGMode.DOODLE) }
        findViewById<View>(R.id.btn_text).setOnClickListener { onTextModeClick() }
        findViewById<View>(R.id.rb_mosaic).setOnClickListener { onModeClick(IMGMode.MOSAIC) }
        findViewById<View>(R.id.btn_clip).setOnClickListener { onModeClick(IMGMode.CLIP) }
        findViewById<View>(R.id.btn_undo).setOnClickListener { onUndoClick() }
        findViewById<View>(R.id.tv_done).setOnClickListener { onDoneClick() }
        findViewById<View>(R.id.tv_cancel).setOnClickListener { onCancelClick() }
    }

    fun updateModeUI() {
        when (mImgView?.mode) {
            IMGMode.DOODLE -> {
                mModeGroup?.check(R.id.rb_doodle)
                setOpSubDisplay(OP_SUB_DOODLE)
            }

            IMGMode.MOSAIC -> {
                mModeGroup?.check(R.id.rb_mosaic)
                setOpSubDisplay(OP_SUB_MOSAIC)
            }

            IMGMode.NONE -> {
                mModeGroup?.clearCheck()
                setOpSubDisplay(OP_HIDE)
            }

            else -> {}
        }
    }

    fun onTextModeClick() {
        if (mTextDialog == null) {
            mTextDialog = IMGTextEditDialog(this, this)
            mTextDialog?.setOnShowListener(this)
            mTextDialog?.setOnDismissListener(this)
        }
        mTextDialog?.show()
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        mColorGroup?.checkColor?.let { onColorChanged(it) }
    }

    fun setOpDisplay(op: Int) {
        if (op >= 0) {
            mOpSwitcher?.displayedChild = op
        }
    }

    fun setOpSubDisplay(opSub: Int) {
        if (opSub < 0) {
            mLayoutOpSub?.visibility = View.GONE
        } else {
            mOpSubSwitcher?.displayedChild = opSub
            mLayoutOpSub?.visibility = View.VISIBLE
        }
    }

    override fun onShow(dialog: DialogInterface) {
        mOpSwitcher?.visibility = View.GONE
    }

    override fun onDismiss(dialog: DialogInterface) {
        mOpSwitcher?.visibility = View.VISIBLE
    }

    abstract val bitmap: Bitmap?
    abstract fun onModeClick(mode: IMGMode?)
    abstract fun onUndoClick()
    abstract fun onCancelClick()
    abstract fun onDoneClick()
    abstract fun onCancelClipClick()
    abstract fun onDoneClipClick()
    abstract fun onResetClipClick()
    abstract fun onRotateClipClick()
    abstract fun onColorChanged(checkedColor: Int)
    abstract override fun onText(text: IMGText?)

    companion object {
        const val OP_HIDE = -1
        const val OP_NORMAL = 0
        const val OP_CLIP = 1
        const val OP_SUB_DOODLE = 0
        const val OP_SUB_MOSAIC = 1
    }
}
