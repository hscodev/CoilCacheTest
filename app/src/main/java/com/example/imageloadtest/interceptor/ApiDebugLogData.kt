package com.example.imageloadtest.interceptor

import androidx.annotation.Keep

@Keep
data class ApiDebugLogData(
    val duration: Duration,
    val result: String,
    val url: String? = null,
    val latency: Long? = null,
    val requestStart: Long? = null,
    val requestEnd: Long? = null,
    val responseStart: Long? = null,
    val responseEnd: Long? = null,
    val xCache: String? = null,
    val xCacheRaw: String? = null,
    val xCacheRemote: String? = null,
    val xCacheRemoteRaw: String? = null,
    val xAkamaiRequestId: String? = null,
    val size: String? = null,
) {
    @Keep
    data class Duration(
        val dnsDuration: Long? = null,
        val tlsDuration: Long? = null,
        val requestDuration: Long? = null,
        val latencyDuration: Long? = null,
        val responseDuration: Long? = null,
        val callDuration: Long? = null,
    )
}
