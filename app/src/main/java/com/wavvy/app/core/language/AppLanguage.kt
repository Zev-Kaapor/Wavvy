package com.wavvy.app.core.language

// Android context, resources and locales
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import java.text.Collator
import java.util.Locale
// Project resources
import com.wavvy.app.R

// The language of the app, the one of the device or one the user picks, the names of the languages stay in their own language
enum class AppLanguage(val tag: String?, @StringRes val titleRes: Int?, val nativeName: String?) {
    System(null, R.string.settings_language_system, null),
    PortugueseBrazil("pt-BR", null, "Português (Brasil)"),
    English("en", null, "English"),
    Afrikaans("af", null, "Afrikaans"),
    Albanian("sq", null, "Shqip"),
    Arabic("ar", null, "العربية"),
    Azerbaijani("az", null, "Azərbaycan dili"),
    Basque("eu", null, "Euskara"),
    Belarusian("be", null, "Беларуская"),
    Bengali("bn-BD", null, "বাংলা (বাংলাদেশ)"),
    BengaliIndia("bn-IN", null, "বাংলা (ভারত)"),
    Bosnian("bs", null, "Bosanski"),
    Bulgarian("bg", null, "Български"),
    Burmese("my", null, "မြန်မာဘာသာ"),
    Catalan("ca", null, "Català"),
    ChineseSimplified("zh-Hans", null, "简体中文"),
    ChineseTraditional("zh-Hant", null, "繁體中文"),
    Croatian("hr", null, "Hrvatski"),
    Czech("cs", null, "Čeština"),
    Dutch("nl", null, "Nederlands"),
    EnglishCanada("en-CA", null, "English (Canada)"),
    Estonian("et", null, "Eesti"),
    Filipino("fil", null, "Filipino"),
    Finnish("fi", null, "Suomi"),
    French("fr", null, "Français"),
    Galician("gl", null, "Galego"),
    German("de", null, "Deutsch"),
    Greek("el", null, "Ελληνικά"),
    Hawaiian("haw", null, "ʻŌlelo Hawaiʻi"),
    Hebrew("he", null, "עברית"),
    Hindi("hi", null, "हिन्दी"),
    Hungarian("hu", null, "Magyar"),
    Indonesian("id", null, "Bahasa Indonesia"),
    Italian("it", null, "Italiano"),
    Japanese("ja", null, "日本語"),
    Kannada("kn", null, "ಕನ್ನಡ"),
    Khmer("km", null, "ខ្មែរ"),
    Korean("ko", null, "한국어"),
    Lao("lo", null, "ລາວ"),
    Lithuanian("lt", null, "Lietuvių"),
    Malay("ms", null, "Bahasa Melayu"),
    Malayalam("ml", null, "മലയാളം"),
    NorwegianBokmal("nb", null, "Norsk bokmål"),
    NorwegianNynorsk("nn", null, "Norsk nynorsk"),
    Persian("fa", null, "فارسی"),
    Polish("pl", null, "Polski"),
    PortuguesePortugal("pt-PT", null, "Português (Portugal)"),
    Punjabi("pa", null, "ਪੰਜਾਬੀ"),
    Romanian("ro", null, "Română"),
    Russian("ru", null, "Русский"),
    SerbianLatin("sr-Latn", null, "Srpski (latinica)"),
    Slovak("sk", null, "Slovenčina"),
    Slovenian("sl", null, "Slovenščina"),
    Spanish("es", null, "Español"),
    SpanishLatinAmerica("es-419", null, "Español (Latinoamérica)"),
    Swedish("sv", null, "Svenska"),
    Tamil("ta", null, "தமிழ்"),
    Telugu("te", null, "తెలుగు"),
    Thai("th", null, "ไทย"),
    Turkish("tr", null, "Türkçe"),
    Ukrainian("uk", null, "Українська"),
    Vietnamese("vi", null, "Tiếng Việt"),
    Urdu("ur", null, "اردو"),
    Marathi("mr", null, "मराठी"),
    Gujarati("gu", null, "ગુજરાતી");

    companion object {
        private const val PreferencesName = "language"
        private const val TagKey = "tag"

        // The language of the device first, then the others in the alphabetical order of their names written in their own language
        val choices: List<AppLanguage> by lazy {
            val collator = Collator.getInstance(Locale.ROOT)
            listOf(System) + entries.filter { it != System }.sortedWith { first, second -> collator.compare(first.nativeName.orEmpty(), second.nativeName.orEmpty()) }
        }

        // The language that is on, the system one when none was picked
        fun current(context: Context): AppLanguage {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java).applicationLocales.takeIf { !it.isEmpty }?.get(0)?.toLanguageTag()
            } else {
                context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(TagKey, null)
            }

            if (tag == null) return System

            // The exact tag first, then the language and the script, so the regions and the scripts of one language stay apart
            val picked = Locale.forLanguageTag(tag)
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
                ?: entries.firstOrNull {
                    val entry = it.tag?.let(Locale::forLanguageTag)
                    entry != null && entry.language == picked.language &&
                        (entry.script.isEmpty() || picked.script.isEmpty() || entry.script == picked.script)
                }
                ?: System
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
