package com.lonewolf.wavvy.data

import android.content.Context

// Data layer
class AuthRepositoryImpl(
    private val context: Context
) : AuthRepository {
    // Session management logic goes here
    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getSessionToken(): String? = null

    override suspend fun logout() {}
}
