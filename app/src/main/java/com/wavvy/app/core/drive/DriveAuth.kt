package com.wavvy.app.core.drive

// Android context and intents
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
// Google sign in and its authorization
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
// Coroutines
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

// The only permission asked, the private folder of the app in the Drive of the user, which no other file of the Drive is seen from
private const val AppDataScope = "https://www.googleapis.com/auth/drive.appdata"

// What the authorization of the Drive answered, the key to use, or the screen of Google that asks the user first
sealed interface DriveAccess {
    data class Ready(val token: String) : DriveAccess
    data class NeedsConsent(val request: PendingIntent) : DriveAccess
}

// Asks Google for the key to the private folder of the app, the key is short lived so it is asked again each time it is needed
object DriveAuth {
    suspend fun authorize(context: Context): DriveAccess = suspendCancellableCoroutine { continuation ->
        val request = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(AppDataScope))).build()
        Identity.getAuthorizationClient(context).authorize(request)
            .addOnSuccessListener { result ->
                val pending = result.pendingIntent
                val token = result.accessToken
                when {
                    result.hasResolution() && pending != null -> continuation.resume(DriveAccess.NeedsConsent(pending))
                    token != null -> continuation.resume(DriveAccess.Ready(token))
                    else -> continuation.resumeWithException(IllegalStateException("No key from Google"))
                }
            }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }

    // The key that comes back from the screen of Google after the user agreed
    fun tokenFrom(context: Context, data: Intent?): String? =
        runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken }.getOrNull()
}
