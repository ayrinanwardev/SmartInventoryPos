package com.example.smartinventorypos.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.smartinventorypos.domain.model.Role
import com.example.smartinventorypos.domain.model.UserSession
import com.example.smartinventorypos.domain.repository.SessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the session encrypted on disk and in memory for quick reads, including from network threads. */
@Singleton
class EncryptedSessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cipher: KeystoreCipher
) : SessionStore {

    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val current = MutableStateFlow<UserSession?>(null)
    override val session: StateFlow<UserSession?> = current.asStateFlow()

    init {
        // Removes the token the previous version stored as plain text.
        context.deleteSharedPreferences(LEGACY_FILE_NAME)
        current.value = readStoredSession()
    }

    @Synchronized
    override fun save(session: UserSession) {
        prefs.edit {
            putString(KEY_TOKEN, cipher.encrypt(session.token))
            putString(KEY_ROLE, session.role?.name)
        }
        current.value = session
    }

    @Synchronized
    override fun clear() {
        prefs.edit { clear() }
        current.value = null
    }

    @Synchronized
    override fun clearIfCurrent(token: String) {
        if (current.value?.token == token) clear()
    }

    /** A token that can't be decrypted, such as one restored from a backup, just means signing in again. */
    private fun readStoredSession(): UserSession? {
        val encrypted = prefs.getString(KEY_TOKEN, null) ?: return null
        return try {
            UserSession(cipher.decrypt(encrypted), Role.fromName(prefs.getString(KEY_ROLE, null)))
        } catch (_: GeneralSecurityException) {
            prefs.edit { clear() }
            null
        } catch (_: IllegalArgumentException) {
            prefs.edit { clear() }
            null
        }
    }

    private companion object {
        const val FILE_NAME = "secure_session"
        const val LEGACY_FILE_NAME = "session"
        const val KEY_TOKEN = "token"
        const val KEY_ROLE = "role"
    }
}
