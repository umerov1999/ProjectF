package dev.ragnarok.fenrir.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class ChatConfig : Parcelable {
    private var models: ModelsBundle
    private var closeOnSend = false
    private var initialText: String? = null
    private var uploadFiles: ArrayList<Uri>? = null
    private var uploadFilesMimeType: String? = null

    constructor() {
        models = ModelsBundle()
    }

    internal constructor(parcel: Parcel) {
        closeOnSend = parcel.getBoolean()
        models = parcel.readTypedObjectCompat(ModelsBundle.CREATOR)!!
        initialText = parcel.readString()
        uploadFiles = parcel.createTypedArrayList(Uri.CREATOR)
        uploadFilesMimeType = parcel.readString()
    }

    fun getModels(): ModelsBundle {
        return models
    }

    fun setModels(models: ModelsBundle) {
        this.models = models
    }

    fun isCloseOnSend(): Boolean {
        return closeOnSend
    }

    fun setCloseOnSend(closeOnSend: Boolean) {
        this.closeOnSend = closeOnSend
    }

    fun getInitialText(): String? {
        return initialText
    }

    fun setInitialText(initialText: String?) {
        this.initialText = initialText
    }

    fun getUploadFiles(): ArrayList<Uri>? {
        return uploadFiles
    }

    fun setUploadFiles(uploadFiles: ArrayList<Uri>?) {
        this.uploadFiles = uploadFiles
    }

    fun getUploadFilesMimeType(): String? {
        return uploadFilesMimeType
    }

    fun setUploadFilesMimeType(uploadFilesMimeType: String?) {
        this.uploadFilesMimeType = uploadFilesMimeType
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.putBoolean(closeOnSend)
        dest.writeTypedObjectCompat(models, flags)
        dest.writeString(initialText)
        dest.writeTypedList(uploadFiles)
        dest.writeString(uploadFilesMimeType)
    }

    fun appendAll(models: Iterable<AbsModel>) {
        for (model in models) {
            this.models.append(model)
        }
    }

    companion object CREATOR : Parcelable.Creator<ChatConfig> {
        override fun createFromParcel(parcel: Parcel): ChatConfig {
            return ChatConfig(parcel)
        }

        override fun newArray(size: Int): Array<ChatConfig?> {
            return arrayOfNulls(size)
        }
    }
}