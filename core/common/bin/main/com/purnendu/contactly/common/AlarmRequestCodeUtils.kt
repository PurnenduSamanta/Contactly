package com.purnendu.contactly.common

/**
 * Centralized utility for generating unique alarm request codes.
 * 
 * This ensures consistency across all alarm activation operations
 * throughout the application (HomeViewModel, ContactlyAlarmManager, 
 * ReActivationReceiver, AliasAlarmReceiver).
 * 
 * Request code scheme:
 * - Base: (contactId % 1,000,000) * 100
 * - Apply alarm: base + (dayOfWeek * 2)
 * - Revert alarm: base + (dayOfWeek * 2) + 1
 * 
 * This allows for:
 * - Up to 7 days * 2 operations = 14 alarms per contact
 * - Unique codes for each day and operation combination
 * - Deterministic codes for easy cancellation/updating
 */
object AlarmRequestCodeUtils {
    
    /**
     * Types of alarm operations
     */
    object AlarmType {
        const val APPLY = 0   // Apply temporary name
        const val REVERT = 1  // Revert to original name
    }
    
    /**
     * Generate a unique request code for an alarm.
     * 
     * @param contactId The contact ID
     * @param dayOfWeek Day of week (0=Sunday, 1=Monday, ..., 6=Saturday)
     * @param alarmType Type of alarm (use AlarmType.APPLY or AlarmType.REVERT)
     * @return Unique request code for the PendingIntent
     */
    fun generateRequestCode(
        contactId: Long,
        dayOfWeek: Int,
        alarmType: Int
    ): Int {
        require(dayOfWeek in 0..6) { "dayOfWeek must be between 0 (Sunday) and 6 (Saturday)" }
        require(alarmType in 0..1) { "alarmType must be 0 (APPLY) or 1 (REVERT)" }
        
        val baseReqCode = (contactId % 1000000).toInt() * 100
        return baseReqCode + (dayOfWeek * 2) + alarmType
    }
    
    /**
     * Generate request code for APPLY operation (temporary name).
     * 
     * @param contactId The contact ID
     * @param dayOfWeek Day of week (0=Sunday, 1=Monday, ..., 6=Saturday)
     * @return Unique request code for the apply alarm
     */
    fun generateApplyRequestCode(contactId: Long, dayOfWeek: Int): Int {
        return generateRequestCode(contactId, dayOfWeek, AlarmType.APPLY)
    }
    
    /**
     * Generate request code for REVERT operation (original name).
     * 
     * @param contactId The contact ID
     * @param dayOfWeek Day of week (0=Sunday, 1=Monday, ..., 6=Saturday)
     * @return Unique request code for the revert alarm
     */
    fun generateRevertRequestCode(contactId: Long, dayOfWeek: Int): Int {
        return generateRequestCode(contactId, dayOfWeek, AlarmType.REVERT)
    }
}
