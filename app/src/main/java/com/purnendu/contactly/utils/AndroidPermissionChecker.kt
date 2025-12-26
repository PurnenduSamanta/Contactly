package com.purnendu.contactly.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Android implementation of PermissionChecker.
 * This class encapsulates all Android-specific permission checks.
 * 
 * In tests, you can replace this with a mock implementation of PermissionChecker.
 */
class AndroidPermissionChecker(private val context: Context) : PermissionChecker {
    
    override fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun hasWriteContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun canScheduleExactAlarms(): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= 31) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Exact alarms permission not required on older Android versions
        }
    }
}
