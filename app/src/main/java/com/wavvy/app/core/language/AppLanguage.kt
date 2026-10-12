package com.wavvy.app.core.language

// Android context, resources and locales
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import java.util.Locale
// Project resources
import com.wavvy.app.R

// The language of the app, the one of the device or one the user picks, the names of the languages stay in their own language
enum class AppLanguage(val tag: String?, @StringRes val titleRes: Int?, val nativeName: String?) {
    System(null, R.string.settings_language_system, null),
    PortugueseBrazil("pt-BR", null, "Português (Brasil)"),
    English("en", null, "English");

    companion object {
        private const val PreferencesName = "language"
        private const val TagKey = "tag"

        // The language that is on, the system one when none was picked
        fun current(context: Context): AppLanguage {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java).applicationLocales.takeIf { !it.isEmpty }?.get(0)?.toLanguageTag()
            } else {
                context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(TagKey, null)
            }

            return entries.firstOrNull { it.tag != null && tag != null && tag.substringBefore('-') == it.tag.substringBefore('-') } ?: System
        }

        // Picks the language, from Android 13 the system keeps it and restarts the screen, before it the app keeps it and restarts the screen itself
        fun apply(context: Context, language: AppLanguage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java).applicationLocales =
                    language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
            } else {
                context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).edit().putString(TagKey, language.tag).apply()
                (context as? Activity)?.recreate()
            }
        }

        // The context that screens and services of Android 12 and before are made with, which carries the language that was picked
        fun wrap(base: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base

            val tag = base.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(TagKey, null) ?: return base
            val locale = Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
            return base.createConfigurationContext(configuration)
        }
    }
}
