package dev.ragnarok.fenrir.fragment.messages.conversationattachments.conversationphotos

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.Apis.get
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.db.serialize.Serializers
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.fragment.messages.conversationattachments.abschatattachments.BaseChatAttachmentsPresenter
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.parcel.ParcelFlags
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.PersistentLogger
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatAttachmentPhotoPresenter(peerId: Long, accountId: Long, savedInstanceState: Bundle?) :
    BaseChatAttachmentsPresenter<Photo, IChatAttachmentPhotosView>(
        peerId,
        accountId,
        savedInstanceState
    ) {
    private val openGalleryDisposableHolder = CompositeJob()
    override fun requestAttachments(
        peerId: Long,
        nextFrom: String?
    ): Flow<Pair<String?, List<Photo>>> {
        return get().vkDefault(accountId)
            .messages()
            .getHistoryAttachments(peerId, VKApiAttachment.TYPE_PHOTO, nextFrom, 1, 1, 45, 50, null)
            .map { response ->
                val photos: MutableList<Photo> = ArrayList()
                response.items.nonNullNoEmpty {
                    for (one in it) {
                        if (one.entry != null && one.entry?.attachment is VKApiPhoto) {
                            val dto = one.entry?.attachment as VKApiPhoto
                            photos.add(
                                Dto2Model.transform(dto).setMsgId(one.messageId)
                                    .setMsgPeerId(peerId)
                            )
                        }
                    }
                }
                create(response.next_from, photos)
            }
    }

    override fun onDataChanged() {
        super.onDataChanged()
        resolveToolbar()
    }

    override fun onGuiCreated(viewHost: IChatAttachmentPhotosView) {
        super.onGuiCreated(viewHost)
        resolveToolbar()
    }

    private fun resolveToolbar() {
        view?.setToolbarTitleString(getString(R.string.attachments_in_chat))
        view?.setToolbarTitleString(getString(R.string.photos_count, Utils.safeCountOf(data)))
    }

    override fun onDestroyed() {
        openGalleryDisposableHolder.cancel()
        super.onDestroyed()
    }

    fun firePhotoClick(position: Int) {
        if (FenrirNative.isNativeLoaded && Settings.get().main().isNative_parcel_photo) {
            view?.goToTempPhotosGallery(
                accountId,
                ParcelNative.createParcelableList(data, ParcelFlags.NULL_LIST),
                position
            )
        } else {
            val source = TmpSource(fireTempDataUsage(), 0)
            openGalleryDisposableHolder.add(
                Stores.instance
                    .tempStore()
                    .putTemporaryData(
                        source.ownerId,
                        source.sourceId,
                        data,
                        Serializers.PHOTOS_SERIALIZER
                    )
                    .fromIOToMain({
                        onPhotosSavedToTmpStore(
                            position,
                            source
                        )
                    }) { PersistentLogger.logThrowable("ChatAttachmentPhotoPresenter", it) })
        }
    }

    private fun onPhotosSavedToTmpStore(index: Int, source: TmpSource) {
        view?.goToTempPhotosGallery(
            accountId,
            source,
            index
        )
    }
}