package com.susankhya.kisab.persistence

import java.io.File
import java.util.Properties

/**
 * File-backed atomic key/value store for Desktop (Windows/macOS/Linux) environments.
 * Persists farm states using atomic write-and-replace semantics on properties storage.
 */
class FileMultiFarmBackend(private val storageDir: File) : MultiFarmStoreBackend {
    private val dataFile = File(storageDir, "kisab_farms.properties")
    private val properties = Properties()
    private val lock = Any()

    init {
        storageDir.mkdirs()
        synchronized(lock) {
            if (dataFile.exists()) {
                dataFile.inputStream().use { properties.load(it) }
            }
        }
    }

    override fun getString(key: String): String? = synchronized(lock) {
        properties.getProperty(key)
    }

    override fun commit(puts: Map<String, String>, removes: Set<String>): Boolean = synchronized(lock) {
        try {
            puts.forEach { (k, v) -> properties.setProperty(k, v) }
            removes.forEach { k -> properties.remove(k) }
            val tempFile = File(storageDir, "kisab_farms.properties.tmp")
            tempFile.outputStream().use { properties.store(it, "Kisab MultiFarmStore Desktop Persistence") }
            if (dataFile.exists()) {
                dataFile.delete()
            }
            tempFile.renameTo(dataFile)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun allKeys(): Set<String> = synchronized(lock) {
        properties.stringPropertyNames().toSet()
    }
}
