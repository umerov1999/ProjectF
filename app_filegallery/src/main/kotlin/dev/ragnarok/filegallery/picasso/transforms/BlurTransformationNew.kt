package dev.ragnarok.filegallery.picasso.transforms

import android.annotation.SuppressLint
import android.graphics.*
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
object BlurTransformationNew {
    private fun applyEffect(it: Params, renderEffect: RenderEffect): Bitmap {
        it.renderNode.setRenderEffect(renderEffect)
        val renderCanvas = it.renderNode.beginRecording()
        renderCanvas.drawBitmap(it.bitmap, 0f, 0f, null)
        it.renderNode.endRecording()
        it.hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        val image = it.imageReader.acquireNextImage() ?: throw RuntimeException("No Image")
        val hardwareBuffer = image.hardwareBuffer ?: throw RuntimeException("No HardwareBuffer")
        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
            ?: throw RuntimeException("Create Bitmap Failed")
        hardwareBuffer.close()
        image.close()
        return bitmap
    }

    fun blur(radius: Float, bitmap: Bitmap?): Bitmap? {
        bitmap ?: return null
        val params = Params(bitmap)
        val blurRenderEffect = RenderEffect.createBlurEffect(
            radius, radius,
            Shader.TileMode.MIRROR
        )
        val out = applyEffect(params, blurRenderEffect)
        params.imageReader.close()
        params.renderNode.discardDisplayList()
        params.hardwareRenderer.destroy()
        bitmap.recycle()
        return out
    }

    class Params(val bitmap: Bitmap) {
        @SuppressLint("WrongConstant")
        val imageReader = ImageReader.newInstance(
            bitmap.width, bitmap.height,
            PixelFormat.RGBA_8888, 1,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )
        val renderNode = RenderNode("RenderEffect")
        val hardwareRenderer = HardwareRenderer()

        init {
            hardwareRenderer.setSurface(imageReader.surface)
            hardwareRenderer.setContentRoot(renderNode)
            renderNode.setPosition(0, 0, imageReader.width, imageReader.height)
        }
    }
}