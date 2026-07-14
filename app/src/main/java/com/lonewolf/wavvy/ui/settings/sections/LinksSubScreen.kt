package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsItemRow

// Community and external web links subscreen layout
@Composable
fun LinksSubScreen(
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
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_community)) {
            SettingsItemRow(title = stringResource(R.string.setting_source_code), subtitle = stringResource(R.string.setting_source_code_desc), icon = Icons.Rounded.Code, showDivider = true, onClick = {})
            SettingsItemRow(title = stringResource(R.string.setting_support_project), subtitle = stringResource(R.string.setting_support_project_desc), icon = Icons.Rounded.Favorite, showDivider = false, onClick = {})
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_support)) {
            SettingsItemRow(title = stringResource(R.string.setting_faq), subtitle = stringResource(R.string.setting_faq_desc), icon = Icons.Rounded.HelpOutline, showDivider = true, onClick = {})
            SettingsItemRow(title = stringResource(R.string.setting_terms_of_service), subtitle = stringResource(R.string.setting_terms_of_service_desc), icon = Icons.Rounded.Description, showDivider = false, onClick = {})
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}
