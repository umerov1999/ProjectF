package dev.ragnarok.filegallery.fragment.base.core

interface IPresenterFactory<T : IPresenter<*>> {
    fun create(): T
}