package com.example.imageloadtest.interceptor;

import android.util.Log
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class DebugLogDataSource {

    private var start =  AtomicLong(0L)
    private var end =  AtomicLong(0L)
    private var endCount = AtomicInteger(0)

    fun start() {
        start.compareAndSet(0, System.currentTimeMillis())
    }

    fun end() {
        end.set(System.currentTimeMillis())
        val endCount = endCount.incrementAndGet()
        if(endCount == 100) {
            val duration = end.get() - start.get()
            Log.e("CCOOVV", "duration ${duration}")
        }
    }

    fun saveImageLatencyData(
        imageUrl: String,
        latencyData: ApiDebugLogData,
    ) {

    }

}
