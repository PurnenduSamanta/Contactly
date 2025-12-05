package com.purnendu.contactly.utils

import java.util.Calendar

/**
 * Utility functions for working with day selection bitmasks
 * and calculating recurring schedule occurrences
 */
object DayUtils {
    
    /**
     * Convert bitmask to list of days (0=Sunday, 1=Monday, ..., 6=Saturday)
     */
    fun extractDaysFromBitmask(bitmask: Int): List<Int> {
        return (0..6).filter { (bitmask and (1 shl it)) != 0 }
    }
    
    /**
     * Convert list of days to bitmask
     */
    fun daysToBitmask(days: Set<Int>): Int {
        return days.fold(0) { acc, day -> acc or (1 shl day) }
    }
    
    /**
     * Get short day names (S, M, T, W, T, F, S)
     */
    fun getShortDayNames(): List<String> {
        return listOf("S", "M", "T", "W", "T", "F", "S")
    }

    /**
     * Get full day names
     */
    fun getFullDayNames(): List<String> {
        return listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    }
    
    /**
     * Calculate next occurrence of a specific day of week
     * @param timeMillis The time of day to schedule (hour:minute)
     * @param dayOfWeek 0=Sunday, 1=Monday, ..., 6=Saturday
     * @return Next occurrence timestamp in milliseconds
     */
    fun calculateNextOccurrence(timeMillis: Long, dayOfWeek: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Convert to 0=Sunday
        val targetDay = dayOfWeek
        
        var daysToAdd = (targetDay - currentDay + 7) % 7
        
        // If it's the same day but the time has passed, schedule for next week
        if (daysToAdd == 0 && calendar.timeInMillis < System.currentTimeMillis()) {
            daysToAdd = 7
        }
        
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return calendar.timeInMillis
    }
    
    /**
     * Format selected days as readable string (e.g., "Mon, Wed, Fri" or "Every day")
     */
    fun formatSelectedDays(bitmask: Int): String {
        if (bitmask == 127) return "Every day"
        
        val days = extractDaysFromBitmask(bitmask)
        if (days.isEmpty()) return "No days selected"
        
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return days.joinToString(", ") { dayNames[it] }
    }
}
