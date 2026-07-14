package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.theme.Poppins

// Storage utilities subscreen layout
@Composable
fun StorageSubScreen(
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
        SettingsGroupCard(title = stringResource(R.string.setting_section_storage)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.setting_storage_coming_soon), fontFamily = Poppins, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}
