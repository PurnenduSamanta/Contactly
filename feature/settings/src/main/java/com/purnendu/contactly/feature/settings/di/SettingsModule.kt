package com.purnendu.contactly.feature.settings.di

import com.purnendu.contactly.ui.screens.setting.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(get(), get(), get()) }
}
