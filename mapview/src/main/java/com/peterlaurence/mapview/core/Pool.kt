package com.peterlaurence.mapview.core

import kotlin.collections.ArrayDeque

/**
 * A simple pool of objects.
 * This class isn't thread-safe.
 */
internal class Pool<T>(private val threshold: Int = 100) {
    var size: Int = 0
        private set

    private val pool = ArrayDeque<T>()

    fun get(): T? {
        return if (pool.isNotEmpty()) {
            pool.removeFirstOrNull().also {
                if (it != null) size--
            }
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