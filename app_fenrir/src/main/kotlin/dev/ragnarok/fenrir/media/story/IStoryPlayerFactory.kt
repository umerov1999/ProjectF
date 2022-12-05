package dev.ragnarok.fenrir.media.story

interface IStoryPlayerFactory {
    fun createStoryPlayer(url: String, isRepeat: Boolean): IStoryPlayer
}