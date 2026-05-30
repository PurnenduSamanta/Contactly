package com.purnendu.contactly.di

import com.purnendu.contactly.MainActivityViewModel
import com.purnendu.contactly.alarm.ContactlyAlarmManager
import com.purnendu.contactly.data.repository.ContactsRepositoryImpl
import com.purnendu.contactly.data.repository.ActivationsRepositoryImpl
import com.purnendu.contactly.data.local.preferences.AppPreferencesImpl
import com.purnendu.contactly.data.local.room.AppDatabase
import com.purnendu.contactly.data.utils.AndroidPermissionChecker
import com.purnendu.contactly.data.utils.DeviceTimeValidationRepository
import com.purnendu.contactly.data.utils.ImageStorageManager
import com.purnendu.contactly.domain.repository.ActivationsRepository as ActivationsRepositoryContract
import com.purnendu.contactly.domain.repository.AlarmSchedulerRepository
import com.purnendu.contactly.domain.repository.AppPreferences
import com.purnendu.contactly.domain.repository.ContactsRepository as ContactsRepositoryContract
import com.purnendu.contactly.domain.repository.GeofenceRepository
import com.purnendu.contactly.domain.repository.ImageStorageRepository
import com.purnendu.contactly.domain.repository.LocationParserRepository
import com.purnendu.contactly.domain.repository.NotificationPermissionRepository
import com.purnendu.contactly.domain.repository.TimeValidationRepository
import com.purnendu.contactly.domain.usecase.CheckBackgroundLocationPermissionUseCase
import com.purnendu.contactly.domain.usecase.CheckNotificationPermissionUseCase
import com.purnendu.contactly.domain.usecase.CreateActivationUseCase
import com.purnendu.contactly.domain.usecase.DeleteActivationUseCase
import com.purnendu.contactly.domain.usecase.ExtractSharedLocationLabelUseCase
import com.purnendu.contactly.domain.usecase.FetchContactsUseCase
import com.purnendu.contactly.domain.usecase.GetActivationsUseCase
import com.purnendu.contactly.domain.usecase.LoadAlarmStatusUseCase
import com.purnendu.contactly.domain.usecase.ManageAppPreferencesUseCase
import com.purnendu.contactly.domain.usecase.ParseSharedLocationUseCase
import com.purnendu.contactly.domain.usecase.SyncAlarmsUseCase
import com.purnendu.contactly.domain.usecase.ToggleInstantActivationUseCase
import com.purnendu.contactly.domain.usecase.UpdateActivationUseCase
import com.purnendu.contactly.domain.usecase.ValidateDeviceTimeUseCase
import com.purnendu.contactly.geofence.ContactlyGeofenceManager
import com.purnendu.contactly.common.PermissionChecker
import com.purnendu.contactly.geofence.GeofenceBroadcastReceiver
import com.purnendu.contactly.geofence.GoogleMapsLocationParserRepository
import com.purnendu.contactly.notification.AndroidNotificationPermissionRepository
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
    single { ContactsRepositoryImpl(androidContext().contentResolver) }
    single<ContactsRepositoryContract> { get<ContactsRepositoryImpl>() }

    single { ActivationsRepositoryImpl(get(), get()) }
    single<ActivationsRepositoryContract> { get<ActivationsRepositoryImpl>() }
    
    // ========== Preferences ==========
    // Interface → Implementation binding
    single<AppPreferences> { AppPreferencesImpl(androidContext()) }
    
    // ========== Abstractions ==========
    // These interfaces hide Android Context from ViewModels, improving testability
    single<PermissionChecker> { AndroidPermissionChecker(androidContext()) }
    
    // ========== Managers ==========
    // ContactlyAlarmManager handles all alarm-related operations
    single { ContactlyAlarmManager(androidContext(), get(), get()) }
    single<AlarmSchedulerRepository> { get<ContactlyAlarmManager>() }

    single { ImageStorageManager(androidContext()) }
    single<ImageStorageRepository> { get<ImageStorageManager>() }

    single { ContactlyGeofenceManager(androidContext(), get(), GeofenceBroadcastReceiver::class.java) }
    single<GeofenceRepository> { get<ContactlyGeofenceManager>() }
    single<TimeValidationRepository> { DeviceTimeValidationRepository(androidContext()) }
    single<LocationParserRepository> { GoogleMapsLocationParserRepository(androidContext()) }
    single<NotificationPermissionRepository> { AndroidNotificationPermissionRepository(androidContext()) }

    // ========== Use Cases ==========
    factory { GetActivationsUseCase(get()) }
    factory { CreateActivationUseCase(get(), get(), get(), get(), get()) }
    factory { UpdateActivationUseCase(get(), get(), get(), get(), get()) }
    factory { DeleteActivationUseCase(get(), get(), get(), get(), get()) }
    factory { ToggleInstantActivationUseCase(get(), get()) }
    factory { SyncAlarmsUseCase(get(), get()) }
    factory { FetchContactsUseCase(get()) }
    factory { ValidateDeviceTimeUseCase(get()) }
    factory { CheckBackgroundLocationPermissionUseCase(get()) }
    factory { ParseSharedLocationUseCase(get()) }
    factory { ExtractSharedLocationLabelUseCase(get()) }
    factory { CheckNotificationPermissionUseCase(get()) }
    factory { LoadAlarmStatusUseCase(get(), get()) }
    factory { ManageAppPreferencesUseCase(get()) }
    
    // ========== ViewModels ==========
    // ViewModels now depend on interfaces, not Android classes
    viewModel { MainActivityViewModel(get(), get()) }
}
