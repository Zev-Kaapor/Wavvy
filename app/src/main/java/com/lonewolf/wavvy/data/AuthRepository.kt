package com.lonewolf.wavvy.data

// Domain entity data model
import com.lonewolf.wavvy.data.models.AccountData

// Authentication repository architecture interface
interface AuthRepository {
    suspend fun signInWithGoogle(cookies: String): Result<Unit>
    suspend fun getSessionToken(): String?
    suspend fun logout()
    suspend fun fetchAuthenticatedAccountDetails(): AccountData?
}
