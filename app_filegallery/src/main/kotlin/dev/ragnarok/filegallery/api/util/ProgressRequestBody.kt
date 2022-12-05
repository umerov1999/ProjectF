package dev.ragnarok.filegallery.api.util

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.io.InputStream

class ProgressRequestBody(
    private val stream: InputStream,
    private val listener: UploadCallbacks?,
    private val mediaType: MediaType?
) : RequestBody() {
    override fun contentType(): MediaType {
        return mediaType!!
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return stream.available().toLong()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val fileLength = stream.available().toLong()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded: Long = 0
        try {
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                listener?.onProgressUpdate((100 * uploaded / fileLength).toInt())
                uploaded += read.toLong()
                sink.write(buffer, 0, read)
            }
        } catch (e: Exception) {
            if (e is IOException) {
                throw e
            } else {
                throw IOException(e)
            }
        } finally {
            stream.close()
        }
    }

    interface UploadCallbacks {
        fun onProgressUpdate(percentage: Int)
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2048
    }
}