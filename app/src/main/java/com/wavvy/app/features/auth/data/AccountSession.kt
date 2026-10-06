package com.wavvy.app.features.auth.data

// Android WebKit and context
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage

// What happens to the account on the device when it signs in and out
object AccountSession {
    // Keeps the name, the user name and the photo of the account, and gives true when the photo changed
    suspend fun apply(context: Context, account: AccountInfo): Result<Boolean> {
        AccountStore(context).save(account)

        return ProfilePhotoStore.save(context, account.photoUrl)
    }

    // Removes everything of the account from the device, the session of the sign in page included
    suspend fun signOut(context: Context) {
        clearWebSession()
        AccountStore(context).clear()
        ProfilePhotoStore.clear(context)
        EntryStore(context).clear()
    }

    // Clears the cookies and the data of the sign in page
    fun clearWebSession() {
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        WebStorage.getInstance().deleteAllData()
    }
}
