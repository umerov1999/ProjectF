package com.yalantis.ucrop

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.yalantis.ucrop.UCropActivity.GestureTypes
import com.yalantis.ucrop.model.AspectRatio
import java.util.Locale

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 *
 *
 * Builder class to ease Intent setup.
 */
class UCrop private constructor(source: Uri, destination: Uri) {
    private val mCropIntent: Intent = Intent()
    private val mCropOptionsBundle: Bundle = Bundle()

    init {
        mCropOptionsBundle.putParcelable(EXTRA_INPUT_URI, source)
        mCropOptionsBundle.putParcelable(EXTRA_OUTPUT_URI, destination)
    }

    /**
     * Set an aspect ratio for crop bounds.
     * User won't see the menu with other ratios options.
     *
     * @param x aspect ratio X
     * @param y aspect ratio Y
     */
    fun withAspectRatio(x: Float, y: Float): UCrop {
        mCropOptionsBundle.putFloat(EXTRA_ASPECT_RATIO_X, x)
        mCropOptionsBundle.putFloat(EXTRA_ASPECT_RATIO_Y, y)
        return this
    }

    /**
     * Set an aspect ratio for crop bounds that is evaluated from source image width and height.
     * User won't see the menu with other ratios options.
     */
    fun useSourceImageAspectRatio(): UCrop {
        mCropOptionsBundle.putFloat(EXTRA_ASPECT_RATIO_X, 0f)
        mCropOptionsBundle.putFloat(EXTRA_ASPECT_RATIO_Y, 0f)
        return this
    }

    /**
     * Set maximum size for result cropped image. Maximum size cannot be less then {@value MIN_SIZE}
     *
     * @param width  max cropped image width
     * @param height max cropped image height
     */
    fun withMaxResultSize(
        @IntRange(from = MIN_SIZE.toLong()) width: Int,
        @IntRange(from = MIN_SIZE.toLong()) height: Int
    ): UCrop {
        var widthS = width
        var heightS = height
        if (widthS < MIN_SIZE) {
            widthS = MIN_SIZE
        }
        if (heightS < MIN_SIZE) {
            heightS = MIN_SIZE
        }
        mCropOptionsBundle.putInt(EXTRA_MAX_SIZE_X, widthS)
        mCropOptionsBundle.putInt(EXTRA_MAX_SIZE_Y, heightS)
        return this
    }

    fun withOptions(options: Options): UCrop {
        mCropOptionsBundle.putAll(options.optionBundle)
        return this
    }

    /**
     * Get Intent to start [UCropActivity]
     *
     * @return Intent for [UCropActivity]
     */
    fun getIntent(context: Context): Intent {
        mCropIntent.setClass(context, UCropActivity::class.java)
        mCropIntent.putExtras(mCropOptionsBundle)
        return mCropIntent
    }

    /**
     * Class that helps to setup advanced configs that are not commonly used.
     * Use it with method [.withOptions]
     */
    class Options {
        val optionBundle: Bundle = Bundle()

        /**
         * Set one of [android.graphics.Bitmap.CompressFormat] that will be used to save resulting Bitmap.
         */
        fun setCompressionFormat(format: CompressFormat): Options {
            optionBundle.putString(EXTRA_COMPRESSION_FORMAT_NAME, format.name)
            return this
        }

        /**
         * Set compression quality [0-100] that will be used to save resulting Bitmap.
         */
        fun setCompressionQuality(@IntRange(from = 0) compressQuality: Int): Options {
            optionBundle.putInt(EXTRA_COMPRESSION_QUALITY, compressQuality)
            return this
        }

        /**
         * Choose what set of gestures will be enabled on each tab - if any.
         */
        fun setAllowedGestures(
            @GestureTypes tabScale: Int,
            @GestureTypes tabRotate: Int,
            @GestureTypes tabAspectRatio: Int
        ): Options {
            optionBundle.putIntArray(
                EXTRA_ALLOWED_GESTURES,
                intArrayOf(tabScale, tabRotate, tabAspectRatio)
            )
            return this
        }

        /**
         * This method sets multiplier that is used to calculate max image scale from min image scale.
         *
         * @param maxScaleMultiplier - (minScale * maxScaleMultiplier) = maxScale
         */
        fun setMaxScaleMultiplier(
            @FloatRange(
                from = 1.0,
                fromInclusive = false
            ) maxScaleMultiplier: Float
        ): Options {
            optionBundle.putFloat(EXTRA_MAX_SCALE_MULTIPLIER, maxScaleMultiplier)
            return this
        }

        /**
         * This method sets animation duration for image to wrap the crop bounds
         *
         * @param durationMillis - duration in milliseconds
         */
        fun setImageToCropBoundsAnimDuration(@IntRange(from = MIN_SIZE.toLong()) durationMillis: Int): Options {
            optionBundle.putInt(EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION, durationMillis)
            return this
        }

        /**
         * Setter for max size for both width and height of bitmap that will be decoded from an input Uri and used in the view.
         *
         * @param maxBitmapSize - size in pixels
         */
        fun setMaxBitmapSize(@IntRange(from = MIN_SIZE.toLong()) maxBitmapSize: Int): Options {
            optionBundle.putInt(EXTRA_MAX_BITMAP_SIZE, maxBitmapSize)
            return this
        }

        /**
         * @param color - desired color of dimmed area around the crop bounds
         */
        fun setDimmedLayerColor(@ColorInt color: Int): Options {
            optionBundle.putInt(EXTRA_DIMMED_LAYER_COLOR, color)
            return this
        }

        /**
         * @param isCircle - set it to true if you want dimmed layer to have an circle inside
         */
        fun setCircleDimmedLayer(isCircle: Boolean): Options {
            optionBundle.putBoolean(EXTRA_CIRCLE_DIMMED_LAYER, isCircle)
            return this
        }

        /**
         * @param show - set to true if you want to see a crop frame rectangle on top of an image
         */
        fun setShowCropFrame(show: Boolean): Options {
            optionBundle.putBoolean(EXTRA_SHOW_CROP_FRAME, show)
            return this
        }

        /**
         * @param color - desired color of crop frame
         */
        fun setCropFrameColor(@ColorInt color: Int): Options {
            optionBundle.putInt(EXTRA_CROP_FRAME_COLOR, color)
            return this
        }

        /**
         * @param width - desired width of crop frame line in pixels
         */
        fun setCropFrameStrokeWidth(@IntRange(from = 0) width: Int): Options {
            optionBundle.putInt(EXTRA_CROP_FRAME_STROKE_WIDTH, width)
            return this
        }

        /**
         * @param show - set to true if you want to see a crop grid/guidelines on top of an image
         */
        fun setShowCropGrid(show: Boolean): Options {
            optionBundle.putBoolean(EXTRA_SHOW_CROP_GRID, show)
            return this
        }

        /**
         * @param count - crop grid rows count.
         */
        fun setCropGridRowCount(@IntRange(from = 0) count: Int): Options {
            optionBundle.putInt(EXTRA_CROP_GRID_ROW_COUNT, count)
            return this
        }

        /**
         * @param count - crop grid columns count.
         */
        fun setCropGridColumnCount(@IntRange(from = 0) count: Int): Options {
            optionBundle.putInt(EXTRA_CROP_GRID_COLUMN_COUNT, count)
            return this
        }

        /**
         * @param color - desired color of crop grid/guidelines
         */
        fun setCropGridColor(@ColorInt color: Int): Options {
            optionBundle.putInt(EXTRA_CROP_GRID_COLOR, color)
            return this
        }

        /**
         * @param width - desired width of crop grid lines in pixels
         */
        fun setCropGridStrokeWidth(@IntRange(from = 0) width: Int): Options {
            optionBundle.putInt(EXTRA_CROP_GRID_STROKE_WIDTH, width)
            return this
        }

        /**
         * @param color - desired resolved color of the toolbar
         */
        fun setToolbarColor(@ColorInt color: Int): Options {
            optionBundle.putInt(EXTRA_TOOL_BAR_COLOR, color)
            return this
        }

        /**
         * @param color - desired resolved color of the active and selected widget and progress wheel middle line (default is white)
         */
        fun setActiveControlsWidgetColor(@ColorInt color: Int): Options {
            optionBundle.putInt(EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE, color)
            return this
        }

        /**
         * @param color - desired resolved color of Toolbar text and buttons (default is darker orange)
         */
        fun setToolbarWidgetColor(@ColorInt color: Int): Options {
            optionBundle.putInt(EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, color)
            return this
        }

        /**
         * @param text - desired text for Toolbar title
         */
        fun setToolbarTitle(text: String?): Options {
            optionBundle.putString(EXTRA_UCROP_TITLE_TEXT_TOOLBAR, text)
            return this
        }

        /**
         * @param drawable - desired drawable for the Toolbar left cancel icon
         */
        fun setToolbarCancelDrawable(@DrawableRes drawable: Int): Options {
            optionBundle.putInt(EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE, drawable)
            return this
        }

        /**
         * @param drawable - desired drawable for the Toolbar right crop icon
         */
        fun setToolbarCropDrawable(@DrawableRes drawable: Int): Options {
            optionBundle.putInt(EXTRA_UCROP_WIDGET_CROP_DRAWABLE, drawable)
            return this
        }

        /**
         * @param color - desired resolved color of logo fill (default is darker grey)
         */
        fun setLogoColor(@ColorInt color: Int): Options {
            optionBundle.putInt(EXTRA_UCROP_LOGO_COLOR, color)
            return this
        }

        /**
         * @param hide - set to true to hide the bottom controls (shown by default)
         */
        fun setHideBottomControls(hide: Boolean): Options {
            optionBundle.putBoolean(EXTRA_HIDE_BOTTOM_CONTROLS, hide)
            return this
        }

        /**
         * @param enabled - set to true to let user resize crop bounds (disabled by default)
         */
        fun setFreeStyleCropEnabled(enabled: Boolean): Options {
            optionBundle.putBoolean(EXTRA_FREE_STYLE_CROP, enabled)
            return this
        }

        /**
         * Pass an ordered list of desired aspect ratios that should be available for a user.
         *
         * @param selectedByDefault - index of aspect ratio option that is selected by default (starts with 0).
         * @param aspectRatio       - list of aspect ratio options that are available to user
         */
        fun setAspectRatioOptions(
            selectedByDefault: Int,
            vararg aspectRatio: AspectRatio?
        ): Options {
            require(selectedByDefault <= aspectRatio.size) {
                String.format(
                    Locale.US,
                    "Index [selectedByDefault = %d] cannot be higher than aspect ratio options count [count = %d].",
                    selectedByDefault, aspectRatio.size
                )
            }
            optionBundle.putInt(EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, selectedByDefault)
            optionBundle.putParcelableArrayList(
                EXTRA_ASPECT_RATIO_OPTIONS, arrayListOf(*aspectRatio)
            )
            return this
        }

        /**
         * @param color - desired background color that should be applied to the root view
         */
        fun setRootViewBackgroundColor(@ColorInt color: Int): Options {
            optionBundle.putInt(EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR, color)
            return this
        }

        /**
         * Set an aspect ratio for crop bounds.
         * User won't see the menu with other ratios options.
         *
         * @param x aspect ratio X
         * @param y aspect ratio Y
         */
        fun withAspectRatio(x: Float, y: Float): Options {
            optionBundle.putFloat(EXTRA_ASPECT_RATIO_X, x)
            optionBundle.putFloat(EXTRA_ASPECT_RATIO_Y, y)
            return this
        }

        /**
         * Set an aspect ratio for crop bounds that is evaluated from source image width and height.
         * User won't see the menu with other ratios options.
         */
        fun useSourceImageAspectRatio(): Options {
            optionBundle.putFloat(EXTRA_ASPECT_RATIO_X, 0f)
            optionBundle.putFloat(EXTRA_ASPECT_RATIO_Y, 0f)
            return this
        }

        /**
         * Set maximum size for result cropped image.
         *
         * @param width  max cropped image width
         * @param height max cropped image height
         */
        fun withMaxResultSize(
            @IntRange(from = MIN_SIZE.toLong()) width: Int,
            @IntRange(from = MIN_SIZE.toLong()) height: Int
        ): Options {
            optionBundle.putInt(EXTRA_MAX_SIZE_X, width)
            optionBundle.putInt(EXTRA_MAX_SIZE_Y, height)
            return this
        }

        companion object {
            const val EXTRA_COMPRESSION_FORMAT_NAME = "$EXTRA_PREFIX.CompressionFormatName"
            const val EXTRA_COMPRESSION_QUALITY = "$EXTRA_PREFIX.CompressionQuality"
            const val EXTRA_ALLOWED_GESTURES = "$EXTRA_PREFIX.AllowedGestures"
            const val EXTRA_MAX_BITMAP_SIZE = "$EXTRA_PREFIX.MaxBitmapSize"
            const val EXTRA_MAX_SCALE_MULTIPLIER = "$EXTRA_PREFIX.MaxScaleMultiplier"
            const val EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION =
                "$EXTRA_PREFIX.ImageToCropBoundsAnimDuration"
            const val EXTRA_DIMMED_LAYER_COLOR = "$EXTRA_PREFIX.DimmedLayerColor"
            const val EXTRA_CIRCLE_DIMMED_LAYER = "$EXTRA_PREFIX.CircleDimmedLayer"
            const val EXTRA_SHOW_CROP_FRAME = "$EXTRA_PREFIX.ShowCropFrame"
            const val EXTRA_CROP_FRAME_COLOR = "$EXTRA_PREFIX.CropFrameColor"
            const val EXTRA_CROP_FRAME_STROKE_WIDTH = "$EXTRA_PREFIX.CropFrameStrokeWidth"
            const val EXTRA_SHOW_CROP_GRID = "$EXTRA_PREFIX.ShowCropGrid"
            const val EXTRA_CROP_GRID_ROW_COUNT = "$EXTRA_PREFIX.CropGridRowCount"
            const val EXTRA_CROP_GRID_COLUMN_COUNT = "$EXTRA_PREFIX.CropGridColumnCount"
            const val EXTRA_CROP_GRID_COLOR = "$EXTRA_PREFIX.CropGridColor"
            const val EXTRA_CROP_GRID_STROKE_WIDTH = "$EXTRA_PREFIX.CropGridStrokeWidth"
            const val EXTRA_TOOL_BAR_COLOR = "$EXTRA_PREFIX.ToolbarColor"
            const val EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE =
                "$EXTRA_PREFIX.UcropColorControlsWidgetActive"
            const val EXTRA_UCROP_WIDGET_COLOR_TOOLBAR = "$EXTRA_PREFIX.UcropToolbarWidgetColor"
            const val EXTRA_UCROP_TITLE_TEXT_TOOLBAR = "$EXTRA_PREFIX.UcropToolbarTitleText"
            const val EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE =
                "$EXTRA_PREFIX.UcropToolbarCancelDrawable"
            const val EXTRA_UCROP_WIDGET_CROP_DRAWABLE = "$EXTRA_PREFIX.UcropToolbarCropDrawable"
            const val EXTRA_UCROP_LOGO_COLOR = "$EXTRA_PREFIX.UcropLogoColor"
            const val EXTRA_HIDE_BOTTOM_CONTROLS = "$EXTRA_PREFIX.HideBottomControls"
            const val EXTRA_FREE_STYLE_CROP = "$EXTRA_PREFIX.FreeStyleCrop"
            const val EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT =
                "$EXTRA_PREFIX.AspectRatioSelectedByDefault"
            const val EXTRA_ASPECT_RATIO_OPTIONS = "$EXTRA_PREFIX.AspectRatioOptions"
            const val EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR =
                "$EXTRA_PREFIX.UcropRootViewBackgroundColor"
        }
    }

    companion object {
        const val RESULT_ERROR = 96
        const val MIN_SIZE = 10
        private const val EXTRA_PREFIX = "com.yalantis.ucrop"
        const val EXTRA_INPUT_URI = "$EXTRA_PREFIX.InputUri"
        const val EXTRA_OUTPUT_URI = "$EXTRA_PREFIX.OutputUri"
        const val EXTRA_OUTPUT_CROP_ASPECT_RATIO = "$EXTRA_PREFIX.CropAspectRatio"
        const val EXTRA_OUTPUT_IMAGE_WIDTH = "$EXTRA_PREFIX.ImageWidth"
        const val EXTRA_OUTPUT_IMAGE_HEIGHT = "$EXTRA_PREFIX.ImageHeight"
        const val EXTRA_OUTPUT_OFFSET_X = "$EXTRA_PREFIX.OffsetX"
        const val EXTRA_OUTPUT_OFFSET_Y = "$EXTRA_PREFIX.OffsetY"
        const val EXTRA_ERROR = "$EXTRA_PREFIX.Error"
        const val EXTRA_ASPECT_RATIO_X = "$EXTRA_PREFIX.AspectRatioX"
        const val EXTRA_ASPECT_RATIO_Y = "$EXTRA_PREFIX.AspectRatioY"
        const val EXTRA_MAX_SIZE_X = "$EXTRA_PREFIX.MaxSizeX"
        const val EXTRA_MAX_SIZE_Y = "$EXTRA_PREFIX.MaxSizeY"

        /**
         * This method creates new Intent builder and sets both source and destination image URIs.
         *
         * @param source      Uri for image to crop
         * @param destination Uri for saving the cropped image
         */
        fun of(source: Uri, destination: Uri): UCrop {
            return UCrop(source, destination)
        }

        /**
         * Retrieve cropped image Uri from the result Intent
         *
         * @param intent crop result intent
         */
        fun getOutput(intent: Intent): Uri? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    EXTRA_OUTPUT_URI,
                    Uri::class.java
                )
            } else {
                @Suppress("deprecation")
                intent.getParcelableExtra(EXTRA_OUTPUT_URI)
            }
        }

        /**
         * Retrieve the width of the cropped image
         *
         * @param intent crop result intent
         */
        fun getOutputImageWidth(intent: Intent): Int {
            return intent.getIntExtra(EXTRA_OUTPUT_IMAGE_WIDTH, -1)
        }

        /**
         * Retrieve the height of the cropped image
         *
         * @param intent crop result intent
         */
        fun getOutputImageHeight(intent: Intent): Int {
            return intent.getIntExtra(EXTRA_OUTPUT_IMAGE_HEIGHT, -1)
        }

        /**
         * Retrieve cropped image aspect ratio from the result Intent
         *
         * @param intent crop result intent
         * @return aspect ratio as a floating point value (x:y) - so it will be 1 for 1:1 or 4/3 for 4:3
         */
        fun getOutputCropAspectRatio(intent: Intent): Float {
            return intent.getFloatExtra(EXTRA_OUTPUT_CROP_ASPECT_RATIO, 0f)
        }

        /**
         * Method retrieves error from the result intent.
         *
         * @param result crop result Intent
         * @return Throwable that could happen while image processing
         */
        fun getError(result: Intent): Throwable? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.getSerializableExtra(
                    EXTRA_ERROR,
                    Throwable::class.java
                )
            } else {
                @Suppress("deprecation")
                result.getSerializableExtra(EXTRA_ERROR) as Throwable?
            }
        }
    }
}
