package com.wavvy.app.core.radio

// Android context and text
import android.content.Context
import java.text.Normalizer
import java.util.Locale
// Project resources
import com.wavvy.app.core.innertube.deviceLocale

// The page that lists every country of the site, and the one of Brazil, which stands in when the country of the user is not there
const val RadiosCountriesPath = "/lista/pais"
private const val RadiosBrazilCountryPath = "/lista/pais/brasil/33"

// The language the site writes the names of the countries in
private val SiteLanguage = Locale.forLanguageTag("pt-BR")

// A text without accents and capitals, so two ways of writing a name compare as the same
fun String.plain(): String = Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()

// The country of the user in the list of the site, found by its name in Portuguese, Brazil when the site does not have it
suspend fun userRadiosCountry(context: Context): RadiosEntry? {
    val countries = RadiosReader.browse(context, RadiosCountriesPath).getOrNull()?.countries ?: return null
    val name = Locale.Builder().setRegion(deviceLocale().country).build().getDisplayCountry(SiteLanguage).plain()

    return countries.firstOrNull { it.title.plain() == name } ?: countries.firstOrNull { it.path == RadiosBrazilCountryPath }
}

// The page that lists all the stations of a country or a state, which has the same numbers as the page of its places
fun String.allStationsPath(): String = replaceFirst("/lista/", "/radio/")
