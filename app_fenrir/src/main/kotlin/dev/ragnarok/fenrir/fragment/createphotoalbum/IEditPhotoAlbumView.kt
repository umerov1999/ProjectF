package dev.ragnarok.fenrir.fragment.createphotoalbum

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.ISteppersView
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStepsHost

interface IEditPhotoAlbumView : ISteppersView<CreatePhotoAlbumStepsHost>,
    IErrorView