package com.example.imageloadtest.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

class CommonHttpLoggingInterceptorBuilder (
    private val logSynchronizer: LogSynchronizer
) {

    fun build(logTag: String): Interceptor {
        return object: Interceptor {
            val interceptor = SynchronizedLoggingInterceptor(logSynchronizer) {
                Log.d(logTag, it)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            override fun intercept(chain: Interceptor.Chain): Response {
                return interceptor.intercept(chain)
            }
        }
    }
}
