package com.wavvy.app.core.cast

// Android context
import android.content.Context
// Google Cast
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
// Project resources
import com.wavvy.app.R

// What the Cast framework asks the app when it starts, the receiver that plays on the TV is the page of Wavvy in docs/receiver
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(context.getString(R.string.cast_receiver_id))
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
