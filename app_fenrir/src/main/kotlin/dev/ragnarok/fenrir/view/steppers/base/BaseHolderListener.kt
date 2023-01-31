package dev.ragnarok.fenrir.view.steppers.base

import java.util.EventListener

interface BaseHolderListener : EventListener {
    fun onNextButtonClick(step: Int)
    fun onCancelButtonClick(step: Int)
}