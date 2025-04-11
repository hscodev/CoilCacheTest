package com.example.imageloadtest.interceptor

import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import kotlin.math.log

@OptIn(DelicateCoroutinesApi::class)
class TimingEventListener(
    private val debugLogDataSource: DebugLogDataSource
) : EventListener() {

    private var callStart: Long? = null
    private var callDuration: Long? = null

    private var dnsStart: Long? = null
    private var dnsEnd: Long? = null
    private var dnsDuration: Long? = null

    private var tlsStart: Long? = null
    private var tlsEnd: Long? = null
    private var tlsDuration: Long? = null

    private var requestStart: Long? = null
    private var requestEnd: Long? = null
    private var requestDuration: Long? = null

    private var responseStart: Long? = null
    private var responseEnd: Long? = null
    private var responseDuration: Long? = null

    private var latencyStart: Long? = null
    private var latencyEnd: Long? = null
    private var latencyDuration: Long? = null

    override fun callStart(call: Call) {
        callStart = System.currentTimeMillis()
        debugLogDataSource.start()
    }

    override fun dnsStart(call: Call, domainName: String) {
        dnsStart = System.currentTimeMillis()
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        dnsEnd = System.currentTimeMillis()
        dnsDuration = System.currentTimeMillis() - dnsStart!!
    }

    override fun secureConnectStart(call: Call) {
        tlsStart = System.currentTimeMillis()
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        tlsEnd = System.currentTimeMillis()
        tlsDuration = System.currentTimeMillis() - tlsStart!!
    }

    override fun requestHeadersStart(call: Call) {
        requestStart = System.currentTimeMillis()
    }

    override fun requestHeadersEnd(call: Call, request: Request) {
        requestEnd = System.currentTimeMillis()
        requestDuration = System.currentTimeMillis() - requestStart!!
        latencyStart = System.currentTimeMillis()
    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        requestEnd = System.currentTimeMillis()
        requestDuration = System.currentTimeMillis() - requestStart!!
        latencyStart = System.currentTimeMillis()
    }

    override fun responseHeadersStart(call: Call) {
        responseStart = System.currentTimeMillis()
        latencyEnd = System.currentTimeMillis()
        latencyDuration = System.currentTimeMillis() - latencyStart!!
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        responseEnd = System.currentTimeMillis()
        responseDuration = System.currentTimeMillis() - responseStart!!
        callDuration = System.currentTimeMillis() - callStart!!
    }

    override fun callEnd(call: Call) {
        if (isInvalidUrl(call)) return

        val logData = buildLogData(call, "success")

        Log.e("CCOOVV", logData.toString())

        debugLogDataSource.saveImageLatencyData(call.request().url.toString(), logData)
        debugLogDataSource.end()
    }

    override fun callFailed(call: Call, ioe: IOException) {
        if (isInvalidUrl(call)) return

        callDuration = System.currentTimeMillis() - callStart!!
        val logData = buildLogData(call, "failed")
        Log.e("CCOOVV", logData.toString())

        debugLogDataSource.saveImageLatencyData(call.request().url.toString(), logData)
        debugLogDataSource.end()
    }

    override fun canceled(call: Call) {
        if (isInvalidUrl(call)) return

        callDuration = System.currentTimeMillis() - callStart!!
        val logData = buildLogData(call, "canceled")
        Log.e("CCOOVV", logData.toString())

        debugLogDataSource.saveImageLatencyData(call.request().url.toString(), logData)
        debugLogDataSource.end()
    }

//    private fun logApiInfo(logData: ApiDebugLogData) {
//        logData.toJsonString()?.let {
//            GlobalScope.launch {
//                debugLogDataSource.saveApiDebugLog(it)
//                debugLogDataSource.fire(
//                    logData = it,
//                    pageId = "debug_latency",
//                    objectType = "api",
//                )
//            }
//        }
//    }

    private fun isInvalidUrl(call: Call): Boolean {
        val url = call.request().url.toString()
        return url.startsWith("https://ab-log")
                || url.startsWith("https://ohslog.")
                || url.startsWith("https://log.")
                || url.startsWith("https://ab-split.")
    }

    private fun buildLogData(call: Call, result: String): ApiDebugLogData {
        return ApiDebugLogData(
            url = call.request().url.toString(),
            latency = callDuration,
            requestStart = requestStart?.let { it - callStart!! },
            requestEnd = requestEnd?.let { it - callStart!! },
            responseStart = responseStart?.let { it - callStart!! },
            responseEnd = responseEnd?.let { it - callStart!! },

            duration = ApiDebugLogData.Duration(
                dnsDuration = dnsDuration,
                tlsDuration = tlsDuration,
                requestDuration = requestDuration,
                latencyDuration = latencyDuration,
                responseDuration = responseDuration,
                callDuration = callDuration,
            ),

            result = result,
        )
    }

    private fun Any?.toJsonString(): String? {
        return try {
            this?.let {
                GsonBuilder()
                    .disableHtmlEscaping()
                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                    .create()
                    .toJson(it)
            }
        } catch (exception: Exception) {
            null
        }
    }

}
