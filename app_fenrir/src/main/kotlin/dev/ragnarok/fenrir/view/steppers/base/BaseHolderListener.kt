package dev.ragnarok.fenrir.view.steppers.base

interface BaseHolderListener {
    fun onNextButtonClick(step: Int)
    fun onCancelButtonClick(step: Int)
}