package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
class PhotoSizeEntity {
    var s: Size? = null
        private set

    var m: Size? = null
        private set

    var x: Size? = null
        private set

    var o: Size? = null
        private set

    var p: Size? = null
        private set

    var q: Size? = null
        private set

    var k: Size? = null
        private set

    var l: Size? = null
        private set

    var r: Size? = null
        private set

    var y: Size? = null
        private set

    var z: Size? = null
        private set

    var w: Size? = null
        private set

    fun setS(s: Size?): PhotoSizeEntity {
        this.s = s
        return this
    }

    fun setM(m: Size?): PhotoSizeEntity {
        this.m = m
        return this
    }

    fun setX(x: Size?): PhotoSizeEntity {
        this.x = x
        return this
    }

    fun setK(k: Size?): PhotoSizeEntity {
        this.k = k
        return this
    }

    fun setL(l: Size?): PhotoSizeEntity {
        this.l = l
        return this
    }

    fun setO(o: Size?): PhotoSizeEntity {
        this.o = o
        return this
    }

    fun setP(p: Size?): PhotoSizeEntity {
        this.p = p
        return this
    }

    fun setQ(q: Size?): PhotoSizeEntity {
        this.q = q
        return this
    }

    fun setR(r: Size?): PhotoSizeEntity {
        this.r = r
        return this
    }

    fun setY(y: Size?): PhotoSizeEntity {
        this.y = y
        return this
    }

    fun setZ(z: Size?): PhotoSizeEntity {
        this.z = z
        return this
    }

    fun setW(w: Size?): PhotoSizeEntity {
        this.w = w
        return this
    }

    @Serializable
    @Keep
    class Size {
        private var width = 0
        var h = 0
            private set
        var url: String? = null
            private set

        fun setH(height: Int): Size {
            h = height
            return this
        }

        fun getW(): Int {
            return width
        }

        fun setW(width: Int): Size {
            this.width = width
            return this
        }

        fun setUrl(url: String?): Size {
            this.url = url
            return this
        }
    }
}