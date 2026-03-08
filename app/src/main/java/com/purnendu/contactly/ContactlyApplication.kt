package com.purnendu.contactly

import android.app.Application
import com.purnendu.contactly.di.appModule
import com.purnendu.contactly.feature.home.di.homeModule
import com.purnendu.contactly.feature.settings.di.settingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Application class that initializes Koin dependency injection.
 * 
 * This is the entry point for setting up all app-wide dependencies.
 */
class ContactlyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin DI
        startKoin {
            // Use Android logger for Koin
            androidLogger()
            
            // Provide Android context
            androidContext(this@ContactlyApplication)
            
            // Load all modules
            modules(appModule, homeModule, settingsModule)
        }
    }
}
