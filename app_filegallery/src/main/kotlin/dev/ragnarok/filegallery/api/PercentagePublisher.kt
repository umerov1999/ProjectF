package dev.ragnarok.filegallery.api

interface PercentagePublisher {
    fun onProgressChanged(percentage: Int)
}