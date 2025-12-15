package by.riyga.shirpid.di

import by.riyga.shirpid.player.AudioPlayer
import by.riyga.shirpid.player.MediaPlayerController
import by.riyga.shirpid.ui.detection_result.DetectionResultViewModel
import by.riyga.shirpid.ui.history.BirdHistoryViewModel
import by.riyga.shirpid.ui.progress.ProgressViewModel
import by.riyga.shirpid.ui.settings.SettingsViewModel
import by.riyga.shirpid.ui.start.StartScreenViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    single<AudioPlayer> { MediaPlayerController(get()) }
    viewModelOf(::ProgressViewModel)
    viewModelOf(::StartScreenViewModel)
    viewModelOf(::BirdHistoryViewModel)
    viewModelOf(::DetectionResultViewModel)
    viewModelOf(::SettingsViewModel)
}