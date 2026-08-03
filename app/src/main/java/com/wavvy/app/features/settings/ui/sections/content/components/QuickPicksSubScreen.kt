package com.wavvy.app.features.settings.ui.sections.content.components

import java.text.Collator
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 components and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and utilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources and components
import com.wavvy.app.R
import com.wavvy.app.core.data.remote.kworb.KworbChartPeriod
import com.wavvy.app.core.data.remote.kworb.KworbChartScope
import com.wavvy.app.core.data.remote.kworb.KworbCountries
import com.wavvy.app.core.designsystem.bottomsheet.SequentialBottomSheet
import com.wavvy.app.core.designsystem.theme.Poppins
import com.wavvy.app.features.home.models.KworbChartConfig
import com.wavvy.app.features.home.models.QuickPicksSource
import com.wavvy.app.features.settings.ui.components.SettingsGroupCard
import com.wavvy.app.features.settings.ui.components.SettingsInteractiveRow

// Country display name
@Composable
private fun countryDisplayName(code: String): String {
    val names = stringArrayResource(R.array.kworb_country_names)
    val index = KworbCountries.SUPPORTED_CODES.indexOf(code)
    return if (index >= 0) names.getOrElse(index) { code.uppercase() } else code.uppercase()
}

// Quick picks subscreen
@Composable
fun QuickPicksSubScreen(
    isUserLoggedIn: Boolean,
    currentSource: QuickPicksSource,
    kworbConfig: KworbChartConfig,
    onShowSourceSheet: () -> Unit,
    onShowScopeSheet: () -> Unit,
    onShowCountrySheet: () -> Unit,
    onShowPeriodSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSource = if (!isUserLoggedIn && currentSource == QuickPicksSource.YTMUSIC_API) {
        QuickPicksSource.KWORB_CHART
    } else {
        currentSource
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_quick_picks_source_title)) {
            val sourceSubtitle = when (activeSource) {
                QuickPicksSource.YTMUSIC_API -> stringResource(R.string.setting_quick_picks_source_ytmusic)
                QuickPicksSource.RECENT_HISTORY -> stringResource(R.string.setting_quick_picks_source_history)
                QuickPicksSource.KWORB_CHART -> stringResource(R.string.setting_quick_picks_source_kworb)
            }

            SettingsInteractiveRow(
                title = stringResource(R.string.setting_quick_picks_source_title),
                subtitle = sourceSubtitle,
                icon = Icons.Rounded.AutoAwesome,
                showDivider = false,
                onClick = onShowSourceSheet
            )
        }

        if (activeSource == QuickPicksSource.KWORB_CHART) {
            SettingsGroupCard(title = stringResource(R.string.setting_quick_picks_kworb_options_title)) {
                val scopeSubtitle = when (kworbConfig.scope) {
                    KworbChartScope.GLOBAL_TRENDING_MUSIC -> stringResource(R.string.setting_quick_picks_scope_global)
                    KworbChartScope.COUNTRY -> stringResource(R.string.setting_quick_picks_scope_country)
                }

                SettingsInteractiveRow(
                    title = stringResource(R.string.setting_quick_picks_scope_title),
                    subtitle = scopeSubtitle,
                    icon = Icons.Rounded.Public,
                    showDivider = kworbConfig.scope == KworbChartScope.COUNTRY,
                    onClick = onShowScopeSheet
                )

                if (kworbConfig.scope == KworbChartScope.COUNTRY) {
                    SettingsInteractiveRow(
                        title = stringResource(R.string.setting_quick_picks_country_title),
                        subtitle = countryDisplayName(kworbConfig.countryCode),
                        icon = Icons.Rounded.Language,
                        showDivider = true,
                        onClick = onShowCountrySheet
                    )

                    val periodSubtitle = when (kworbConfig.period) {
                        KworbChartPeriod.DAILY -> stringResource(R.string.setting_quick_picks_period_daily)
                        KworbChartPeriod.WEEKLY -> stringResource(R.string.setting_quick_picks_period_weekly)
                    }

                    SettingsInteractiveRow(
                        title = stringResource(R.string.setting_quick_picks_period_title),
                        subtitle = periodSubtitle,
                        icon = Icons.Rounded.DateRange,
                        showDivider = false,
                        onClick = onShowPeriodSheet
                    )
                }
            }
        }
    }
}

// Quick picks sheet overlays
@Composable
fun QuickPicksSheetOverlays(
    isUserLoggedIn: Boolean,
    currentSource: QuickPicksSource,
    kworbConfig: KworbChartConfig,
    onSourceSelected: (QuickPicksSource) -> Unit,
    onKworbConfigChanged: (KworbChartConfig) -> Unit,
    showSourceSheet: Boolean,
    onDismissSource: () -> Unit,
    showScopeSheet: Boolean,
    onDismissScope: () -> Unit,
    showCountrySheet: Boolean,
    onDismissCountry: () -> Unit,
    showPeriodSheet: Boolean,
    onDismissPeriod: () -> Unit,
) {
    val activeSource = if (!isUserLoggedIn && currentSource == QuickPicksSource.YTMUSIC_API) {
        QuickPicksSource.KWORB_CHART
    } else {
        currentSource
    }

    val availableSources = if (isUserLoggedIn) {
        QuickPicksSource.entries.toList()
    } else {
        listOf(QuickPicksSource.KWORB_CHART, QuickPicksSource.RECENT_HISTORY)
    }

    // Source sheet
    if (showSourceSheet) {
        SequentialBottomSheet(onDismiss = onDismissSource, autoWrap = true) { animateDismiss ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_title_quick_picks_source),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                availableSources.forEachIndexed { index, source ->
                    val isSelected = activeSource == source
                    val label = when (source) {
                        QuickPicksSource.YTMUSIC_API -> stringResource(R.string.setting_quick_picks_source_ytmusic)
                        QuickPicksSource.RECENT_HISTORY -> stringResource(R.string.setting_quick_picks_source_history)
                        QuickPicksSource.KWORB_CHART -> stringResource(R.string.setting_quick_picks_source_kworb)
                    }
                    val icon = when (source) {
                        QuickPicksSource.YTMUSIC_API -> Icons.Rounded.AutoAwesome
                        QuickPicksSource.RECENT_HISTORY -> Icons.Rounded.History
                        QuickPicksSource.KWORB_CHART -> Icons.AutoMirrored.Rounded.ShowChart
                    }

                    SelectionSheetRow(
                        label = label,
                        icon = icon,
                        isSelected = isSelected,
                        onClick = {
                            onSourceSelected(source)
                            animateDismiss()
                        }
                    )

                    if (index < availableSources.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }
            }
        }
    }

    // Scope sheet
    if (showScopeSheet) {
        SequentialBottomSheet(onDismiss = onDismissScope, autoWrap = true) { animateDismiss ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_title_quick_picks_scope),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                KworbChartScope.entries.forEachIndexed { index, scope ->
                    val isSelected = kworbConfig.scope == scope
                    val label = when (scope) {
                        KworbChartScope.GLOBAL_TRENDING_MUSIC -> stringResource(R.string.setting_quick_picks_scope_global)
                        KworbChartScope.COUNTRY -> stringResource(R.string.setting_quick_picks_scope_country)
                    }
                    val icon = when (scope) {
                        KworbChartScope.GLOBAL_TRENDING_MUSIC -> Icons.Rounded.Public
                        KworbChartScope.COUNTRY -> Icons.Rounded.TravelExplore
                    }

                    SelectionSheetRow(
                        label = label,
                        icon = icon,
                        isSelected = isSelected,
                        onClick = {
                            onKworbConfigChanged(kworbConfig.copy(scope = scope))
                            animateDismiss()
                        }
                    )

                    if (index < KworbChartScope.entries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }
            }
        }
    }

    // Country sheet
    if (showCountrySheet) {
        val configuration = LocalConfiguration.current
        val locale = configuration.locales[0] ?: LocalLocale.current.platformLocale
        val countryNames = stringArrayResource(R.array.kworb_country_names)
        val countriesWithNames = remember(countryNames) {
            KworbCountries.SUPPORTED_CODES.mapIndexed { index, code ->
                code to (countryNames.getOrElse(index) { code.uppercase() })
            }
        }
        val sortedCountries = remember(locale, countriesWithNames) {
            val collator = Collator.getInstance(locale)
            countriesWithNames.sortedWith(compareBy(collator) { it.second })
        }

        SequentialBottomSheet(onDismiss = onDismissCountry, autoWrap = true) { animateDismiss ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_title_quick_picks_country),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                sortedCountries.forEachIndexed { index, (code, label) ->
                    val isSelected = kworbConfig.countryCode == code

                    SelectionSheetRow(
                        label = label,
                        icon = Icons.Rounded.Language,
                        isSelected = isSelected,
                        onClick = {
                            onKworbConfigChanged(kworbConfig.copy(countryCode = code))
                            animateDismiss()
                        }
                    )

                    if (index < sortedCountries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }
            }
        }
    }

    // Period sheet
    if (showPeriodSheet) {
        SequentialBottomSheet(onDismiss = onDismissPeriod, autoWrap = true) { animateDismiss ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_title_quick_picks_period),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                KworbChartPeriod.entries.forEachIndexed { index, period ->
                    val isSelected = kworbConfig.period == period
                    val label = when (period) {
                        KworbChartPeriod.DAILY -> stringResource(R.string.setting_quick_picks_period_daily)
                        KworbChartPeriod.WEEKLY -> stringResource(R.string.setting_quick_picks_period_weekly)
                    }

                    SelectionSheetRow(
                        label = label,
                        icon = Icons.Rounded.DateRange,
                        isSelected = isSelected,
                        onClick = {
                            onKworbConfigChanged(kworbConfig.copy(period = period))
                            animateDismiss()
                        }
                    )

                    if (index < KworbChartPeriod.entries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        )
                    }
                }
            }
        }
    }
}

// Selection sheet row
@Composable
private fun SelectionSheetRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tintColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val backgroundTint = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val contentTint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(backgroundTint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) contentTint else tintColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontFamily = Poppins,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = tintColor,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
