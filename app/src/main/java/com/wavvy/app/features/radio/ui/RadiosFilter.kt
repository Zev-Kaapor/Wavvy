package com.wavvy.app.features.radio.ui

// Project resources
import com.wavvy.app.R

// What the search looks at, the word the site takes for it and the name on the button
enum class RadiosFilter(val wire: String, val titleRes: Int) {
    Name("nome", R.string.radios_filter_name),
    City("cidade", R.string.radios_filter_city),
    State("uf", R.string.radios_filter_state),
    Country("pais", R.string.radios_filter_country)
}
