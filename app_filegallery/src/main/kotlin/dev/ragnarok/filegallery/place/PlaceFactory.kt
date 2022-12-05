package dev.ragnarok.filegallery.place

import dev.ragnarok.filegallery.activity.VideoPlayerActivity
import dev.ragnarok.filegallery.activity.photopager.PhotoPagerActivity
import dev.ragnarok.filegallery.fragment.filemanager.FileManagerFragment
import dev.ragnarok.filegallery.fragment.tagdir.TagDirFragment
import dev.ragnarok.filegallery.fragment.tagowner.TagOwnerFragment
import dev.ragnarok.filegallery.model.Video


object PlaceFactory {
    fun getFileManagerPlace(
        path: String,
        base: Boolean,
        isSelect: Boolean,
    ): Place {
        return Place(Place.FILE_MANAGER)
            .setArguments(FileManagerFragment.buildArgs(path, base, isSelect))
    }

    fun getSettingsThemePlace(): Place {
        return Place(Place.SETTINGS_THEME)
    }

    fun getPreferencesPlace(): Place {
        return Place(Place.PREFERENCES)
    }

    fun getTagsPlace(isSelect: Boolean): Place {
        return Place(Place.TAGS).setArguments(TagOwnerFragment.buildArgs(isSelect))
    }

    fun getPlayerPlace(): Place {
        return Place(Place.AUDIO_PLAYER)
    }

    fun getLocalMediaServerPlace(): Place {
        return Place(Place.LOCAL_MEDIA_SERVER)
    }

    fun getPhotoLocalServerPlace(photos: Long, position: Int, invert: Boolean): Place {
        return Place(Place.PHOTO_LOCAL_SERVER).setArguments(
            PhotoPagerActivity.buildArgsForAlbum(
                photos,
                position,
                invert
            )
        )
    }

    fun getPhotoLocalPlace(photos: Long, position: Int, invert: Boolean): Place {
        return Place(Place.PHOTO_LOCAL).setArguments(
            PhotoPagerActivity.buildArgsForAlbum(
                photos,
                position,
                invert
            )
        )
    }

    fun getInternalPlayerPlace(
        video: Video
    ): Place {
        val place = Place(Place.VIDEO_PLAYER)
        place.prepareArguments().putParcelable(VideoPlayerActivity.EXTRA_VIDEO, video)
        return place
    }

    fun getTagDirsPlace(ownerId: Int, isSelect: Boolean): Place {
        return Place(Place.TAG_DIRS)
            .setArguments(TagDirFragment.buildArgs(ownerId, isSelect))
    }

    val securitySettingsPlace: Place
        get() = Place(Place.SECURITY)
}