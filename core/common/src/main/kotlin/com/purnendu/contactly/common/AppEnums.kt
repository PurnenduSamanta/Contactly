package com.purnendu.contactly.common

enum class ViewMode {
    LIST,
    GRID
}

enum class ActivationMode {
    ONE_TIME,    // Activate once for specific date/time
    REPEAT,      // Repeat every week on selected days
    INSTANT,     // Manual instant toggle (no alarms)
    NEARBY;      // Location-based geofence trigger

    companion object {
        fun toInt(type: ActivationMode): Int = when (type) {
            ONE_TIME -> 0
            REPEAT -> 1
            INSTANT -> 2
            NEARBY -> 3
        }

        fun fromInt(value: Int): ActivationMode = when (value) {
            0 -> ONE_TIME
            1 -> REPEAT
            2 -> INSTANT
            3 -> NEARBY
            else -> ONE_TIME
        }
    }
}

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
