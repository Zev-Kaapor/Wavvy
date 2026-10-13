package com.wavvy.app.core.discord

// Android context and DataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and reactive flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
// Project resources
import com.wavvy.app.R

// DataStore file and keys, apart from the settings and left out of the backups of the system and of the app, since a token is a key to the account
private val Context.discordDataStore: DataStore<Preferences> by preferencesDataStore(name = DiscordStoreName)
private val AccessKey = stringPreferencesKey("access_token")
private val RefreshKey = stringPreferencesKey("refresh_token")
private val ExpiresKey = longPreferencesKey("expires_at")
private val VerifierKey = stringPreferencesKey("pkce_verifier")
private val StateKey = stringPreferencesKey("pkce_state")
private val EnabledKey = booleanPreferencesKey("presence_enabled")
private val StatusKey = stringPreferencesKey("presence_status")
private val KindKey = stringPreferencesKey("presence_kind")
private val NameKey = stringPreferencesKey("presence_name")
private val DetailsKey = stringPreferencesKey("presence_details")
private val LineKey = stringPreferencesKey("presence_state")
private val CoverKey = booleanPreferencesKey("presence_cover")
private val Button1OnKey = booleanPreferencesKey("button1_on")
private val Button1LabelKey = stringPreferencesKey("button1_label")
private val Button1UrlKey = stringPreferencesKey("button1_url")
private val Button2OnKey = booleanPreferencesKey("button2_on")
private val Button2LabelKey = stringPreferencesKey("button2_label")
private val Button2UrlKey = stringPreferencesKey("button2_url")

// What the login with Discord gave, the key that is used, the one that renews it and when it stops working
data class DiscordTokens(val access: String, val refresh: String, val expiresAt: Long)

// How the user shows as, the word Discord takes for it
enum class DiscordStatus(val wire: String, val titleRes: Int) {
    Online("online", R.string.integrations_discord_status_online),
    Idle("idle", R.string.integrations_discord_status_idle),
    DoNotDisturb("dnd", R.string.integrations_discord_status_dnd)
}

// What the activity says the user does, the number Discord takes for it
enum class DiscordActivityKind(val wire: Int, val titleRes: Int) {
    Playing(0, R.string.integrations_discord_kind_playing),
    Listening(2, R.string.integrations_discord_kind_listening),
    Watching(3, R.string.integrations_discord_kind_watching),
    Competing(5, R.string.integrations_discord_kind_competing)
}

// A button under the activity, empty texts mean the ones the app has by default
data class DiscordButton(val isEnabled: Boolean, val label: String = "", val url: String = "")

// What the activity shows, the texts take {title}, {artist} and {id}, and an empty one means the default
data class DiscordSettings(
    val isEnabled: Boolean = true,
    val status: DiscordStatus = DiscordStatus.Online,
    val kind: DiscordActivityKind = DiscordActivityKind.Listening,
    val name: String = "",
    val details: String = "",
    val state: String = "",
    val showCover: Boolean = true,
    val button1: DiscordButton = DiscordButton(isEnabled = true),
    val button2: DiscordButton = DiscordButton(isEnabled = false)
)

// Keeps the tokens of the Discord of the user on this phone, and the secret of the login that is open until it comes back
class DiscordStore(context: Context) {
    private val dataStore = context.applicationContext.discordDataStore

    val isConnected: Flow<Boolean> = dataStore.data.map { it[AccessKey] != null }

    suspend fun tokens(): DiscordTokens? {
        val preferences = dataStore.data.first()
        val access = preferences[AccessKey] ?: return null
        return DiscordTokens(access, preferences[RefreshKey].orEmpty(), preferences[ExpiresKey] ?: 0L)
    }

    suspend fun saveTokens(tokens: DiscordTokens) {
        dataStore.edit {
            it[AccessKey] = tokens.access
            it[RefreshKey] = tokens.refresh
            it[ExpiresKey] = tokens.expiresAt
        }
    }

    // The secret of a login that was started and the code that tells it is the same one when it comes back
    suspend fun saveLogin(verifier: String, state: String) {
        dataStore.edit {
            it[VerifierKey] = verifier
            it[StateKey] = state
        }
    }

    // The secret of the login that is open, once, so a link that comes back twice is not used twice
    suspend fun takeLogin(): Pair<String, String>? {
        val preferences = dataStore.data.first()
        val verifier = preferences[VerifierKey]
        val state = preferences[StateKey]
        dataStore.edit {
            it.remove(VerifierKey)
            it.remove(StateKey)
        }
        return if (verifier != null && state != null) verifier to state else null
    }

    // Signing out takes the tokens away, what the user chose to show stays
    suspend fun clear() {
        dataStore.edit {
            it.remove(AccessKey)
            it.remove(RefreshKey)
            it.remove(ExpiresKey)
            it.remove(VerifierKey)
            it.remove(StateKey)
        }
    }

    val settings: Flow<DiscordSettings> = dataStore.data.map { preferences ->
        DiscordSettings(
            isEnabled = preferences[EnabledKey] ?: true,
            status = DiscordStatus.entries.firstOrNull { it.name == preferences[StatusKey] } ?: DiscordStatus.Online,
            kind = DiscordActivityKind.entries.firstOrNull { it.name == preferences[KindKey] } ?: DiscordActivityKind.Listening,
            name = preferences[NameKey].orEmpty(),
            details = preferences[DetailsKey].orEmpty(),
            state = preferences[LineKey].orEmpty(),
            showCover = preferences[CoverKey] ?: true,
            button1 = DiscordButton(preferences[Button1OnKey] ?: true, preferences[Button1LabelKey].orEmpty(), preferences[Button1UrlKey].orEmpty()),
            button2 = DiscordButton(preferences[Button2OnKey] ?: false, preferences[Button2LabelKey].orEmpty(), preferences[Button2UrlKey].orEmpty())
        )
    }

    suspend fun update(change: (DiscordSettings) -> DiscordSettings) {
        val next = change(settings.first())
        dataStore.edit {
            it[EnabledKey] = next.isEnabled
            it[StatusKey] = next.status.name
            it[KindKey] = next.kind.name
            it[NameKey] = next.name
            it[DetailsKey] = next.details
            it[LineKey] = next.state
            it[CoverKey] = next.showCover
            it[Button1OnKey] = next.button1.isEnabled
            it[Button1LabelKey] = next.button1.label
            it[Button1UrlKey] = next.button1.url
            it[Button2OnKey] = next.button2.isEnabled
            it[Button2LabelKey] = next.button2.label
            it[Button2UrlKey] = next.button2.url
        }
    }
}
