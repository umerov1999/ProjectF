package dev.ragnarok.fenrir.view.steppers.base

import java.util.*

interface BaseHolderListener : EventListener {
    fun onNextButtonClick(step: Int)
    fun onCancelButtonClick(step: Int)
}