package me.minetsh.imaging.core

/**
 * Created by felix on 2017/12/1 下午2:43.
 */
class IMGText(var text: String?, var color: Int) {

    val isEmpty: Boolean
        get() = text.isNullOrEmpty()

    fun length(): Int {
        return if (isEmpty) 0 else (text?.length ?: 0)
    }

    override fun toString(): String {
        return "IMGText{" +
                "text='" + text + '\'' +
                ", color=" + color +
                '}'
    }
}
