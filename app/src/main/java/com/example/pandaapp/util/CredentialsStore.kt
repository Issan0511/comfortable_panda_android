package com.example.pandaapp.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class Credentials(
    val username: String,
    val password: String
)

class CredentialsStore(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val fileName = PREF_FILE

        fun newEncryptedPrefs(): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        return try {
            newEncryptedPrefs()
        } catch (e: javax.crypto.AEADBadTagException) {
            // 暗号データと鍵が噛み合っていない → データ破棄して作り直し
            context.deleteSharedPreferences(fileName)
            newEncryptedPrefs()
        }
    }

    fun save(credentials: Credentials) {
        prefs.edit()
            .putString(KEY_USERNAME, credentials.username)
            .putString(KEY_PASSWORD, credentials.password)
            .apply()
    }

    fun load(): Credentials? {
        val username = prefs.getString(KEY_USERNAME, null)
        val password = prefs.getString(KEY_PASSWORD, null)
        return if (username.isNullOrBlank() || password.isNullOrBlank()) {
            null
        } else {
            Credentials(username, password)
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREF_FILE = "panda_credentials"
        const val KEY_USERNAME = "ecs_id"
        const val KEY_PASSWORD = "ecs_password"
    }
}
