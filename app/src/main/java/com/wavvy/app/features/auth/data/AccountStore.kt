package com.wavvy.app.features.auth.data

// Android context and DataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and reactive flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Name and user name shown for the account that signed in
data class AccountDetails(val name: String, val handle: String?)

// DataStore file and keys for the details
private val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore(name = "account")
private val NameKey = stringPreferencesKey("name")
private val HandleKey = stringPreferencesKey("handle")

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

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
