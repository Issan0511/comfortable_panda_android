package com.example.pandaapp.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class Credentials(
    val username: String,
    val password: String
)

class CredentialsStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(credentials: Credentials) {
        sharedPreferences.edit()
            .putString(KEY_USERNAME, credentials.username)
            .putString(KEY_PASSWORD, credentials.password)
            .apply()
    }

    fun load(): Credentials? {
        val username = sharedPreferences.getString(KEY_USERNAME, null)
        val password = sharedPreferences.getString(KEY_PASSWORD, null)
        return if (username.isNullOrBlank() || password.isNullOrBlank()) {
            null
        } else {
            Credentials(username, password)
        }
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    private companion object {
        const val PREF_FILE = "panda_credentials"
        const val KEY_USERNAME = "ecs_id"
        const val KEY_PASSWORD = "ecs_password"
    }
}
