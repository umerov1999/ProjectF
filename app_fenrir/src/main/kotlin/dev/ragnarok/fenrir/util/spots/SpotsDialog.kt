package dev.ragnarok.fenrir.util.spots

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.dp
import dev.ragnarok.fenrir.util.Utils.hasMarshmallow
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView

class SpotsDialog internal constructor(
    context: Context,
    private var message: String?,
    cancelable: Boolean,
    cancelListener: DialogInterface.OnCancelListener?
) : AlertDialog(
    context
) {
    private val size = 8
    private var spots: ArrayList<AnimatedView> = ArrayList()
    private var animator: AnimatorPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (ignored: Exception) {
        }
        setContentView(R.layout.dmax_spots_dialog)
        setCanceledOnTouchOutside(false)
        initMessage()
        initProgress()
    }

    override fun onStart() {
        super.onStart()
        for (view in spots) view.visibility = View.VISIBLE
        animator = AnimatorPlayer(createAnimations())
        animator?.play()
    }

    override fun onStop() {
        super.onStop()
        animator?.stop()
    }

    override fun setMessage(message: CharSequence?) {
        this.message = message.toString()
        if (isShowing) initMessage()
    }

    private fun initMessage() {
        if (message.nonNullNoEmpty()) {
            (findViewById<View>(R.id.dmax_spots_title) as MaterialTextView?)?.text = message
        }
    }

    private fun initProgress() {
        val progress = findViewById<FrameLayout>(R.id.dmax_spots_progress)
        spots = ArrayList(size)
        val sizeP = context.resources.getDimensionPixelSize(R.dimen.spot_size)
        val progressWidth = context.resources.getDimensionPixelSize(R.dimen.progress_width)
        for (i in 0..size) {
            val v = AnimatedView(context)
            v.setBackgroundResource(R.drawable.dmax_spots_spot)
            v.target = progressWidth
            v.xFactor = -1f
            v.visibility = View.INVISIBLE
            progress?.addView(v, sizeP, sizeP)
            spots.add(v)
        }
    }

    private fun createAnimations(): ArrayList<Animator> {
        val animators = ArrayList<Animator>(size)
        for (i in 0..size) {
            val animatedView = spots[i]
            @SuppressLint("Recycle") val move: Animator =
                ObjectAnimator.ofFloat(animatedView, "xFactor", 0f, 1f)
            move.duration = DURATION.toLong()
            move.interpolator = HesitateInterpolator()
            move.startDelay = DELAY.toLong() * i
            move.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animatedView.visibility = View.INVISIBLE
                }

                override fun onAnimationStart(animation: Animator) {
                    animatedView.visibility = View.VISIBLE
                }
            })
            animators.add(move)
        }
        return animators
    }

    class Builder {
        private lateinit var context: Context
        private var message: String? = null
        private var messageId = 0
        private var cancelable = true // default dialog behaviour
        private var cancelListener: DialogInterface.OnCancelListener? = null
        fun setContext(context: Context): Builder {
            this.context = context
            return this
        }

        fun setMessage(message: String?): Builder {
            this.message = message
            return this
        }

        fun setMessage(@StringRes messageId: Int): Builder {
            this.messageId = messageId
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        fun setCancelListener(cancelListener: DialogInterface.OnCancelListener?): Builder {
            this.cancelListener = cancelListener
            return this
        }

        fun build(): AlertDialog {
            if (!Settings.get()
                    .other().isNew_loading_dialog || !hasMarshmallow() || !FenrirNative.isNativeLoaded
            ) {
                return SpotsDialog(
                    context,
                    (if (messageId != 0) context.getString(messageId) else message),
                    cancelable,
                    cancelListener
                )
            }
            val root = View.inflate(context, R.layout.dialog_progress, null)
            (root.findViewById<View>(R.id.item_progress_text) as MaterialTextView).text =
                if (messageId != 0) context.getString(messageId) else message
            val anim: RLottieImageView = root.findViewById(R.id.lottie_animation)
            anim.fromRes(
                dev.ragnarok.fenrir_common.R.raw.s_loading,
                dp(180f),
                dp(180f),
                intArrayOf(
                    0x333333,
                    CurrentTheme.getColorPrimary(context),
                    0x777777,
                    CurrentTheme.getColorSecondary(context)
                )
            )
            anim.playAnimation()
            return MaterialAlertDialogBuilder(context).setView(root)
                .setCancelable(cancelable)
                .setOnCancelListener(cancelListener).create()
        }
    }

    companion object {
        private const val DELAY = 150
        private const val DURATION = 1500
    }

    init {
        setCancelable(cancelable)
        cancelListener?.let { setOnCancelListener(it) }
    }
}