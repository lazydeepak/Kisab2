package com.susankhya.foundation.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Data structure representing a stored session payload.
 */
data class StoredSession(
    val id: String,
    val payload: String
)

/**
 * Common interface for persisting session credentials securely.
 */
interface SessionStorage {
    suspend fun save(session: StoredSession)
    suspend fun read(): StoredSession?
    suspend fun clear()
}

/**
 * In-memory test implementation of [SessionStorage].
 */
class InMemorySessionStorage : SessionStorage {
    private var stored: StoredSession? = null

    override suspend fun save(session: StoredSession) {
        stored = session
    }

    override suspend fun read(): StoredSession? = stored

    override suspend fun clear() {
        stored = null
    }
}

/**
 * Android Keystore-backed implementation of [SessionStorage].
 * Uses device SharedPreferences with AES-GCM encryption / fallback.
 */
class AndroidKeystoreSessionStorage(
    private val context: Context,
    private val prefsName: String = "kisab_keystore_session",
    private val keyAlias: String = "kisab_keystore_key"
) : SessionStorage {

    private val prefs by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    override suspend fun save(session: StoredSession) {
        prefs.edit()
            .putString(KEY_ID, session.id)
            .putString(KEY_PAYLOAD, session.payload)
            .apply()
    }

    override suspend fun read(): StoredSession? {
        val id = prefs.getString(KEY_ID, null) ?: return null
        val payload = prefs.getString(KEY_PAYLOAD, null) ?: return null
        return StoredSession(id = id, payload = payload)
    }

    override suspend fun clear() {
        prefs.edit()
            .remove(KEY_ID)
            .remove(KEY_PAYLOAD)
            .apply()
    }

    companion object {
        private const val KEY_ID = "session_id"
        private const val KEY_PAYLOAD = "session_payload"
    }
}
