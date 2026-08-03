package com.wavvy.app.features.home.models

// Chart scope and period selection for the kworb source
import com.wavvy.app.core.data.remote.kworb.KworbChartPeriod
import com.wavvy.app.core.data.remote.kworb.KworbChartScope

data class KworbChartConfig(
    val scope: KworbChartScope = KworbChartScope.GLOBAL_TRENDING_MUSIC,
    val countryCode: String = "us",
    val period: KworbChartPeriod = KworbChartPeriod.DAILY
)
