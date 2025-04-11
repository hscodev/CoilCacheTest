package com.example.imageloadtest.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class CoilLogInterceptor(
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val chainRequest = chain.request()
        val newBuilder = chainRequest.newBuilder()
        newBuilder.addHeader("pragma", "akamai-x-cache-on,akamai-x-cache-remote-on,akamai-x-get-request-id")
        val response = chain.proceed(newBuilder.build())
        val xCache = response.headers["x-cache"]
        val xCacheRemote = response.headers["x-cache-remote"]
        val xAkamaiRequestId = response.headers["x-akamai-request-id"]
        val size = response.headers["Content-Length"]
//
//        GlobalScope.launch {
//            delay(100)
//            runCatching {
//                debugLogDataSource.get()
//                    .getImageLatencyData(chainRequest.url.toString())
//                    ?.toLogData(
//                        xCache = xCache,
//                        xCacheRemote = xCacheRemote,
//                        xAkamaiRequestId = xAkamaiRequestId,
//                        size = size,
//                        url = chainRequest.url.toString(),
//                    )?.let { logData ->
//                        debugLogDataSource.get().setHitCount(xCache?.split(" ")?.first())
//                        debugLogDataSource.get().saveImageDebugLog(logData)
//                        debugLogDataSource.get().fire(
//                            logData = logData,
//                            pageId = "debug_latency",
//                            objectType = "image",
//                        )
//                    }
//            }.onFailure {
//                it.printStackTrace()
//            }
//        }
        return response
    }

//    private fun ApiDebugLogData.toLogData(
//        url: String,
//        xCache: String?,
//        xCacheRemote: String?,
//        xAkamaiRequestId: String?,
//        size: String?,
//    ): String? {
//        return copy(
//            xCache = xCache?.split(" ")?.first(),
//            xCacheRaw = xCache,
//            xCacheRemote = xCacheRemote?.split(" ")?.first(),
//            xCacheRemoteRaw = xCacheRemote,
//            xAkamaiRequestId = xAkamaiRequestId,
//            size = size?.toIntOrNull()?.let { it / 1024 }?.toString(),
//            url = url,
//        ).toJsonString()
//    }
//
//    private fun Any?.toJsonString(): String? {
//        return try {
//            this?.let {
//                GsonBuilder()
//                    .disableHtmlEscaping()
//                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
//                    .create()
//                    .toJson(it)
//            }
//        } catch (exception: Exception) {
//            null
//        }
//    }
}
