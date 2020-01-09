package com.peterlaurence.mapview.core

import java.util.*

internal class Pool<T>(private val threshold: Int = 100) {
    var size: Int = 0
        private set

    private val pool = LinkedList<T>()

    fun get(): T? {
        return if (pool.isNotEmpty()) {
            size--
            pool.removeFirst()
        } else {
            null
        }
    }

    fun put(o: T) {
        if (size < threshold) {
            size++
            pool.add(o)
        }
    }
}