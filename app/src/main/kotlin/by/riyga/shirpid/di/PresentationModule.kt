package by.riyga.shirpid.di

import by.riyga.shirpid.data.birds.RecordRepository
import by.riyga.shirpid.data.preferences.AppPreferences
import by.riyga.shirpid.presentation.ui.detection_result.BirdDetectionResultViewModel
import by.riyga.shirpid.presentation.ui.history.BirdHistoryViewModel
import by.riyga.shirpid.presentation.ui.progress.ProgressViewModel
import by.riyga.shirpid.presentation.ui.settings.SettingsViewModel
import by.riyga.shirpid.presentation.ui.start.StartScreenViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    single { ProgressViewModel(get(), get()) }
    viewModel { StartScreenViewModel(androidContext(), get()) }
    viewModel { BirdHistoryViewModel(get<RecordRepository>()) }
    viewModel { BirdDetectionResultViewModel(get<RecordRepository>(), androidContext()) }
    viewModel { SettingsViewModel(get<AppPreferences>()) }
}