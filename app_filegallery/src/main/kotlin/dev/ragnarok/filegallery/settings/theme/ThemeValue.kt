package dev.ragnarok.filegallery.settings.theme

import android.graphics.Color
import androidx.annotation.StyleRes

class ThemeValue {
    val id: String
    val name: String
    val colorDayPrimary: Int
    val colorDaySecondary: Int
    val colorNightPrimary: Int
    val colorNightSecondary: Int
    var disabled: Boolean
    var special: Boolean

    @StyleRes
    val themeRes: Int

    @StyleRes
    val themeAmoledRes: Int

    @StyleRes
    val themeMD1Res: Int

    constructor(
        id: String,
        colorPrimary: String,
        colorSecondary: String,
        name: String,
        @StyleRes themeRes: Int,
        @StyleRes themeAmoledRes: Int,
        @StyleRes themeMD1Res: Int
    ) {
        colorDayPrimary = Color.parseColor(colorPrimary)
        colorDaySecondary = Color.parseColor(colorSecondary)
        colorNightPrimary = colorDayPrimary
        colorNightSecondary = colorDaySecondary
        disabled = false
        special = false
        this.id = id
        this.name = name
        this.themeRes = themeRes
        this.themeAmoledRes = themeAmoledRes
        this.themeMD1Res = themeMD1Res
    }

    constructor(
        id: String,
        colorDayPrimary: String,
        colorDaySecondary: String,
        colorNightPrimary: String,
        colorNightSecondary: String,
        name: String,
        @StyleRes themeRes: Int,
        @StyleRes themeAmoledRes: Int,
        @StyleRes themeMD1Res: Int
    ) {
        this.colorDayPrimary = Color.parseColor(colorDayPrimary)
        this.colorDaySecondary = Color.parseColor(colorDaySecondary)
        this.colorNightPrimary = Color.parseColor(colorNightPrimary)
        this.colorNightSecondary = Color.parseColor(colorNightSecondary)
        disabled = false
        special = false
        this.id = id
        this.name = name
        this.themeRes = themeRes
        this.themeAmoledRes = themeAmoledRes
        this.themeMD1Res = themeMD1Res
    }

    fun enable(bEnable: Boolean): ThemeValue {
        disabled = !bEnable
        return this
    }

    fun specialised(bSpecial: Boolean): ThemeValue {
        special = bSpecial
        return this
    }
}
