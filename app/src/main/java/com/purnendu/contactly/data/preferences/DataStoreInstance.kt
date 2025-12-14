package com.purnendu.contactly.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance to be shared across all preferences
 * This prevents multiple DataStore instances for the same file
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
