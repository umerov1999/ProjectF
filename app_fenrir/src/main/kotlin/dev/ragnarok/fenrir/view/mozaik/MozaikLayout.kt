package dev.ragnarok.fenrir.view.mozaik

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.RelativeLayout
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.PostImage
import dev.ragnarok.fenrir.view.mozaik.MatrixCalculator.Libra
import kotlin.math.roundToInt

class MozaikLayout : RelativeLayout {
    private var photos: List<PostImage> = ArrayList()
    private val libra: Libra = object : Libra {
        override fun getWeight(index: Int): Float {
            return photos[index].aspectRatio
        }
    }
    private var maxSingleImageHeight = 0
    private var prefImageSize = 0
    private var spacing = 0
    private var layoutParamsCalculator: MozaikLayoutParamsCalculator? = null

    constructor(context: Context) : super(context) {
        //this.maxSingleImageHeight = (int) context.getResources().getDimension(R.dimen.max_single_image_height);
        maxSingleImageHeight = displayHeight
        prefImageSize = context.resources.getDimension(R.dimen.pref_image_size).toInt()
        spacing = dpToPx(1f).toInt()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initDimensions(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initDimensions(context, attrs)
    }

    private val displayHeight: Int
        get() = resources.displayMetrics.heightPixels

    private fun initDimensions(context: Context, attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.MozaikLayout, 0, 0)
        try {
            //maxSingleImageHeight = a.getDimensionPixelSize(R.styleable.MozaikLayout_maxSingleImageHeight, (int) context.getResources().getDimension(R.dimen.max_single_image_height));
            maxSingleImageHeight = a.getDimensionPixelSize(
                R.styleable.MozaikLayout_maxSingleImageHeight,
                displayHeight
            )
            prefImageSize = a.getDimension(
                R.styleable.MozaikLayout_prefImageSize,
                context.resources.getDimension(R.dimen.pref_image_size)
            ).toInt()
            spacing = a.getDimensionPixelSize(R.styleable.MozaikLayout_spacing, dpToPx(1f).toInt())
        } finally {
            a.recycle()
        }
    }

    private fun initCalculator(parentWidth: Int) {
        val matrix = createMatrix(parentWidth)
        layoutParamsCalculator =
            matrix?.let { MozaikLayoutParamsCalculator(it, photos, parentWidth, spacing) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        //int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (photos.size == 1) {
            val parent = getChildAt(0)
            val parentparams = getLayoutParamsForSingleImage(
                photos[0],
                parent.layoutParams as LayoutParams,
                parentWidth
            )
            parent.measure(parentparams.width, parentparams.height)
        } else {
            if (layoutParamsCalculator == null) {
                initCalculator(parentWidth)
            }
            for (p in photos.indices) {
                val image = photos[p]
                val parent = getChildAt(p)
                if (parent.visibility == GONE) {
                    continue
                }
                if (image.position == null) {
                    image.position = layoutParamsCalculator?.getPostImagePosition(p)
                }
                val position = image.position
                val params = parent.layoutParams as LayoutParams
                params.width = position?.sizeX ?: 0
                params.height = position?.sizeY ?: 0
                params.topMargin = position?.marginY ?: 0
                params.leftMargin = position?.marginX ?: 0
                parent.measure(image.position?.sizeX ?: 0, image.position?.sizeY ?: 0)
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun createMatrix(maxWidth: Int): Array<IntArray>? {
        val prefRowCount = getPreferedRowCount(maxWidth)

        //long start = System.currentTimeMillis();
        val matrixCalculator = MatrixCalculator(photos.size, libra)
        return matrixCalculator.calculate(prefRowCount)

        //Exestime.log("MozaikLayout.createMatrix", start, "photocount: " + photos.size() + ", prefRowCount: " + prefRowCount);
        //return matrix;
    }

    private fun getPreferedRowCount(maxWidthPx: Int): Int {
        val dpPerProportion = (prefImageSize / density).toInt()
        var proportionDpSum = 0
        for (image in photos) {
            val proportion = image.aspectRatio
            proportionDpSum = (proportionDpSum + proportion * dpPerProportion).toInt()
        }
        val maxContainerWidthDp = convertPixtoDip(maxWidthPx)
        var prefRowCount =
            (proportionDpSum.toDouble() / maxContainerWidthDp.toDouble()).roundToInt()
        if (prefRowCount == 0) {
            prefRowCount = 1
        }
        return prefRowCount
    }

    val density: Float
        get() = resources.displayMetrics.density

    private fun convertPixtoDip(pixel: Int): Int {
        val scale = density
        return ((pixel - 0.5f) / scale).toInt()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (photos.size == 1) {
            val parent = getChildAt(0)
            val params =
                getLayoutParamsForSingleImage(photos[0], parent.layoutParams as LayoutParams, width)
            parent.layout(
                params.leftMargin,
                params.topMargin,
                params.rightMargin,
                params.bottomMargin
            )
        } else {
            if (layoutParamsCalculator == null) {
                initCalculator(width)
            }
            for (p in photos.indices) {
                val postImage = photos[p]
                val parent = getChildAt(p)
                if (parent.visibility == GONE) {
                    continue
                }
                if (postImage.position == null) {
                    postImage.position = (layoutParamsCalculator ?: return).getPostImagePosition(p)
                }
                val params = parent.layoutParams as LayoutParams
                val position = postImage.position
                params.width = position?.sizeX ?: 0
                params.height = position?.sizeY ?: 0
                params.topMargin = position?.marginY ?: 0
                params.leftMargin = position?.marginX ?: 0

                //parent.setLayoutParams(params);
                parent.layout(
                    position?.marginX ?: 0,
                    position?.marginY ?: 0,
                    params.rightMargin,
                    params.bottomMargin
                )
            }
        }
        super.onLayout(changed, l, t, r, b)
    }

    fun setPhotos(photos: List<PostImage>) {
        this.photos = photos
        layoutParamsCalculator = null
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    private fun getLayoutParamsForSingleImage(
        photo: PostImage,
        params: LayoutParams,
        maxWidth: Int
    ): LayoutParams {
        val coef = photo.width.toDouble() / photo.height.toDouble()
        var measuredwidth = maxWidth
        var measuredheight = (maxWidth / coef).toInt()
        if (maxSingleImageHeight < measuredheight) {
            measuredheight = maxSingleImageHeight
            measuredwidth = (measuredheight * coef).toInt()
        }
        params.height = measuredheight
        params.width = measuredwidth
        return params
    }
}