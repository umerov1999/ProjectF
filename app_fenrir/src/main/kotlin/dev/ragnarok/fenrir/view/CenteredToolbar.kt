package dev.ragnarok.fenrir.view

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView

class CenteredToolbar : MaterialToolbar {
    private var tvTitle: MaterialTextView? = null
    private var tvSubtitle: MaterialTextView? = null

    constructor(context: Context) : super(context) {
        setupTextViews()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    ) {
        setupTextViews()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        setupTextViews()
    }

    override fun getTitle(): CharSequence {
        return tvTitle?.text.toString()
    }

    override fun setTitle(@StringRes resId: Int) {
        title = resources.getString(resId)
    }

    override fun setTitle(title: CharSequence) {
        tvTitle?.text = title
    }

    override fun getSubtitle(): CharSequence {
        return tvSubtitle?.text.toString()
    }

    override fun setSubtitle(@StringRes resId: Int) {
        subtitle = resources.getString(resId)
    }

    override fun setSubtitle(subtitle: CharSequence) {
        tvSubtitle?.visibility = VISIBLE
        tvSubtitle?.text = subtitle
    }

    private fun setupTextViews() {
        tvTitle = MaterialTextView(context)
        tvTitle?.setSingleLine()
        tvTitle?.ellipsize = TextUtils.TruncateAt.END
        tvSubtitle = MaterialTextView(context)
        tvSubtitle?.setSingleLine()
        tvSubtitle?.ellipsize = TextUtils.TruncateAt.END
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tvSubtitle?.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
        } else {
            tvSubtitle?.setTextAppearance(
                context,
                com.google.android.material.R.style.TextAppearance_Material3_BodySmall
            )
        }
        val linear = LinearLayout(context)
        linear.gravity = Gravity.CENTER
        linear.orientation = LinearLayout.VERTICAL
        linear.addView(tvTitle)
        linear.addView(tvSubtitle)
        tvSubtitle?.visibility = GONE
        val lp =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.CENTER
        linear.layoutParams = lp
        addView(linear)
    }
}