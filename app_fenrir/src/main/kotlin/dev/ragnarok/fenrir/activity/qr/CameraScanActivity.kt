package dev.ragnarok.fenrir.activity.qr

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.NoMainActivity
import dev.ragnarok.fenrir.activity.slidr.Slidr
import dev.ragnarok.fenrir.activity.slidr.model.SlidrConfig
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsResultAbs
import dev.ragnarok.fenrir.util.Utils
import java.nio.ByteBuffer
import java.util.EnumMap
import java.util.EnumSet

class CameraScanActivity : NoMainActivity() {
    private lateinit var textureView: PreviewView
    private var camera: Camera? = null
    private var finder: FinderView? = null
    private var flashButton: FloatingActionButton? = null
    private val reader: MultiFormatReader = MultiFormatReader()
    private var isFlash = false

    inner class DecoderResultPointCallback : ResultPointCallback {
        override fun foundPossibleResultPoint(point: ResultPoint) {
            finder?.pushPoints(point)
        }
    }

    init {
        val hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)
        hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = DecoderResultPointCallback()
        hints[DecodeHintType.POSSIBLE_FORMATS] = EnumSet.of(
            BarcodeFormat.QR_CODE,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.RSS_14,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.ITF
        )
        reader.setHints(hints)
    }

    private val requestCameraPermission = requestPermissionsResultAbs(
        arrayOf(
            Manifest.permission.CAMERA
        ), {
            startCamera()
        }, { finish() })

    private fun updateFlashButton() {
        Utils.setColorFilter(
            flashButton,
            if (isFlash) CurrentTheme.getColorPrimary(this) else ContextCompat.getColor(
                this,
                com.google.android.material.R.color.m3_fab_efab_foreground_color_selector
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Slidr.attach(
            this,
            SlidrConfig.Builder().scrimColor(CurrentTheme.getColorBackground(this)).build()
        )
        setContentView(R.layout.activity_camera_scan)
        textureView = findViewById(R.id.preview)
        finder = findViewById(R.id.view_finder)
        flashButton = findViewById(R.id.flash_button)
        updateFlashButton()

        flashButton?.setOnClickListener {
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                isFlash = !isFlash
                camera?.cameraControl?.enableTorch(isFlash)
                updateFlashButton()
            }
        }

        if (AppPerms.hasCameraPermission(this)) {
            startCamera()
        } else {
            requestCameraPermission.launch()
        }
    }

    private fun detect(generatedQRCode: Bitmap): String? {
        val width = generatedQRCode.width
        val height = generatedQRCode.height
        val pixels = IntArray(width * height)
        generatedQRCode.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        val result: Result = try {
            reader.decodeWithState(binaryBitmap)
        } catch (e: Exception) {
            return e.localizedMessage
        } ?: run {
            return "error"
        }
        finder?.possibleResultPoints?.clear()
        finder?.invalidate()

        return result.text
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun startCamera() {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(textureView.surfaceProvider)
        val imageAnalysis = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size.parseSize("1200x1200"),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
                        )
                    ).setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(this)
        ) { imageProxy: ImageProxy ->
            @SuppressLint("UnsafeOptInUsageError") val image =
                imageProxy.image ?: return@setAnalyzer imageProxy.close()
            val aspect = image.width.coerceAtMost(image.height)
            finder?.updatePreviewSize(aspect, aspect)
            val firstBuffer = image.planes[0].buffer
            firstBuffer.rewind()
            val firstBytes = ByteArray(firstBuffer.remaining())
            firstBuffer[firstBytes]
            val bmp = Bitmap.createBitmap(
                image.width, image.height,
                Bitmap.Config.ARGB_8888
            )
            val buffer = ByteBuffer.wrap(firstBytes)
            bmp.copyPixelsFromBuffer(buffer)
            val m = Matrix()
            m.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            val res = Bitmap.createBitmap(
                bmp,
                (image.width - aspect) / 2,
                (image.height - aspect) / 2,
                aspect,
                aspect,
                m,
                true
            )
            if (res != bmp) {
                bmp.recycle()
            }
            val data = detect(res)
            if (data.nonNullNoEmpty()) {
                val retIntent = Intent()
                retIntent.putExtra(Extra.URL, data)
                setResult(Activity.RESULT_OK, retIntent)
                imageProxy.close()
                finish()
                return@setAnalyzer
            }
            imageProxy.close()
        }

        // request a ProcessCameraProvider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // verify that initialization succeeded when View was created
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build(),
                    imageAnalysis, preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))

        textureView.setOnTouchListener { _, event ->
            return@setOnTouchListener when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                        textureView.width.toFloat(), textureView.height.toFloat()
                    )
                    val autoFocusPoint = factory.createPoint(event.x, event.y)
                    try {
                        camera?.cameraControl?.startFocusAndMetering(
                            FocusMeteringAction.Builder(
                                autoFocusPoint,
                                FocusMeteringAction.FLAG_AF
                            ).apply {
                                //focus only when the user tap the preview
                                disableAutoCancel()
                            }.build()
                        )
                    } catch (e: CameraInfoUnavailableException) {
                        Log.d("ERROR", "cannot access camera", e)
                    }
                    true
                }

                else -> false // Unhandled event.
            }
        }
    }

    open class FinderView(context: Context, attrs: AttributeSet?) :
        View(context, attrs) {
        private val frame: Rect = Rect()
        private val rectTmp: RectF = RectF()
        private val path = Path()
        private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val cornerPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val laserPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val pointPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val POINT_SIZE = 6f
        private var scannerAlpha = 0
        private val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
        private val ANIMATION_DELAY = 80L
        private var previewSize = Size(0, 0)
        var possibleResultPoints: ArrayList<ResultPoint> = ArrayList()

        fun updatePreviewSize(width: Int, height: Int) {
            if (previewSize.height != height || previewSize.width != width) {
                previewSize = Size(width, height)
                invalidate()
            }
        }

        fun pushPoints(p: ResultPoint) {
            possibleResultPoints.add(p)
            if (possibleResultPoints.size > 20) {
                possibleResultPoints.clear()
            }
        }

        init {
            cornerPaint.color = Color.parseColor("#ffffff")
            paint.color = Color.parseColor("#88000000")
            laserPaint.color = CurrentTheme.getColorInActive(context)
            pointPaint.color = CurrentTheme.getColorPrimary(context)
        }

        private fun aroundPoint(x: Int, y: Int, r: Int): RectF {
            rectTmp.set((x - r).toFloat(), (y - r).toFloat(), (x + r).toFloat(), (y + r).toFloat())
            return rectTmp
        }

        private fun lerp(a: Int, b: Int, f: Float): Int {
            return (a + f * (b - a)).toInt()
        }

        override fun onDraw(canvas: Canvas) {
            val s = width.coerceAtMost(height) - Utils.dp(10f)
            frame.left = (width - s) / 2
            frame.top = (height - s) / 2
            frame.bottom = frame.top + s
            frame.right = frame.left + s
            val width: Int = width
            val height: Int = height

            canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
            canvas.drawRect(
                0f,
                frame.top.toFloat(),
                frame.left.toFloat(),
                (frame.bottom + 1).toFloat(),
                paint
            )
            canvas.drawRect(
                (frame.right + 1).toFloat(),
                frame.top.toFloat(),
                width.toFloat(),
                (frame.bottom + 1).toFloat(),
                paint
            )
            canvas.drawRect(
                0f,
                (frame.bottom + 1).toFloat(),
                width.toFloat(),
                height.toFloat(),
                paint
            )
            val lineWidth =
                lerp(0, Utils.dp(4f), 1f)
            val halfLineWidth = lineWidth / 2
            val lineLength = lerp(
                (frame.right - frame.left).coerceAtMost(frame.bottom - frame.top),
                Utils.dp(20f),
                1f
            )
            path.reset()
            path.arcTo(aroundPoint(frame.left, frame.top + lineLength, halfLineWidth), 0f, 180f)
            path.arcTo(
                aroundPoint(
                    (frame.left + lineWidth * 1.5f).toInt(),
                    (frame.top + lineWidth * 1.5f).toInt(), lineWidth * 2
                ), 180f, 90f
            )
            path.arcTo(aroundPoint(frame.left + lineLength, frame.top, halfLineWidth), 270f, 180f)
            path.lineTo(
                (frame.left + halfLineWidth).toFloat(),
                (frame.top + halfLineWidth).toFloat()
            )
            path.arcTo(
                aroundPoint(
                    (frame.left + lineWidth * 1.5f).toInt(),
                    (frame.top + lineWidth * 1.5f).toInt(), lineWidth
                ), 270f, -90f
            )
            path.close()
            canvas.drawPath(path, cornerPaint)
            path.reset()
            path.arcTo(aroundPoint(frame.right, frame.top + lineLength, halfLineWidth), 180f, -180f)
            path.arcTo(
                aroundPoint(
                    (frame.right - lineWidth * 1.5f).toInt(),
                    (frame.top + lineWidth * 1.5f).toInt(), lineWidth * 2
                ), 0f, -90f
            )
            path.arcTo(aroundPoint(frame.right - lineLength, frame.top, halfLineWidth), 270f, -180f)
            path.arcTo(
                aroundPoint(
                    (frame.right - lineWidth * 1.5f).toInt(),
                    (frame.top + lineWidth * 1.5f).toInt(), lineWidth
                ), 270f, 90f
            )
            path.close()
            canvas.drawPath(path, cornerPaint)
            path.reset()
            path.arcTo(aroundPoint(frame.left, frame.bottom - lineLength, halfLineWidth), 0f, -180f)
            path.arcTo(
                aroundPoint(
                    (frame.left + lineWidth * 1.5f).toInt(),
                    (frame.bottom - lineWidth * 1.5f).toInt(), lineWidth * 2
                ), 180f, -90f
            )
            path.arcTo(
                aroundPoint(frame.left + lineLength, frame.bottom, halfLineWidth),
                90f,
                -180f
            )
            path.arcTo(
                aroundPoint(
                    (frame.left + lineWidth * 1.5f).toInt(),
                    (frame.bottom - lineWidth * 1.5f).toInt(), lineWidth
                ), 90f, 90f
            )
            path.close()
            canvas.drawPath(path, cornerPaint)
            path.reset()
            path.arcTo(
                aroundPoint(frame.right, frame.bottom - lineLength, halfLineWidth),
                180f,
                180f
            )
            path.arcTo(
                aroundPoint(
                    (frame.right - lineWidth * 1.5f).toInt(),
                    (frame.bottom - lineWidth * 1.5f).toInt(), lineWidth * 2
                ), 0f, 90f
            )
            path.arcTo(
                aroundPoint(frame.right - lineLength, frame.bottom, halfLineWidth),
                90f,
                180f
            )
            path.arcTo(
                aroundPoint(
                    (frame.right - lineWidth * 1.5f).toInt(),
                    (frame.bottom - lineWidth * 1.5f).toInt(), lineWidth
                ), 90f, -90f
            )
            path.close()
            canvas.drawPath(path, cornerPaint)

            laserPaint.alpha = SCANNER_ALPHA[scannerAlpha]
            if (scannerAlpha == 0) {
                possibleResultPoints.clear()
            }
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.size

            val middle = frame.height() / 2 + frame.top
            canvas.drawRect(
                (frame.left + 2).toFloat(),
                (middle - 1).toFloat(),
                (frame.right - 1).toFloat(),
                (middle + 2).toFloat(),
                laserPaint
            )

            if (previewSize.width > 0 && previewSize.height > 0) {
                val scaleX: Float = frame.width() / previewSize.width.toFloat()
                val scaleY: Float = frame.height() / previewSize.height.toFloat()

                // draw current possible result points
                if (possibleResultPoints.isNotEmpty()) {
                    for (point in possibleResultPoints) {
                        canvas.drawCircle(
                            frame.left + point.x * scaleX,
                            frame.top + point.y * scaleY,
                            POINT_SIZE, pointPaint
                        )
                    }
                }
            }

            postInvalidateDelayed(
                ANIMATION_DELAY,
                (frame.left - POINT_SIZE).toInt(),
                (frame.top - POINT_SIZE).toInt(),
                (frame.right + POINT_SIZE).toInt(),
                (frame.bottom + POINT_SIZE).toInt()
            )
        }
    }

    companion object {
        fun decodeFromBitmap(gen: Bitmap?): String? {
            if (gen == null) {
                return "error"
            }
            var generatedQRCode: Bitmap = gen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && generatedQRCode.config == Bitmap.Config.HARDWARE) {
                generatedQRCode = generatedQRCode.copy(Bitmap.Config.ARGB_8888, true)
                if (generatedQRCode == null) {
                    return "error"
                }
            }
            val reader = MultiFormatReader()
            val hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)
            hints[DecodeHintType.POSSIBLE_FORMATS] = EnumSet.of(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.RSS_14,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF
            )
            reader.setHints(hints)

            val width = generatedQRCode.width
            val height = generatedQRCode.height
            val pixels = IntArray(width * height)
            generatedQRCode.getPixels(pixels, 0, width, 0, 0, width, height)
            for (i in pixels.indices) {
                if (Color.alpha(pixels[i]) < 90) {
                    pixels[i] = Color.WHITE
                }
            }
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val result: Result = try {
                reader.decodeWithState(binaryBitmap)
            } catch (e: Exception) {
                return e.localizedMessage
            } ?: run {
                return "error"
            }

            return result.text
        }
    }
}