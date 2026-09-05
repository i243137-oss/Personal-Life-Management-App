package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_secure_session")

class UserSessionManager(private val context: Context) {

    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_BACKEND_URL = stringPreferencesKey("backend_url")

        const val DEFAULT_BACKEND_URL = "https://personal-life-management-app.onrender.com/"
    }

    val authTokenFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_AUTH_TOKEN]
        }

    val isLoggedInFlow: Flow<Boolean> = authTokenFlow.map { token ->
        !token.isNullOrBlank()
    }

    val currentUserFlow: Flow<UserDto?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val id = preferences[KEY_USER_ID]
            val name = preferences[KEY_USER_NAME]
            val email = preferences[KEY_USER_EMAIL]

            if (!id.isNullOrBlank() && !name.isNullOrBlank() && !email.isNullOrBlank()) {
                UserDto(id = id, name = name, email = email)
            } else {
                null
            }
        }

    val backendUrlFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val stored = preferences[KEY_BACKEND_URL]
            if (stored.isNullOrBlank() || stored.contains("10.0.2.2") || stored.contains("localhost")) {
                DEFAULT_BACKEND_URL
            } else {
                stored
            }
        }

    suspend fun saveSession(token: String, user: UserDto) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = token
            preferences[KEY_USER_ID] = user.effectiveId
            preferences[KEY_USER_NAME] = user.name
            preferences[KEY_USER_EMAIL] = user.email
        }
    }

    suspend fun updateBackendUrl(url: String) {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKEND_URL] = sanitized
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_AUTH_TOKEN)
            preferences.remove(KEY_USER_ID)
            preferences.remove(KEY_USER_NAME)
            preferences.remove(KEY_USER_EMAIL)
        }
    }
}
