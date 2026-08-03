package com.wavvy.app.features.auth.data

// Domain models
import com.wavvy.app.features.auth.models.AccountData
import com.wavvy.app.features.home.models.QuickPick
import com.wavvy.app.features.home.ui.components.ForgottenTrack

// Authentication repository architecture interface
interface AuthRepository {
    suspend fun signInWithGoogle(cookies: String): Result<Unit>
    suspend fun getSessionToken(): String?
    suspend fun logout()
    suspend fun fetchAuthenticatedAccountDetails(): AccountData?

    // Remote data tracking for home screen selections
    suspend fun fetchQuickPicks(): List<QuickPick>
    suspend fun fetchForgottenFavorites(): List<ForgottenTrack>
    suspend fun fetchSongsByGenre(genre: String): List<QuickPick>
}
