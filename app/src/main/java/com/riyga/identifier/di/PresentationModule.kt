package com.riyga.identifier.di

import com.riyga.identifier.presentation.ui.IdentifierViewModel
import org.koin.dsl.module

val presentationModule = module {
    single { IdentifierViewModel(get(), get()) }
}