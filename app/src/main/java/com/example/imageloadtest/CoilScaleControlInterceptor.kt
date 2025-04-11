package com.example.imageloadtest

import okhttp3.Interceptor
import okhttp3.Response

class CoilScaleControlInterceptor(
    private val settingDataSource: SettingDataSource,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val chainRequest = chain.request()
        val originalUrl = chainRequest.url
        val newUrl = originalUrl.newBuilder()

        val imageWidth = settingDataSource.imageWidth
        if(imageWidth != 0) {
            newUrl.removeAllQueryParameters("w")
            newUrl.addQueryParameter("w", imageWidth.toString())
        }

        val imageQuality = settingDataSource.imageQuality
        if(imageQuality != 0) {
            newUrl.removeAllQueryParameters("q")
            newUrl.addQueryParameter("q", imageQuality.toString())
        }

        val newRequestBuilder = chainRequest.newBuilder().url(newUrl.build())

        val imageWebp = settingDataSource.imageWebp

        if(imageWebp) {
            newRequestBuilder.header("Accept", "image/webp,image/apng,image/svg+xml,image/*,*/*;")
        }

        return chain.proceed(newRequestBuilder.build())
    }

}
