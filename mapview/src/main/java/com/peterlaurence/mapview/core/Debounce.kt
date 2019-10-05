package com.peterlaurence.mapview.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Returns a [SendChannel] which accepts messages of type [T].
 * The provided [block] function is executed only if a particular timespan (here, [wait]) has passed
 * without the [SendChannel] receiving an object.
 * When [block] is executed, it's provided with the last [T] value sent to the channel.
 *
 * @author peterLaurence
 */
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