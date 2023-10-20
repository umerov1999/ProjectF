package me.minetsh.imaging.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.RadioGroup

/**
 * Created by felix on 2017/12/1 下午3:07.
 */
class IMGColorGroup : RadioGroup {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    var checkColor: Int
        get() {
            val checkedId = checkedRadioButtonId
            val radio = findViewById<IMGColorRadio>(checkedId)
            return radio?.color ?: Color.WHITE
        }
        set(color) {
            val count = childCount
            for (i in 0 until count) {
                val radio = getChildAt(i) as IMGColorRadio
                if (radio.color == color) {
                    radio.isChecked = true
                    break
                }
            }
        }
}
