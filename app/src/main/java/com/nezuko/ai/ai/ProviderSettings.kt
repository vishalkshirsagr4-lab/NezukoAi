package com.nezuko.ai.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nezuko.ai.security.KeyStoreSecrets
import kotlinx.coroutines.flow.first

private val Context.settingsStore by preferencesDataStore(name = "provider_settings")

class ProviderSettings(private val context: Context, private val secrets: KeyStoreSecrets = KeyStoreSecrets()) {
    private val kindKey = stringPreferencesKey("provider_kind")
    private val modelKey = stringPreferencesKey("model")
    private val secretKey = stringPreferencesKey("encrypted_api_key")

    suspend fun save(config: ProviderConfig) {
        val encrypted = if (config.apiKey.isBlank()) "" else secrets.encrypt(config.apiKey)
        context.settingsStore.edit {
            it[kindKey] = config.kind.name
            it[modelKey] = config.model
            it[secretKey] = encrypted
        }
    }
    suspend fun load(): ProviderConfig {
        val p = context.settingsStore.data.first()
        val encrypted = p[secretKey].orEmpty()
        val apiKey = if (encrypted.isBlank()) "" else runCatching { secrets.decrypt(encrypted) }.getOrDefault("")
        return ProviderConfig(
            runCatching { ProviderKind.valueOf(p[kindKey] ?: "GEMINI") }.getOrDefault(ProviderKind.GEMINI),
            apiKey,
            p[modelKey] ?: "gemini-2.0-flash"
        )
    }
    suspend fun clearKey() = context.settingsStore.edit { it[secretKey] = "" }
}
