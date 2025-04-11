package com.example.imageloadtest

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.util.DebugLogger
import com.example.imageloadtest.interceptor.CoilLogInterceptor
import com.example.imageloadtest.interceptor.CommonHttpLoggingInterceptorBuilder
import com.example.imageloadtest.interceptor.DebugLogDataSource
import com.example.imageloadtest.interceptor.LogSynchronizer
import com.example.imageloadtest.interceptor.TimingEventListener
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class App : Application(), SingletonImageLoader.Factory {
    val debugLogDataSource = DebugLogDataSource()
    val settingDataSource = SettingDataSource()
    val logSynchronizer = LogSynchronizer()
    val commonHttpLoggingInterceptorBuilder = CommonHttpLoggingInterceptorBuilder(logSynchronizer)

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        var builder = ImageLoader.Builder(this)

        builder = builder
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)

        builder = builder.components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = {
                        val okHttpClient = OkHttpClient.Builder()
                            .eventListenerFactory { TimingEventListener(debugLogDataSource) }
                            .callTimeout(60L, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(false)
                            .addInterceptor(CoilScaleControlInterceptor(settingDataSource))
                            .addInterceptor(CoilLogInterceptor())
                            .addNetworkInterceptor(commonHttpLoggingInterceptorBuilder.build("Coil"))

                        if (settingDataSource.imageThreadCount != 0) {
                            okHttpClient.dispatcher(
                                Dispatcher().apply { maxRequests = settingDataSource.imageThreadCount }
                            )
                        }

                        okHttpClient.build()
                    }
                )
            )
        }

        builder.logger(DebugLogger())

        return builder.build()
    }
}
