package com.purnendu.contactly.di

import com.purnendu.contactly.MainActivityViewModel
import com.purnendu.contactly.alarm.AlarmScheduler
import com.purnendu.contactly.alarm.AlarmSyncManager
import com.purnendu.contactly.alarm.AndroidAlarmScheduler
import com.purnendu.contactly.data.local.preferences.AppPreferences
import com.purnendu.contactly.data.local.preferences.AppPreferencesImpl
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.repository.ContactsRepository
import com.purnendu.contactly.data.repository.SchedulesRepository
import com.purnendu.contactly.ui.screens.schedule.SchedulesViewModel
import com.purnendu.contactly.ui.screens.setting.SettingsViewModel
import com.purnendu.contactly.utils.AndroidPermissionChecker
import com.purnendu.contactly.utils.PermissionChecker
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin Dependency Injection Module
 * 
 * This module defines all the dependencies for the app:
 * - Database (singleton)
 * - Repositories (singletons)
 * - Preferences (interface → implementation)
 * - Abstractions (interfaces bound to implementations)
 * - ViewModels
 * 
 * Pattern: We use interfaces to abstract Android-specific implementations:
 * - AppPreferences → AppPreferencesImpl
 * - PermissionChecker → AndroidPermissionChecker
 * - AlarmScheduler → AndroidAlarmScheduler
 * 
 * This makes all ViewModels fully testable without Android mocks.
 */
val appModule = module {
    
    // ========== Database ==========
    single { AppDatabase.getDataBase(androidContext()) }
    
    // ========== Repositories ==========
    single { SchedulesRepository(get()) }
    single { ContactsRepository(androidContext().contentResolver) }
    
    // ========== Preferences ==========
    // Interface → Implementation binding
    single<AppPreferences> { AppPreferencesImpl(androidContext()) }
    
    // ========== Abstractions ==========
    // These interfaces hide Android Context from ViewModels, improving testability
    single<PermissionChecker> { AndroidPermissionChecker(androidContext()) }
    single<AlarmScheduler> { AndroidAlarmScheduler(androidContext()) }
    
    // ========== Managers ==========
    // AlarmSyncManager needs context for AlarmManager access
    // Using factory so each usage gets fresh context reference
    factory { AlarmSyncManager(androidContext(), get(), get()) }
    
    // ========== ViewModels ==========
    // ViewModels now depend on interfaces, not Android classes
    viewModel { MainActivityViewModel(get()) }
    viewModel { SchedulesViewModel(get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
