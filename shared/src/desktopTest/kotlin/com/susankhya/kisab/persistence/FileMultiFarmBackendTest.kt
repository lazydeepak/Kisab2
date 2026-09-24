package com.susankhya.kisab.persistence

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileMultiFarmBackendTest {

    @Test
    fun testFilePersistenceLifecycle() {
        val tempDir = Files.createTempDirectory("kisab_test_desktop").toFile()
        try {
            val backend1 = FileMultiFarmBackend(tempDir)
            backend1.commit(
                puts = mapOf("active_farm" to "farm_123", "farm_farm_123" to "payload_123"),
                removes = emptySet()
            )

            assertEquals("farm_123", backend1.getString("active_farm"))
            assertEquals("payload_123", backend1.getString("farm_farm_123"))

            // Verify persistence by reloading with a fresh instance
            val backend2 = FileMultiFarmBackend(tempDir)
            assertEquals("farm_123", backend2.getString("active_farm"))
            assertEquals("payload_123", backend2.getString("farm_farm_123"))

            // Remove a key
            backend2.commit(puts = emptyMap(), removes = setOf("active_farm"))
            val backend3 = FileMultiFarmBackend(tempDir)
            assertNull(backend3.getString("active_farm"))
            assertEquals("payload_123", backend3.getString("farm_farm_123"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
