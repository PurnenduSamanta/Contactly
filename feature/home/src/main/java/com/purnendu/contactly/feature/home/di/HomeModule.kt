package com.purnendu.contactly.feature.home.di

import com.purnendu.contactly.ui.screens.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
