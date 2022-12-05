package dev.ragnarok.fenrir.media.exo

import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerLibraryInfo
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.HttpDataSource.*
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Util
import com.google.common.base.Predicate
import com.google.common.net.HttpHeaders
import com.google.common.util.concurrent.SettableFuture
import com.google.errorprone.annotations.CanIgnoreReturnValue
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.concurrent.ExecutionException

/**
 * An [HttpDataSource] that delegates to Square's [OkHttpClient].
 *
 *
 * Note: HTTP request headers will be set using all parameters passed via (in order of decreasing
 * priority) the `dataSpec`, [.setRequestProperty] and the default parameters used to
 * construct the instance.
 */
class OkHttpDataSource internal constructor(
    callFactory: OkHttpClient,
    userAgent: String?,
    cacheControl: CacheControl?,
    defaultRequestProperties: RequestProperties?,
    contentTypePredicate: Predicate<String>?
) : BaseDataSource( /* isNetwork = */true), HttpDataSource {
    companion object {
        init {
            ExoPlayerLibraryInfo.registerModule("goog.exo.okhttp")
        }
    }

    private val callFactory: OkHttpClient
    private val requestProperties: RequestProperties
    private val userAgent: String?
    private val cacheControl: CacheControl?
    private val defaultRequestProperties: RequestProperties?
    private val contentTypePredicate: Predicate<String>?
    private var dataSpec: DataSpec? = null
    private var response: Response? = null
    private var responseByteStream: InputStream? = null
    private var opened = false
    private var bytesToRead: Long = 0
    private var bytesRead: Long = 0
    override fun getUri(): Uri {
        return Uri.parse(response?.request?.url.toString())
    }

    override fun getResponseCode(): Int {
        return response?.code ?: -1
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        return response?.headers?.toMultimap() ?: emptyMap()
    }

    override fun setRequestProperty(name: String, value: String) {
        Assertions.checkNotNull(name)
        Assertions.checkNotNull(value)
        requestProperties[name] = value
    }

    override fun clearRequestProperty(name: String) {
        Assertions.checkNotNull(name)
        requestProperties.remove(name)
    }

    override fun clearAllRequestProperties() {
        requestProperties.clear()
    }

    @Throws(HttpDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)
        val request = makeRequest(dataSpec)
        val response: Response?
        val responseBody: ResponseBody
        val call = callFactory.newCall(request)
        try {
            this.response = executeCall(call)
            response = this.response
            responseBody = response?.body ?: return -1
            responseByteStream = responseBody.byteStream()
        } catch (e: IOException) {
            throw HttpDataSourceException.createForIOException(
                e, dataSpec, HttpDataSourceException.TYPE_OPEN
            )
        }
        val responseCode = response.code

        // Check for a valid response code.
        if (!response.isSuccessful) {
            if (responseCode == 416) {
                val documentSize = HttpUtil.getDocumentSize(
                    response.headers[HttpHeaders.CONTENT_RANGE]
                )
                if (dataSpec.position == documentSize) {
                    opened = true
                    transferStarted(dataSpec)
                    return if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else 0
                }
            }
            val errorResponseBody: ByteArray = try {
                Util.toByteArray(Assertions.checkNotNull(responseByteStream))
            } catch (e: IOException) {
                Util.EMPTY_BYTE_ARRAY
            }
            val headers = response.headers.toMultimap()
            closeConnectionQuietly()
            val cause: IOException? =
                if (responseCode == 416) DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE) else null
            throw InvalidResponseCodeException(
                responseCode, response.message, cause, headers, dataSpec, errorResponseBody
            )
        }

        // Check for a valid content type.
        val mediaType = responseBody.contentType()
        val contentType = mediaType?.toString() ?: ""
        if (contentTypePredicate != null && !contentTypePredicate.apply(contentType)) {
            closeConnectionQuietly()
            throw InvalidContentTypeException(contentType, dataSpec)
        }

        // If we requested a range starting from a non-zero position and received a 200 rather than a
        // 206, then the server does not support partial requests. We'll need to manually skip to the
        // requested position.
        val bytesToSkip =
            if (responseCode == 200 && dataSpec.position != 0L) dataSpec.position else 0

        // Determine the length of the data to be read, after skipping.
        bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            val contentLength = responseBody.contentLength()
            if (contentLength != -1L) contentLength - bytesToSkip else C.LENGTH_UNSET.toLong()
        }
        opened = true
        transferStarted(dataSpec)
        try {
            skipFully(bytesToSkip, dataSpec)
        } catch (e: HttpDataSourceException) {
            closeConnectionQuietly()
            throw e
        }
        return bytesToRead
    }

    @Throws(HttpDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return try {
            readInternal(buffer, offset, length)
        } catch (e: IOException) {
            throw HttpDataSourceException.createForIOException(
                e, Util.castNonNull(dataSpec), HttpDataSourceException.TYPE_READ
            )
        }
    }

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
            closeConnectionQuietly()
        }
    }

    /**
     * Establishes a connection.
     */
    @Throws(HttpDataSourceException::class)
    private fun makeRequest(dataSpec: DataSpec): Request {
        val position = dataSpec.position
        val length = dataSpec.length
        val url = dataSpec.uri.toString().toHttpUrlOrNull()
            ?: throw HttpDataSourceException(
                "Malformed URL",
                dataSpec,
                PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                HttpDataSourceException.TYPE_OPEN
            )
        val builder: Request.Builder = Request.Builder().url(url)
        if (cacheControl != null) {
            builder.cacheControl(cacheControl)
        }
        val headers: MutableMap<String, String> = HashMap()
        if (defaultRequestProperties != null) {
            headers.putAll(defaultRequestProperties.snapshot)
        }
        headers.putAll(requestProperties.snapshot)
        headers.putAll(dataSpec.httpRequestHeaders)
        for ((key, value) in headers) {
            builder.header(key, value)
        }
        val rangeHeader = HttpUtil.buildRangeRequestHeader(position, length)
        if (rangeHeader != null) {
            builder.addHeader(HttpHeaders.RANGE, rangeHeader)
        }
        if (userAgent != null) {
            builder.addHeader(HttpHeaders.USER_AGENT, userAgent)
        }
        if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) {
            builder.addHeader(HttpHeaders.ACCEPT_ENCODING, "identity")
        }
        var requestBody: RequestBody? = null
        if (dataSpec.httpBody != null) {
            requestBody = dataSpec.httpBody?.toRequestBody(null)
        } else if (dataSpec.httpMethod == DataSpec.HTTP_METHOD_POST) {
            // OkHttp requires a non-null body for POST requests.
            requestBody = Util.EMPTY_BYTE_ARRAY.toRequestBody(null)
        }
        builder.method(dataSpec.httpMethodString, requestBody)
        return builder.build()
    }

    /**
     * This method is an interrupt safe replacement of OkHttp Call.execute() which can get in bad
     * states if interrupted while writing to the shared connection socket.
     */
    @Throws(IOException::class)
    private fun executeCall(call: Call): Response {
        val future: SettableFuture<Response> = SettableFuture.create()
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    future.setException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    future.set(response)
                }
            })
        return try {
            future.get()
        } catch (e: InterruptedException) {
            call.cancel()
            throw InterruptedIOException()
        } catch (ee: ExecutionException) {
            throw IOException(ee)
        }
    }

    /**
     * Attempts to skip the specified number of bytes in full.
     *
     * @param bytesToSkipLong The number of bytes to skip.
     * @param dataSpec    The [DataSpec].
     * @throws HttpDataSourceException If the thread is interrupted during the operation, or an error
     * occurs while reading from the source, or if the data ended before skipping the specified
     * number of bytes.
     */
    @Throws(HttpDataSourceException::class)
    private fun skipFully(bytesToSkipLong: Long, dataSpec: DataSpec) {
        var bytesToSkip = bytesToSkipLong
        if (bytesToSkip == 0L) {
            return
        }
        val skipBuffer = ByteArray(4096)
        try {
            while (bytesToSkip > 0) {
                val readLength = bytesToSkip.coerceAtMost(skipBuffer.size.toLong()).toInt()
                val read = Util.castNonNull(responseByteStream).read(skipBuffer, 0, readLength)
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedIOException()
                }
                if (read == -1) {
                    throw HttpDataSourceException(
                        dataSpec,
                        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                        HttpDataSourceException.TYPE_OPEN
                    )
                }
                bytesToSkip -= read.toLong()
                bytesTransferred(read)
            }
        } catch (e: IOException) {
            if (e is HttpDataSourceException) {
                throw e
            } else {
                throw HttpDataSourceException(
                    dataSpec,
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    HttpDataSourceException.TYPE_OPEN
                )
            }
        }
    }

    /**
     * Reads up to `length` bytes of data and stores them into `buffer`, starting at index
     * `offset`.
     *
     *
     * This method blocks until at least one byte of data can be read, the end of the opened range
     * is detected, or an exception is thrown.
     *
     * @param buffer     The buffer into which the read data should be stored.
     * @param offset     The start offset into `buffer` at which data should be written.
     * @param readLengthInt The maximum number of bytes to read.
     * @return The number of bytes read, or [C.RESULT_END_OF_INPUT] if the end of the opened
     * range is reached.
     * @throws IOException If an error occurs reading from the source.
     */
    @Throws(IOException::class)
    private fun readInternal(buffer: ByteArray, offset: Int, readLengthInt: Int): Int {
        var readLength = readLengthInt
        if (readLength == 0) {
            return 0
        }
        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            val bytesRemaining = bytesToRead - bytesRead
            if (bytesRemaining == 0L) {
                return C.RESULT_END_OF_INPUT
            }
            readLength = readLength.toLong().coerceAtMost(bytesRemaining).toInt()
        }
        val read = Util.castNonNull(responseByteStream).read(buffer, offset, readLength)
        if (read == -1) {
            return C.RESULT_END_OF_INPUT
        }
        bytesRead += read.toLong()
        bytesTransferred(read)
        return read
    }

    /**
     * Closes the current connection quietly, if there is one.
     */
    private fun closeConnectionQuietly() {
        if (response != null) {
            response?.body?.close()
            response = null
        }
        responseByteStream = null
    }

    /**
     * [DataSource.Factory] for [OkHttpDataSource] instances.
     */
    class Factory(private val callFactory: OkHttpClient) : HttpDataSource.Factory {
        private val defaultRequestProperties: RequestProperties = RequestProperties()
        private var userAgent: String? = null
        private var transferListener: TransferListener? = null
        private var cacheControl: CacheControl? = null
        private var contentTypePredicate: Predicate<String>? = null

        @CanIgnoreReturnValue
        override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): Factory {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
            return this
        }

        /**
         * Sets the user agent that will be used.
         *
         *
         * The default is `null`, which causes the default user agent of the underlying [ ] to be used.
         *
         * @param userAgent The user agent that will be used, or `null` to use the default user
         * agent of the underlying [OkHttpClient].
         * @return This factory.
         */
        @CanIgnoreReturnValue
        fun setUserAgent(userAgent: String?): Factory {
            this.userAgent = userAgent
            return this
        }

        /**
         * Sets the [CacheControl] that will be used.
         *
         *
         * The default is `null`.
         *
         * @param cacheControl The cache control that will be used.
         * @return This factory.
         */
        @CanIgnoreReturnValue
        fun setCacheControl(cacheControl: CacheControl?): Factory {
            this.cacheControl = cacheControl
            return this
        }

        /**
         * Sets a content type [Predicate]. If a content type is rejected by the predicate then a
         * [HttpDataSource.InvalidContentTypeException] is thrown from [ ][OkHttpDataSource.open].
         *
         *
         * The default is `null`.
         *
         * @param contentTypePredicate The content type [Predicate], or `null` to clear a
         * predicate that was previously set.
         * @return This factory.
         */
        @CanIgnoreReturnValue
        fun setContentTypePredicate(contentTypePredicate: Predicate<String>?): Factory {
            this.contentTypePredicate = contentTypePredicate
            return this
        }

        /**
         * Sets the [TransferListener] that will be used.
         *
         *
         * The default is `null`.
         *
         *
         * See [DataSource.addTransferListener].
         *
         * @param transferListener The listener that will be used.
         * @return This factory.
         */
        @CanIgnoreReturnValue
        fun setTransferListener(transferListener: TransferListener?): Factory {
            this.transferListener = transferListener
            return this
        }

        override fun createDataSource(): OkHttpDataSource {
            val dataSource = OkHttpDataSource(
                callFactory, userAgent, cacheControl, defaultRequestProperties, contentTypePredicate
            )
            transferListener?.let { dataSource.addTransferListener(it) }
            return dataSource
        }

    }

    init {
        this.callFactory = callFactory
        this.userAgent = userAgent
        this.cacheControl = cacheControl
        this.defaultRequestProperties = defaultRequestProperties
        this.contentTypePredicate = contentTypePredicate
        requestProperties = RequestProperties()
    }
}