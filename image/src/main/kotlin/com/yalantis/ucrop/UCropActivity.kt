package com.yalantis.ucrop

import android.content.Intent
import android.graphics.Bitmap.CompressFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.model.AspectRatio
import com.yalantis.ucrop.util.SelectedStateListDrawable
import com.yalantis.ucrop.view.CropImageView
import com.yalantis.ucrop.view.GestureCropImageView
import com.yalantis.ucrop.view.OverlayView
import com.yalantis.ucrop.view.TransformImageView.TransformImageListener
import com.yalantis.ucrop.view.UCropView
import com.yalantis.ucrop.view.widget.AspectRatioTextView
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView.ScrollingListener
import me.minetsh.imaging.R
import java.util.Locale

class UCropActivity : AppCompatActivity(), MenuProvider {
    private val mCropAspectRatioViews: MutableList<ViewGroup> = ArrayList()
    var mShowLoader = true
    var mUCropView: UCropView? = null
    var mGestureCropImageView: GestureCropImageView? = null
    var mBlockingView: View? = null
    private var mToolbarTitle: String? = null

    // Enables dynamic coloring
    private var mToolbarColor = 0
    private var mActiveControlsWidgetColor = 0
    private var mToolbarWidgetColor = 0

    @ColorInt
    private var mRootViewBackgroundColor = 0

    @DrawableRes
    private var mToolbarCancelDrawable = 0

    @DrawableRes
    private var mToolbarCropDrawable = 0
    private var mLogoColor = 0
    private var mShowBottomControls = false
    private var mOverlayView: OverlayView? = null
    private var mWrapperStateAspectRatio: ViewGroup? = null
    private var mWrapperStateRotate: ViewGroup? = null
    private var mWrapperStateScale: ViewGroup? = null
    private var mLayoutAspectRatio: ViewGroup? = null
    private var mLayoutRotate: ViewGroup? = null
    private var mLayoutScale: ViewGroup? = null
    private var mTextViewRotateAngle: TextView? = null
    private var mTextViewScalePercent: TextView? = null
    private val mImageListener: TransformImageListener = object : TransformImageListener {
        override fun onRotate(currentAngle: Float) {
            setAngleText(currentAngle)
        }

        override fun onScale(currentScale: Float) {
            setScaleText(currentScale)
        }

        override fun onLoadComplete() {
            mUCropView?.animate()?.alpha(1f)?.setDuration(300)
                ?.setInterpolator(AccelerateInterpolator())
            mBlockingView?.isClickable = false
            mShowLoader = false
            supportInvalidateOptionsMenu()
        }

        override fun onLoadFailure(e: Throwable) {
            setResultError(e)
            finish()
        }
    }
    private var mControlsTransition: Transition? = null
    private var mCompressFormat = DEFAULT_COMPRESS_FORMAT
    private var mCompressQuality = DEFAULT_COMPRESS_QUALITY
    private var mAllowedGestures = intArrayOf(SCALE, ROTATE, ALL)
    private val mStateClickListener = View.OnClickListener {
        if (!it.isSelected) {
            setWidgetState(it.id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ucrop_activity_photobox)
        addMenuProvider(this, this)
        val intent = intent
        setupViews(intent)
        setImageData(intent)
        setInitialState()
        addBlockingView()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.ucrop_menu_activity, menu)

        // Change crop & loader menu icons color to match the rest of the UI colors
        val menuItemLoader = menu.findItem(R.id.menu_loader)
        val menuItemLoaderIcon = menuItemLoader.icon
        if (menuItemLoaderIcon != null) {
            try {
                menuItemLoaderIcon.mutate()
                menuItemLoaderIcon.colorFilter =
                    PorterDuffColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP)
                menuItemLoader.setIcon(menuItemLoaderIcon)
            } catch (e: IllegalStateException) {
                Log.i(
                    TAG,
                    String.format(
                        "%s - %s",
                        e.message,
                        getString(R.string.ucrop_mutate_exception_hint)
                    )
                )
            }
            (menuItemLoader.icon as Animatable?)?.start()
        }
        val menuItemCrop = menu.findItem(R.id.menu_crop)
        val menuItemCropIcon = ContextCompat.getDrawable(this, mToolbarCropDrawable)
        if (menuItemCropIcon != null) {
            menuItemCropIcon.mutate()
            menuItemCropIcon.colorFilter =
                PorterDuffColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP)
            menuItemCrop.setIcon(menuItemCropIcon)
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_crop).setVisible(!mShowLoader)
        menu.findItem(R.id.menu_loader).setVisible(mShowLoader)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_crop) {
            cropAndSaveImage()
            return true
        } else if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    override fun onStop() {
        super.onStop()
        mGestureCropImageView?.cancelAllAnimations()
    }

    /**
     * This method extracts all data from the incoming intent and setups views properly.
     */
    private fun setImageData(intent: Intent) {
        val inputUri: Uri?
        val outputUri: Uri?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            inputUri = intent.getParcelableExtra(UCrop.EXTRA_INPUT_URI, Uri::class.java)
            outputUri = intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI, Uri::class.java)
        } else {
            @Suppress("deprecation")
            inputUri = intent.getParcelableExtra(UCrop.EXTRA_INPUT_URI)
            @Suppress("deprecation")
            outputUri = intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI)
        }
        processOptions(intent)
        if (inputUri != null && outputUri != null) {
            try {
                mGestureCropImageView?.setImageUri(inputUri, outputUri)
            } catch (e: Exception) {
                setResultError(e)
                finish()
            }
        } else {
            setResultError(NullPointerException(getString(R.string.ucrop_error_input_data_is_absent)))
            finish()
        }
    }

    /**
     * This method extracts [#optionsBundle][com.yalantis.ucrop.UCrop.Options] from incoming intent
     * and setups Activity, [OverlayView] and [CropImageView] properly.
     */
    private fun processOptions(intent: Intent) {
        // Bitmap compression options
        val compressionFormatName =
            intent.getStringExtra(UCrop.Options.EXTRA_COMPRESSION_FORMAT_NAME)
        var compressFormat: CompressFormat? = null
        if (!compressionFormatName.isNullOrEmpty()) {
            compressFormat = CompressFormat.valueOf(compressionFormatName)
        }
        mCompressFormat = compressFormat ?: DEFAULT_COMPRESS_FORMAT
        mCompressQuality =
            intent.getIntExtra(UCrop.Options.EXTRA_COMPRESSION_QUALITY, DEFAULT_COMPRESS_QUALITY)

        // Gestures options
        val allowedGestures = intent.getIntArrayExtra(UCrop.Options.EXTRA_ALLOWED_GESTURES)
        if (allowedGestures != null && allowedGestures.size == TABS_COUNT) {
            mAllowedGestures = allowedGestures
        }

        // Crop image view options
        mGestureCropImageView?.maxBitmapSize = intent.getIntExtra(
            UCrop.Options.EXTRA_MAX_BITMAP_SIZE,
            CropImageView.DEFAULT_MAX_BITMAP_SIZE
        )
        mGestureCropImageView?.setMaxScaleMultiplier(
            intent.getFloatExtra(
                UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER,
                CropImageView.DEFAULT_MAX_SCALE_MULTIPLIER
            )
        )
        mGestureCropImageView?.setImageToWrapCropBoundsAnimDuration(
            intent.getIntExtra(
                UCrop.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION,
                CropImageView.DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION
            ).toLong()
        )

        // Overlay view options
        mOverlayView?.isFreestyleCropEnabled = intent.getBooleanExtra(
            UCrop.Options.EXTRA_FREE_STYLE_CROP,
            OverlayView.DEFAULT_FREESTYLE_CROP_MODE != OverlayView.FREESTYLE_CROP_MODE_DISABLE
        )
        mOverlayView?.setDimmedColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_DIMMED_LAYER_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_default_dimmed)
            )
        )
        mOverlayView?.setCircleDimmedLayer(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_CIRCLE_DIMMED_LAYER,
                OverlayView.DEFAULT_CIRCLE_DIMMED_LAYER
            )
        )
        mOverlayView?.setShowCropFrame(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_SHOW_CROP_FRAME,
                OverlayView.DEFAULT_SHOW_CROP_FRAME
            )
        )
        mOverlayView?.setCropFrameColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_FRAME_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_default_crop_frame)
            )
        )
        mOverlayView?.setCropFrameStrokeWidth(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_FRAME_STROKE_WIDTH,
                resources.getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width)
            )
        )
        mOverlayView?.setShowCropGrid(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_SHOW_CROP_GRID,
                OverlayView.DEFAULT_SHOW_CROP_GRID
            )
        )
        mOverlayView?.setCropGridRowCount(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_ROW_COUNT,
                OverlayView.DEFAULT_CROP_GRID_ROW_COUNT
            )
        )
        mOverlayView?.setCropGridColumnCount(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_COLUMN_COUNT,
                OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT
            )
        )
        mOverlayView?.setCropGridColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_COLOR,
                ContextCompat.getColor(this, R.color.ucrop_color_default_crop_grid)
            )
        )
        mOverlayView?.setCropGridStrokeWidth(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_STROKE_WIDTH,
                resources.getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width)
            )
        )

        // Aspect ratio options
        val aspectRatioX = intent.getFloatExtra(UCrop.EXTRA_ASPECT_RATIO_X, 0f)
        val aspectRatioY = intent.getFloatExtra(UCrop.EXTRA_ASPECT_RATIO_Y, 0f)
        val aspectRationSelectedByDefault =
            intent.getIntExtra(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0)
        val aspectRatioList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(
                UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS,
                AspectRatio::class.java
            )
        } else {
            @Suppress("deprecation")
            intent.getParcelableArrayListExtra(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS)
        }
        if (aspectRatioX > 0 && aspectRatioY > 0) {
            mWrapperStateAspectRatio?.visibility = View.GONE
            mGestureCropImageView?.targetAspectRatio = aspectRatioX / aspectRatioY
        } else if (aspectRatioList != null && aspectRationSelectedByDefault < aspectRatioList.size) {
            mGestureCropImageView?.targetAspectRatio =
                aspectRatioList[aspectRationSelectedByDefault].aspectRatioX /
                        aspectRatioList[aspectRationSelectedByDefault].aspectRatioY
        } else {
            mGestureCropImageView?.targetAspectRatio = CropImageView.SOURCE_IMAGE_ASPECT_RATIO
        }

        // Result bitmap max size options
        val maxSizeX = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_X, 0)
        val maxSizeY = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_Y, 0)
        if (maxSizeX > 0 && maxSizeY > 0) {
            mGestureCropImageView?.setMaxResultImageSizeX(maxSizeX)
            mGestureCropImageView?.setMaxResultImageSizeY(maxSizeY)
        }
    }

    private fun setupViews(intent: Intent) {
        mToolbarColor = intent.getIntExtra(
            UCrop.Options.EXTRA_TOOL_BAR_COLOR,
            ContextCompat.getColor(this, R.color.ucrop_color_toolbar)
        )
        mActiveControlsWidgetColor = intent.getIntExtra(
            UCrop.Options.EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE,
            ContextCompat.getColor(this, R.color.ucrop_color_active_controls_color)
        )
        mToolbarWidgetColor = intent.getIntExtra(
            UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR,
            ContextCompat.getColor(this, R.color.ucrop_color_toolbar_widget)
        )
        mToolbarCancelDrawable = intent.getIntExtra(
            UCrop.Options.EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE,
            R.drawable.ucrop_ic_cross
        )
        mToolbarCropDrawable = intent.getIntExtra(
            UCrop.Options.EXTRA_UCROP_WIDGET_CROP_DRAWABLE,
            R.drawable.ucrop_ic_done
        )
        mToolbarTitle = intent.getStringExtra(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR)
        mToolbarTitle =
            if (mToolbarTitle != null) mToolbarTitle else resources.getString(R.string.ucrop_label_edit_photo)
        mLogoColor = intent.getIntExtra(
            UCrop.Options.EXTRA_UCROP_LOGO_COLOR,
            ContextCompat.getColor(this, R.color.ucrop_color_default_logo)
        )
        mShowBottomControls =
            !intent.getBooleanExtra(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS, false)
        mRootViewBackgroundColor = intent.getIntExtra(
            UCrop.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR,
            ContextCompat.getColor(this, R.color.ucrop_color_crop_background)
        )
        setupAppBar()
        initiateRootViews()
        if (mShowBottomControls) {
            val viewGroup = findViewById<ViewGroup>(R.id.ucrop_photobox)
            val wrapper = viewGroup.findViewById<ViewGroup>(R.id.controls_wrapper)
            wrapper.visibility = View.VISIBLE
            LayoutInflater.from(this).inflate(R.layout.ucrop_controls, wrapper, true)
            mControlsTransition = AutoTransition()
            mControlsTransition?.setDuration(CONTROLS_ANIMATION_DURATION)
            mWrapperStateAspectRatio = findViewById(R.id.state_aspect_ratio)
            mWrapperStateAspectRatio?.setOnClickListener(mStateClickListener)
            mWrapperStateRotate = findViewById(R.id.state_rotate)
            mWrapperStateRotate?.setOnClickListener(mStateClickListener)
            mWrapperStateScale = findViewById(R.id.state_scale)
            mWrapperStateScale?.setOnClickListener(mStateClickListener)
            mLayoutAspectRatio = findViewById(R.id.layout_aspect_ratio)
            mLayoutRotate = findViewById(R.id.layout_rotate_wheel)
            mLayoutScale = findViewById(R.id.layout_scale_wheel)
            setupAspectRatioWidget(intent)
            setupRotateWidget()
            setupScaleWidget()
            setupStatesWrapper()
        }
    }

    /**
     * Configures and styles both status bar and toolbar.
     */
    private fun setupAppBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Set all of the Toolbar coloring
        toolbar.setBackgroundColor(mToolbarColor)
        toolbar.setTitleTextColor(mToolbarWidgetColor)
        val toolbarTitle = toolbar.findViewById<TextView>(R.id.toolbar_title)
        toolbarTitle.setTextColor(mToolbarWidgetColor)
        toolbarTitle.text = mToolbarTitle

        // Color buttons inside the Toolbar
        val stateButtonDrawable = ContextCompat.getDrawable(this, mToolbarCancelDrawable)?.mutate()
        stateButtonDrawable?.colorFilter =
            PorterDuffColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP)
        toolbar.navigationIcon = stateButtonDrawable
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun initiateRootViews() {
        mUCropView = findViewById(R.id.ucrop)
        mGestureCropImageView = mUCropView?.cropImageView
        mOverlayView = mUCropView?.overlayView
        mGestureCropImageView?.setTransformImageListener(mImageListener)
        findViewById<ImageView>(R.id.image_view_logo).setColorFilter(
            mLogoColor,
            PorterDuff.Mode.SRC_ATOP
        )
        findViewById<View>(R.id.ucrop_frame).setBackgroundColor(mRootViewBackgroundColor)
        if (!mShowBottomControls) {
            val params =
                findViewById<View>(R.id.ucrop_frame).layoutParams as RelativeLayout.LayoutParams
            params.bottomMargin = 0
            findViewById<View>(R.id.ucrop_frame).requestLayout()
        }
    }

    /**
     * Use [.mActiveControlsWidgetColor] for color filter
     */
    private fun setupStatesWrapper() {
        val stateScaleImageView = findViewById<ImageView>(R.id.image_view_state_scale)
        val stateRotateImageView = findViewById<ImageView>(R.id.image_view_state_rotate)
        val stateAspectRatioImageView = findViewById<ImageView>(R.id.image_view_state_aspect_ratio)
        stateScaleImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateScaleImageView.drawable,
                mActiveControlsWidgetColor
            )
        )
        stateRotateImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateRotateImageView.drawable,
                mActiveControlsWidgetColor
            )
        )
        stateAspectRatioImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateAspectRatioImageView.drawable,
                mActiveControlsWidgetColor
            )
        )
    }

    private fun setupAspectRatioWidget(intent: Intent) {
        var aspectRationSelectedByDefault =
            intent.getIntExtra(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0)
        var aspectRatioList: ArrayList<AspectRatio?>?
        aspectRatioList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(
                UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS,
                AspectRatio::class.java
            )
        } else {
            @Suppress("deprecation")
            intent.getParcelableArrayListExtra(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS)
        }
        if (aspectRatioList.isNullOrEmpty()) {
            aspectRationSelectedByDefault = 2
            aspectRatioList = ArrayList()
            aspectRatioList.add(AspectRatio(null, 1f, 1f))
            aspectRatioList.add(AspectRatio(null, 3f, 4f))
            aspectRatioList.add(
                AspectRatio(
                    getString(R.string.ucrop_label_original).uppercase(Locale.getDefault()),
                    CropImageView.SOURCE_IMAGE_ASPECT_RATIO, CropImageView.SOURCE_IMAGE_ASPECT_RATIO
                )
            )
            aspectRatioList.add(AspectRatio(null, 3f, 2f))
            aspectRatioList.add(AspectRatio(null, 16f, 9f))
        }
        val wrapperAspectRatioList = findViewById<LinearLayout>(R.id.layout_aspect_ratio)
        var wrapperAspectRatio: FrameLayout
        var aspectRatioTextView: AspectRatioTextView
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        lp.weight = 1f
        for (aspectRatio in aspectRatioList) {
            wrapperAspectRatio =
                layoutInflater.inflate(R.layout.ucrop_aspect_ratio, null) as FrameLayout
            wrapperAspectRatio.layoutParams = lp
            aspectRatioTextView = wrapperAspectRatio.getChildAt(0) as AspectRatioTextView
            aspectRatioTextView.setActiveColor(mActiveControlsWidgetColor)
            if (aspectRatio != null) {
                aspectRatioTextView.setAspectRatio(aspectRatio)
            }
            wrapperAspectRatioList.addView(wrapperAspectRatio)
            mCropAspectRatioViews.add(wrapperAspectRatio)
        }
        mCropAspectRatioViews[aspectRationSelectedByDefault].isSelected = true
        for (cropAspectRatioView in mCropAspectRatioViews) {
            cropAspectRatioView.setOnClickListener {
                mGestureCropImageView?.targetAspectRatio =
                    ((it as ViewGroup).getChildAt(0) as AspectRatioTextView).getAspectRatio(it.isSelected)
                mGestureCropImageView?.setImageToWrapCropBounds()
                if (!it.isSelected) {
                    for (cropAspectRatioView1 in mCropAspectRatioViews) {
                        cropAspectRatioView1.isSelected = cropAspectRatioView1 === it
                    }
                }
            }
        }
    }

    private fun setupRotateWidget() {
        mTextViewRotateAngle = findViewById(R.id.text_view_rotate)
        findViewById<HorizontalProgressWheelView>(R.id.rotate_scroll_wheel)
            .setScrollingListener(object : ScrollingListener {
                override fun onScroll(delta: Float, totalDistance: Float) {
                    mGestureCropImageView?.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT)
                }

                override fun onScrollEnd() {
                    mGestureCropImageView?.setImageToWrapCropBounds()
                }

                override fun onScrollStart() {
                    mGestureCropImageView?.cancelAllAnimations()
                }
            })
        findViewById<HorizontalProgressWheelView>(R.id.rotate_scroll_wheel).setMiddleLineColor(
            mActiveControlsWidgetColor
        )
        findViewById<View>(R.id.wrapper_reset_rotate).setOnClickListener { resetRotation() }
        findViewById<View>(R.id.wrapper_rotate_by_angle).setOnClickListener {
            rotateByAngle(
                90
            )
        }
        setAngleTextColor(mActiveControlsWidgetColor)
    }

    private fun setupScaleWidget() {
        mTextViewScalePercent = findViewById(R.id.text_view_scale)
        findViewById<HorizontalProgressWheelView>(R.id.scale_scroll_wheel)
            .setScrollingListener(object : ScrollingListener {
                override fun onScroll(delta: Float, totalDistance: Float) {
                    mGestureCropImageView?.let {
                        if (delta > 0) {
                            it.zoomInImage(
                                it.currentScale
                                        + delta * ((it.maxScale - it.minScale) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT)
                            )
                        } else {
                            it.zoomOutImage(
                                it.currentScale
                                        + delta * ((it.maxScale - it.minScale) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT)
                            )
                        }
                    }
                }

                override fun onScrollEnd() {
                    mGestureCropImageView?.setImageToWrapCropBounds()
                }

                override fun onScrollStart() {
                    mGestureCropImageView?.cancelAllAnimations()
                }
            })
        findViewById<HorizontalProgressWheelView>(R.id.scale_scroll_wheel).setMiddleLineColor(
            mActiveControlsWidgetColor
        )
        setScaleTextColor(mActiveControlsWidgetColor)
    }

    fun setAngleText(angle: Float) {
        mTextViewRotateAngle?.text = String.format(Locale.getDefault(), "%.1f°", angle)
    }

    private fun setAngleTextColor(textColor: Int) {
        mTextViewRotateAngle?.setTextColor(textColor)
    }

    fun setScaleText(scale: Float) {
        mTextViewScalePercent?.text =
            String.format(Locale.getDefault(), "%d%%", (scale * 100).toInt())
    }

    private fun setScaleTextColor(textColor: Int) {
        mTextViewScalePercent?.setTextColor(textColor)
    }

    private fun resetRotation() {
        mGestureCropImageView?.let {
            it.postRotate(-it.currentAngle)
            it.setImageToWrapCropBounds()
        }
    }

    private fun rotateByAngle(angle: Int) {
        mGestureCropImageView?.postRotate(angle.toFloat())
        mGestureCropImageView?.setImageToWrapCropBounds()
    }

    private fun setInitialState() {
        if (mShowBottomControls) {
            if (mWrapperStateAspectRatio?.visibility == View.VISIBLE) {
                setWidgetState(R.id.state_aspect_ratio)
            } else {
                setWidgetState(R.id.state_scale)
            }
        } else {
            setAllowedGestures(0)
        }
    }

    private fun setWidgetState(@IdRes stateViewId: Int) {
        if (!mShowBottomControls) return
        mWrapperStateAspectRatio?.isSelected = stateViewId == R.id.state_aspect_ratio
        mWrapperStateRotate?.isSelected = stateViewId == R.id.state_rotate
        mWrapperStateScale?.isSelected = stateViewId == R.id.state_scale
        mLayoutAspectRatio?.visibility =
            if (stateViewId == R.id.state_aspect_ratio) View.VISIBLE else View.GONE
        mLayoutRotate?.visibility =
            if (stateViewId == R.id.state_rotate) View.VISIBLE else View.GONE
        mLayoutScale?.visibility =
            if (stateViewId == R.id.state_scale) View.VISIBLE else View.GONE
        changeSelectedTab(stateViewId)
        when (stateViewId) {
            R.id.state_scale -> {
                setAllowedGestures(0)
            }

            R.id.state_rotate -> {
                setAllowedGestures(1)
            }

            else -> {
                setAllowedGestures(2)
            }
        }
    }

    private fun changeSelectedTab(stateViewId: Int) {
        TransitionManager.beginDelayedTransition(
            findViewById(R.id.ucrop_photobox),
            mControlsTransition
        )
        mWrapperStateScale?.findViewById<View>(R.id.text_view_scale_info)?.visibility =
            if (stateViewId == R.id.state_scale) View.VISIBLE else View.GONE
        mWrapperStateAspectRatio?.findViewById<View>(R.id.text_view_crop)?.visibility =
            if (stateViewId == R.id.state_aspect_ratio) View.VISIBLE else View.GONE
        mWrapperStateRotate?.findViewById<View>(R.id.text_view_rotate_info)?.visibility =
            if (stateViewId == R.id.state_rotate) View.VISIBLE else View.GONE
    }

    private fun setAllowedGestures(tab: Int) {
        mGestureCropImageView?.isScaleEnabled =
            mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == SCALE
        mGestureCropImageView?.isRotateEnabled =
            mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == ROTATE
    }

    /**
     * Adds view that covers everything below the Toolbar.
     * When it's clickable - user won't be able to click/touch anything below the Toolbar.
     * Need to block user input while loading and cropping an image.
     */
    private fun addBlockingView() {
        if (mBlockingView == null) {
            mBlockingView = View(this)
            val lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            lp.addRule(RelativeLayout.BELOW, R.id.toolbar)
            mBlockingView?.layoutParams = lp
            mBlockingView?.isClickable = true
        }
        findViewById<RelativeLayout>(R.id.ucrop_photobox).addView(mBlockingView)
    }

    private fun cropAndSaveImage() {
        mBlockingView?.isClickable = true
        mShowLoader = true
        supportInvalidateOptionsMenu()
        mGestureCropImageView?.cropAndSaveImage(
            mCompressFormat,
            mCompressQuality,
            object : BitmapCropCallback {
                override fun onBitmapCropped(
                    resultUri: Uri,
                    offsetX: Int,
                    offsetY: Int,
                    imageWidth: Int,
                    imageHeight: Int
                ) {
                    setResultUri(
                        resultUri,
                        mGestureCropImageView?.targetAspectRatio ?: 0f,
                        offsetX,
                        offsetY,
                        imageWidth,
                        imageHeight
                    )
                    finish()
                }

                override fun onCropFailure(t: Throwable) {
                    setResultError(t)
                    finish()
                }
            })
    }

    internal fun setResultUri(
        uri: Uri?,
        resultAspectRatio: Float,
        offsetX: Int,
        offsetY: Int,
        imageWidth: Int,
        imageHeight: Int
    ) {
        setResult(
            RESULT_OK, Intent()
                .putExtra(UCrop.EXTRA_OUTPUT_URI, uri)
                .putExtra(UCrop.EXTRA_OUTPUT_CROP_ASPECT_RATIO, resultAspectRatio)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, offsetX)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, offsetY)
        )
    }

    internal fun setResultError(throwable: Throwable?) {
        setResult(UCrop.RESULT_ERROR, Intent().putExtra(UCrop.EXTRA_ERROR, throwable))
    }

    @IntDef(NONE, SCALE, ROTATE, ALL)
    @Retention(AnnotationRetention.SOURCE)
    annotation class GestureTypes
    companion object {
        const val DEFAULT_COMPRESS_QUALITY = 100
        val DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG
        const val NONE = 0
        const val SCALE = 1
        const val ROTATE = 2
        const val ALL = 3
        private const val TAG = "UCropActivity"
        private const val CONTROLS_ANIMATION_DURATION: Long = 50
        private const val TABS_COUNT = 3
        private const val SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000
        private const val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
