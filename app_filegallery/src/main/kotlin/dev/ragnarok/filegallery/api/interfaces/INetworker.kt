package dev.ragnarok.filegallery.api.interfaces

interface INetworker {
    fun localServerApi(): ILocalServerApi
    fun uploads(): IUploadApi
}