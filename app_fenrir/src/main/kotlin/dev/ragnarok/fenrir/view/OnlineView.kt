package dev.ragnarok.fenrir.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.appcompat.widget.AppCompatImageView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.util.Utils

class OnlineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {
    companion object {
        private val PAINT = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            PAINT.style = Paint.Style.FILL
            PAINT.isAntiAlias = true
            PAINT.isDither = true
        }
    }

    //@DrawableRes
    //private int mIcon;
    //@Dimension
    //private float mIconPadding;
    @ColorInt
    private var mCircleColor = 0

    @ColorInt
    private var mStrokeColor = 0

    @Dimension
    private var mStrokeWidth = 0f
    override fun onDraw(canvas: Canvas) {
        drawStroke(canvas)
        drawSolid(canvas)
        super.onDraw(canvas)
    }

    private fun drawSolid(canvas: Canvas) {
        val minSize = heightF.coerceAtMost(widthF)
        val radius = minSize / 2 - mStrokeWidth
        PAINT.color = mCircleColor
        canvas.drawCircle(widthF / 2, heightF / 2, radius, PAINT)
    }

    private fun drawStroke(canvas: Canvas) {
        val minSize = heightF.coerceAtMost(widthF)
        val radius = minSize / 2
        PAINT.color = mStrokeColor
        canvas.drawCircle(widthF / 2, heightF / 2, radius, PAINT)
    }

    private val widthF: Float
        get() = width.toFloat()
    private val heightF: Float
        get() = height.toFloat()

    private fun pxOf(dp: Float): Float {
        return Utils.dpToPx(dp, context)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        //LayoutInflater.from(getContext()).inflate(R.layout.view_online, this);
        val a = context.obtainStyledAttributes(attrs, R.styleable.OnlineView)
        try {
            mCircleColor = a.getColor(R.styleable.OnlineView_circle_color, Color.BLUE)
            mStrokeColor = a.getColor(R.styleable.OnlineView_stroke_color, Color.WHITE)
            mStrokeWidth = a.getDimension(R.styleable.OnlineView_stroke_width, pxOf(1f))
            //mIconPadding = a.getDimension(R.styleable.OnlineView_icon_padding, 8);
            //mIcon = a.getResourceId(R.styleable.OnlineView_icon, 0);
        } finally {
            a.recycle()
        }

        //View strokeView = findViewById(R.id.stroke);
        //ImageView circle = (ImageView) findViewById(R.id.circle);

        //strokeView.getBackground().setColorFilter(mStrokeColor, PorterDuff.Mode.MULTIPLY);
        //circle.getBackground().setColorFilter(mCircleColor, PorterDuff.Mode.MULTIPLY);
        //circle.setImageResource(mIcon);
        //int padding = (int) mIconPadding;
        //circle.setPadding(padding, padding, padding, padding);

        //int dp4 = (int) pxOf(4f);
        //setPadding(dp4, dp4, dp4, dp4);
    }

    fun setIcon(resourceId: Int) {
        setImageResource(resourceId)
        //mIcon = resourceId;
        //ImageView circle = (ImageView) findViewById(R.id.circle);
        //circle.setImageResource(mIcon);
    }

    fun setStrokeColor(color: Int) {
        mStrokeColor = color
        invalidate()
        //View strokeView = findViewById(R.id.stroke);
        //strokeView.getBackground().setColorFilter(mStrokeColor, PorterDuff.Mode.MULTIPLY);
    }

    fun setCircleColor(color: Int) {
        mCircleColor = color
        invalidate()
        //View strokeView = findViewById(R.id.stroke);
        //strokeView.getBackground().setColorFilter(mStrokeColor, PorterDuff.Mode.MULTIPLY);
    }

    init {
        init(context, attrs)
    }
}