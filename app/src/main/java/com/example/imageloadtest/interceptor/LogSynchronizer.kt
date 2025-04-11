package com.example.imageloadtest.interceptor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import okhttp3.logging.HttpLoggingInterceptor

class LogSynchronizer {

    private var logActor: SendChannel<LogMessage>? = null

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun getActor(): SendChannel<LogMessage> {
        val actor: SendChannel<LogMessage>? = logActor

        if (actor != null && !actor.isClosedForSend) {
            return actor
        } else {
            val newActor: SendChannel<LogMessage> = GlobalScope.actor(Dispatchers.Default) {
                for (message in channel) {
                    handleMessage(message)
                }
            }
            logActor = newActor
            return newActor
        }
    }

    fun log(message: List<String>, messenger: HttpLoggingInterceptor.Logger) {
        GlobalScope.launch(Dispatchers.Default) {
            getActor().send(LogMessage(message, messenger))
        }
    }

    private fun handleMessage(logMessage: LogMessage) {
        logMessage.message.forEach {
            logMessage.logger.log(it)
        }
    }

    data class LogMessage(
        val message: List<String>,
        val logger: HttpLoggingInterceptor.Logger
    )
}
