package com.wavvy.app.features.auth.data

// Android context and DataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and reactive flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Name and user name shown for the account that signed in
data class AccountDetails(val name: String, val handle: String?)

// The address of the banner of the channel, null when the channel has none
data class AccountBanner(val url: String?, val fetchedAt: Long)

// DataStore file and keys for the details
private val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore(name = "account")
private val NameKey = stringPreferencesKey("name")
private val HandleKey = stringPreferencesKey("handle")
private val BannerKey = stringPreferencesKey("channel_banner")
private val BannerFetchedKey = longPreferencesKey("channel_banner_fetched")

// Keeps the name and the user name of the account on the device, so they show even without a connection
class AccountStore(context: Context) {
    private val dataStore = context.applicationContext.accountDataStore

    // Empty until an account signed in
    val details: Flow<AccountDetails?> = dataStore.data.map { preferences ->
        preferences[NameKey]?.let { AccountDetails(name = it, handle = preferences[HandleKey]) }
    }

    suspend fun save(account: AccountInfo) {
        dataStore.edit { preferences ->
            preferences[NameKey] = account.name
            account.handle?.let { preferences[HandleKey] = it } ?: preferences.remove(HandleKey)
        }
    }

    // The banner of the channel and when it was looked for, so it is not asked at every opening
    val banner: Flow<AccountBanner> = dataStore.data.map { AccountBanner(url = it[BannerKey], fetchedAt = it[BannerFetchedKey] ?: 0L) }

    suspend fun saveBanner(url: String?, fetchedAt: Long) {
        dataStore.edit { preferences ->
            if (url != null) preferences[BannerKey] = url else preferences.remove(BannerKey)
            preferences[BannerFetchedKey] = fetchedAt
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
