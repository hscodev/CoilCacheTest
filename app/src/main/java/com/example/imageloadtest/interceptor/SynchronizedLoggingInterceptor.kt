package com.example.imageloadtest.interceptor

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.http.promisesBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import okio.Buffer
import okio.GzipSource
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class SynchronizedLoggingInterceptor(
    private val logSynchronizer: LogSynchronizer,
    private val logger: HttpLoggingInterceptor.Logger
) : Interceptor {

    @Volatile private var headersToRedact = emptySet<String>()

    @set:JvmName("level")
    @Volatile var level = Level.NONE

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val level = this.level

        val request = chain.request()
        if (level == Level.NONE) {
            return chain.proceed(request)
        }

        val logBody = level == Level.BODY
        val logHeaders = logBody || level == Level.HEADERS

        val requestBody = request.body

        val requestMessages = mutableListOf<String>()

        val connection = chain.connection()
        var requestStartMessage =
            ("--> ${request.method} ${request.url}${if (connection != null) " " + connection.protocol() else ""}")
        if (!logHeaders && requestBody != null) {
            requestStartMessage += " (${requestBody.contentLength()}-byte body)"
        }
        requestMessages.add(requestStartMessage)

        if (logHeaders) {
            val requestHeaderLogs = parseRequestHeaderLogs(
                request,
                requestBody,
                logBody
            )
            requestMessages.addAll(requestHeaderLogs)
        }

        logSynchronizer.log(requestMessages, logger)

        val responseMessages = mutableListOf<String>()
        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            responseMessages.add("<-- HTTP FAILED: $e")
            logSynchronizer.log(requestMessages, logger)
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body!!
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        responseMessages.add(
            "<-- ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url} (${tookMs}ms${if (!logHeaders) ", $bodySize body" else ""})")

        if (logHeaders) {
            val responseHeaderLogs = parseResponseHeaderLogs(
                response,
                responseBody,
                logBody,
                contentLength
            )
            responseMessages.addAll(responseHeaderLogs)
        }

        logSynchronizer.log(responseMessages, logger)
        return response
    }

    private fun parseRequestHeaderLogs(
        request : Request,
        requestBody: RequestBody?,
        logBody: Boolean
    ) : List<String>{
        return buildList {
            val headers = request.headers

            if (requestBody != null) {
                requestBody.contentType()?.let {
                    if (headers["Content-Type"] == null) {
                        add("Content-Type: $it")
                    }
                }
                if (requestBody.contentLength() != -1L) {
                    if (headers["Content-Length"] == null) {
                        add("Content-Length: ${requestBody.contentLength()}")
                    }
                }
            }

            for (i in 0 until headers.size) {
                add(logHeader(headers, i))
            }

            if (!logBody || requestBody == null) {
                add("--> END ${request.method}")
            } else if (bodyHasUnknownEncoding(request.headers)) {
                add("--> END ${request.method} (encoded body omitted)")
            } else if (requestBody.isDuplex()) {
                add("--> END ${request.method} (duplex request body omitted)")
            } else if (requestBody.isOneShot()) {
                add("--> END ${request.method} (one-shot body omitted)")
            } else {
                val buffer = Buffer()
                requestBody.writeTo(buffer)

                val contentType = requestBody.contentType()
                val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

                add("")
                if (buffer.isProbablyUtf8()) {
                    add(buffer.readString(charset))
                    add("--> END ${request.method} (${requestBody.contentLength()}-byte body)")
                } else {
                    add(
                        "--> END ${request.method} (binary ${requestBody.contentLength()}-byte body omitted)")
                }
            }
        }
    }

    private fun parseResponseHeaderLogs(
        response: Response,
        responseBody: ResponseBody,
        logBody: Boolean,
        contentLength: Long
    ): List<String> {
        return buildList {
            val headers = response.headers
            for (i in 0 until headers.size) {
                add(logHeader(headers, i))
            }

            if (!logBody || !response.promisesBody()) {
                add("<-- END HTTP")
            } else if (bodyHasUnknownEncoding(response.headers)) {
                add("<-- END HTTP (encoded body omitted)")
            } else {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE) // Buffer the entire body.
                var buffer: Buffer = source.buffer

                var gzippedLength: Long? = null
                if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
                    gzippedLength = buffer.size
                    GzipSource(buffer.clone()).use { gzippedResponseBody ->
                        buffer = Buffer()
                        buffer.writeAll(gzippedResponseBody)
                    }
                }

                val contentType = responseBody.contentType()
                val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

                if (!buffer.isProbablyUtf8()) {
                    add("")
                    add("<-- END HTTP (binary ${buffer.size}-byte body omitted)")
                    return@buildList
                }

                if (contentLength != 0L) {
                    add("")
                    add(buffer.clone().readString(charset))
                }

                if (gzippedLength != null) {
                    add("<-- END HTTP (${buffer.size}-byte, $gzippedLength-gzipped-byte body)")
                } else {
                    add("<-- END HTTP (${buffer.size}-byte body)")
                }
            }
        }
    }

    private fun logHeader(headers: Headers, i: Int): String {
        val value = if (headers.name(i) in headersToRedact) "██" else headers.value(i)
        return headers.name(i) + ": " + value
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }

    private fun Buffer.isProbablyUtf8(): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = size.coerceAtMost(64)
            copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (_: EOFException) {
            return false // Truncated UTF-8 sequence.
        }
    }
}
