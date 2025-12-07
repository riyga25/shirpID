package com.riyga.identifier.di

import com.riyga.identifier.data.birds.RecordRepository
import com.riyga.identifier.presentation.ui.detection_result.BirdDetectionResultViewModel
import com.riyga.identifier.presentation.ui.history.BirdHistoryViewModel
import com.riyga.identifier.presentation.ui.progress.ProgressViewModel
import com.riyga.identifier.presentation.ui.start.StartScreenViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    single { ProgressViewModel(get(), get()) }
    viewModel { StartScreenViewModel(androidContext(), get()) }
    viewModel { BirdHistoryViewModel(get<RecordRepository>()) }
    viewModel { BirdDetectionResultViewModel(get<RecordRepository>(), androidContext()) }
}