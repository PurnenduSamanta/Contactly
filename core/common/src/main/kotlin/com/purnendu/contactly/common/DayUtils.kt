package com.purnendu.contactly.common

import java.util.Calendar

/**
 * Utility functions for working with day selection bitmasks
 * and calculating recurring activation occurrences
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
     * Calculate next occurrence for a start/end time pair, ensuring they stay consistent.
     * If the start time has passed for the target day, BOTH start and end times are moved
     * to the following week. This prevents the scenario where start is next week but end is today.
     * 
     * IMPORTANT: This function always uses TODAY's date as the base for calculations.
     * Only the hour:minute from the input times are used - the date portion is ignored.
     * This ensures correct behavior when updating activations with stored future dates.
     * 
     * @param startTimeMillis The start time (only hour:minute are used, date is ignored)
     * @param endTimeMillis The end time (only hour:minute are used, date is ignored)
     * @param dayOfWeek 0=Sunday, 1=Monday, ..., 6=Saturday
     * @return Pair of (nextStartMillis, nextEndMillis) - both guaranteed to be on the same day
     */
    fun calculateNextOccurrencePair(startTimeMillis: Long, endTimeMillis: Long, dayOfWeek: Int): Pair<Long, Long> {
        // Extract only hour:minute from input times
        val inputStartCal = Calendar.getInstance().apply { timeInMillis = startTimeMillis }
        val inputEndCal = Calendar.getInstance().apply { timeInMillis = endTimeMillis }
        
        val startHour = inputStartCal.get(Calendar.HOUR_OF_DAY)
        val startMinute = inputStartCal.get(Calendar.MINUTE)
        val endHour = inputEndCal.get(Calendar.HOUR_OF_DAY)
        val endMinute = inputEndCal.get(Calendar.MINUTE)
        
        // Create calendars based on TODAY's date with the extracted times
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Use TODAY's day for the calculation
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 // Convert to 0=Sunday
        val targetDay = dayOfWeek
        
        var daysToAdd = (targetDay - currentDay + 7) % 7
        
        // Calculate what the start time would be after adding days
        val projectedStartCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, daysToAdd)
        }
        
        // If it's the same day (daysToAdd == 0) and START time has passed, move BOTH to next week
        if (daysToAdd == 0 && projectedStartCalendar.timeInMillis < System.currentTimeMillis()) {
            daysToAdd = 7
        }
        
        // Apply the same daysToAdd to BOTH start and end times
        startCalendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        endCalendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        
        return Pair(startCalendar.timeInMillis, endCalendar.timeInMillis)
    }
}
