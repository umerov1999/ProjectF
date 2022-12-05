package dev.ragnarok.fenrir.fragment.base.core

interface IPresenterFactory<T : IPresenter<*>> {
    fun create(): T
}