package by.riyga.shirpid.di

import data.database.RecordRepository
import data.preferences.AppPreferences
import by.riyga.shirpid.presentation.ui.detection_result.DetectionResultViewModel
import by.riyga.shirpid.presentation.ui.history.BirdHistoryViewModel
import by.riyga.shirpid.presentation.ui.progress.ProgressViewModel
import by.riyga.shirpid.presentation.ui.settings.SettingsViewModel
import by.riyga.shirpid.presentation.ui.start.StartScreenViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::ProgressViewModel)
    viewModelOf(::StartScreenViewModel)
    viewModelOf(::BirdHistoryViewModel)
    viewModelOf(::DetectionResultViewModel)
    viewModelOf(::SettingsViewModel)
}