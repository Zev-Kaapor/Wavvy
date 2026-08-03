package com.wavvy.app.core.di

// ViewModels
import com.wavvy.app.features.auth.ui.viewmodel.AuthViewModel
import com.wavvy.app.features.home.ui.HomeViewModel
import com.wavvy.app.features.player.ui.PlayerViewModel
import com.wavvy.app.features.settings.ui.SettingsViewModel

// Data layer and managers
import com.wavvy.app.core.data.local.ChartResolutionCache
import com.wavvy.app.core.data.local.SettingsStorage
import com.wavvy.app.core.data.remote.kworb.KworbChartRepository
import com.wavvy.app.features.auth.data.AuthRepository
import com.wavvy.app.features.auth.data.AuthRepositoryImpl
import com.wavvy.app.features.auth.data.SavedAccountsManager
import com.wavvy.app.features.auth.ui.viewmodel.AuthManager
import com.wavvy.app.features.home.data.QuickPicksRepository
import com.wavvy.app.features.home.data.RecentHistoryManager
import com.wavvy.app.features.search.data.SearchHistoryManager

// Koin framework
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Dependency injection module
val appModule = module {

    // Singletons and managers
    single<SettingsStorage> { SettingsStorage(androidContext()) }
    single<RecentHistoryManager> { RecentHistoryManager(androidContext()) }
    single<SearchHistoryManager> { SearchHistoryManager(androidContext()) }
    single<SavedAccountsManager> { SavedAccountsManager(androidContext()) }
    single<AuthManager> { AuthManager(androidContext()) }
    single<ChartResolutionCache> { ChartResolutionCache(androidContext()) }
    single<KworbChartRepository> { KworbChartRepository() }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(androidContext()) }
    single<QuickPicksRepository> {
        QuickPicksRepository(
            authRepository = get(),
            recentHistoryManager = get(),
            kworbChartRepository = get(),
            chartResolutionCache = get(),
            settingsStorage = get()
        )
    }

    // ViewModels
    viewModel {
        HomeViewModel(
            settingsStorage = get(),
            authRepository = get(),
            recentHistoryManager = get(),
            quickPicksRepository = get()
        )
    }

    viewModel {
        AuthViewModel(androidApplication())
    }

    viewModel {
        PlayerViewModel(androidApplication())
    }

    viewModel {
        SettingsViewModel(
            settingsStorage = get(),
            recentHistoryManager = get(),
            searchHistoryManager = get(),
            quickPicksRepository = get(),
            authRepository = get()
        )
    }
}
