package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadOwnerPhotoResponse {
    // response={"photo_hash":"aeb0c37a143182779da895766ae91413",
    // "photo_src":"http:\/\/cs625317.vk.me\/v625317989\/42a6c\/Vz-XBvhN_8c.jpg",
    // "photo_src_big":"http:\/\/cs625317.vk.me\/v625317989\/42a6d\/WpiNL1m8VAk.jpg",
    // "photo_src_small":"http:\/\/cs625317.vk.me\/v625317989\/42a6b\/ILQx6m7KhCY.jpg",
    // "saved":1,
    // "post_id":2596}
    @SerialName("saved")
    var saved = false

    @SerialName("post_id")
    var postId = 0
}