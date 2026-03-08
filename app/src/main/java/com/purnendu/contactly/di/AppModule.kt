package com.purnendu.contactly.di

import com.purnendu.contactly.MainActivityViewModel
import com.purnendu.contactly.alarm.ContactlyAlarmManager
import com.purnendu.contactly.geofence.ContactlyGeofenceManager
import com.purnendu.contactly.domain.repository.AppPreferences
import com.purnendu.contactly.data.local.preferences.AppPreferencesImpl
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.repository.ContactsRepository
import com.purnendu.contactly.data.repository.ActivationsRepository
import com.purnendu.contactly.data.utils.AndroidPermissionChecker
import com.purnendu.contactly.data.utils.ImageStorageManager
import com.purnendu.contactly.common.PermissionChecker
import com.purnendu.contactly.geofence.GeofenceBroadcastReceiver
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
 * - ContactlyAlarmManager: Central manager for all alarm operations
 * 
 * This makes all ViewModels fully testable without Android mocks.
 */
val appModule = module {
    
    // ========== Database ==========
    single { AppDatabase.getDataBase(androidContext()) }
    
    // ========== Repositories ==========
    single { ActivationsRepository(get(), get()) }
    single { ContactsRepository(androidContext().contentResolver) }
    
    // ========== Preferences ==========
    // Interface → Implementation binding
    single<AppPreferences> { AppPreferencesImpl(androidContext()) }
    
    // ========== Abstractions ==========
    // These interfaces hide Android Context from ViewModels, improving testability
    single<PermissionChecker> { AndroidPermissionChecker(androidContext()) }
    
    // ========== Managers ==========
    // ContactlyAlarmManager handles all alarm-related operations
    single { ContactlyAlarmManager(androidContext(), get(), get()) }
    single { ImageStorageManager(androidContext()) }
    single { ContactlyGeofenceManager(androidContext(), get(), GeofenceBroadcastReceiver::class.java) }
    
    // ========== ViewModels ==========
    // ViewModels now depend on interfaces, not Android classes
    viewModel { MainActivityViewModel(get(), get(), get()) }
}
