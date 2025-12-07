package com.riyga.identifier.di

import com.riyga.identifier.data.birds.RecordRepository
import com.riyga.identifier.presentation.ui.BirdDetectionResultViewModel
import com.riyga.identifier.presentation.ui.BirdHistoryViewModel
import com.riyga.identifier.presentation.ui.ProgressViewModel
import com.riyga.identifier.presentation.ui.StartScreenViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    single { ProgressViewModel(get(), get()) }
    viewModel { StartScreenViewModel(androidContext(), get()) }
    viewModel { BirdHistoryViewModel(get<RecordRepository>()) }
    viewModel { BirdDetectionResultViewModel(get<RecordRepository>(), androidContext()) }
}