package com.example.data.provider

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Global centralized file-locking registry across all provider and repository instances.
 * Guarantees that any read, write, or validation operation for a specific canonical file path
 * is strictly serialized across threads and distinct class instances.
 */
object StorageLockManager {
    private val pathLocks = ConcurrentHashMap<String, Any>()

    fun getLockFor(file: File): Any {
        val key = try {
            file.canonicalPath
        } catch (_: Exception) {
            file.absolutePath
        }
        return pathLocks.computeIfAbsent(key) { Any() }
    }
}
