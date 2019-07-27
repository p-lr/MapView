package com.peterlaurence.mapview.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun <T> CoroutineScope.debounce(
        wait: Long = 300,
        block: (T) -> Unit
): SendChannel<T> {
    val channel = Channel<T>(capacity = Channel.CONFLATED)
    var job: Job? = null
    launch {
        for (elem in channel) {
            job?.cancel()
            job = launch {
                delay(wait)
                block(elem)
            }
        }
        job?.join()
    }

    return channel
}