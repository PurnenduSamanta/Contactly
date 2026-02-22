package com.purnendu.contactly.utils

enum class ViewMode {
    LIST,
    GRID
}

enum class ScheduleType {
    ONE_TIME,    // Schedule once for specific date/time
    REPEAT,      // Repeat every week on selected days
    INSTANT;     // Manual instant toggle (no alarms)

    companion object {
        fun toInt(type: ScheduleType): Int = when (type) {
            ONE_TIME -> 0
            REPEAT -> 1
            INSTANT -> 2
        }

        fun fromInt(value: Int): ScheduleType = when (value) {
            0 -> ONE_TIME
            1 -> REPEAT
            2 -> INSTANT
            else -> ONE_TIME
        }
    }
}

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
