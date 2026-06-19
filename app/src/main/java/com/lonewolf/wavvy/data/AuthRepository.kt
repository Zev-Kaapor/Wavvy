package com.lonewolf.wavvy.data

// Data layer
interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun getSessionToken(): String?
    suspend fun logout()
}
