package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsInteractiveRow

// Content and feeds filtering subscreen layout
@Composable
fun ContentSubScreen(
    isPlayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    val internalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_filters)) {
            SettingsInteractiveRow(title = stringResource(R.string.setting_content_language), subtitle = stringResource(R.string.setting_content_language_global), icon = Icons.Rounded.Translate, showDivider = true)
            SettingsInteractiveRow(title = stringResource(R.string.setting_content_country), subtitle = stringResource(R.string.setting_content_country_br), icon = Icons.Rounded.Place, showDivider = false)
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}
