package by.riyga.shirpid.di

import data.di.dataModule
import org.koin.core.module.Module

val appModules = listOf<Module>(
    presentationModule,
    dataModule
)