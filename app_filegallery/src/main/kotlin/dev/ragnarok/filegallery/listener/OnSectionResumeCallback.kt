package dev.ragnarok.filegallery.listener

import dev.ragnarok.filegallery.model.SectionItem

interface OnSectionResumeCallback {
    fun onSectionResume(@SectionItem section: Int)
}