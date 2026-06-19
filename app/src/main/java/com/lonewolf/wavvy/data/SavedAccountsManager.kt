package com.lonewolf.wavvy.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lonewolf.wavvy.data.models.AccountData
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

private val Context.accountsDataStore: DataStore<Preferences> by preferencesDataStore(name = "saved_accounts")
private val SavedAccountsKey = stringPreferencesKey("accounts_list")

private const val KEY_ALIAS = "wavvy_accounts_key"
private const val TTL_MS = 7 * 24 * 60 * 60 * 1000L

// Saved accounts storage with encryption and TTL
class SavedAccountsManager(private val context: Context) {

    // Keystore-backed AES-GCM key
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGen.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGen.generateKey()
    }

    // Encrypt plaintext string
    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // Decrypt ciphertext string
    private fun decrypt(ciphertext: String): String {
        val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    // Load all non-expired accounts from storage
    suspend fun getSavedAccounts(): List<SavedAccount> {
        val raw = context.accountsDataStore.data
            .map { it[SavedAccountsKey] }
            .firstOrNull() ?: return emptyList()

        return try {
            val arr = JSONArray(raw)
            val now = System.currentTimeMillis()
            val result = mutableListOf<SavedAccount>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lastUsed = obj.getLong("lastUsedAt")
                if (now - lastUsed <= TTL_MS) {
                    result.add(
                        SavedAccount(
                            name = obj.getString("name"),
                            handle = obj.optString("handle").ifBlank { null },
                            pictureUrl = obj.optString("pictureUrl").ifBlank { null },
                            cookies = decrypt(obj.getString("cookies")),
                            lastUsedAt = lastUsed
                        )
                    )
                }
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Save or update account entry, resetting TTL
    suspend fun saveAccount(account: AccountData, cookies: String) {
        val existing = getSavedAccounts().toMutableList()
        existing.removeAll { it.handle == account.handle }
        existing.add(
            SavedAccount(
                name = account.name,
                handle = account.handle,
                pictureUrl = account.pictureUrl,
                cookies = cookies,
                lastUsedAt = System.currentTimeMillis()
            )
        )
        persist(existing)
    }

    // Remove specific account entry
    suspend fun removeAccount(handle: String?) {
        val updated = getSavedAccounts().filter { it.handle != handle }
        persist(updated)
    }

    // Serialize and write accounts list to DataStore
    private suspend fun persist(accounts: List<SavedAccount>) {
        val arr = JSONArray()
        for (acc in accounts) {
            arr.put(JSONObject().apply {
                put("name", acc.name)
                put("handle", acc.handle ?: "")
                put("pictureUrl", acc.pictureUrl ?: "")
                put("cookies", encrypt(acc.cookies))
                put("lastUsedAt", acc.lastUsedAt)
            })
        }
        context.accountsDataStore.edit { it[SavedAccountsKey] = arr.toString() }
    }
}

// Account entry model
data class SavedAccount(
    val name: String,
    val handle: String?,
    val pictureUrl: String?,
    val cookies: String,
    val lastUsedAt: Long
)
