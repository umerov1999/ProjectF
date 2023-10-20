package me.minetsh.imaging.core

/**
 * Created by felix on 2017/11/16 下午5:49.
 */
interface IMGViewPortrait {
    fun getWidth(): Int

    fun getHeight(): Int

    fun getScaleX(): Float

    fun setScaleX(scaleX: Float)

    fun getScaleY(): Float

    fun setScaleY(scaleY: Float)

    fun getRotation(): Float

    fun setRotation(rotate: Float)

    fun getPivotX(): Float

    fun getPivotY(): Float

    fun getX(): Float

    fun setX(x: Float)

    fun getY(): Float

    fun setY(y: Float)

    fun getScale(): Float

    fun setScale(scale: Float)

    fun addScale(scale: Float)
}
