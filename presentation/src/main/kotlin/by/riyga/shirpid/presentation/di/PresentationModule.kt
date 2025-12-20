package by.riyga.shirpid.presentation.di

import by.riyga.shirpid.presentation.player.AudioPlayer
import by.riyga.shirpid.presentation.player.MediaPlayerController
import by.riyga.shirpid.presentation.ui.ComposeAppViewModel
import by.riyga.shirpid.presentation.ui.record.RecordViewModel
import by.riyga.shirpid.presentation.ui.history.BirdHistoryViewModel
import by.riyga.shirpid.presentation.ui.progress.ProgressViewModel
import by.riyga.shirpid.presentation.ui.settings.SettingsViewModel
import by.riyga.shirpid.presentation.ui.start.StartScreenViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    single<AudioPlayer> { MediaPlayerController(get()) }
    viewModelOf(::ProgressViewModel)
    viewModelOf(::StartScreenViewModel)
    viewModelOf(::BirdHistoryViewModel)
    viewModelOf(::RecordViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ComposeAppViewModel)
}