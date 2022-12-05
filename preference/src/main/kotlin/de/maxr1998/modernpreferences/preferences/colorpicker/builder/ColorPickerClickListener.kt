package de.maxr1998.modernpreferences.preferences.colorpicker.builder

import android.content.DialogInterface

interface ColorPickerClickListener {
    fun onClick(d: DialogInterface?, lastSelectedColor: Int, allColors: Array<Int?>?)
}